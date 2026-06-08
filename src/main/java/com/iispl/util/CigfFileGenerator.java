package com.iispl.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import com.iispl.dto.xml.cigf.CigfBatchInfoXml;
import com.iispl.dto.xml.cigf.CigfImageListXml;
import com.iispl.dto.xml.cigf.CigfImageXml;
import com.iispl.dto.xml.cigf.CigfRootXml;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheque;

/**
 * File    : com/iispl/util/CigfFileGenerator.java
 * Purpose : Generates the NPCI CTS CIGF (Cheque Image Group File) XML
 *           manifest for one outward batch and writes it to disk.
 *
 * The CIGF lists every image file in the batch's exported package with:
 *   - sequence number
 *   - cheque number it belongs to
 *   - which side (FRONT or BACK)
 *   - file name in the package
 *   - file size in bytes
 *   - SHA-256 checksum
 *
 * NPCI uses the CIGF to verify the image package on receive.
 *
 * Only CHECKER_PASSED cheques have their images listed.
 * If a cheque's image path is missing on disk, that entry is SKIPPED
 * (logged as warning) — the batch is not aborted.
 */
public class CigfFileGenerator {

    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Generates one CIGF file for the given batch and writes it to outputDir.
     *
     * @param batch      the outward batch entity
     * @param cheques    all cheques (only CHECKER_PASSED with valid images are listed)
     * @param outputDir  target directory (will be created if missing)
     * @return absolute path of the generated .cigf file
     * @throws Exception if marshalling or file write fails
     */
    public static String generate(OutwardBatch         batch,
                                   List<OutwardCheque> cheques,
                                   File                outputDir) throws Exception {

        if (batch == null) throw new IllegalArgumentException("batch is null");
        if (cheques == null || cheques.isEmpty()) {
            throw new IllegalArgumentException("cheques list is empty");
        }

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new RuntimeException("Failed to create output dir: " + outputDir);
        }

        // Step 1: build the image list (skip missing images)
        List<CigfImageXml> images   = new ArrayList<>();
        long               totalBytes = 0L;
        int                seq       = 1;

        for (OutwardCheque c : cheques) {
            if (!"CHECKER_PASSED".equalsIgnoreCase(c.getStatus())) continue;

            CigfImageXml front = buildImageEntry(seq, c.getChequeNo(),
                    "FRONT", c.getFrontImagePath());
            if (front != null) {
                images.add(front);
                totalBytes += front.getFileSize();
                seq++;
            }

            CigfImageXml back = buildImageEntry(seq, c.getChequeNo(),
                    "BACK", c.getBackImagePath());
            if (back != null) {
                images.add(back);
                totalBytes += back.getFileSize();
                seq++;
            }
        }

        if (images.isEmpty()) {
            throw new RuntimeException(
                "Batch " + batch.getBatchId()
                + " has no image files to manifest in CIGF.");
        }

        System.out.println("CigfFileGenerator → Building CIGF for batch=" + batch.getBatchId()
                + " | images=" + images.size()
                + " | totalBytes=" + totalBytes);

        // Step 2: assemble root DTO
        CigfRootXml root = new CigfRootXml();
        root.setVersion("1.0");

        CigfBatchInfoXml info = new CigfBatchInfoXml();
        info.setBatchId(batch.getBatchId());
        info.setImageCount(images.size());
        info.setTotalImageSize(totalBytes);
        info.setGeneratedAt(LocalDateTime.now().format(TIMESTAMP_FMT));
        root.setBatchInfo(info);

        CigfImageListXml listWrap = new CigfImageListXml();
        listWrap.setImages(images);
        root.setImageList(listWrap);

        // Step 3: marshal to file
        File outputFile = new File(outputDir, batch.getBatchId() + ".cigf");

        try {
            JAXBContext ctx        = JAXBContext.newInstance(CigfRootXml.class);
            Marshaller  marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING,         "UTF-8");
            marshaller.marshal(root, outputFile);
        } catch (JAXBException e) {
            throw new RuntimeException("CIGF marshalling failed: " + e.getMessage(), e);
        }

        System.out.println("CigfFileGenerator → Wrote CIGF file: " + outputFile.getAbsolutePath()
                + " (" + outputFile.length() + " bytes)");
        return outputFile.getAbsolutePath();
    }

    // ════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════

    /**
     * Builds one CigfImageXml entry, computing file size and SHA-256.
     * Returns null if the imagePath is blank or the file does not exist.
     */
    private static CigfImageXml buildImageEntry(int    seqNo,
                                                 String chequeNo,
                                                 String side,
                                                 String imagePath) {

        if (imagePath == null || imagePath.trim().isEmpty()) {
            System.out.println("CigfFileGenerator → Skipping " + side
                    + " image for cheque " + chequeNo + ": path is empty");
            return null;
        }

        File imgFile = new File(imagePath.trim());
        if (!imgFile.exists() || !imgFile.isFile()) {
            System.err.println("CigfFileGenerator → Skipping " + side
                    + " image for cheque " + chequeNo
                    + ": file not found at " + imgFile.getAbsolutePath());
            return null;
        }

        CigfImageXml img = new CigfImageXml();
        img.setSeqNo(seqNo);
        img.setChequeNo(chequeNo != null ? chequeNo : "");
        img.setSide(side);
        img.setFilePath(imgFile.getName());     // store only the filename
        img.setFileSize(imgFile.length());
        img.setChecksum(sha256OrEmpty(imgFile));
        return img;
    }

    /**
     * Returns the SHA-256 hex digest of a file, or "" if it fails.
     * Failure is non-fatal — the entry still exports without a checksum.
     */
    private static String sha256OrEmpty(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int    n;
            while ((n = fis.read(buf)) != -1) {
                digest.update(buf, 0, n);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            System.err.println("CigfFileGenerator → SHA-256 failed for "
                    + file.getName() + ": " + e.getMessage());
            return "";
        }
    }
}
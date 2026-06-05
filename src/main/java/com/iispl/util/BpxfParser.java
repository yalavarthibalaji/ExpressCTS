package com.iispl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.iispl.jaxb.BpxfRoot;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * File    : com/iispl/util/BpxfParser.java
 * Purpose : Handles ZIP extraction and JAXB-based XML parsing for inward flow.
 *
 * Responsibilities:
 *   1. Accept the uploaded ZIP file
 *   2. Extract its contents to /opt/cts/inward/images/<batchId>/
 *   3. Locate the XML file inside the ZIP
 *   4. Parse the XML using JAXB → returns BpxfRoot object
 */
public class BpxfParser {

    private static final String BASE_IMAGE_DIR = "";

    // ── Result class ──────────────────────────────────────────────────────

    public static class ParseResult {

        private final BpxfRoot bpxfRoot;
        private final String   extractedDirPath;

        public ParseResult(BpxfRoot bpxfRoot, String extractedDirPath) {
            this.bpxfRoot         = bpxfRoot;
            this.extractedDirPath = extractedDirPath;
        }

        public BpxfRoot getBpxfRoot()         { return bpxfRoot; }
        public String   getExtractedDirPath() { return extractedDirPath; }
    }

    // ── Main parse method ─────────────────────────────────────────────────

    /**
     * Extracts the ZIP and parses the XML file inside it.
     *
     * @param zipFile  The ZIP file (from manual upload or NIO watcher)
     * @param batchId  Used as the folder name under BASE_IMAGE_DIR
     * @return ParseResult containing BpxfRoot and extraction folder path
     */
    public static ParseResult parse(File zipFile, String batchId)
            throws IOException, JAXBException {

        // 1. Create extraction directory
        String extractedDirPath = BASE_IMAGE_DIR + batchId + "/";
        File   extractedDir     = new File(extractedDirPath);
        if (!extractedDir.exists()) {
            extractedDir.mkdirs();
            System.out.println("BpxfParser → Created directory: " + extractedDirPath);
        }

        // 2. Extract ZIP
        File xmlFile = extractZip(zipFile, extractedDir);
        if (xmlFile == null) {
            throw new IOException("No XML file found inside ZIP: " + zipFile.getName());
        }
        System.out.println("BpxfParser → XML found: " + xmlFile.getAbsolutePath());

        // 3. Parse XML via JAXB
        BpxfRoot bpxfRoot = parseXml(xmlFile);
        System.out.println("BpxfParser → Parsed successfully. Cheques: "
                + (bpxfRoot.getCheques() != null ? bpxfRoot.getCheques().size() : 0));

        return new ParseResult(bpxfRoot, extractedDirPath);
    }

    // ── ZIP extraction ────────────────────────────────────────────────────

    private static File extractZip(File zipFile, File targetDir)
            throws IOException {

        File xmlFile = null;

        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry    = entries.nextElement();
                File     destFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    destFile.mkdirs();
                    continue;
                }

                destFile.getParentFile().mkdirs();

                try (InputStream     in  = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(destFile)) {
                    byte[] buf = new byte[4096];
                    int    n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }

                System.out.println("BpxfParser → Extracted: " + destFile.getPath());

                if (entry.getName().toLowerCase().endsWith(".xml")) {
                    xmlFile = destFile;
                }
            }
        }

        return xmlFile;
    }

    // ── JAXB XML parse ────────────────────────────────────────────────────

    private static BpxfRoot parseXml(File xmlFile) throws JAXBException {
        JAXBContext  ctx = JAXBContext.newInstance(BpxfRoot.class);
        Unmarshaller um  = ctx.createUnmarshaller();
        return (BpxfRoot) um.unmarshal(xmlFile);
    }

    // ── Image path builder ────────────────────────────────────────────────

    /**
     * Resolves relative image path from XML to absolute disk path.
     *
     * Example:
     *   batchId      = "Bpxf_Batch_1"
     *   relativePath = "/cheques/cheque001(front)"
     *   result       = "/opt/cts/inward/images/Bpxf_Batch_1/cheques/cheque001(front).png"
     */
    public static String buildImagePath(String batchId, String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) return null;
        return BASE_IMAGE_DIR + batchId + "/"
                + relativePath.replaceFirst("^/", "").trim() + ".png";
    }
}
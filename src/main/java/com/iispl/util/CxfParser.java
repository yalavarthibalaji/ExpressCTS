package com.iispl.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.iispl.dto.xml.BatchXml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * File    : com/iispl/util/CxfParser.java
 * Purpose : Handles ZIP extraction and JAXB-based XML parsing.
 *
 * Responsibilities:
 *   1. Accept the uploaded ZIP file
 *   2. Extract its contents to  /opt/cts/images/outward/<batchId>/
 *   3. Locate the XML file inside the ZIP
 *   4. Parse the XML using JAXB → returns BatchXml object
 *
 * Usage:
 *   CxfParser.ParseResult result = CxfParser.parse(zipFile, batchId);
 *   BatchXml batchXml           = result.getBatchXml();
 *   String   extractedDir       = result.getExtractedDirPath();
 */
public class CxfParser {

    /**
     * Base directory where images and XML are stored after extraction.
     * Change this to "C:/cts/images" if running on Windows.
     */
    private static final String BASE_IMAGE_DIR = "/opt/cts/images/outward/";

    // ── Public Result Class ──────────────────────────────────────────────────

    /**
     * Holds the result of a successful parse.
     * BatchXml contains all cheque data.
     * extractedDirPath is the folder where images were extracted.
     */
    public static class ParseResult {

        private final BatchXml batchXml;
        private final String   extractedDirPath;

        public ParseResult(BatchXml batchXml, String extractedDirPath) {
            this.batchXml         = batchXml;
            this.extractedDirPath = extractedDirPath;
        }

        public BatchXml getBatchXml() {
            return batchXml;
        }

        public String getExtractedDirPath() {
            return extractedDirPath;
        }
    }

    // ── Main Parse Method ────────────────────────────────────────────────────

    /**
     * Extracts the ZIP and parses the XML file inside it.
     *
     * @param zipFile  The uploaded ZIP file (e.g. from ZK media upload)
     * @param batchId  The generated batch ID (e.g. B-2026-0603-001)
     *                 Used as the folder name for extracted files
     * @return ParseResult containing BatchXml and the extraction folder path
     * @throws IOException   if ZIP extraction fails
     * @throws JAXBException if XML parsing fails
     */
    public static ParseResult parse(File zipFile, String batchId)
            throws IOException, JAXBException {

        // ── Step 1: Create extraction directory ──
        String extractedDirPath = BASE_IMAGE_DIR + batchId + "/";
        File extractedDir = new File(extractedDirPath);
        if (!extractedDir.exists()) {
            extractedDir.mkdirs();
            System.out.println("CxfParser → Created directory: " + extractedDirPath);
        }

        // ── Step 2: Extract ZIP contents ──
        File xmlFile = extractZip(zipFile, extractedDir);

        if (xmlFile == null) {
            throw new IOException("No XML file found inside the ZIP. "
                    + "Expected one .xml file at the root of the ZIP.");
        }

        System.out.println("CxfParser → XML file found: " + xmlFile.getAbsolutePath());

        // ── Step 3: Parse XML using JAXB ──
        BatchXml batchXml = parseXml(xmlFile);

        System.out.println("CxfParser → Parsed successfully. "
                + "Total cheques in XML: "
                + (batchXml.getCheques() != null
                    ? batchXml.getCheques().getChequeList().size()
                    : 0));

        return new ParseResult(batchXml, extractedDirPath);
    }

    // ── Private Helper: Extract ZIP ──────────────────────────────────────────

    /**
     * Extracts all entries from the ZIP into the target directory.
     * Returns the XML file found inside the ZIP, or null if not found.
     */
    private static File extractZip(File zipFile, File targetDir)
            throws IOException {

        File xmlFile = null;

        try (ZipFile zip = new ZipFile(zipFile)) {

            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // Build the destination file path
                File destFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    // Create subdirectory (e.g. images/)
                    destFile.mkdirs();
                    System.out.println("CxfParser → Created folder: " + destFile.getPath());
                    continue;
                }

                // Make sure parent folder exists
                destFile.getParentFile().mkdirs();

                // Write file contents
                try (InputStream in  = zip.getInputStream(entry);
                     FileOutputStream out = new FileOutputStream(destFile)) {

                    byte[] buffer = new byte[4096];
                    int    bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("CxfParser → Extracted: " + destFile.getPath());

                // Track the XML file
                if (entry.getName().toLowerCase().endsWith(".xml")) {
                    xmlFile = destFile;
                }
            }
        }

        return xmlFile;
    }

    // ── Private Helper: Parse XML with JAXB ─────────────────────────────────

    /**
     * Uses JAXB Unmarshaller to convert the XML file into a BatchXml object.
     * This is the core of JAXB — one method call reads the entire XML.
     */
    private static BatchXml parseXml(File xmlFile) throws JAXBException {

        // Create JAXB context for our root class
        JAXBContext context = JAXBContext.newInstance(BatchXml.class);

        // Create unmarshaller (XML → Java object converter)
        Unmarshaller unmarshaller = context.createUnmarshaller();

        // Parse and cast
        BatchXml batchXml = (BatchXml) unmarshaller.unmarshal(xmlFile);

        return batchXml;
    }

    // ── Public Helper: Build Full Image Path ─────────────────────────────────

    /**
     * Builds the full server path for a cheque image.
     * Used by BatchUploadServiceImpl when saving to outward_cheque table.
     *
     * @param batchId      e.g. B-2026-0603-001
     * @param relativePath e.g. images/CHQ001_FRONT.jpg (from XML)
     * @return full path   e.g. /opt/cts/images/outward/B-2026-0603-001/images/CHQ001_FRONT.jpg
     */
    public static String buildImagePath(String batchId, String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) return null;
        return BASE_IMAGE_DIR + batchId + "/" + relativePath.trim();
    }
}
package com.iispl.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * File    : com/iispl/util/JasperPdfGenerator.java
 * Purpose : Loads a .jrxml template from the classpath, fills it with
 *           a list of beans, and exports the result to a PDF byte[].
 *
 * Templates location:  src/main/resources/reports/templates/*.jrxml
 *
 * Compiled JasperReports are cached in memory so the JRXML is parsed
 * + compiled only once per server startup.
 */
public class JasperPdfGenerator {

    /** In-memory cache: templatePath → compiled JasperReport */
    private static final Map<String, JasperReport> COMPILE_CACHE = new ConcurrentHashMap<>();

    /**
     * Generates a PDF byte[] for a given Jasper template + data list.
     *
     * @param templatePath  classpath path, e.g. "/reports/templates/dailySummary.jrxml"
     * @param dataList      list of DTO beans matching the JRXML field declarations
     * @param params        optional template parameters (title, from/to date, etc.); may be null
     * @return PDF as byte[]
     * @throws Exception on compile/fill/export failure
     */
    public static byte[] generatePdf(String       templatePath,
                                      List<?>      dataList,
                                      Map<String, Object> params) throws Exception {

        if (templatePath == null || templatePath.trim().isEmpty()) {
            throw new IllegalArgumentException("templatePath is required");
        }

        JasperReport report = COMPILE_CACHE.get(templatePath);
        if (report == null) {
            report = compileTemplate(templatePath);
            COMPILE_CACHE.put(templatePath, report);
            System.out.println("JasperPdfGenerator → compiled and cached: " + templatePath);
        }

        // Jasper requires a non-null data source; supply an empty list if needed
        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(
                dataList != null ? dataList : new java.util.ArrayList<>());

        Map<String, Object> safeParams = params != null
                ? params : new HashMap<String, Object>();

        JasperPrint jasperPrint = JasperFillManager.fillReport(report, safeParams, ds);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JasperExportManager.exportReportToPdfStream(jasperPrint, baos);

        byte[] pdf = baos.toByteArray();
        System.out.println("JasperPdfGenerator → generated PDF, "
                + pdf.length + " bytes from " + templatePath
                + " (records=" + (dataList != null ? dataList.size() : 0) + ")");
        return pdf;
    }

    /** Loads the JRXML from classpath and compiles it. */
    private static JasperReport compileTemplate(String classpathPath) throws Exception {
        InputStream in = JasperPdfGenerator.class.getResourceAsStream(classpathPath);
        if (in == null) {
            throw new RuntimeException("Jasper template not found on classpath: "
                    + classpathPath + "  (place .jrxml under src/main/resources)");
        }
        try {
            return JasperCompileManager.compileReport(in);
        } finally {
            try { in.close(); } catch (Exception ignored) {}
        }
    }

    /** For dev/testing only — clears the compile cache. */
    public static void clearCache() {
        COMPILE_CACHE.clear();
    }
}
package com.iispl.test;

import java.io.File;

import com.iispl.db.HibernateUtil;
import com.iispl.service.OutwardUploadService;
import com.iispl.serviceImpl.OutwardUploadServiceImpl;

public class TestXmlUpload {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("  CTS - XML Upload Test Starting...    ");
        System.out.println("========================================");

        try {

            // ── Step 1: Point to your ZIP file location ───────────────
            // Place your ZIP file in the project root folder
            // ZIP should contain:
            //   batch_data.xml
            //   images/front/100833_front.jpg
            //   images/back/100833_back.jpg

            String zipFilePath = "test-data/outward_batch.zip";
            File zipFile = new File(zipFilePath);

            // check if file exists before proceeding
            if (!zipFile.exists()) {
                System.err.println("ZIP file not found at: " + zipFilePath);
                System.err.println("Please place your ZIP file at: " + zipFilePath);
                return;
            }

            System.out.println("ZIP file found: " + zipFile.getAbsolutePath());

            // ── Step 2: Call the service ──────────────────────────────
            // operator01 is the logged in user for testing
            OutwardUploadService uploadService = new OutwardUploadServiceImpl();
            uploadService.processUpload(zipFile, "operator01");

            System.out.println("========================================");
            System.out.println("  Test Completed Successfully!         ");
            System.out.println("  Check Supabase tables:               ");
            System.out.println("  → outward_batches                    ");
            System.out.println("  → outward_cheque_staging             ");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("  Test Failed!                         ");
            System.err.println("  Error: " + e.getMessage());
            System.err.println("========================================");
            e.printStackTrace();

        } finally {
            // always close Hibernate session factory after test
            HibernateUtil.shutdown();
            System.out.println("Hibernate shutdown complete.");
        }
    }
}
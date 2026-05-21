package com.iispl.serviceImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.iispl.dao.OutwardBatchDao;
import com.iispl.dao.OutwardChequeStagingDao;
import com.iispl.daoImpl.OutwardBatchDaoImpl;
import com.iispl.daoImpl.OutwardChequeStagingDaoImpl;
import com.iispl.entity.OutwardBatch;
import com.iispl.entity.OutwardChequeStaging;
import com.iispl.service.OutwardUploadService;
import com.iispl.xml.BatchHeaderXml;
import com.iispl.xml.ChequeXml;
import com.iispl.xml.OutwardBatchXml;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

public class OutwardUploadServiceImpl implements OutwardUploadService {

	private OutwardBatchDao batchDao = new OutwardBatchDaoImpl();
	private OutwardChequeStagingDao stagingDao = new OutwardChequeStagingDaoImpl();

	// folder where we extract the ZIP contents temporarily
	private static final String EXTRACT_BASE_PATH = "uploads/outward/";

	@Override
	public void processUpload(File zipFile, String uploadedBy) {
		System.out.println("Upload started by: " + uploadedBy);
		System.out.println("ZIP file: " + zipFile.getName());

		// Step 1 - extract ZIP to a temp folder
		String extractedFolderPath = extractZip(zipFile);
		System.out.println("ZIP extracted to: " + extractedFolderPath);

		// Step 2 - find the XML file inside extracted folder
		File xmlFile = findXmlFile(extractedFolderPath);
		if (xmlFile == null) {
			throw new RuntimeException("No XML file found inside the ZIP.");
		}
		System.out.println("XML file found: " + xmlFile.getName());

		// Step 3 - parse XML using JAXB
		OutwardBatchXml batchXml = parseXml(xmlFile);
		System.out.println("XML parsed successfully.");

		// Step 4 - read batch header from XML
		BatchHeaderXml header = batchXml.getBatchHeader();

		// Step 5 - generate batch id
		String batchId = generateBatchId(header.getBranchCode(), header.getClearingDate());
		System.out.println("Generated BatchId: " + batchId);

		// Step 6 - count iqa pass and fail from cheque list
		List<ChequeXml> chequeList = batchXml.getCheques().getChequeList();
		int iqaPass = countIqaPass(chequeList);
		int iqaFail = chequeList.size() - iqaPass;

		// Step 7 - check if any cheque has micr repair needed
		boolean hasMicrRepair = checkMicrRepair(chequeList);

		// Step 8 - build OutwardBatch entity and save to DB
		OutwardBatch batch = buildBatchEntity(batchId, header, zipFile.getName(), xmlFile.getName(), uploadedBy,
				iqaPass, iqaFail, hasMicrRepair);
		batchDao.saveBatch(batch);
		System.out.println("Batch saved to DB.");

		// Step 9 - loop through each cheque and save to staging
		for (ChequeXml chequeXml : chequeList) {
			OutwardChequeStaging staging = buildStagingEntity(chequeXml, batchId, zipFile.getName(), uploadedBy,
					extractedFolderPath);
			stagingDao.saveStagingCheque(staging);
		}

		System.out.println("All cheques saved to staging. Total: " + chequeList.size());
		System.out.println("Upload process completed for batch: " + batchId);
	}

	// ── Step 1: Extract ZIP ───────────────────────────────────────────

	private String extractZip(File zipFile) {
		// create a folder named after the zip file (without .zip)
		String folderName = zipFile.getName().replace(".zip", "");
		String extractPath = EXTRACT_BASE_PATH + folderName + "/";

		File extractDir = new File(extractPath);
		if (!extractDir.exists()) {
			extractDir.mkdirs();
		}

		try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
			ZipEntry entry;
			while ((entry = zipIn.getNextEntry()) != null) {
				String filePath = extractPath + entry.getName();

				if (entry.isDirectory()) {
					// create subfolder
					new File(filePath).mkdirs();
				} else {
					// create parent folders if needed
					new File(filePath).getParentFile().mkdirs();

					// write file content
					try (FileOutputStream fos = new FileOutputStream(filePath)) {
						byte[] buffer = new byte[1024];
						int length;
						while ((length = zipIn.read(buffer)) > 0) {
							fos.write(buffer, 0, length);
						}
					}
				}
				zipIn.closeEntry();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to extract ZIP: " + e.getMessage());
		}

		return extractPath;
	}

	// ── Step 2: Find XML file inside extracted folder ─────────────────

	private File findXmlFile(String folderPath) {
		File folder = new File(folderPath);
		return searchXmlRecursively(folder);
	}

	// searches XML in folder and all subfolders recursively
	private File searchXmlRecursively(File folder) {
		File[] files = folder.listFiles();

		if (files != null) {
			for (File file : files) {
				if (file.isFile() && file.getName().endsWith(".xml")) {
					// found it
					return file;
				} else if (file.isDirectory()) {
					// go inside subfolder and search
					File found = searchXmlRecursively(file);
					if (found != null) {
						return found;
					}
				}
			}
		}
		return null;
	}

	// ── Step 3: Parse XML using JAXB ──────────────────────────────────

	private OutwardBatchXml parseXml(File xmlFile) {
		try {
			// tell JAXB which class is the root
			JAXBContext context = JAXBContext.newInstance(OutwardBatchXml.class);
			Unmarshaller unmarshaller = context.createUnmarshaller();

			// unmarshal = read XML and fill Java object
			return (OutwardBatchXml) unmarshaller.unmarshal(xmlFile);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to parse XML: " + e.getMessage());
		}
	}

	// ── Step 5: Generate Batch ID ──────────────────────────────────────

	private String generateBatchId(String branchCode, String clearingDate) {
		// format: BATCH-BR001-20250520-001

		// get today's date in YYYYMMDD format
		String dateFormatted = clearingDate.replace("-", "");

		// count how many batches already exist today for this branch
		int existingCount = batchDao.countBatchesTodayForBranch(branchCode, clearingDate);

		// sequence = existing count + 1, padded to 3 digits
		String sequence = String.format("%03d", existingCount + 1);

		return "BATCH-" + branchCode + "-" + dateFormatted + "-" + sequence;
	}

	// ── Step 6: Count IQA Pass ─────────────────────────────────────────

	private int countIqaPass(List<ChequeXml> chequeList) {
		int count = 0;
		for (ChequeXml cheque : chequeList) {
			if ("PASS".equalsIgnoreCase(cheque.getIqaStatus())) {
				count++;
			}
		}
		return count;
	}

	// ── Step 7: Check MICR Repair ──────────────────────────────────────

	private boolean checkMicrRepair(List<ChequeXml> chequeList) {
		for (ChequeXml cheque : chequeList) {
			if ("REPAIR_NEEDED".equalsIgnoreCase(cheque.getMicrStatus())) {
				return true;
			}
		}
		return false;
	}

	// ── Step 8: Build OutwardBatch Entity ──────────────────────────────

	private OutwardBatch buildBatchEntity(String batchId, BatchHeaderXml header, String zipFileName, String xmlFileName,
			String uploadedBy, int iqaPass, int iqaFail, boolean hasMicrRepair) {

		OutwardBatch batch = new OutwardBatch();

		batch.setBatchId(batchId);
		batch.setBranchCode(header.getBranchCode());
		batch.setClearingDate(LocalDate.parse(header.getClearingDate()));
		batch.setClearingSessionRef(header.getClearingSessionRef());
		batch.setRoute(header.getRoute());
		batch.setStatus("PENDING");
		batch.setIsMicrRepairBatch(hasMicrRepair);
		batch.setTotalCheques(header.getTotalCheques());
		batch.setTotalAmount(header.getTotalAmount());
		batch.setIqaPass(iqaPass);
		batch.setIqaFail(iqaFail);
		batch.setXmlFileName(xmlFileName);

		// store zip file name in cbx_file field
		// since ZIP is the container file for this batch
		batch.setCbxFile(zipFileName);

		// default flags - all false at upload time
		batch.setMakerDone(false);
		batch.setCheckerDone(false);
		batch.setCxfGenerated(false);
		batch.setDemSent(false);
		batch.setSupervisorVerified(false);

		batch.setCreatedBy(uploadedBy);
		batch.setScannedAt(LocalDateTime.now());

		return batch;
	}

	// ── Step 9: Build OutwardChequeStaging Entity ──────────────────────

	private OutwardChequeStaging buildStagingEntity(ChequeXml chequeXml, String batchId, String sourceFileName,
			String uploadedBy, String extractedFolderPath) {

		OutwardChequeStaging staging = new OutwardChequeStaging();

		staging.setBatchId(batchId);
		staging.setSourceFileName(sourceFileName);
		staging.setUploadedBy(uploadedBy);
		staging.setUploadedAt(LocalDateTime.now());

		staging.setChequeNumber(chequeXml.getChequeNumber());
		staging.setBankName(chequeXml.getBankName());
		staging.setBranchName(chequeXml.getBranchName());
		staging.setIfscCode(chequeXml.getIfscCode());
		staging.setMicrCode(chequeXml.getMicrCode());
		staging.setMicrStatus(chequeXml.getMicrStatus());

		// convert date strings to LocalDate
		staging.setChequeDate(LocalDate.parse(chequeXml.getChequeDate()));
		staging.setPresentationDate(LocalDate.parse(chequeXml.getPresentationDate()));

		staging.setDrawerName(chequeXml.getDrawerName());
		staging.setDrawerAccountNumber(chequeXml.getDrawerAccountNumber());
		staging.setPayeeName(chequeXml.getPayeeName());
		staging.setAmountInWords(chequeXml.getAmountInWords());
		staging.setAmountInFigures(chequeXml.getAmountInFigures());
		staging.setDepositorAccountNumber(chequeXml.getDepositorAccountNumber());

		// build full image paths using extracted folder + relative path from XML
		staging.setImageFrontPath(extractedFolderPath + chequeXml.getImageFrontPath());
		staging.setImageBackPath(extractedFolderPath + chequeXml.getImageBackPath());

		staging.setIqaStatus(chequeXml.getIqaStatus());

		// default staging status at upload time
		staging.setStagingStatus("PENDING");

		return staging;
	}

	// ── Helper: Calculate HV Category ─────────────────────────────────

	public static String calculateHvCategory(long amountInFigures) {
		// HV = High Value (1 lakh and above)
		// NV = Normal Value (below 1 lakh)
		if (amountInFigures >= 100000) {
			return "HV";
		}
		return "NV";
	}

}
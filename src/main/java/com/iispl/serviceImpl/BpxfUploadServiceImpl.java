package com.iispl.serviceImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.util.media.Media;

import com.iispl.dao.InwardBatchDao;
import com.iispl.dao.InwardChequeDao;
import com.iispl.daoImpl.InwardBatchDaoImpl;
import com.iispl.daoImpl.InwardChequeDaoImpl;
import com.iispl.dto.InwardBatchDto;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.jaxb.BpxfCheque;
import com.iispl.jaxb.BpxfRoot;
import com.iispl.service.BpxfUploadService;
import com.iispl.util.BpxfParser;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

public class BpxfUploadServiceImpl implements BpxfUploadService {

    private final InwardBatchDao  batchDao  = new InwardBatchDaoImpl();
    private final InwardChequeDao chequeDao = new InwardChequeDaoImpl();

    // ── Manual upload entry point ─────────────────────────────────────────
    @Override
    public void parseAndSave(Media file, String batchName, LoginDTO operator) {
        try {
            // 1. Write Media to temp file — BpxfParser needs a File object
            File tempZip = File.createTempFile("bpxf_", ".zip");
            try (InputStream in = file.isBinary()
                        ? file.getStreamData()
                        : new java.io.ByteArrayInputStream(
                              file.getStringData().getBytes(StandardCharsets.UTF_8));
                 FileOutputStream out = new FileOutputStream(tempZip)) {
                byte[] buf = new byte[4096];
                int    n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }

            // 2. Resolve batch ID
            String batchId = (batchName != null && !batchName.isBlank())
                    ? batchName
                    : file.getName().replace(".zip", "");

            // 3. Parse ZIP via BpxfParser
            BpxfParser.ParseResult result = BpxfParser.parse(tempZip, batchId);
            BpxfRoot root = result.getBpxfRoot();
            tempZip.delete();

            // 4. Resolve image paths
            if (root.getCheques() != null) {
                for (BpxfCheque bpxf : root.getCheques()) {
                    bpxf.setFrontImagePath(
                            BpxfParser.buildImagePath(batchId, bpxf.getFrontImagePath()));
                    bpxf.setBackImagePath(
                            BpxfParser.buildImagePath(batchId, bpxf.getBackImagePath()));
                }
            }

            // 5. Delegate to shared core logic
            String resolvedBatchId = (batchName != null && !batchName.isBlank())
                    ? batchName
                    : root.getHeader().getBatchId();

            parseAndSaveRoot(root, resolvedBatchId,
                    file.getName(), operator);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse/save BPXF file: " + e.getMessage(), e);
        }
    }

    // ── NIO folder watcher entry point ────────────────────────────────────
    @Override
    public void parseAndSaveRoot(BpxfRoot root, String fileName, LoginDTO operator) {
        parseAndSaveRoot(root, root.getHeader().getBatchId(), fileName, operator);
    }

    // ── Shared core logic ─────────────────────────────────────────────────
    private void parseAndSaveRoot(BpxfRoot root, String resolvedBatchId,
                                  String fileName, LoginDTO operator) {
        try {
            // 1. Build InwardBatch
            InwardBatch batch = new InwardBatch();
            batch.setBatchId(resolvedBatchId);
            batch.setBatchDate(LocalDate.parse(root.getHeader().getBatchDate()));
            batch.setSourceFileName(fileName);
            batch.setSourceFilePath("");
            batch.setStatus("RECEIVED");
            batch.setParsedAt(LocalDateTime.now());

            User user = new User();
            user.setId(operator.getUserId());
            batch.setReceivedBy(user);

            // 2. Map BpxfCheque → InwardCheque
            List<BpxfCheque>   parsed  = root.getCheques();
            List<InwardCheque> cheques = new ArrayList<>();
            int micrErrors = 0;
            int seq        = 1;

            for (BpxfCheque bpxf : parsed) {
                InwardCheque cheque = new InwardCheque();
                cheque.setSeqNo(seq++);
                cheque.setChequeNo(bpxf.getChequeNo());
                cheque.setChequeDate(bpxf.getChequeDate());
                cheque.setAmount(bpxf.getAmount());
                cheque.setChequeDateOcr(bpxf.getChequeDateOcr());
                cheque.setAmountOcr(bpxf.getAmountOcr());
                cheque.setAmountInWords(bpxf.getAmountInWords());
                cheque.setMicrCodeRaw(bpxf.getMicrCodeRaw());
                cheque.setMicrCodeCorrected(bpxf.getMicrCodeCorrected());
                cheque.setCityCode(bpxf.getCityCode());
                cheque.setBankCode(bpxf.getBankCode());
                cheque.setBranchCode(bpxf.getBranchCode());
                cheque.setDraweeAccountNumber(bpxf.getDraweeAccountNumber());
                cheque.setDraweeAccountHolder(bpxf.getDraweeAccountHolder());
                cheque.setAccountBalance(bpxf.getAccountBalance());
                cheque.setIsAccountValid(bpxf.getIsAccountValid());
                cheque.setIsBankMatched(bpxf.getIsBankMatched());
                cheque.setPayeeName(bpxf.getPayeeName());
                cheque.setIqaStatus(bpxf.getIqaStatus());
                cheque.setFrontImagePath(bpxf.getFrontImagePath()); // already resolved
                cheque.setBackImagePath(bpxf.getBackImagePath());   // already resolved
                cheque.setRemarks(bpxf.getRemarks());
                cheque.setPresentingBankCode(root.getHeader().getPresentingBankCode());
                cheque.setPresentingBankName(root.getHeader().getPresentingBankName());

                boolean hasMicrError = containsNonNumeric(bpxf.getMicrCodeRaw())
                        || containsNonNumeric(bpxf.getBranchCode())
                        || containsNonNumeric(bpxf.getBankCode())
                        || containsNonNumeric(bpxf.getCityCode());

                cheque.setMicrError(hasMicrError);
                cheque.setRepairStatus(hasMicrError ? "NEEDS_REPAIR" : "NOT_REQUIRED");
                if (hasMicrError) micrErrors++;

                cheque.setStatus("RECEIVED");
                cheque.setBatch(batch);
                cheques.add(cheque);
            }

            // 3. Set batch totals
            batch.setTotalCheques(cheques.size());
            batch.setTotalAmount(root.getHeader().getTotalAmount());
            batch.setMicrErrorCount(micrErrors);
            batch.setRepairStatus(micrErrors > 0 ? "NEEDS_REPAIR" : "NOT_REQUIRED");

            // 4. Save batch first
            batchDao.save(batch);

            // 5. Save cheques explicitly
            chequeDao.saveAll(cheques);

        } catch (Exception e) {
            throw new RuntimeException("Failed to build/save batch: " + e.getMessage(), e);
        }
    }

    @Override
    public List<InwardBatchDto> getAllBatches() {
        List<InwardBatch>    batches = batchDao.findAll();
        List<InwardBatchDto> dtos    = new ArrayList<>();
        for (InwardBatch b : batches) {
            dtos.add(new InwardBatchDto(
                    b.getBatchId(),
                    b.getSourceFileName(),
                    b.getTotalCheques(),
                    b.getMicrErrorCount(),
                    b.getParsedAt(),
                    b.getStatus()
            ));
        }
        return dtos;
    }

    private boolean containsNonNumeric(String value) {
        if (value == null || value.isBlank()) return false;
        return !value.matches("\\d+");
    }
}
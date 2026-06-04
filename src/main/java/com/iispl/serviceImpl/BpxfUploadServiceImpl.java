package com.iispl.serviceImpl;

import java.io.InputStream;
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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;

public class BpxfUploadServiceImpl implements BpxfUploadService {

    private final InwardBatchDao batchDao = new InwardBatchDaoImpl();
    private final InwardChequeDao chequeDao = new InwardChequeDaoImpl();

    @Override
    public void parseAndSave(Media file, String batchName, LoginDTO operator) {
        try {
            // 1. Parse XML → BpxfRoot (JAXB only touches BpxfCheque, never InwardCheque)
        	InputStream is;
        	if (file.isBinary()) {
        	    is = file.getStreamData();
        	} else {
        	    is = new java.io.ByteArrayInputStream(
        	             file.getStringData().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        	}           
        	JAXBContext  ctx = JAXBContext.newInstance(BpxfRoot.class); // ✅ only BpxfRoot
            Unmarshaller um  = ctx.createUnmarshaller();
            BpxfRoot root    = (BpxfRoot) um.unmarshal(is);

            // 2. Build InwardBatch from header
            InwardBatch batch = new InwardBatch();

            String resolvedBatchId = (batchName != null && !batchName.isBlank())
                    ? batchName
                    : root.getHeader().getBatchId();

            batch.setBatchId(resolvedBatchId);
            batch.setBatchDate(LocalDate.parse(root.getHeader().getBatchDate()));
            batch.setSourceFileName(file.getName());
            batch.setSourceFilePath("");
            batch.setStatus("RECEIVED");
            batch.setParsedAt(LocalDateTime.now());

            User user = new User();
            user.setId(operator.getUserId());
            batch.setReceivedBy(user);

            // 3. Map BpxfCheque → InwardCheque
            List<BpxfCheque>  parsed   = root.getCheques();
            List<InwardCheque> cheques = new ArrayList<>();
            int micrErrors = 0;
            int seq        = 1;

            for (BpxfCheque bpxf : parsed) {

                InwardCheque cheque = new InwardCheque();

                // ── Fields from XML ──
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
                cheque.setFrontImagePath(bpxf.getFrontImagePath());
                cheque.setBackImagePath(bpxf.getBackImagePath());
                cheque.setRemarks(bpxf.getRemarks());

                // ── Presenting bank from header ──
                cheque.setPresentingBankCode(root.getHeader().getPresentingBankCode());
                cheque.setPresentingBankName(root.getHeader().getPresentingBankName());

                // ── MICR error detection ──
                boolean hasMicrError = containsNonNumeric(bpxf.getMicrCodeRaw())
                        || containsNonNumeric(bpxf.getBranchCode())
                        || containsNonNumeric(bpxf.getBankCode())
                        || containsNonNumeric(bpxf.getCityCode());

                cheque.setMicrError(hasMicrError);
	            cheque.setRepairStatus(hasMicrError ? "NEEDS_REPAIR" : "NOT_REQUIRED");
                if (hasMicrError) micrErrors++;

                // ── Operational defaults ──
                cheque.setStatus("RECEIVED");

                // ── Wire to batch ──
                cheque.setBatch(batch);

                cheques.add(cheque);
            }

            // 4. Set batch totals
            batch.setTotalCheques(cheques.size());
            batch.setTotalAmount(root.getHeader().getTotalAmount());
            batch.setMicrErrorCount(micrErrors);
            batch.setRepairStatus(micrErrors > 0 ? "NEEDS_REPAIR" : "NOT_REQUIRED");
            batch.setCheques(cheques); // ✅ now List<InwardCheque>

            // 5. Persist — CascadeType.ALL saves cheques automatically
            batchDao.save(batch);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse/save BPXF file: " + e.getMessage(), e);
        }
    }

    public void parseAndSaveRoot(BpxfRoot root, String resolvedBatchId,
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
		cheque.setFrontImagePath(bpxf.getFrontImagePath());
		cheque.setBackImagePath(bpxf.getBackImagePath());
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
		
		// 4. Save batch first — must exist in DB before cheques reference it
		batchDao.save(batch);
		
		// 5. Save cheques explicitly — batch.id is now valid
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
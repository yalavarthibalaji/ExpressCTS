package com.iispl.daoImpl;

import com.iispl.dao.CheckerDao;
import com.iispl.entity.CheckerBatch;
import com.iispl.entity.CheckerCheque;

import java.util.ArrayList;
import java.util.List;

/**
 * CheckerDaoImpl.java DAO Implementation — in-memory data for testing. Package
 * : com.iispl.daoImpl
 *
 * TEST DATA: 1 batch → BATCH15042025001 (CHECKER_PENDING) 1 cheque → CHQ001
 * (State Bank of India, Rajesh Kumar, ₹2,50,000)
 *
 * When you click "Open Batch" → you see 1 cheque row. When you click "View" on
 * that row → full detail screen opens. This lets you verify the entire flow end
 * to end.
 *
 * MICR queue: CHQ001 is IQA PASS, so MICR queue is empty for now. To test MICR:
 * change "PASS" to "FAIL" in addCheque below.
 */
public class CheckerDaoImpl implements CheckerDao {

	private static final List<CheckerBatch> BATCHES = new ArrayList<>();
	private static final List<CheckerCheque> CHEQUES = new ArrayList<>();

	static {
		initData();
	}

	// ── Test data — 1 batch, 1 cheque ───────────────────────────────

	private static void initData() {

		// 1 batch — CHECKER_PENDING — 1 cheque — ₹2,50,000
		CheckerBatch b1 = new CheckerBatch("BATCH15042025001", 1, 250000L, "CHECKER_PENDING");
		b1.setPendingCount(1);
		BATCHES.add(b1);

		// 1 cheque — matches the Figma screenshot exactly
		addCheque("CHQ001", // chequeId
				"TXN20250415001", // transactionId
				"000123456", // chequeNumber
				"State Bank of India", // bankName
				"MG Road Branch", // branchName
				"SBIN0001234", // ifscCode
				"600002010", // micrCode
				"Rajesh Kumar", // drawerName
				"50100123456789", // drawerAccountNumber
				"ACTIVE", // chequeStatus
				"", // bounceReason
				"15-Apr-2025", // chequeDate
				"15-Apr-2025", // presentationDate
				250000L, // amountInFigures ₹2,50,000
				"Two Lakh Fifty Thousand Rupees Only", // amountInWords
				"ABC Traders", // payeeName
				"20100987654321", // depositorAccount
				"PASS", // iqaStatus
				"BATCH15042025001", // batchId
				"", // makerFlag (no special flag)
				"Verified all fields against cheque image" // makerRemarks
		);

		syncBatchCounters("BATCH15042025001");
	}

	// ── Helper ───────────────────────────────────────────────────────

	private static void addCheque(String chequeId, String txnId, String chequeNo, String bank, String branch,
			String ifsc, String micr, String drawerName, String drawerAcc, String chequeStatus, String bounceReason,
			String chequeDate, String presentDate, long amount, String amountWords, String payee, String depositorAcc,
			String iqa, String batchId, String makerFlag, String makerRemarks) {

		CheckerCheque c = new CheckerCheque();
		c.setChequeId(chequeId);
		c.setTransactionId(txnId);
		c.setChequeNumber(chequeNo);
		c.setBankName(bank);
		c.setBranchName(branch);
		c.setIfscCode(ifsc);
		c.setMicrCode(micr);
		c.setDrawerName(drawerName);
		c.setDrawerAccountNumber(drawerAcc);
		c.setChequeStatus(chequeStatus);
		c.setBounceReason(bounceReason);
		c.setChequeDate(chequeDate);
		c.setPresentationDate(presentDate);
		c.setAmountInFigures(amount);
		c.setAmountInWords(amountWords);
		c.setPayeeName(payee);
		c.setDepositorAccount(depositorAcc);
		c.setIqaStatus(iqa);
		c.setBatchId(batchId);
		c.setMakerFlag(makerFlag);
		c.setMakerRemarks(makerRemarks);
		c.setCheckerStatus("pending");
		c.setMakerStatus("done");
		CHEQUES.add(c);
	}

	// ── DAO Implementations ──────────────────────────────────────────

	@Override
	public List<CheckerBatch> getAllCheckerBatches() {
		return new ArrayList<>(BATCHES);
	}

	@Override
	public List<CheckerCheque> getChequesByBatchId(String batchId) {
		List<CheckerCheque> result = new ArrayList<>();
		for (CheckerCheque c : CHEQUES) {
			if (batchId != null && batchId.equals(c.getBatchId())) {
				result.add(c);
			}
		}
		return result;
	}

	@Override
	public CheckerCheque getChequeById(String chequeId) {
		for (CheckerCheque c : CHEQUES) {
			if (chequeId != null && chequeId.equals(c.getChequeId())) {
				return c;
			}
		}
		return null;
	}

	@Override
	public void updateChequeCheckerStatus(String chequeId, String status, String remarks) {
		for (CheckerCheque c : CHEQUES) {
			if (chequeId != null && chequeId.equals(c.getChequeId())) {
				c.setCheckerStatus(status);
				c.setCheckerRemarks(remarks);
				c.setReviewed(true);
				syncBatchCounters(c.getBatchId());
				return;
			}
		}
	}

	@Override
	public void approveBatch(String batchId) {
		for (CheckerBatch b : BATCHES) {
			if (batchId != null && batchId.equals(b.getBatchId())) {
				b.setStatus("APPROVED");
				b.setCheckerDone(true);
				syncBatchCounters(batchId);
				return;
			}
		}
	}

	@Override
	public void returnBatchToMaker(String batchId, String remarks) {
		for (CheckerBatch b : BATCHES) {
			if (batchId != null && batchId.equals(b.getBatchId())) {
				b.setStatus("CHECKER_PENDING");
				b.setCheckerDone(false);
				b.setCheckerRemarks(remarks);
				for (CheckerCheque c : CHEQUES) {
					if (batchId.equals(c.getBatchId())) {
						c.setCheckerStatus("pending");
						c.setCheckerRemarks("");
						c.setReviewed(false);
					}
				}
				syncBatchCounters(batchId);
				return;
			}
		}
	}

	@Override
	public List<CheckerCheque> getMicrRepairCheques() {
		List<CheckerCheque> result = new ArrayList<>();
		for (CheckerCheque c : CHEQUES) {
			if ("FAIL".equals(c.getIqaStatus())) {
				result.add(c);
			}
		}
		return result;
	}

	@Override
	public void submitMicrRepair(String chequeId, String micrCode, String chequeNum, String bankName, String ifscCode,
			String remarks) {
		for (CheckerCheque c : CHEQUES) {
			if (chequeId != null && chequeId.equals(c.getChequeId())) {
				if (!micrCode.isEmpty())
					c.setMicrCode(micrCode);
				if (!chequeNum.isEmpty())
					c.setChequeNumber(chequeNum);
				if (!bankName.isEmpty())
					c.setBankName(bankName);
				if (!ifscCode.isEmpty())
					c.setIfscCode(ifscCode);
				c.setMakerRemarks(remarks);
				c.setCheckerStatus("repaired");
				c.setReviewed(false);
				syncBatchCounters(c.getBatchId());
				return;
			}
		}
	}

	@Override
	public void resetMicrChequeStatus(String chequeId, String remarks) {
		// Checker rejected the repair — reset to pending so maker repairs again
		for (CheckerCheque c : CHEQUES) {
			if (chequeId != null && chequeId.equals(c.getChequeId())) {
				c.setCheckerStatus("pending");
				c.setCheckerRemarks(remarks);
				c.setReviewed(false);
				syncBatchCounters(c.getBatchId());
				return;
			}
		}
	}

	// ── Private helper ───────────────────────────────────────────────

	private static void syncBatchCounters(String batchId) {
		for (CheckerBatch b : BATCHES) {
			if (batchId != null && batchId.equals(b.getBatchId())) {
				int approved = 0, rejected = 0, pending = 0;
				for (CheckerCheque c : CHEQUES) {
					if (batchId.equals(c.getBatchId())) {
						switch (c.getCheckerStatus()) {
						case "approved":
							approved++;
							break;
						case "rejected":
							rejected++;
							break;
						default:
							pending++;
							break;
						}
					}
				}
				b.setApprovedCount(approved);
				b.setRejectedCount(rejected);
				b.setPendingCount(pending);
				return;
			}
		}
	}
}
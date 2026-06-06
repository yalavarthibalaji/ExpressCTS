package com.iispl.serviceImpl;

import com.iispl.dao.CheckerInwardVerificationDao;
import com.iispl.daoImpl.CheckerInwardVerificationDaoImpl;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CheckerInwardVerificationService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CheckerInwardVerificationServiceImpl implements CheckerInwardVerificationService {

	private CheckerInwardVerificationDao checkerInwardVerificationDao = new CheckerInwardVerificationDaoImpl();

	@Override
	public List<InwardBatch> getPendingBatches(String keyword) {

		List<InwardBatch> allBatches = checkerInwardVerificationDao.findPendingBatches();

		if (keyword == null || keyword.isEmpty()) {
			return allBatches;
		}

		List<InwardBatch> filtered = new ArrayList<>();
		String lowerKeyword = keyword.toLowerCase();

		for (InwardBatch batch : allBatches) {

			boolean matchesBatchId = batch.getBatchId() != null
					&& batch.getBatchId().toLowerCase().contains(lowerKeyword);

			boolean matchesBank = false;
			if (batch.getCheques() != null) {
				for (InwardCheque cheque : batch.getCheques()) {
					if (cheque.getPresentingBankName() != null
							&& cheque.getPresentingBankName().toLowerCase().contains(lowerKeyword)) {
						matchesBank = true;
						break;
					}
				}
			}

			if (matchesBatchId || matchesBank) {
				filtered.add(batch);
			}
		}

		return filtered;
	}

	@Override
	public List<InwardBatch> getClearedBatches(String keyword, Date fromDate, Date toDate) {

		if (fromDate != null && toDate != null && fromDate.after(toDate)) {
			throw new IllegalArgumentException("From Date cannot be after To Date.");
		}

		List<InwardBatch> allCleared = checkerInwardVerificationDao.findClearedBatches(fromDate, toDate);

		if (keyword == null || keyword.isEmpty()) {
			return allCleared;
		}

		List<InwardBatch> filtered = new ArrayList<>();
		String lowerKeyword = keyword.toLowerCase();

		for (InwardBatch batch : allCleared) {

			boolean matchesBatchId = batch.getBatchId() != null
					&& batch.getBatchId().toLowerCase().contains(lowerKeyword);

			boolean matchesChecker = false;
			if (batch.getCheckerActions() != null) {
				for (var action : batch.getCheckerActions()) {
					if (action.getChecker() != null && action.getChecker().getUserLoginId() != null
							&& action.getChecker().getUserLoginId().toLowerCase().contains(lowerKeyword)) {
						matchesChecker = true;
						break;
					}
				}
			}

			if (matchesBatchId || matchesChecker) {
				filtered.add(batch);
			}
		}

		return filtered;
	}

	@Override
	public List<InwardCheque> getReturnedCheques(String keyword, Date fromDate, Date toDate, String batchId) {

		if (fromDate != null && toDate != null && fromDate.after(toDate)) {
			throw new IllegalArgumentException("From Date cannot be after To Date.");
		}

		List<InwardCheque> allReturned = checkerInwardVerificationDao.findReturnedCheques(fromDate, toDate, batchId);

		if (keyword == null || keyword.isEmpty()) {
			return allReturned;
		}

		List<InwardCheque> filtered = new ArrayList<>();
		String lowerKeyword = keyword.toLowerCase();

		for (InwardCheque cheque : allReturned) {

			boolean matchesChequeNo = cheque.getChequeNo() != null
					&& cheque.getChequeNo().toLowerCase().contains(lowerKeyword);

			boolean matchesBank = cheque.getPresentingBankName() != null
					&& cheque.getPresentingBankName().toLowerCase().contains(lowerKeyword);

			if (matchesChequeNo || matchesBank) {
				filtered.add(cheque);
			}
		}

		return filtered;
	}
}
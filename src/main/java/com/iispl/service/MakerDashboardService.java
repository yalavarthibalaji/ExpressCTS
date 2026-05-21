package com.iispl.service;

import com.iispl.entity.OutwardChequeDummy;
import com.iispl.util.ChequeDataUtil;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MakerDashboardService.java
 * Pure in-memory service — NO database, NO DAO.
 * All data lives in static maps for the session lifetime.
 *
 * Package  : com.iispl.service
 * Used by  : MakerDashboardController
 *
 * Data stores:
 *   batches   — Map<batchId, String[]>  (batch metadata)
 *   cheques   — Map<chequeId, OutwardCheque>  (all cheques across batches)
 */
public class MakerDashboardService {

    // ── In-memory stores (static = shared across controller instances) ──
    private static final Map<String, String[]> batches   = new LinkedHashMap<>();
    // String[] = { batchId, date, session, route, remarks, status }

    private static final Map<String, OutwardChequeDummy> cheques = new LinkedHashMap<>();

    // Batch ID counter
    private static int batchCounter = 1;

    // ── Pre-load dummy data on first instantiation ──────────────────
    static {
        // Create one default batch and load the 20 dummy cheques into it
        String defaultBatch = "OW-" + todayStr() + "-001";
        batches.put(defaultBatch, new String[]{
            defaultBatch, todayStr(), "MORNING", "MAKER_CHECKER", "Pre-loaded batch", "IN_PROGRESS"
        });
        batchCounter = 2;

        List<OutwardChequeDummy> dummy = ChequeDataUtil.buildDummyCheques(defaultBatch);
        for (OutwardChequeDummy c : dummy) {
            cheques.put(c.getId(), c);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // STATS — for dashboard counters
    // ════════════════════════════════════════════════════════════════

    public int getTotalBatches() {
        return batches.size();
    }

    public int getTotalCheques() {
        return cheques.size();
    }

    public int getPendingMakerCount() {
        return (int) cheques.values().stream()
            .filter(c -> "pending".equals(c.getMakerStatus()))
            .count();
    }

    public int getSubmittedToCheckerCount() {
        return (int) cheques.values().stream()
            .filter(c -> "done".equals(c.getMakerStatus()))
            .count();
    }

    // ════════════════════════════════════════════════════════════════
    // BATCH ID GENERATION
    // ════════════════════════════════════════════════════════════════

    public String generateNextBatchId() {
        return String.format("OW-%s-%03d", todayStr(), batchCounter);
    }

    // ════════════════════════════════════════════════════════════════
    // CREATE BATCH
    // ════════════════════════════════════════════════════════════════

    public void createBatch(String batchId, String session, String route) {
        if (batchId == null || batchId.trim().isEmpty()) return;
        batches.put(batchId, new String[]{
            batchId, todayStr(), session, route, "", "CREATED"
        });
        batchCounter++;
    }

    // ════════════════════════════════════════════════════════════════
    // BATCH TABLE ROWS — for Create Batch section table
    // String[] = { batchId, date, session, route, chequeCount, status }
    // ════════════════════════════════════════════════════════════════

    public List<String[]> getBatchTableRows() {
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, String[]> e : batches.entrySet()) {
            String batchId = e.getKey();
            String[] b    = e.getValue();
            long count    = cheques.values().stream()
                              .filter(c -> batchId.equals(c.getBatchId())).count();
            rows.add(new String[]{
                batchId,        // 0 batchId
                b[1],           // 1 date
                b[2],           // 2 session
                b[3],           // 3 route
                String.valueOf(count),  // 4 chequeCount
                b[5]            // 5 status
            });
        }
        return rows;
    }

    // ════════════════════════════════════════════════════════════════
    // DASHBOARD BATCH SUMMARY
    // String[] = { batchId, chequeCount, doneCount, status }
    // ════════════════════════════════════════════════════════════════

    public List<String[]> getBatchSummaryRows() {
        List<String[]> rows = new ArrayList<>();
        for (String batchId : batches.keySet()) {
            long total = cheques.values().stream()
                           .filter(c -> batchId.equals(c.getBatchId())).count();
            long done  = cheques.values().stream()
                           .filter(c -> batchId.equals(c.getBatchId())
                                     && "done".equals(c.getMakerStatus())).count();
            String status = batches.get(batchId)[5];
            rows.add(new String[]{
                batchId,
                String.valueOf(total),
                String.valueOf(done),
                status
            });
        }
        return rows;
    }

    // ════════════════════════════════════════════════════════════════
    // LOAD CHEQUES — simulate XML upload (just use dummy data)
    // ════════════════════════════════════════════════════════════════

    public int loadChequesFromXml(String batchId, org.zkoss.util.media.Media media) {
        // In a real system: parse the XML file from media.getStreamData()
        // Here: load fresh dummy cheques into the selected batch
        List<OutwardChequeDummy> loaded = ChequeDataUtil.buildDummyCheques(batchId);

        // Remove existing cheques for this batch first (avoid duplicates)
        cheques.entrySet().removeIf(e -> batchId.equals(e.getValue().getBatchId()));

        for (OutwardChequeDummy c : loaded) {
            // Give unique IDs per batch to avoid collisions
            String uid = batchId + "_" + c.getId();
            c.setId(uid);
            cheques.put(uid, c);
        }
        // Update batch status
        if (batches.containsKey(batchId)) {
            batches.get(batchId)[5] = "LOADED";
        }
        return loaded.size();
    }

    public List<OutwardChequeDummy> getAllLoadedCheques() {
        return new ArrayList<>(cheques.values());
    }

    public int moveChequesToMakerQueue(String batchId) {
        int count = 0;
        for (OutwardChequeDummy c : cheques.values()) {
            if (batchId.equals(c.getBatchId()) && "PASS".equals(c.getIqaStatus())) {
                c.setMakerStatus("pending");
                count++;
            }
        }
        if (batches.containsKey(batchId)) {
            batches.get(batchId)[5] = "IN_PROGRESS";
        }
        return count;
    }

    // ════════════════════════════════════════════════════════════════
    // MAKER QUEUE — BATCH LIST
    // String[] = { batchId, totalCheques, totalAmt, iqaPass, iqaFail, status, doneCheques }
    // ════════════════════════════════════════════════════════════════

    public List<String[]> getMakerBatchRows() {
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : batches.entrySet()) {
            String batchId = entry.getKey();
            String status  = entry.getValue()[5];

            List<OutwardChequeDummy> bCheques = cheques.values().stream()
                .filter(c -> batchId.equals(c.getBatchId()))
                .collect(Collectors.toList());

            if (bCheques.isEmpty()) continue; // only show batches with cheques

            long total   = bCheques.size();
            long iqaPass = bCheques.stream().filter(c -> "PASS".equals(c.getIqaStatus())).count();
            long iqaFail = bCheques.stream().filter(c -> "FAIL".equals(c.getIqaStatus())).count();
            long done    = bCheques.stream().filter(c -> "done".equals(c.getMakerStatus())).count();
            long totalAmt = bCheques.stream().mapToLong(OutwardChequeDummy::getAmountInFigures).sum();

            rows.add(new String[]{
                batchId,
                String.valueOf(total),
                fmtAmt(totalAmt),
                String.valueOf(iqaPass),
                String.valueOf(iqaFail),
                status,
                String.valueOf(done)
            });
        }
        return rows;
    }

    public void loadBatchIntoMakerQueue(String batchId) {
        // No-op in dummy mode — cheques already in memory
    }

    // ════════════════════════════════════════════════════════════════
    // BATCH META — for batch detail header
    // String[] = { batchId, totalCheques, totalAmt, iqaFail, status }
    // ════════════════════════════════════════════════════════════════

    public String[] getBatchMeta(String batchId) {
        if (!batches.containsKey(batchId)) return null;

        List<OutwardChequeDummy> bc = cheques.values().stream()
            .filter(c -> batchId.equals(c.getBatchId()))
            .collect(Collectors.toList());

        long total   = bc.size();
        long iqaFail = bc.stream().filter(c -> "FAIL".equals(c.getIqaStatus())).count();
        long totalAmt = bc.stream().mapToLong(OutwardChequeDummy::getAmountInFigures).sum();
        String status = batches.get(batchId)[5];

        return new String[]{
            batchId,
            String.valueOf(total),
            fmtAmt(totalAmt),
            String.valueOf(iqaFail),
            status
        };
    }

    // ════════════════════════════════════════════════════════════════
    // BATCH CHEQUE COUNTS
    // int[] = { total, done, pending }
    // ════════════════════════════════════════════════════════════════

    public int[] getBatchChequeCounts(String batchId) {
        List<OutwardChequeDummy> bc = cheques.values().stream()
            .filter(c -> batchId.equals(c.getBatchId()))
            .collect(Collectors.toList());
        int total   = bc.size();
        int done    = (int) bc.stream().filter(c -> "done".equals(c.getMakerStatus())).count();
        int pending = total - done;
        return new int[]{ total, done, pending };
    }

    public String getBatchStatus(String batchId) {
        String[] b = batches.get(batchId);
        return b != null ? b[5] : "UNKNOWN";
    }

    // ════════════════════════════════════════════════════════════════
    // CHEQUES FOR BATCH — filtered list for batch detail table
    // ════════════════════════════════════════════════════════════════

    public List<OutwardChequeDummy> getChequesForBatch(String batchId, String search) {
        String q = (search == null) ? "" : search.toLowerCase().trim();
        return cheques.values().stream()
            .filter(c -> batchId.equals(c.getBatchId()))
            .filter(c -> q.isEmpty()
                || contains(c.getChequeNumber(), q)
                || contains(c.getDrawerName(), q)
                || contains(c.getPayeeName(), q)
                || contains(c.getBankName(), q))
            .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // CHEQUE BY ID
    // ════════════════════════════════════════════════════════════════

    public OutwardChequeDummy getChequeById(String chequeId) {
        return cheques.get(chequeId);
    }

    // ════════════════════════════════════════════════════════════════
    // CHEQUE NAVIGATION — prev / next within a batch
    // int[] = { currentIndex, ?, totalCount }
    // ════════════════════════════════════════════════════════════════

    public int[] getChequeNavIndex(String batchId, String chequeId) {
        List<String> ids = cheques.values().stream()
            .filter(c -> batchId != null && batchId.equals(c.getBatchId()))
            .map(OutwardChequeDummy::getId)
            .collect(Collectors.toList());
        int idx = ids.indexOf(chequeId);
        return new int[]{ idx, idx, ids.size() };
    }

    public String getPrevChequeId(String batchId, String chequeId) {
        List<String> ids = cheques.values().stream()
            .filter(c -> batchId != null && batchId.equals(c.getBatchId()))
            .map(OutwardChequeDummy::getId)
            .collect(Collectors.toList());
        int idx = ids.indexOf(chequeId);
        return (idx > 0) ? ids.get(idx - 1) : null;
    }

    public String getNextChequeId(String batchId, String chequeId) {
        List<String> ids = cheques.values().stream()
            .filter(c -> batchId != null && batchId.equals(c.getBatchId()))
            .map(OutwardChequeDummy::getId)
            .collect(Collectors.toList());
        int idx = ids.indexOf(chequeId);
        return (idx >= 0 && idx < ids.size() - 1) ? ids.get(idx + 1) : null;
    }

    // ════════════════════════════════════════════════════════════════
    // SAVE MAKER DATA
    // ════════════════════════════════════════════════════════════════

    public void saveMakerChequeData(String chequeId, long amount,
                                     String payeeName, String depositorAcc,
                                     String flag, String remarks) {
        OutwardChequeDummy c = cheques.get(chequeId);
        if (c == null) return;
        if (amount > 0)        c.setAmountInFigures(amount);
        if (payeeName != null) c.setPayeeName(payeeName);
        if (depositorAcc != null) c.setDepositorAcc(depositorAcc);
        c.setMakerFlag(flag);
        c.setMakerRemarks(remarks);
        c.setMakerStatus("done");
        c.setReviewed(true);
        // Update batch status
        String batchId = c.getBatchId();
        if (batchId != null && batches.containsKey(batchId)) {
            batches.get(batchId)[5] = "IN_PROGRESS";
        }
    }

    // ════════════════════════════════════════════════════════════════
    // SUBMIT BATCH TO CHECKER
    // ════════════════════════════════════════════════════════════════

    public int submitBatchToChecker(String batchId) {
        List<OutwardChequeDummy> done = cheques.values().stream()
            .filter(c -> batchId.equals(c.getBatchId()) && "done".equals(c.getMakerStatus()))
            .collect(Collectors.toList());
        if (done.isEmpty()) return -1;
        done.forEach(c -> c.setCheckerStatus("pending"));
        if (batches.containsKey(batchId)) {
            batches.get(batchId)[5] = "CHECKER_PENDING";
        }
        return done.size();
    }

    // ════════════════════════════════════════════════════════════════
    // MARK CHEQUE RETURNED
    // ════════════════════════════════════════════════════════════════

    public void markChequeReturned(String chequeId, String remarks) {
        OutwardChequeDummy c = cheques.get(chequeId);
        if (c == null) return;
        c.setMakerStatus("done");
        c.setMakerFlag("returned");
        c.setMakerRemarks("RETURN: " + (remarks != null ? remarks : ""));
    }

    // ════════════════════════════════════════════════════════════════
    // MICR REPAIR QUEUE
    // String[] = { chequeNo, bankName, readMicr, issue }
    // ════════════════════════════════════════════════════════════════

    public List<String[]> getMicrRepairRows() {
        return cheques.values().stream()
            .filter(c -> "FAIL".equals(c.getIqaStatus()))
            .map(c -> new String[]{
                c.getChequeNumber(),
                c.getBankName(),
                c.getMicrCode() != null ? c.getMicrCode() : "UNREADABLE",
                "IQA failed — MICR unreadable"
            })
            .collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // TIMELINE — for dashboard
    // ════════════════════════════════════════════════════════════════

    public boolean isStep1Done() { return !batches.isEmpty(); }
    public boolean isStep2Done() { return !cheques.isEmpty(); }
    public boolean isStep4Active() {
        return getPendingMakerCount() > 0 || getSubmittedToCheckerCount() > 0;
    }

    // ════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════

    private static String todayStr() {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
    }

    private static String fmtAmt(long amt) {
        return NumberFormat.getNumberInstance(new Locale("en", "IN")).format(amt);
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }
}
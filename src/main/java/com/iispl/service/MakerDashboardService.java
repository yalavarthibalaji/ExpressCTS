package com.iispl.service;

import com.iispl.entity.OutwardCheque;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MakerDashboardService.java
 * In-memory service — NO database, NO DAO (temporary, to be replaced with real DB calls).
 *
 * Package  : com.iispl.service
 * Used by  : MakerDashboardController
 */
public class MakerDashboardService {

    // ── In-memory stores ──────────────────────────────────────────────
    // String[] = { batchId, date, session, route, remarks, status }
    private static final Map<String, String[]> batches = new LinkedHashMap<>();
    private static final Map<String, OutwardCheque> cheques = new LinkedHashMap<>();

    private static int batchCounter = 1;

    // ── Static initializer — loads one default batch with sample data ──
    static {
        String defaultBatch = "OW-" + todayStr() + "-001";
        batches.put(defaultBatch, new String[]{
            defaultBatch, todayStr(), "MORNING", "MAKER_CHECKER", "Pre-loaded batch", "IN_PROGRESS"
        });
        batchCounter = 2;

        // Load sample cheques into the default batch
        List<OutwardCheque> sampleCheques = buildSampleCheques(defaultBatch);
        for (OutwardCheque c : sampleCheques) {
            cheques.put(c.getChequeId(), c);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // SAMPLE DATA BUILDER — replaces ChequeDataUtil
    // ════════════════════════════════════════════════════════════════

    private static List<OutwardCheque> buildSampleCheques(String batchId) {
        List<OutwardCheque> list = new ArrayList<>();
        String[] banks    = {"State Bank of India", "HDFC Bank", "ICICI Bank", "Axis Bank", "Punjab National Bank"};
        String[] branches = {"MG Road Branch", "Anna Nagar Branch", "Kochi Main Branch", "Thrissur Branch", "Palakkad Branch"};
        String[] drawers  = {"Rajesh Kumar", "Priya Nair", "Suresh Menon", "Anjali Pillai", "Manoj Sharma"};
        String[] ifsc     = {"SBIN0001234", "HDFC0002345", "ICIC0003456", "UTIB0004567", "PUNB0005678"};
        String[] micr     = {"680002010", "680002020", "680002030", "680002040", "680002050"};

        for (int i = 1; i <= 10; i++) {
            OutwardCheque c = new OutwardCheque();
            c.setChequeId("CHQ" + String.format("%03d", i));
            c.setTransactionId("TXN" + todayStr() + String.format("%03d", i));
            c.setChequeNumber(String.format("%09d", 100000 + i));
            c.setBankName(banks[i % banks.length]);
            c.setBranchName(branches[i % branches.length]);
            c.setIfscCode(ifsc[i % ifsc.length]);
            c.setMicrCode(micr[i % micr.length]);
            c.setDrawerName(drawers[i % drawers.length]);
            c.setDrawerAccountNumber("5010" + String.format("%010d", i));
            c.setPayeeName("Payee Company " + i);
            long amt = (i % 3 == 0) ? 600000L * i : 50000L * i;
            c.setAmountInFigures(amt);
            c.setAmountInWords("Sample Amount " + i);
            c.setDepositorAccountNumber("DEPO" + String.format("%08d", i));
            c.setIqaStatus(i % 4 == 0 ? "FAIL" : "PASS");
            c.setMakerStatus("pending");
            c.setCheckerStatus("pending");
            c.setReviewed(false);
            // batchId stored via the OutwardBatch relation — we keep it in chequeId prefix for filtering
            // We store batchId in makerUserId temporarily for in-memory filtering (will be replaced with DB)
            c.setMakerUserId(batchId);
            list.add(c);
        }
        return list;
    }

    // ════════════════════════════════════════════════════════════════
    // STATS
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
    // BATCH TABLE ROWS
    // String[] = { batchId, date, session, route, chequeCount, status }
    // ════════════════════════════════════════════════════════════════

    public List<String[]> getBatchTableRows() {
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, String[]> e : batches.entrySet()) {
            String batchId = e.getKey();
            String[] b = e.getValue();
            long count = cheques.values().stream()
                .filter(c -> batchId.equals(c.getMakerUserId())).count();
            rows.add(new String[]{
                batchId,
                b[1],
                b[2],
                b[3],
                String.valueOf(count),
                b[5]
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
                .filter(c -> batchId.equals(c.getMakerUserId())).count();
            long done = cheques.values().stream()
                .filter(c -> batchId.equals(c.getMakerUserId()) && "done".equals(c.getMakerStatus())).count();
            String status = batches.get(batchId)[5];
            rows.add(new String[]{ batchId, String.valueOf(total), String.valueOf(done), status });
        }
        return rows;
    }

    // ════════════════════════════════════════════════════════════════
    // LOAD CHEQUES (simulated XML upload)
    // ════════════════════════════════════════════════════════════════

    public int loadChequesFromXml(String batchId, org.zkoss.util.media.Media media) {
        // Remove existing cheques for this batch first
        cheques.entrySet().removeIf(e -> batchId.equals(e.getValue().getMakerUserId()));

        List<OutwardCheque> loaded = buildSampleCheques(batchId);
        for (OutwardCheque c : loaded) {
            String uid = batchId + "_" + c.getChequeId();
            c.setChequeId(uid);
            cheques.put(uid, c);
        }
        if (batches.containsKey(batchId)) {
            batches.get(batchId)[5] = "LOADED";
        }
        return loaded.size();
    }

    public List<OutwardCheque> getAllLoadedCheques() {
        return new ArrayList<>(cheques.values());
    }

    public int moveChequesToMakerQueue(String batchId) {
        int count = 0;
        for (OutwardCheque c : cheques.values()) {
            if (batchId.equals(c.getMakerUserId()) && "PASS".equals(c.getIqaStatus())) {
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
            String status = entry.getValue()[5];

            List<OutwardCheque> bCheques = cheques.values().stream()
                .filter(c -> batchId.equals(c.getMakerUserId()))
                .collect(Collectors.toList());

            if (bCheques.isEmpty()) continue;

            long total   = bCheques.size();
            long iqaPass = bCheques.stream().filter(c -> "PASS".equals(c.getIqaStatus())).count();
            long iqaFail = bCheques.stream().filter(c -> "FAIL".equals(c.getIqaStatus())).count();
            long done    = bCheques.stream().filter(c -> "done".equals(c.getMakerStatus())).count();
            long totalAmt = bCheques.stream()
                .mapToLong(c -> c.getAmountInFigures() != null ? c.getAmountInFigures() : 0L).sum();

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
        // No-op in in-memory mode
    }

    // ════════════════════════════════════════════════════════════════
    // BATCH META
    // String[] = { batchId, totalCheques, totalAmt, iqaFail, status }
    // ════════════════════════════════════════════════════════════════

    public String[] getBatchMeta(String batchId) {
        if (!batches.containsKey(batchId)) return null;
        List<OutwardCheque> bc = cheques.values().stream()
            .filter(c -> batchId.equals(c.getMakerUserId()))
            .collect(Collectors.toList());
        long total   = bc.size();
        long iqaFail = bc.stream().filter(c -> "FAIL".equals(c.getIqaStatus())).count();
        long totalAmt = bc.stream()
            .mapToLong(c -> c.getAmountInFigures() != null ? c.getAmountInFigures() : 0L).sum();
        String status = batches.get(batchId)[5];
        return new String[]{ batchId, String.valueOf(total), fmtAmt(totalAmt), String.valueOf(iqaFail), status };
    }

    // ════════════════════════════════════════════════════════════════
    // BATCH CHEQUE COUNTS — int[] = { total, done, pending }
    // ════════════════════════════════════════════════════════════════

    public int[] getBatchChequeCounts(String batchId) {
        List<OutwardCheque> bc = cheques.values().stream()
            .filter(c -> batchId.equals(c.getMakerUserId()))
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
    // CHEQUES FOR BATCH
    // ════════════════════════════════════════════════════════════════

    public List<OutwardCheque> getChequesForBatch(String batchId, String search) {
        String q = (search == null) ? "" : search.toLowerCase().trim();
        return cheques.values().stream()
            .filter(c -> batchId.equals(c.getMakerUserId()))
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

    public OutwardCheque getChequeById(String chequeId) {
        return cheques.get(chequeId);
    }

    // ════════════════════════════════════════════════════════════════
    // CHEQUE NAVIGATION
    // int[] = { currentIndex, currentIndex, totalCount }
    // ════════════════════════════════════════════════════════════════

    public int[] getChequeNavIndex(String batchId, String chequeId) {
        List<String> ids = cheques.values().stream()
            .filter(c -> batchId != null && batchId.equals(c.getMakerUserId()))
            .map(OutwardCheque::getChequeId)
            .collect(Collectors.toList());
        int idx = ids.indexOf(chequeId);
        return new int[]{ idx, idx, ids.size() };
    }

    public String getPrevChequeId(String batchId, String chequeId) {
        List<String> ids = cheques.values().stream()
            .filter(c -> batchId != null && batchId.equals(c.getMakerUserId()))
            .map(OutwardCheque::getChequeId)
            .collect(Collectors.toList());
        int idx = ids.indexOf(chequeId);
        return (idx > 0) ? ids.get(idx - 1) : null;
    }

    public String getNextChequeId(String batchId, String chequeId) {
        List<String> ids = cheques.values().stream()
            .filter(c -> batchId != null && batchId.equals(c.getMakerUserId()))
            .map(OutwardCheque::getChequeId)
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
        OutwardCheque c = cheques.get(chequeId);
        if (c == null) return;
        if (amount > 0)           c.setAmountInFigures(amount);
        if (payeeName != null)    c.setPayeeName(payeeName);
        if (depositorAcc != null) c.setDepositorAccountNumber(depositorAcc);
        c.setMakerRemarks(remarks);
        c.setMakerStatus("done");
        c.setReviewed(true);
        String batchId = c.getMakerUserId();
        if (batchId != null && batches.containsKey(batchId)) {
            batches.get(batchId)[5] = "IN_PROGRESS";
        }
    }

    // ════════════════════════════════════════════════════════════════
    // SUBMIT BATCH TO CHECKER
    // ════════════════════════════════════════════════════════════════

    public int submitBatchToChecker(String batchId) {
        List<OutwardCheque> done = cheques.values().stream()
            .filter(c -> batchId.equals(c.getMakerUserId()) && "done".equals(c.getMakerStatus()))
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
        OutwardCheque c = cheques.get(chequeId);
        if (c == null) return;
        c.setMakerStatus("done");
        c.setMakerRemarks("RETURN: " + (remarks != null ? remarks : ""));
    }

    // ════════════════════════════════════════════════════════════════
    // MICR REPAIR QUEUE
    // String[] = { chequeNo, bankName, micrCode, issue }
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
    // TIMELINE
    // ════════════════════════════════════════════════════════════════

    public boolean isStep1Done()   { return !batches.isEmpty(); }
    public boolean isStep2Done()   { return !cheques.isEmpty(); }
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
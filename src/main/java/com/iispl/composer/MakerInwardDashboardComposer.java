package com.iispl.composer;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listbox;

/**
 * MakerInwardDashboardComposer
 *
 * Handles the Maker Inward Dashboard page:
 *  - Navigates to File Processing on btnFileProcessing click
 *  - Navigates to Reject & Repair on btnRejectRepair click
 *  - Toggles empty-state message vs listbox for Inward Batches
 */
public class MakerInwardDashboardComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    // ── Wired ZUL components ──────────────────────────────────────────────
    

    @Wire("#batchListbox")
    private Listbox batchListbox;

    @Wire("#emptyBatchesMsg")
    private Div emptyBatchesMsg;

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        // Uncomment when service is ready:
        // loadBatches();

        // Default state: show empty message, hide listbox
        emptyBatchesMsg.setVisible(true);
        batchListbox.setVisible(false);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Fetch inward batches and bind to listbox.
     * Uncomment and wire InwardBatchService when ready.
     */
//    private void loadBatches() {
//        try {
//            List<InwardBatch> batches = inwardBatchService.getInwardBatches();
//            if (batches == null || batches.isEmpty()) {
//                emptyBatchesMsg.setVisible(true);
//                batchListbox.setVisible(false);
//            } else {
//                emptyBatchesMsg.setVisible(false);
//                batchListbox.setVisible(true);
//                batchListbox.setModel(new ListModelList<>(batches));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            emptyBatchesMsg.setVisible(true);
//            batchListbox.setVisible(false);
//        }
//    }

    // ── Event handlers ────────────────────────────────────────────────────

    /**
     * Navigate to File Processing page.
     * FIX #2: Must use Executions.getCurrent().sendRedirect() — not static call.
     */
    @Listen("onClick = #btnFileProcessing")
    public void onFileProcessing() {
        // FIX: getCurrent() is required; Executions.sendRedirect() does not exist
//        Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul");
    	Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul");
    }

    /**
     * Navigate to Reject & Repair page.
     */
    @Listen("onClick = #btnRejectRepair")
    public void onRejectRepair() {
        Executions.getCurrent().sendRedirect("/inward/inwardMicr/RejectRepair.zul");
    }
    
}
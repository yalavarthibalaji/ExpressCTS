package com.iispl.composer;

import com.iispl.entity.inward.InwardBatch;
import com.iispl.service.RejectRepairService;
import com.iispl.serviceImpl.RejectRepairServiceImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RejectRepairComposer
 *
 * Handles the Reject & Repair Step 1 page: - On load: fetches repair-eligible
 * batches - Shows empty state if no eligible batches - Shows listbox with batch
 * rows if batches exist - "Go to File Processing" button redirects to upload
 * page - Row click navigates to Step 2 (date & amount correction) passing
 * selected batchId as request param
 */
public class RejectRepairComposer extends SelectorComposer<Component> {

	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	// ── Wired components ──────────────────────────────────────────────────
	@Wire("#emptyState")
	private Div emptyState;

	@Wire("#batchListWrap")
	private Div batchListWrap;

	@Wire("#batchListbox")
	private Listbox batchListbox;

	@Wire("#btnGoToFileProcessing")
	private Button btnGoToFileProcessing;

	// ── Service ───────────────────────────────────────────────────────────
	private final RejectRepairService rejectRepairService = new RejectRepairServiceImpl();

	// ── Lifecycle ─────────────────────────────────────────────────────────

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		loadRepairEligibleBatches();
	}

	// ── Private helpers ───────────────────────────────────────────────────

	private void loadRepairEligibleBatches() {
		List<InwardBatch> batches = rejectRepairService.getRepairEligibleBatches();

		if (batches == null || batches.isEmpty()) {
			// Show empty state, hide listbox
			emptyState.setVisible(true);
			batchListWrap.setVisible(false);
			return;
		}

		// Populate listbox
		batchListbox.getItems().clear();

		for (InwardBatch batch : batches) {
			Listitem item = new Listitem();

			// Store batchId as item value for row-click navigation
			item.setValue(batch.getBatchId());

			item.appendChild(new Listcell(batch.getBatchId()));
			item.appendChild(new Listcell(batch.getSourceFileName() != null ? batch.getSourceFileName() : ""));
			item.appendChild(new Listcell(batch.getBatchDate() != null ? batch.getBatchDate().format(DATE_FMT) : ""));
			item.appendChild(new Listcell(String.valueOf(batch.getTotalCheques())));
			item.appendChild(new Listcell(String.valueOf(batch.getMicrErrorCount())));
			item.appendChild(new Listcell(batch.getStatus() != null ? batch.getStatus() : ""));
			item.appendChild(new Listcell(batch.getRepairStatus() != null ? batch.getRepairStatus() : ""));

			// Action cell — Repair button per row
			Listcell actionCell = new Listcell();
			Button repairBtn = new Button("Repair");
			repairBtn.setSclass("btn-repair-row");
			final String batchId = batch.getBatchId();
			repairBtn.addEventListener("onClick", e -> navigateToStep2(batchId));
			actionCell.appendChild(repairBtn);
			item.appendChild(actionCell);

			batchListbox.appendChild(item);
		}

		emptyState.setVisible(false);
		batchListWrap.setVisible(true);
	}

	/**
	 * Navigate to Step 2 (Date & Amount correction) passing the selected batchId.
	 */
	private void navigateToStep2(String batchId) {
		Executions.getCurrent().sendRedirect("/rejectrepair/step2DateAmount.zul?batchId=" + batchId);
	}

	// ── Event handlers ────────────────────────────────────────────────────

	/**
	 * "Go to File Processing" — redirect to upload page.
	 */
	@Listen("onClick = #btnRejectRepair")
	public void onGoToFileProcessing() {
		Executions.getCurrent().sendRedirect("/inward/bpxfUpload/bpxfUpload.zul");
	}
}
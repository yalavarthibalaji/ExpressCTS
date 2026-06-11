package com.iispl.composer;

import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.PayeeAccountService;
import com.iispl.serviceImpl.PayeeAccountServiceImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PayeeAccountComposer — Step 3 Payee Name & Account Entry.
 *
 * Two panels: • listPanel — paginated table of all cheques in the batch •
 * detailPanel — split-screen: left = cheque reference card, right = payee +
 * account entry form
 *
 * Navigation between panels is driven by the "Enter" button in the table (list
 * → detail) and the "← Back to List" button (detail → list).
 *
 * Prev / Next arrows in the detail panel let the maker walk through all cheques
 * without returning to the list.
 */
public class PayeeAccountComposer extends SelectorComposer<Component> {

	private static final long serialVersionUID = 1L;

	// ── Session / URL constants ────────────────────────────────────────────
	private static final String SESSION_BATCH_ID = "cts_inward_batch_id";
	private static final String PAGE_STEP2 = "/inward/inwardMicr/DateAmount.zul";
	private static final String PAGE_CHECKER = "/dashboard/checkerInward/checkerInwardDashboard.zul";
	private static final int PAGE_SIZE = 10;

	// ══════════════════════════════════════════════════════════════════════
	// @Wire — top-nav / breadcrumb labels
	// ══════════════════════════════════════════════════════════════════════
	@Wire("#lblUserRole")
	private Label lblUserRole;

	// Image viewer
	@Wire("#btnViewFront")
	private Button btnViewFront;
	@Wire("#btnViewBack")
	private Button btnViewBack;
	@Wire("#btnViewGray")
	private Button btnViewGray;
	@Wire("#btnZoomIn")
	private Button btnZoomIn;
	@Wire("#btnZoomOut")
	private Button btnZoomOut;
	@Wire("#btnZoomFit")
	private Button btnZoomFit;
	@Wire("#lblZoomLevel")
	private Label lblZoomLevel;
	@Wire("#divFrontImage")
	private Div divFrontImage;
	@Wire("#divBackImage")
	private Div divBackImage;
	@Wire("#divGrayImage")
	private Div divGrayImage;
	@Wire("#imgFront")
	private Image imgFront;
	@Wire("#imgBack")
	private Image imgBack;
	@Wire("#imgGray")
	private Image imgGray;
	@Wire("#lblMicrBandStrip")
	private Label lblMicrBandStrip;
	@Wire("#ocrWarningBar")
	private Div ocrWarningBar;

	// ══════════════════════════════════════════════════════════════════════
	// @Wire — LIST panel
	// ══════════════════════════════════════════════════════════════════════
	@Wire("#listPanel")
	private Div listPanel;
	@Wire("#lblBatchBadge")
	private Label lblBatchBadge;
	@Wire("#lblPendingBadge")
	private Label lblPendingBadge;
	@Wire("#cmbFilter")
	private Combobox cmbFilter;
	@Wire("#chequeListbox")
	private Listbox chequeListbox;
	@Wire("#btnPrevPage")
	private Button btnPrevPage;
	@Wire("#btnNextPage")
	private Button btnNextPage;
	@Wire("#lblPageInfo")
	private Label lblPageInfo;
	@Wire("#btnBackToStep2")
	private Button btnBackToStep2;
	@Wire("#btnProceedChecker")
	private Button btnProceedChecker;

	// ══════════════════════════════════════════════════════════════════════
	// @Wire — DETAIL panel
	// ══════════════════════════════════════════════════════════════════════
	@Wire("#detailPanel")
	private Div detailPanel;
	@Wire("#lblDetailBatchBadge")
	private Label lblDetailBatchBadge;

	// Left card — cheque reference
	@Wire("#btnPrevCheque")
	private Button btnPrevCheque;
	@Wire("#btnNextCheque")
	private Button btnNextCheque;
	@Wire("#lblNavIndicator")
	private Label lblNavIndicator;
	@Wire("#lblRefBank")
	private Label lblRefBank;
	@Wire("#lblRefPayeePlaceholder")
	private Label lblRefPayeePlaceholder;
	@Wire("#lblRefAmtWords")
	private Label lblRefAmtWords;
	@Wire("#lblRefAmount")
	private Label lblRefAmount;
	@Wire("#lblRefChequeNo")
	private Label lblRefChequeNo;

	// Right form
	@Wire("#txtPayeeName")
	private Textbox txtPayeeName;
	@Wire("#txtAccNo")
	private Textbox txtAccNo;
	@Wire("#txtEntryRemarks")
	private Textbox txtEntryRemarks;
	@Wire("#btnSaveEntry")
	private Button btnSaveEntry;

	@Wire("#btnBackToList")
	private Button btnBackToList;

	// ══════════════════════════════════════════════════════════════════════
	// Service & State
	// ══════════════════════════════════════════════════════════════════════
	private final PayeeAccountService service = new PayeeAccountServiceImpl();

	private String currentBatchId;
	private List<InwardCheque> allCheques; // full unfiltered list for the batch

	// List-panel pagination
	private int currentPage = 1;

	// Detail-panel navigation index (into allCheques, not filtered list)
	private int detailIdx = 0;

	// ══════════════════════════════════════════════════════════════════════
	// Lifecycle
	// ══════════════════════════════════════════════════════════════════════

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		resolveBatchId();
		loadCheques();
		wireEvents();
		showListPanel();
	}

	// ──────────────────────────────────────────────────────────────────────

	private void resolveBatchId() {
		String param = Executions.getCurrent().getParameter("batchId");
		if (param != null && !param.isBlank()) {
			currentBatchId = param.trim();
			Sessions.getCurrent().setAttribute(SESSION_BATCH_ID, currentBatchId);
		} else {
			Object sess = Sessions.getCurrent().getAttribute(SESSION_BATCH_ID);
			if (sess != null) {
				currentBatchId = sess.toString();
			}
		}
	}

	private void loadCheques() {
		if (currentBatchId == null || currentBatchId.isBlank()) {
			allCheques = Collections.emptyList();
			return;
		}
		allCheques = service.getChequesByBatchId(currentBatchId);
		if (allCheques == null)
			allCheques = Collections.emptyList();
	}

	// ══════════════════════════════════════════════════════════════════════
	// Event wiring (programmatic — works alongside @Listen)
	// ══════════════════════════════════════════════════════════════════════

	private void wireEvents() {
		// Filter combo
		if (cmbFilter != null) {
			cmbFilter.addEventListener(Events.ON_SELECT, e -> {
				currentPage = 1;
				renderTable();
			});
		}
		// Pagination
		if (btnPrevPage != null) {
			btnPrevPage.addEventListener(Events.ON_CLICK, e -> {
				if (currentPage > 1) {
					currentPage--;
					renderTable();
				}
			});
		}
		if (btnNextPage != null) {
			btnNextPage.addEventListener(Events.ON_CLICK, e -> {
				if (currentPage < totalPages()) {
					currentPage++;
					renderTable();
				}
			});
		}
		// Detail navigation
		if (btnPrevCheque != null) {
			btnPrevCheque.addEventListener(Events.ON_CLICK, e -> {
				if (detailIdx > 0) {
					detailIdx--;
					loadDetailRecord();
				}
			});
		}
		if (btnNextCheque != null) {
			btnNextCheque.addEventListener(Events.ON_CLICK, e -> {
				if (detailIdx < allCheques.size() - 1) {
					detailIdx++;
					loadDetailRecord();
				}
			});
		}
		// Detail form actions
		if (btnSaveEntry != null) {
			btnSaveEntry.addEventListener(Events.ON_CLICK, e -> doSaveEntry());
		}

		if (btnBackToList != null) {
			btnBackToList.addEventListener(Events.ON_CLICK, e -> showListPanel());
		}
	}

	private void loadChequeImages(InwardCheque c) {
		setImageViaServlet(imgFront, c.getFrontImagePath());
		setImageViaServlet(imgBack, c.getBackImagePath());
		setImageViaServlet(imgGray, c.getFrontImagePath());
		if (imgGray != null)
			imgGray.setStyle("filter:grayscale(100%);max-width:100%;display:block;");
	}

	private void setImageViaServlet(Image img, String path) {
		if (img == null)
			return;
		if (path == null || path.trim().isEmpty()) {
			img.setSrc("");
			return;
		}
		try {
			String encoded = URLEncoder.encode(path.trim(), "UTF-8");
			img.setSrc("/imageServlet?path=" + encoded);
		} catch (UnsupportedEncodingException e) {
			img.setSrc("/imageServlet?path=" + path.trim());
		}
	}

	// ══════════════════════════════════════════════════════════════════════
	// PANEL SWITCHING
	// ══════════════════════════════════════════════════════════════════════

	private void showListPanel() {
		setVis(listPanel, true);
		setVis(detailPanel, false);
		updateBadges();
		renderTable();
	}

	private void showDetailPanel(int idx) {
		detailIdx = idx;
		setVis(listPanel, false);
		setVis(detailPanel, true);
		if (lblDetailBatchBadge != null)
			lblDetailBatchBadge.setValue("BATCH: " + currentBatchId);
		loadDetailRecord();
	}

	// ══════════════════════════════════════════════════════════════════════
	// LIST PANEL — render table
	// ══════════════════════════════════════════════════════════════════════

	private void renderTable() {
		if (chequeListbox == null)
			return;

		List<InwardCheque> filtered = getFilteredList();
		int total = filtered.size();
		currentPage = Math.min(currentPage, Math.max(1, totalPages(filtered)));

		int from = (currentPage - 1) * PAGE_SIZE;
		int to = Math.min(from + PAGE_SIZE, total);

		chequeListbox.getItems().clear();
		int rowNum = from + 1;

		for (int i = from; i < to; i++) {
			InwardCheque c = filtered.get(i);
			Listitem row = new Listitem();

			addCell(row, String.valueOf(rowNum++));
			addCell(row, nvl(c.getChequeNo()));
			addCell(row, nvl(c.getPresentingBankName()));

			// Amount — right-aligned
			Listcell amtCell = new Listcell(c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");
			amtCell.setStyle("text-align:right; font-family:'IBM Plex Mono',monospace; font-size:13px;");
			row.appendChild(amtCell);

			// Payee Name
			addCell(row, nvlDash(c.getPayeeName()));

			// Account No
			addCell(row, nvlDash(c.getDraweeAccountNumber()));

			// Status badge
			Listcell statusCell = new Listcell();
			statusCell.setStyle("text-align:center;");
			Label badge = new Label(resolveStatusLabel(c.getRepairStatus()));
			badge.setSclass(resolveStatusSclass(c.getRepairStatus()));
			statusCell.appendChild(badge);
			row.appendChild(statusCell);

			// Action — "Enter" button; find true index in allCheques for navigation
			final int allIdx = allCheques.indexOf(c);
			Listcell actionCell = new Listcell();
			actionCell.setStyle("text-align:center;");
			Button enterBtn = new Button("Enter");
			enterBtn.setSclass("btn-pa-enter");
			enterBtn.addEventListener(Events.ON_CLICK, ev -> showDetailPanel(allIdx));
			actionCell.appendChild(enterBtn);
			row.appendChild(actionCell);

			chequeListbox.appendChild(row);
		}

		// Pagination info
		int tp = totalPages(filtered);
		if (lblPageInfo != null)
			lblPageInfo.setValue("Page " + currentPage + " of " + tp + " | " + total + " records");
		if (btnPrevPage != null)
			btnPrevPage.setDisabled(currentPage <= 1);
		if (btnNextPage != null)
			btnNextPage.setDisabled(currentPage >= tp);

		updateBadges();
	}

	private void updateBadges() {
		if (lblBatchBadge != null && currentBatchId != null)
			lblBatchBadge.setValue("BATCH: " + currentBatchId);
		if (lblPendingBadge != null) {
			long pending = service.countPending(allCheques);
			lblPendingBadge.setValue(pending + " PENDING");
		}
	}

	// ══════════════════════════════════════════════════════════════════════
	// DETAIL PANEL — load record
	// ══════════════════════════════════════════════════════════════════════

	private void loadDetailRecord() {
		if (allCheques.isEmpty())
			return;
		detailIdx = Math.max(0, Math.min(detailIdx, allCheques.size() - 1));
		InwardCheque c = allCheques.get(detailIdx);

		// Navigation indicator
		if (lblNavIndicator != null)
			lblNavIndicator.setValue((detailIdx + 1) + " of " + allCheques.size());
		if (btnPrevCheque != null)
			btnPrevCheque.setDisabled(detailIdx == 0);
		if (btnNextCheque != null)
			btnNextCheque.setDisabled(detailIdx == allCheques.size() - 1);

		// Left card — cheque reference
		if (lblRefBank != null)
			lblRefBank.setValue(nvl(c.getPresentingBankName()));
		if (lblRefChequeNo != null)
			lblRefChequeNo.setValue("Cheque No: " + nvl(c.getChequeNo()));
		if (lblRefPayeePlaceholder != null) {
			String payee = c.getPayeeName();
			if (payee == null || payee.isBlank()) {
				lblRefPayeePlaceholder.setValue("[Enter Payee Name]");
				lblRefPayeePlaceholder.setStyle("color:#e65100; font-weight:600;");
			} else {
				lblRefPayeePlaceholder.setValue(payee);
				lblRefPayeePlaceholder.setStyle("color:#1a2b45; font-weight:600;");
			}
		}
		if (lblRefAmtWords != null)
			lblRefAmtWords.setValue(nvl(amountInWords(c.getAmount())));
		if (lblRefAmount != null)
			lblRefAmount.setValue(c.getAmount() != null ? "₹ " + fmt(c.getAmount()) : "—");

		// Right form — pre-fill with existing values
		if (txtPayeeName != null)
			txtPayeeName.setValue(nvlEmpty(c.getPayeeName()));
		if (txtAccNo != null)
			txtAccNo.setValue(nvlEmpty(c.getDraweeAccountNumber()));
		if (txtEntryRemarks != null)
			txtEntryRemarks.setValue(nvlEmpty(c.getRemarks()));

		loadChequeImages(c);
	}

	// ══════════════════════════════════════════════════════════════════════
	// DETAIL ACTIONS
	// ══════════════════════════════════════════════════════════════════════

	private void doSaveEntry() {
		if (allCheques.isEmpty())
			return;
		InwardCheque c = allCheques.get(detailIdx);

		String payee = txtPayeeName != null ? txtPayeeName.getValue().trim() : "";
		String accNo = txtAccNo != null ? txtAccNo.getValue().trim() : "";
		String remark = txtEntryRemarks != null ? txtEntryRemarks.getValue() : "";

		if (payee.isEmpty()) {
			Messagebox.show("Payee Name is required.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}
		if (accNo.isEmpty()) {
			Messagebox.show("Account Number is required.", "Validation", Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}

		c.setPayeeName(payee);
		c.setDraweeAccountNumber(accNo);
		c.setRemarks(remark);
		service.saveEntry(c);

		Messagebox.show("Entry saved for cheque " + c.getChequeNo() + " ✓", "Success", Messagebox.OK,
				Messagebox.INFORMATION);

		// Auto-advance to next pending cheque if available
		advanceToNextPending();
	}

	/**
	 * After saving or referring, jump to the next cheque that still needs entry. If
	 * none remain, go back to the list.
	 */
	private void advanceToNextPending() {
		// Refresh in-memory list from DB
		allCheques = service.getChequesByBatchId(currentBatchId);
		if (allCheques == null)
			allCheques = Collections.emptyList();

		// Look for next pending after current index
		for (int i = detailIdx + 1; i < allCheques.size(); i++) {
			InwardCheque c = allCheques.get(i);
			if (isPending(c)) {
				detailIdx = i;
				loadDetailRecord();
				return;
			}
		}
		// No more pending — return to list
		showListPanel();
	}

	private boolean isPending(InwardCheque c) {
		String status = c.getRepairStatus();
		if ("REFERRED_PAYEEACCOUNT".equalsIgnoreCase(status))
			return false;
		if ("ENTRY_DONE".equalsIgnoreCase(status))
			return false;
		return true;
	}

	// ══════════════════════════════════════════════════════════════════════
	// @Listen — wizard bar & nav buttons
	// ══════════════════════════════════════════════════════════════════════

	@Listen("onClick = #btnStep1")
	public void onStep1() {
		Executions.getCurrent().sendRedirect("/inward/inwardMicr/RejectRepair.zul?batchId=" + currentBatchId);
	}

	@Listen("onClick = #btnStep2")
	public void onStep2() {
		Executions.getCurrent().sendRedirect(PAGE_STEP2 + "?batchId=" + currentBatchId);
	}

	@Listen("onClick = #btnStep3")
	public void onStep3() {
		/* already on Step 3 */ }

	@Listen("onClick = #btnBackToStep2")
	public void onBackToStep2() {
		Executions.getCurrent().sendRedirect(PAGE_STEP2 + "?batchId=" + currentBatchId);
	}

	@Listen("onClick = #btnProceedChecker")
	public void onProceedChecker() {
		long pending = service.countPending(allCheques);
		String msg = pending > 0 ? pending + " cheque(s) still pending. Proceed to Inward Checker anyway?"
				: "All entries complete. Proceed to Inward Checker?";

		Messagebox.show(msg, "Confirm", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, evt -> {
			if (Messagebox.ON_YES.equals(evt.getName())) {
				boolean ok = service.proceedToInwardChecker(currentBatchId);
				if (ok) {
					Executions.getCurrent().sendRedirect(PAGE_CHECKER + "?batchId=" + currentBatchId);
				} else {
					Messagebox.show("Failed to advance batch. Please try again.", "Error", Messagebox.OK,
							Messagebox.ERROR);
				}
			}
		});
	}

	// ══════════════════════════════════════════════════════════════════════
	// HELPERS
	// ══════════════════════════════════════════════════════════════════════

	private List<InwardCheque> getFilteredList() {
		if (allCheques == null)
			return Collections.emptyList();
		final String filterVal = selectedFilterValue();
		if (filterVal.isEmpty())
			return allCheques;
		return allCheques.stream().filter(c -> {
			String rs = c.getRepairStatus() != null ? c.getRepairStatus() : "";
			// "NEEDS_ENTRY" filter matches blank / null / unrecognised status
			if ("NEEDS_ENTRY".equalsIgnoreCase(filterVal)) {
				return rs.isEmpty()
						|| (!rs.equalsIgnoreCase("ENTRY_DONE") && !rs.equalsIgnoreCase("REFERRED_PAYEEACCOUNT"));
			}
			return rs.equalsIgnoreCase(filterVal);
		}).collect(Collectors.toList());
	}

	private String selectedFilterValue() {
		if (cmbFilter == null)
			return "";
		Comboitem sel = cmbFilter.getSelectedItem();
		if (sel == null)
			return "";
		Object v = sel.getValue();
		return v != null ? v.toString() : "";
	}

	private int totalPages() {
		return totalPages(getFilteredList());
	}

	private int totalPages(List<?> list) {
		return Math.max(1, (int) Math.ceil((double) list.size() / PAGE_SIZE));
	}

	private void addCell(Listitem row, String text) {
		row.appendChild(new Listcell(text != null ? text : "—"));
	}

	private void setVis(Component c, boolean visible) {
		if (c != null)
			c.setVisible(visible);
	}

	private String nvl(String v) {
		return (v != null && !v.isBlank()) ? v : "—";
	}

	private String nvlDash(String v) {
		return (v != null && !v.isBlank()) ? v : "—";
	}

	private String nvlEmpty(String v) {
		return v != null ? v : "";
	}

	private String fmt(BigDecimal v) {
		return String.format("%,.2f", v);
	}

	/**
	 * Very simple amount-in-words (English, Indian numbering). Replace with a
	 * proper library (e.g. ICU4J) if needed.
	 */
	private String amountInWords(BigDecimal amount) {
		if (amount == null)
			return "";
		long units = amount.longValue();
		// Basic lookup for common amounts — good enough for UI reference card
		String[] ones = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven",
				"Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen" };
		String[] tens = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };

		if (units == 0)
			return "Zero Only";
		StringBuilder sb = new StringBuilder();
		if (units >= 100000) {
			sb.append(ones[(int) (units / 100000)]).append(" Lakh ");
			units %= 100000;
		}
		if (units >= 1000) {
			int t = (int) (units / 1000);
			if (t >= 20)
				sb.append(tens[t / 10]).append(" ").append(ones[t % 10]).append(" ");
			else
				sb.append(ones[t]).append(" ");
			sb.append("Thousand ");
			units %= 1000;
		}
		if (units >= 100) {
			sb.append(ones[(int) (units / 100)]).append(" Hundred ");
			units %= 100;
		}
		if (units >= 20) {
			sb.append(tens[(int) (units / 10)]).append(" ").append(ones[(int) (units % 10)]).append(" ");
		} else if (units > 0) {
			sb.append(ones[(int) units]).append(" ");
		}
		return sb.toString().trim() + " Only";
	}

	private String resolveStatusLabel(String s) {
		if (s == null || s.isBlank())
			return "NEEDS ENTRY";
		return switch (s.toUpperCase()) {
		case "ENTRY_DONE" -> "COMPLETED";
		case "REFERRED_PAYEEACCOUNT" -> "REFERRED PAYEEACCOUNT";
		default -> "NEEDS ENTRY";
		};
	}

	private String resolveStatusSclass(String s) {
		if (s == null || s.isBlank())
			return "badge-needs-repair";
		return switch (s.toUpperCase()) {
		case "ENTRY_DONE" -> "badge-repaired";
		case "REFERRED_PAYEEACCOUNT" -> "badge-referred";
		default -> "badge-needs-repair";
		};
	}
}
package com.iispl.composer;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;

import com.iispl.dto.CbsValidationResult;
import com.iispl.dto.LoginDTO;
import com.iispl.entity.User;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.service.CbsFirebaseService;
import com.iispl.service.CheckerBatchProcessService;
import com.iispl.serviceImpl.CheckerBatchProcessServiceImpl;
import com.iispl.util.InwardReturnReason;
import com.iispl.util.SessionUtil;

public class ProcessBatchComposer extends SelectorComposer<Component> {

	// ── Batch Summary Bar ─────────────────────────────────────────────────────
	@Wire
	private Label lblBatchId;
	@Wire
	private Label lblBatchBadge;
	@Wire
	private Label lblBatchDate;
	@Wire
	private Label lblTotalCheques;
	@Wire
	private Label lblTotalAmount;
	@Wire
	private Label lblMicrErrors;
	@Wire
	private Label lblSourceFile;

	// ── Navigation ────────────────────────────────────────────────────────────
	@Wire
	private Button btnPrev;
	@Wire
	private Label lblRecordNav;
	@Wire
	private Button btnNext;
	@Wire
	private Label lblProgressCounter;

	// ── Progress Bar ──────────────────────────────────────────────────────────
	@Wire
	private Label lblProgressText;
	@Wire
	private Div divProgressFill;

	// ── LEFT — Image viewer ───────────────────────────────────────────────────
	@Wire
	private Tabbox pvTabbox;
	@Wire
	private Image pvFrontImg;
	@Wire
	private Image pvBackImg;
	@Wire
	private Image pvGreyscaleImg;
	@Wire
	private Label pvNoImgMsg;

	// ── LEFT — Cheque card labels ─────────────────────────────────────────────
	@Wire
	private Label lblChequeBankName;
	@Wire
	private Label lblChequePresenting;
	@Wire
	private Label lblChequeDate;
	@Wire
	private Label lblChequePayee;
	@Wire
	private Label lblChequeAmountWords;
	@Wire
	private Label lblChequeAmountBox;
	@Wire
	private Label lblMicrLine;

	// ── RIGHT — Status badge ──────────────────────────────────────────────────
	@Wire
	private Label lblTechStatus;

	// ── RIGHT — CBS Validation ────────────────────────────────────────────────
	@Wire
	private Label lblCbsMicrCode;
	@Wire
	private Label lblCbsBankCode;
	@Wire
	private Label lblCbsOurBankCode;

	// ── RIGHT — Presenting Details ────────────────────────────────────────────
	@Wire
	private Label lblPresBank;
	@Wire
	private Label lblPresChequeNo;
	@Wire
	private Label lblPresAmount;

	// ── RIGHT — Our Account (CBS — loaded from Firebase) ─────────────────────
	@Wire
	private Label lblAcctNo;
	@Wire
	private Label lblAcctHolder;
	@Wire
	private Label lblAcctBalance;
	@Wire
	private Label lblCbsAcctValid;
	@Wire
	private Label lblCbsBankMatch;

	// ── RIGHT — Action ────────────────────────────────────────────────────────
	@Wire
	private Button btnAccept;
	@Wire
	private Button btnReturn;
	@Wire
	private Button btnSendBack;
	@Wire
	private Div divReturnReasonBox;
	@Wire
	private Combobox comboReturnReason;
	@Wire
	private Label lblReturnReasonError;
	@Wire
	private Button btnConfirmReturn;

	// ── Footer ────────────────────────────────────────────────────────────────
	@Wire
	private Label lblFooterProgress;
	@Wire
	private Button btnSubmit;

	// ── Refer Back Popup ──────────────────────────────────────────────────────
	// winReferBack is a <div> in ZUL (custom overlay), NOT a ZK Window
	@Wire
	private Div winReferBack;
	@Wire
	private Combobox comboReferReason;
	@Wire
	private Combobox comboReferModule;
	@Wire
	private Textbox txtReferRemarks;
	@Wire
	private Label lblReferReasonError;
	@Wire
	private Button btnConfirmReferBack;
	@Wire
	private Button btnCancelReferBack;

	// ── State ─────────────────────────────────────────────────────────────────
	private InwardBatch currentBatch = null;
	private List<InwardCheque> cheques = null;
	private int currentIndex = 0;
	private Map<Long, String> actionMap = new HashMap<>();
	private Map<Long, String> reasonMap = new HashMap<>();
	private InwardCheque pendingReferCheque = null;

	// ── Services ──────────────────────────────────────────────────────────────
	private final CheckerBatchProcessService batchService = new CheckerBatchProcessServiceImpl();
	private final CbsFirebaseService cbsService = new CbsFirebaseService();
	private final java.util.Set<Long> savedViaConfirmReturn = new java.util.HashSet<>();

	// ══════════════════════════════════════════════════════════════════════════
	// Lifecycle
	// ══════════════════════════════════════════════════════════════════════════

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		getSelf().getDesktop().enableServerPush(true);

		String batchId = (String) Sessions.getCurrent().getAttribute("selectedBatchId");

		if (batchId == null || batchId.trim().isEmpty()) {
			Messagebox.show("No batch selected. Please go back and select a batch.", "Error", Messagebox.OK,
					Messagebox.ERROR);
			return;
		}

		try {
			currentBatch = batchService.loadBatchForProcessing(batchId.trim());
		} catch (Exception e) {
			Messagebox.show("Failed to load batch: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
			return;
		}

		// Use a separate mutable list so we can remove referred-back cheques
		cheques = new ArrayList<>(currentBatch.getCheques());

		if (cheques.isEmpty()) {
			Messagebox.show("This batch has no cheques.", "Info", Messagebox.OK, Messagebox.INFORMATION);
			return;
		}

		// Make sure the popup is hidden on load
		// Ensure popup is hidden on load
		if (winReferBack != null)
			winReferBack.setSclass("rb-overlay");

		populateSummaryBar();
		buildReturnReasonDropdown();
		renderCheque(0);
		refreshProgress();
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Summary Bar
	// ══════════════════════════════════════════════════════════════════════════

	private void populateSummaryBar() {
		set(lblBatchId, currentBatch.getBatchId());
		set(lblBatchBadge, "BATCH: " + currentBatch.getBatchId());
		set(lblBatchDate, currentBatch.getBatchDate() != null ? currentBatch.getBatchDate().toString() : "—");
		set(lblTotalCheques, String.valueOf(currentBatch.getTotalCheques()));
		set(lblTotalAmount, "₹ " + fmtAmt(currentBatch.getTotalAmount()));
		set(lblMicrErrors, String.valueOf(currentBatch.getMicrErrorCount()));
		set(lblSourceFile, nvl(currentBatch.getSourceFileName()));
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Render one cheque
	// ══════════════════════════════════════════════════════════════════════════

	private void renderCheque(int index) {
		if (cheques == null || index < 0 || index >= cheques.size())
			return;
		currentIndex = index;

		InwardCheque c = cheques.get(index);
		int total = cheques.size();
		int record = index + 1;

		set(lblRecordNav, "Record " + record + " of " + total);
		set(lblProgressCounter, record + "/" + total);
		

		if (btnPrev != null)
			btnPrev.setDisabled(index == 0);
		if (btnNext != null)
			btnNext.setDisabled(index == total - 1);

		loadImages(c);
		renderChequeCard(c);
		renderTechnicalPanel(c);
		restoreActionState(c.getId());
	}

	// ══════════════════════════════════════════════════════════════════════════
	// LEFT — Image Loading
	// ══════════════════════════════════════════════════════════════════════════

	private void loadImages(InwardCheque c) {
		String front = c.getFrontImagePath();
		String back = c.getBackImagePath();
		boolean hasFront = front != null && !front.trim().isEmpty();
		boolean hasBack = back != null && !back.trim().isEmpty();

		if (!hasFront && !hasBack) {
			if (pvNoImgMsg != null)
				pvNoImgMsg.setVisible(true);
			if (pvTabbox != null)
				pvTabbox.setVisible(false);
			return;
		}

		if (pvNoImgMsg != null)
			pvNoImgMsg.setVisible(false);
		if (pvTabbox != null)
			pvTabbox.setVisible(true);

		String frontSrc = hasFront ? imgUrl(front.trim()) : "";
		String backSrc = hasBack ? imgUrl(back.trim()) : "";

		if (pvFrontImg != null)
			pvFrontImg.setSrc(frontSrc);
		if (pvBackImg != null)
			pvBackImg.setSrc(backSrc);
		if (pvGreyscaleImg != null)
			pvGreyscaleImg.setSrc(frontSrc);
		if (pvTabbox != null)
			pvTabbox.setSelectedIndex(0);
	}

	private String imgUrl(String path) {
		try {
			return "/imageServlet?path=" + URLEncoder.encode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "/imageServlet?path=" + path;
		}
	}

	// ══════════════════════════════════════════════════════════════════════════
	// LEFT — Cheque Card
	// ══════════════════════════════════════════════════════════════════════════

	private void renderChequeCard(InwardCheque c) {
		set(lblChequeBankName, "CSB Bank Limited");
		set(lblChequePresenting, "Presenting: " + nvl(c.getPresentingBankName()));
		set(lblChequeDate, c.getChequeDate() != null ? fmtDate(c.getChequeDate().toString()) : "—");
		set(lblChequePayee, "Pay: " + (c.getPayeeName() != null ? c.getPayeeName() : nvl(c.getDraweeAccountHolder())));
		set(lblChequeAmountWords, nvl(c.getAmountInWords()));
		set(lblChequeAmountBox, "₹ " + fmtAmt(c.getAmount()));
		set(lblMicrLine, buildMicrLine(c));
	}

	// ══════════════════════════════════════════════════════════════════════════
	// RIGHT — Technical Panel
	// ══════════════════════════════════════════════════════════════════════════

	private void renderTechnicalPanel(InwardCheque c) {
		set(lblTechStatus, "PENDING");
		if (lblTechStatus != null)
			lblTechStatus.setSclass("pv-badge-pending");

		String micr = c.getMicrCodeCorrected() != null ? c.getMicrCodeCorrected() : nvl(c.getMicrCodeRaw());
		set(lblCbsMicrCode, micr);
		set(lblCbsBankCode, nvl(c.getBankCode()));
		set(lblCbsOurBankCode, "700  (CSB Bank)");

		set(lblPresBank, nvl(c.getPresentingBankName()));
		set(lblPresChequeNo, nvl(c.getChequeNo()));
		set(lblPresAmount, "₹ " + fmtAmt(c.getAmount()));

		set(lblAcctNo, nvl(c.getDraweeAccountNumber()));
		set(lblAcctHolder, "Checking CBS...");
		set(lblAcctBalance, "—");

		if (lblCbsAcctValid != null) {
			lblCbsAcctValid.setValue("—");
			lblCbsAcctValid.setSclass("badge b-grey");
		}
		if (lblCbsBankMatch != null) {
			lblCbsBankMatch.setValue("—");
			lblCbsBankMatch.setSclass("badge b-grey");
		}

		fillCbsFromFirebase(c);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// CBS Firebase — background thread
	// ══════════════════════════════════════════════════════════════════════════

	private void fillCbsFromFirebase(InwardCheque cheque) {
		String accountNumber = cheque.getDraweeAccountNumber();
		String bankCode = cheque.getBankCode();
		Desktop desktop = getSelf().getDesktop();

		if (desktop == null) {
			System.err.println("fillCbsFromFirebase → desktop is null, skipping.");
			return;
		}

		new Thread(() -> {
			CbsValidationResult cbs = cbsService.validate(accountNumber, bankCode);
			boolean bankMatched = cbsService.isBankMatched(bankCode);

			if (!desktop.isAlive()) {
				System.err.println("fillCbsFromFirebase → desktop no longer alive.");
				return;
			}

			try {
				Executions.activate(desktop);
				try {
					if (cbs.isFound()) {
						set(lblAcctHolder, cbs.getAccountHolder());
						set(lblAcctBalance, "₹ " + fmtBigDecimal(cbs.getBalance()));
						boolean active = cbs.isActive();
						if (lblCbsAcctValid != null) {
							lblCbsAcctValid.setValue(active ? "Valid" : "Inactive");
							lblCbsAcctValid.setSclass(active ? "badge b-pass" : "badge b-fail");
						}
					} else {
						set(lblAcctHolder, "Not found in CBS");
						set(lblAcctBalance, "—");
						if (lblCbsAcctValid != null) {
							lblCbsAcctValid.setValue("Not Found");
							lblCbsAcctValid.setSclass("badge b-fail");
						}
					}
					if (lblCbsBankMatch != null) {
						lblCbsBankMatch.setValue(bankMatched ? "Matched" : "Mismatch");
						lblCbsBankMatch.setSclass(bankMatched ? "badge b-pass" : "badge b-fail");
					}
				} finally {
					Executions.deactivate(desktop);
				}
			} catch (Exception e) {
				System.err.println("fillCbsFromFirebase → UI update failed: " + e.getMessage());
			}
		}).start();
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Navigation
	// ══════════════════════════════════════════════════════════════════════════

	@Listen("onClick = #btnPrev")
	public void onPrev() {
		if (currentIndex > 0)
			renderCheque(currentIndex - 1);
	}

	@Listen("onClick = #btnNext")
	public void onNext() {
		if (cheques != null && currentIndex < cheques.size() - 1)
			renderCheque(currentIndex + 1);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Action Buttons
	// ══════════════════════════════════════════════════════════════════════════

	@Listen("onClick = #btnAccept")
	public void onAccept() {
		applyAction("ACCEPTED");
	}

	@Listen("onClick = #btnReturn")
	public void onReturn() {
		applyAction("RETURNED");
	}

	@Listen("onClick = #btnSendBack")
	public void onSendBack() {
		if (cheques == null || currentIndex >= cheques.size())
			return;
		pendingReferCheque = cheques.get(currentIndex);

		buildReferReasonDropdown();
		buildReferModuleDropdown();

		if (comboReferReason != null) {
			comboReferReason.setValue("");
			comboReferReason.setSelectedItem(null);
		}
		if (comboReferModule != null) {
			comboReferModule.setValue("");
			comboReferModule.setSelectedItem(null);
		}
		if (txtReferRemarks != null)
			txtReferRemarks.setValue("");
		if (lblReferReasonError != null)
			lblReferReasonError.setVisible(false);

		// Add rb-overlay-open class to show the overlay (CSS display:flex)
		if (winReferBack != null)
			winReferBack.setSclass("rb-overlay rb-overlay-open");
	}

	@Listen("onClick = #btnCancelReferBack")
	public void onCancelReferBack() {
		pendingReferCheque = null;
		// Remove rb-overlay-open to hide (CSS display:none takes over)
		if (winReferBack != null)
			winReferBack.setSclass("rb-overlay");
	}

	@Listen("onClick = #btnConfirmReferBack")
	public void onConfirmReferBack() {
		if (pendingReferCheque == null)
			return;

		Comboitem selectedReason = (comboReferReason != null) ? comboReferReason.getSelectedItem() : null;
		Comboitem selectedModule = (comboReferModule != null) ? comboReferModule.getSelectedItem() : null;

		if (selectedReason == null) {
			showReferError("Please select a reason for referring back.");
			return;
		}
		if (selectedModule == null) {
			showReferError("Please select which module to send the cheque back to.");
			return;
		}

		String reasonCode = (String) selectedReason.getValue();
		String moduleCode = (String) selectedModule.getValue();
		String remarks = (txtReferRemarks != null) ? txtReferRemarks.getValue().trim() : "";
		String moduleName = selectedModule.getLabel();

		LoginDTO userDto = (LoginDTO) Sessions.getCurrent().getAttribute(SessionUtil.SESSION_KEY);
		if (userDto == null) {
			showReferError("Session expired. Please log in again.");
			return;
		}

		User checker = new User();
		checker.setId(userDto.getUserId());

		try {
			// Step 1: Save SEND_BACK action + update cheque status in DB
			batchService.confirmReferBack(currentBatch, pendingReferCheque, reasonCode, moduleCode, remarks, checker);

			// Step 2: Decrement total_cheques count in DB
			batchService.decrementBatchChequeCount(currentBatch);

		} catch (Exception e) {
			showReferError("Failed to save: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		// Step 3: Remove cheque from the in-memory working list
		InwardCheque referred = pendingReferCheque;
		cheques.remove(referred);
		actionMap.remove(referred.getId());
		reasonMap.remove(referred.getId());
		pendingReferCheque = null;

		// Step 4: Adjust currentIndex so navigation stays correct after removal
		if (currentIndex >= cheques.size()) {
			currentIndex = cheques.size() - 1;
		}

		// Step 5: Update the summary bar total count
		set(lblTotalCheques, String.valueOf(currentBatch.getTotalCheques()));

		// Step 6: Close the popup
		if (winReferBack != null)
			winReferBack.setSclass("rb-overlay");

		// Step 7: Refresh progress bar with the reduced list
		refreshProgress();

		// Step 8: Show confirmation then move to next cheque
		Messagebox.show(
				"Cheque " + referred.getChequeNo() + " referred back to: " + moduleName
						+ "\nRemaining cheques in this batch: " + cheques.size(),
				"Refer Back Confirmed", Messagebox.OK, Messagebox.INFORMATION, ev -> {
					if (cheques.isEmpty()) {
						Messagebox.show("All cheques have been referred back. Returning to batch list.",
								"Batch Complete", Messagebox.OK, Messagebox.INFORMATION, e2 -> navigateBack());
					} else if (currentIndex >= 0 && currentIndex < cheques.size()) {
						renderCheque(currentIndex);
					} else {
						renderCheque(cheques.size() - 1);
					}
				});
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Confirm Return
	// ══════════════════════════════════════════════════════════════════════════

	@Listen("onClick = #btnConfirmReturn")
	public void onConfirmReturn() {
		if (cheques == null || currentIndex >= cheques.size())
			return;

		Comboitem selected = (comboReturnReason != null) ? comboReturnReason.getSelectedItem() : null;

		if (selected == null || selected.getValue() == null || selected.getValue().toString().trim().isEmpty()) {
			showReturnReasonError("Please select a return reason before confirming.");
			return;
		}

		hideReturnReasonError();

		String reasonCode = (String) selected.getValue();
		InwardCheque cheque = cheques.get(currentIndex);
		reasonMap.put(cheque.getId(), reasonCode);

		LoginDTO userDto = (LoginDTO) Sessions.getCurrent().getAttribute(SessionUtil.SESSION_KEY);
		if (userDto == null) {
			Messagebox.show("Session expired. Please log in again.", "Session Error", Messagebox.OK, Messagebox.ERROR);
			return;
		}

		User checker = new User();
		checker.setId(userDto.getUserId());

		try {
			batchService.confirmReturn(currentBatch, cheque, reasonCode, checker);
			savedViaConfirmReturn.add(cheque.getId());
		} catch (IllegalArgumentException e) {
			showReturnReasonError(e.getMessage());
			return;
		} catch (Exception e) {
			Messagebox.show("Failed to save return: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
			e.printStackTrace();
			return;
		}

		actionMap.put(cheque.getId(), "RETURNED");
		refreshProgress();

		Messagebox.show("Cheque " + (currentIndex + 1) + " marked as RETURNED. Reason: " + selected.getLabel(),
				"Return Confirmed", Messagebox.OK, Messagebox.INFORMATION, ev -> {
					if (currentIndex < cheques.size() - 1)
						renderCheque(currentIndex + 1);
				});
	}

	private void applyAction(String action) {
		if (cheques == null || currentIndex >= cheques.size())
			return;

		Long id = cheques.get(currentIndex).getId();
		actionMap.put(id, action);

		if (!"RETURNED".equals(action)) {
			reasonMap.remove(id);
			if (comboReturnReason != null) {
				comboReturnReason.setValue("");
				comboReturnReason.setSelectedItem(null);
			}
		}

		highlightActionButtons(action);
		toggleReturnReasonBox(id);
		refreshProgress();

		if (!"RETURNED".equals(action) && currentIndex < cheques.size() - 1)
			renderCheque(currentIndex + 1);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Dropdowns
	// ══════════════════════════════════════════════════════════════════════════

	private void buildReturnReasonDropdown() {
		if (comboReturnReason == null)
			return;
		comboReturnReason.getItems().clear();

		Map<String, String> reasons = InwardReturnReason.getReasonDropdownMap();
		for (Map.Entry<String, String> entry : reasons.entrySet()) {
			Comboitem item = comboReturnReason.appendItem(entry.getValue());
			item.setValue(entry.getKey());
		}

		comboReturnReason.addEventListener("onSelect", ev -> {
			Comboitem sel = comboReturnReason.getSelectedItem();
			if (sel != null && cheques != null && currentIndex < cheques.size())
				reasonMap.put(cheques.get(currentIndex).getId(), (String) sel.getValue());
			hideReturnReasonError();
		});
	}

	private void buildReferReasonDropdown() {
		if (comboReferReason == null)
			return;
		comboReferReason.getItems().clear();

		String[][] reasons = { { "11", "MICR Defect / Unreadable" }, { "12", "Amount in Words and Figures Differ" },
				{ "02", "Signature Mismatch" }, { "05", "Cheque Altered / Tampered" },
				{ "13", "Cheque Image Not Legible" }, { "14", "Instrument Mutilated" }, { "07", "Post Dated Cheque" },
				{ "06", "Stale Cheque" }, { "10", "Refer to Drawer" }, { "01", "Funds Insufficient" } };

		for (String[] r : reasons) {
			Comboitem item = comboReferReason.appendItem(r[0] + " — " + r[1]);
			item.setValue(r[0]);
		}
	}

	private void buildReferModuleDropdown() {
		if (comboReferModule == null)
			return;
		comboReferModule.getItems().clear();

		String[][] modules = { { "MICR_REPAIR", "1. Reject and Repair (MICR Repair)" },
				{ "DATE_AMOUNT", "2. Date and Amount" }, { "PAYEE_ACCOUNT", "3. Payee and Account" } };

		for (String[] m : modules) {
			Comboitem item = comboReferModule.appendItem(m[1]);
			item.setValue(m[0]);
		}
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Progress
	// ══════════════════════════════════════════════════════════════════════════

	private void refreshProgress() {
	    if (cheques == null) return;

	    int total    = cheques.size();
	    int actioned = 0;
	    for (InwardCheque c : cheques) {
	        if (actionMap.containsKey(c.getId())) actioned++;
	    }

	    int pct   = total > 0 ? actioned * 100 / total : 100;
	    String text = actioned + " of " + total + " cheques actioned";

	    set(lblProgressText,   text);
	    set(lblFooterProgress, text);

	    if (divProgressFill != null)
	        divProgressFill.setStyle("width:" + pct + "%");

	    // Disable submit if batch is CheckerReferred regardless of actioned count
	    boolean isReferred = currentBatch != null &&
	                         "CheckerReferred".equals(currentBatch.getStatus());

	    if (btnSubmit != null)
	        btnSubmit.setDisabled(total == 0 || actioned < total || isReferred);
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Submit
	// ══════════════════════════════════════════════════════════════════════════

	@Listen("onClick = #btnSubmit")
	public void onSubmitBatch() {
		LoginDTO user = (LoginDTO) Sessions.getCurrent().getAttribute(SessionUtil.SESSION_KEY);
		if ("CheckerReferred".equals(currentBatch.getStatus())) {
	        Messagebox.show(
	            "This batch cannot be submitted.\n\n" +
	            "One or more cheques have been referred back to the Maker for correction.\n" +
	            "Please wait until the Maker corrects the referred cheque(s) and " +
	            "resubmits the batch with status MakerVerified.",
	            "Submit Blocked", Messagebox.OK, Messagebox.EXCLAMATION
	        );
	        return;
	    }

		User checker = new User();
		checker.setId(user.getUserId());

		List<InwardCheckerAction> actions = new ArrayList<>();
		for (InwardCheque c : cheques) {
		    String action = actionMap.get(c.getId());
		    if ("SEND_BACK".equals(action)) continue;
		    
		    // Skip cheques already individually saved via confirmReturn
		    if (savedViaConfirmReturn.contains(c.getId())) continue;
		    
		    InwardCheckerAction ca = new InwardCheckerAction();
		    ca.setInwardCheque(c);
		    ca.setAction(action);
		    if ("RETURNED".equals(action))
		        ca.setReasonCode(reasonMap.get(c.getId()));
		    actions.add(ca);
		}

		try {
			batchService.submitBatch(currentBatch, actions, checker);
			Messagebox.show("Batch " + currentBatch.getBatchId() + " submitted successfully.", "Success", Messagebox.OK,
					Messagebox.INFORMATION, ev -> navigateBack());
		} catch (IllegalArgumentException e) {
			Messagebox.show(e.getMessage(), "Validation Error", Messagebox.OK, Messagebox.EXCLAMATION);
		} catch (Exception e) {
			Messagebox.show("Submit failed: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
			e.printStackTrace();
		}
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Navigation Back
	// ══════════════════════════════════════════════════════════════════════════

	@Listen("onClick = #btnBackToList")
	public void onBackToList() {
		navigateBack();
	}

	@Listen("onClick = #btnCancel")
	public void onCancel() {
		navigateBack();
	}

	private void navigateBack() {
		Executions.getCurrent().sendRedirect("/inward/inwardChecker/inwardCheckerVerification.zul");
	}

	// ══════════════════════════════════════════════════════════════════════════
	// Helpers
	// ══════════════════════════════════════════════════════════════════════════

	private void restoreActionState(Long id) {
		String action = actionMap.get(id);
		highlightActionButtons(action);
		toggleReturnReasonBox(id);

		if ("RETURNED".equals(action) && comboReturnReason != null) {
			String code = reasonMap.get(id);
			if (code != null) {
				for (Comboitem item : comboReturnReason.getItems()) {
					if (code.equals(item.getValue())) {
						comboReturnReason.setSelectedItem(item);
						break;
					}
				}
			}
		}
	}

	private void highlightActionButtons(String action) {
		if (btnAccept != null)
			btnAccept.setSclass("ACCEPTED".equals(action) ? "btn bs btn-on" : "btn bs");
		if (btnReturn != null)
			btnReturn.setSclass("RETURNED".equals(action) ? "btn bd btn-on" : "btn bd");
		if (btnSendBack != null)
			btnSendBack.setSclass("SEND_BACK".equals(action) ? "btn bo btn-on" : "btn bo");
	}

	private void toggleReturnReasonBox(Long id) {
		boolean show = "RETURNED".equals(actionMap.get(id));
		if (divReturnReasonBox != null)
			divReturnReasonBox.setVisible(show);
		if (!show)
			hideReturnReasonError();
	}

	private String buildMicrLine(InwardCheque c) {
		String no = nvl(c.getChequeNo());
		String micr = c.getMicrCodeCorrected() != null ? c.getMicrCodeCorrected() : nvl(c.getMicrCodeRaw());
		return "«" + no + "«   " + micr + "«   —" + c.getSeqNo() + "—";
	}

	private String fmtDate(String iso) {
		if (iso == null || iso.length() < 10)
			return iso != null ? iso : "—";
		try {
			String[] p = iso.split("-");
			if (p.length == 3)
				return p[2] + "/" + p[1] + "/" + p[0];
		} catch (Exception ignored) {
		}
		return iso;
	}

	private String fmtAmt(BigDecimal bd) {
		if (bd == null)
			return "0.00";
		return fmtBigDecimal(bd);
	}

	private String fmtBigDecimal(BigDecimal bd) {
		try {
			NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
			nf.setMinimumFractionDigits(2);
			nf.setMaximumFractionDigits(2);
			return nf.format(bd);
		} catch (Exception e) {
			return bd.toPlainString();
		}
	}

	private String nvl(String s) {
		return (s != null && !s.trim().isEmpty()) ? s : "—";
	}

	private void set(Label lbl, String val) {
		if (lbl != null)
			lbl.setValue(val != null ? val : "—");
	}

	private void showReturnReasonError(String message) {
		if (lblReturnReasonError != null) {
			lblReturnReasonError.setValue(message);
			lblReturnReasonError.setVisible(true);
		}
	}

	private void hideReturnReasonError() {
		if (lblReturnReasonError != null) {
			lblReturnReasonError.setValue("");
			lblReturnReasonError.setVisible(false);
		}
	}

	private void showReferError(String message) {
		if (lblReferReasonError != null) {
			lblReferReasonError.setValue(message);
			lblReferReasonError.setVisible(true);
		}
	}
}
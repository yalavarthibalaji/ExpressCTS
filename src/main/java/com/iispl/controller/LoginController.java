package com.iispl.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

import com.iispl.model.UserModel;
import com.iispl.service.AuthService;

/**
 * LoginController.java ZK MVC Composer — controls login.zul
 *
 * Phase 1 Change: AuthService now authenticates against the Supabase DB instead
 * of the hardcoded UserUtil list. No changes needed here except adding the
 * "admin" redirect.
 */
public class LoginController extends SelectorComposer<Component> {

	@Wire("#txtUserId")
	private Textbox txtUserId;
	@Wire("#txtPassword")
	private Textbox txtPassword;

	@Wire("#detectedRoleWrap")
	private Div detectedRoleWrap;
	@Wire("#detectedRoleBox")
	private Div detectedRoleBox;
	@Wire("#detectedRoleIcon")
	private Label detectedRoleIcon;
	@Wire("#detectedRoleLabel")
	private Label detectedRoleLabel;

	@Wire("#loginError")
	private Div loginError;
	@Wire("#loginErrorText")
	private Label loginErrorText;

	// Phase 1: AuthService now queries the DB
	private final AuthService authService = new AuthService();

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		loginError.setVisible(false);
		detectedRoleWrap.setVisible(false);
		txtUserId.setFocus(true);
	}

	// ── Role auto-detection on every keystroke ───────────────────────
	@Listen("onChanging = #txtUserId; onChanging = #txtPassword")
	public void onCredentialChange(Event event) {
		loginError.setVisible(false);

		InputEvent ie = (InputEvent) event;
		String typedNow = ie.getValue() == null ? "" : ie.getValue().trim();

		String userId, password;
		if (event.getTarget() == txtUserId) {
			userId = typedNow;
			password = txtPassword.getValue().trim();
		} else {
			userId = txtUserId.getValue().trim();
			password = typedNow;
		}

		// This now hits the DB to detect role
		UserModel detected = authService.detectRole(userId, password);

		if (detected != null) {
			detectedRoleWrap.setVisible(true);
			detectedRoleIcon.setValue(detected.getRoleIcon());
			detectedRoleLabel.setValue(detected.getRoleLabel());
			detectedRoleBox.setSclass("cts-detected-role cts-detected-role-matched");
		} else {
			detectedRoleWrap.setVisible(false);
			detectedRoleBox.setSclass("cts-detected-role");
		}
	}

	// ── Login button ─────────────────────────────────────────────────
	@Listen("onClick = #btnSignIn; onOK = #txtPassword")
	public void doLogin(Event event) {
		String userId = txtUserId.getValue().trim();
		String password = txtPassword.getValue().trim();

		if (userId.isEmpty() || password.isEmpty()) {
			showError("Please enter your User ID and Password.");
			return;
		}

		// Phase 1: authenticate() now queries Supabase DB + BCrypt verify
		UserModel user = authService.authenticate(userId, password);

		if (user == null) {
			showError("Invalid User ID or Password. Please try again.");
			txtPassword.setValue("");
			txtPassword.setFocus(true);
			detectedRoleWrap.setVisible(false);
			return;
		}

		// Store user in ZK session
		Sessions.getCurrent().setAttribute("loggedInUser", user);

		// ── Role-based redirect ──────────────────────────────────────
		String roleId = user.getRoleId();
		switch (roleId) {
		case "checker":
			Executions.sendRedirect("/zul/checkerDashboard.zul");
			break;
		case "admin":
			// Phase 1: CTS Admin goes to admin dashboard (Phase 2 will build this ZUL)
			// For now redirect to makerDashboard until adminDashboard.zul is created
			Executions.sendRedirect("/zul/adminDashboard.zul");
			break;
		default:
			// maker, supervisor, micr_repair → maker dashboard
			Executions.sendRedirect("/zul/makerDashboard.zul");
			break;
		}
	}

	private void showError(String message) {
		loginErrorText.setValue(message);
		loginError.setVisible(true);
	}
}
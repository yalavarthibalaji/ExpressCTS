package com.iispl.composer;

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
 * Fix: onChanging uses InputEvent.getValue() for the currently-typed field, and
 * reads the committed value from the OTHER field via .getValue(). This ensures
 * role detection works on every keystroke.
 */
public class LoginComposer extends SelectorComposer<Component> {

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

	private final AuthService authService = new AuthService();

	// ── Lifecycle ────────────────────────────────────────────────
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		loginError.setVisible(false);
		detectedRoleWrap.setVisible(false);
		txtUserId.setFocus(true);
	}

	// ── Role auto-detection on every keystroke ───────────────────
	/**
	 * KEY FIX: onChanging fires BEFORE the textbox value is committed. Cast to
	 * InputEvent and call getValue() to get what was JUST typed. Read the OTHER
	 * field with .getValue() (already committed).
	 */
	@Listen("onChanging = #txtUserId; onChanging = #txtPassword")
	public void onCredentialChange(Event event) {
		loginError.setVisible(false);

		InputEvent ie = (InputEvent) event;
		String typedNow = ie.getValue() == null ? "" : ie.getValue().trim();

		// Figure out which field fired, read the other from committed value
		String userId, password;
		if (event.getTarget() == txtUserId) {
			userId = typedNow;
			password = txtPassword.getValue().trim();
		} else {
			userId = txtUserId.getValue().trim();
			password = typedNow;
		}

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

	// ── Login ────────────────────────────────────────────────────
	@Listen("onClick = #btnSignIn; onOK = #txtPassword")
	public void doLogin(Event event) {
		String userId = txtUserId.getValue().trim();
		String password = txtPassword.getValue().trim();

		if (userId.isEmpty() || password.isEmpty()) {
			showError("Please enter your User ID and Password.");
			return;
		}

		UserModel user = authService.authenticate(userId, password);

		if (user == null) {
			showError("Invalid User ID or Password. Please try again.");
			txtPassword.setValue("");
			txtPassword.setFocus(true);
			detectedRoleWrap.setVisible(false);
			return;
		}

		Sessions.getCurrent().setAttribute("loggedInUser", user);
		Executions.sendRedirect("/zul/maker/makerDashboard.zul");
	}

	// ── Helper ───────────────────────────────────────────────────
	private void showError(String message) {
		loginErrorText.setValue(message);
		loginError.setVisible(true);
	}
}
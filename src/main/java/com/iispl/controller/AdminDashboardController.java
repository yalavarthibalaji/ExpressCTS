package com.iispl.controller;

import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserModel;
import com.iispl.service.UserManagementService;
import com.iispl.serviceImpl.UserManagementServiceImpl;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import java.util.List;

public class AdminDashboardController extends SelectorComposer<Component> {

	// ── Logged in admin user ──────────────────────────────────────────
	private UserModel loggedInAdmin;

	// ── Service ───────────────────────────────────────────────────────
	private UserManagementService userMgmtService = new UserManagementServiceImpl();

	// ── Add User form fields ──────────────────────────────────────────
	@Wire("#txtNewUserId")
	private Textbox txtNewUserId;
	@Wire("#txtNewFullName")
	private Textbox txtNewFullName;
	@Wire("#txtNewBranchCode")
	private Textbox txtNewBranchCode;
	@Wire("#txtNewInitial")
	private Textbox txtNewInitial;
	@Wire("#cmbNewRole")
	private Combobox cmbNewRole;
	@Wire("#lblAddUserMsg")
	private Label lblAddUserMsg;

	// ── Modify User form fields ───────────────────────────────────────
	@Wire("#txtModifyUserId")
	private Textbox txtModifyUserId;
	@Wire("#txtModifyFullName")
	private Textbox txtModifyFullName;
	@Wire("#txtModifyBranchCode")
	private Textbox txtModifyBranchCode;
	@Wire("#txtModifyInitial")
	private Textbox txtModifyInitial;
	@Wire("#lblModifyMsg")
	private Label lblModifyMsg;

	// ── Enable/Disable fields ─────────────────────────────────────────
	@Wire("#txtEnableUserId")
	private Textbox txtEnableUserId;
	@Wire("#lblEnableMsg")
	private Label lblEnableMsg;
	@Wire("#txtDisableUserId")
	private Textbox txtDisableUserId;
	@Wire("#lblDisableMsg")
	private Label lblDisableMsg;

	// ── Role mapping fields ───────────────────────────────────────────
	@Wire("#txtRoleUserId")
	private Textbox txtRoleUserId;
	@Wire("#cmbRoleSelect")
	private Combobox cmbRoleSelect;
	@Wire("#lblRoleMsg")
	private Label lblRoleMsg;

	// ── Reset password fields ─────────────────────────────────────────
	@Wire("#txtResetUserId")
	private Textbox txtResetUserId;
	@Wire("#txtNewPassword")
	private Textbox txtNewPassword;
	@Wire("#lblResetMsg")
	private Label lblResetMsg;

	// ── User list ─────────────────────────────────────────────────────
	@Wire("#lstUsers")
	private Listbox lstUsers;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		// check session
		loggedInAdmin = (UserModel) Sessions.getCurrent().getAttribute("loggedInUser");

		if (loggedInAdmin == null || !"admin".equals(loggedInAdmin.getRoleId())) {
			Executions.sendRedirect("/zul/login.zul");
			return;
		}

		// load roles into dropdowns
		loadRolesIntoCombobox(cmbNewRole);
		loadRolesIntoCombobox(cmbRoleSelect);

		// load user list
		loadUserList();
	}

	// ── Add New User ──────────────────────────────────────────────────

	@Listen("onClick = #btnAddUser")
	public void onAddUser(Event event) {
		String userId = txtNewUserId.getValue().trim();
		String fullName = txtNewFullName.getValue().trim();
		String branchCode = txtNewBranchCode.getValue().trim();
		String initial = txtNewInitial.getValue().trim();
		String roleCode = getSelectedRoleCode(cmbNewRole);

		// basic validation
		if (userId.isEmpty() || fullName.isEmpty() || branchCode.isEmpty() || roleCode == null) {
			lblAddUserMsg.setValue("Please fill all required fields.");
			lblAddUserMsg.setSclass("cts-msg-error");
			return;
		}

		try {
			userMgmtService.addUser(userId, fullName, branchCode, initial, roleCode, loggedInAdmin.getUserId());

			lblAddUserMsg.setValue("User '" + userId + "' created successfully. Default password: Test@123");
			lblAddUserMsg.setSclass("cts-msg-success");

			// clear form
			txtNewUserId.setValue("");
			txtNewFullName.setValue("");
			txtNewBranchCode.setValue("");
			txtNewInitial.setValue("");
			cmbNewRole.setValue("");

			// refresh user list
			loadUserList();

		} catch (Exception e) {
			lblAddUserMsg.setValue("Error: " + e.getMessage());
			lblAddUserMsg.setSclass("cts-msg-error");
		}
	}

	// ── Load User for Modify ──────────────────────────────────────────

	@Listen("onClick = #btnLoadModifyUser")
	public void onLoadModifyUser(Event event) {
		String userId = txtModifyUserId.getValue().trim();
		if (userId.isEmpty()) {
			lblModifyMsg.setValue("Please enter a User ID.");
			return;
		}

		SystemUser user = userMgmtService.findByUserId(userId);
		if (user == null) {
			lblModifyMsg.setValue("User not found: " + userId);
			lblModifyMsg.setSclass("cts-msg-error");
			return;
		}

		txtModifyFullName.setValue(user.getFullName());
		txtModifyBranchCode.setValue(user.getBranchCode());
		txtModifyInitial.setValue(user.getInitial() != null ? user.getInitial() : "");
		lblModifyMsg.setValue("User loaded. Edit and save.");
		lblModifyMsg.setSclass("cts-msg-info");
	}

	// ── Save Modified User ────────────────────────────────────────────

	@Listen("onClick = #btnSaveModifyUser")
	public void onSaveModifyUser(Event event) {
		String userId = txtModifyUserId.getValue().trim();
		String fullName = txtModifyFullName.getValue().trim();
		String branchCode = txtModifyBranchCode.getValue().trim();
		String initial = txtModifyInitial.getValue().trim();

		if (userId.isEmpty() || fullName.isEmpty() || branchCode.isEmpty()) {
			lblModifyMsg.setValue("Please fill all required fields.");
			lblModifyMsg.setSclass("cts-msg-error");
			return;
		}

		try {
			userMgmtService.updateUser(userId, fullName, branchCode, initial, loggedInAdmin.getUserId());

			lblModifyMsg.setValue("User '" + userId + "' updated successfully.");
			lblModifyMsg.setSclass("cts-msg-success");
			loadUserList();

		} catch (Exception e) {
			lblModifyMsg.setValue("Error: " + e.getMessage());
			lblModifyMsg.setSclass("cts-msg-error");
		}
	}

	// ── Enable User ───────────────────────────────────────────────────

	@Listen("onClick = #btnEnableUser")
	public void onEnableUser(Event event) {
		String userId = txtEnableUserId.getValue().trim();
		if (userId.isEmpty()) {
			lblEnableMsg.setValue("Please enter a User ID.");
			return;
		}

		try {
			userMgmtService.enableUser(userId, loggedInAdmin.getUserId());
			lblEnableMsg.setValue("User '" + userId + "' enabled successfully.");
			lblEnableMsg.setSclass("cts-msg-success");
			loadUserList();
		} catch (Exception e) {
			lblEnableMsg.setValue("Error: " + e.getMessage());
			lblEnableMsg.setSclass("cts-msg-error");
		}
	}

	// ── Disable User ──────────────────────────────────────────────────

	@Listen("onClick = #btnDisableUser")
	public void onDisableUser(Event event) {
		String userId = txtDisableUserId.getValue().trim();
		if (userId.isEmpty()) {
			lblDisableMsg.setValue("Please enter a User ID.");
			return;
		}

		// prevent admin from disabling themselves
		if (userId.equals(loggedInAdmin.getUserId())) {
			lblDisableMsg.setValue("You cannot disable your own account.");
			lblDisableMsg.setSclass("cts-msg-error");
			return;
		}

		try {
			userMgmtService.disableUser(userId, loggedInAdmin.getUserId());
			lblDisableMsg.setValue("User '" + userId + "' disabled successfully.");
			lblDisableMsg.setSclass("cts-msg-success");
			loadUserList();
		} catch (Exception e) {
			lblDisableMsg.setValue("Error: " + e.getMessage());
			lblDisableMsg.setSclass("cts-msg-error");
		}
	}

	// ── Change Role ───────────────────────────────────────────────────

	@Listen("onClick = #btnChangeRole")
	public void onChangeRole(Event event) {
		String userId = txtRoleUserId.getValue().trim();
		String roleCode = getSelectedRoleCode(cmbRoleSelect);

		if (userId.isEmpty() || roleCode == null) {
			lblRoleMsg.setValue("Please enter User ID and select a role.");
			lblRoleMsg.setSclass("cts-msg-error");
			return;
		}

		try {
			userMgmtService.changeUserRole(userId, roleCode, loggedInAdmin.getUserId());
			lblRoleMsg.setValue("Role updated for '" + userId + "' to: " + roleCode);
			lblRoleMsg.setSclass("cts-msg-success");
			loadUserList();
		} catch (Exception e) {
			lblRoleMsg.setValue("Error: " + e.getMessage());
			lblRoleMsg.setSclass("cts-msg-error");
		}
	}

	// ── Reset Password ────────────────────────────────────────────────

	@Listen("onClick = #btnResetPassword")
	public void onResetPassword(Event event) {
		String userId = txtResetUserId.getValue().trim();
		String newPassword = txtNewPassword.getValue().trim();

		if (userId.isEmpty() || newPassword.isEmpty()) {
			lblResetMsg.setValue("Please enter User ID and new password.");
			lblResetMsg.setSclass("cts-msg-error");
			return;
		}

		if (newPassword.length() < 6) {
			lblResetMsg.setValue("Password must be at least 6 characters.");
			lblResetMsg.setSclass("cts-msg-error");
			return;
		}

		try {
			userMgmtService.resetPassword(userId, newPassword, loggedInAdmin.getUserId());
			lblResetMsg.setValue("Password reset successfully for: " + userId);
			lblResetMsg.setSclass("cts-msg-success");
			txtNewPassword.setValue("");
		} catch (Exception e) {
			lblResetMsg.setValue("Error: " + e.getMessage());
			lblResetMsg.setSclass("cts-msg-error");
		}
	}

	// ── Logout ────────────────────────────────────────────────────────

	@Listen("onClick = #btnLogout")
	public void onLogout(Event event) {
		Sessions.getCurrent().invalidate();
		Executions.sendRedirect("/zul/login.zul");
	}

	// ── Helper: Load roles into combobox ──────────────────────────────

	private void loadRolesIntoCombobox(Combobox combobox) {
		combobox.getItems().clear();
		List<SystemRole> roles = userMgmtService.getAllRoles();
		for (SystemRole role : roles) {
			Comboitem item = combobox.appendItem(role.getRoleLabel());
			item.setValue(role.getRoleCode());
		}
	}

	// ── Helper: Get selected role code from combobox ──────────────────

	private String getSelectedRoleCode(Combobox combobox) {
		Comboitem selected = combobox.getSelectedItem();
		if (selected == null)
			return null;
		return selected.getValue();
	}

	// ── Helper: Load all users into listbox ───────────────────────────

	private void loadUserList() {
		lstUsers.getItems().clear();
		List<SystemUser> users = userMgmtService.getAllUsers();

		for (SystemUser user : users) {
			String roleCode = userMgmtService.getRoleCodeForUser(user.getUserId());

			Listitem item = new Listitem();
			item.appendChild(new Listcell(user.getUserId()));
			item.appendChild(new Listcell(user.getFullName()));
			item.appendChild(new Listcell(user.getBranchCode()));
			item.appendChild(new Listcell(roleCode != null ? roleCode.toUpperCase() : "NO ROLE"));
			item.appendChild(new Listcell(Boolean.TRUE.equals(user.getActive()) ? "Active" : "Inactive"));
			lstUsers.appendChild(item);
		}
	}
}
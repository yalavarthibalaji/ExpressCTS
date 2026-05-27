package com.iispl.controller;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import com.iispl.dto.UserModel;
import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRequest;
import com.iispl.service.UserManagementService;
import com.iispl.serviceImpl.UserManagementServiceImpl;

public class AdminDashboardController extends SelectorComposer<Component> {

    // ── Logged-in admin ───────────────────────────────────────────────
    private UserModel loggedInAdmin;

    // ── Service ───────────────────────────────────────────────────────
    private UserManagementService userMgmtService = new UserManagementServiceImpl();

    // ── Top bar ───────────────────────────────────────────────────────
    @Wire("#lblLoggedInUser") private Label lblLoggedInUser;

    // ── Sidebar menu items ────────────────────────────────────────────
    @Wire("#menuAddUser")     private Div menuAddUser;
    @Wire("#menuDeleteUser")  private Div menuDeleteUser;
    @Wire("#menuEnableUser")  private Div menuEnableUser;
    @Wire("#menuDisableUser") private Div menuDisableUser;
    @Wire("#menuUpdateUser")  private Div menuUpdateUser;
    @Wire("#menuChangeRole")  private Div menuChangeRole;
    @Wire("#menuResetPwd")    private Div menuResetPwd;
    @Wire("#menuAllUsers")    private Div menuAllUsers;
    @Wire("#menuUserRequest") private Div menuUserRequest;

    // ── Content panels ────────────────────────────────────────────────
    @Wire("#panelAddUser")     private Div panelAddUser;
    @Wire("#panelDeleteUser")  private Div panelDeleteUser;
    @Wire("#panelEnableUser")  private Div panelEnableUser;
    @Wire("#panelDisableUser") private Div panelDisableUser;
    @Wire("#panelUpdateUser")  private Div panelUpdateUser;
    @Wire("#panelChangeRole")  private Div panelChangeRole;
    @Wire("#panelResetPwd")    private Div panelResetPwd;
    @Wire("#panelAllUsers")    private Div panelAllUsers;
    @Wire("#panelUserRequest") private Div panelUserRequest;

    // ── 1. Add User ───────────────────────────────────────────────────
    @Wire("#txtNewUserId")     private Textbox  txtNewUserId;
    @Wire("#txtNewFullName")   private Textbox  txtNewFullName;
    @Wire("#txtNewBranchCode") private Textbox  txtNewBranchCode;
    @Wire("#txtNewInitial")    private Textbox  txtNewInitial;
    @Wire("#cmbNewRole")       private Combobox cmbNewRole;
    @Wire("#lblAddUserMsg")    private Label    lblAddUserMsg;

    // ── 2. Delete User ────────────────────────────────────────────────
    @Wire("#txtDeleteUserId")  private Textbox txtDeleteUserId;
    @Wire("#lblDeleteUserMsg") private Label   lblDeleteUserMsg;

    // ── 3. Enable User ────────────────────────────────────────────────
    @Wire("#txtEnableUserId") private Textbox txtEnableUserId;
    @Wire("#lblEnableMsg")    private Label   lblEnableMsg;
    @Wire("#lstEnabledUsers") private Listbox lstEnabledUsers;

    // ── 4. Disable User ───────────────────────────────────────────────
    @Wire("#txtDisableUserId") private Textbox txtDisableUserId;
    @Wire("#lblDisableMsg")    private Label   lblDisableMsg;
    @Wire("#lstDisabledUsers") private Listbox lstDisabledUsers;

    // ── 5. Update User ────────────────────────────────────────────────
    @Wire("#txtModifyUserId")     private Textbox txtModifyUserId;
    @Wire("#txtModifyFullName")   private Textbox txtModifyFullName;
    @Wire("#txtModifyBranchCode") private Textbox txtModifyBranchCode;
    @Wire("#txtModifyInitial")    private Textbox txtModifyInitial;
    @Wire("#lblModifyMsg")        private Label   lblModifyMsg;

    // ── 6. Change Role ────────────────────────────────────────────────
    @Wire("#txtRoleUserId") private Textbox  txtRoleUserId;
    @Wire("#cmbRoleSelect") private Combobox cmbRoleSelect;
    @Wire("#lblRoleMsg")    private Label    lblRoleMsg;

    // ── 7. Reset Password ─────────────────────────────────────────────
    @Wire("#txtResetUserId") private Textbox txtResetUserId;
    @Wire("#txtNewPassword") private Textbox txtNewPassword;
    @Wire("#lblResetMsg")    private Label   lblResetMsg;

    // ── 8. Show All Users ─────────────────────────────────────────────
    @Wire("#lstAllUsers")      private Listbox lstAllUsers;
    @Wire("#lblCountTotal")    private Label   lblCountTotal;
    @Wire("#lblCountActive")   private Label   lblCountActive;
    @Wire("#lblCountInactive") private Label   lblCountInactive;

    // ── 9. User Request ───────────────────────────────────────────────
    @Wire("#lstRequests")   private Listbox lstRequests;
    @Wire("#lblRequestMsg") private Label   lblRequestMsg;

    // ── Arrays for show/hide ──────────────────────────────────────────
    private Div[] allPanels;
    private Div[] allMenuItems;

    // ─────────────────────────────────────────────────────────────────
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        loggedInAdmin = (UserModel) Sessions.getCurrent().getAttribute("loggedInUser");
        if (loggedInAdmin == null || !"admin".equals(loggedInAdmin.getRoleId())) {
            Executions.sendRedirect("/login.zul");
            return;
        }

        //lblLoggedInUser.setValue("Admin: " + loggedInAdmin.getFullName());

        allPanels = new Div[]{
            panelAddUser, panelDeleteUser, panelEnableUser, panelDisableUser,
            panelUpdateUser, panelChangeRole, panelResetPwd, panelAllUsers, panelUserRequest
        };
        allMenuItems = new Div[]{
            menuAddUser, menuDeleteUser, menuEnableUser, menuDisableUser,
            menuUpdateUser, menuChangeRole, menuResetPwd, menuAllUsers, menuUserRequest
        };

        loadRolesIntoCombobox(cmbNewRole);
        loadRolesIntoCombobox(cmbRoleSelect);

        showPanel(panelAddUser, menuAddUser);
    }

    // ── Sidebar menu handlers ─────────────────────────────────────────

    @Listen("onClick = #menuAddUser")
    public void onMenuAddUser()    { showPanel(panelAddUser,    menuAddUser);    }

    @Listen("onClick = #menuDeleteUser")
    public void onMenuDeleteUser() { showPanel(panelDeleteUser, menuDeleteUser); }

    @Listen("onClick = #menuEnableUser")
    public void onMenuEnableUser() {
        showPanel(panelEnableUser, menuEnableUser);
        loadEnabledUsers();
    }

    @Listen("onClick = #menuDisableUser")
    public void onMenuDisableUser() {
        showPanel(panelDisableUser, menuDisableUser);
        loadDisabledUsers();
    }

    @Listen("onClick = #menuUpdateUser")
    public void onMenuUpdateUser()  { showPanel(panelUpdateUser,  menuUpdateUser);  }

    @Listen("onClick = #menuChangeRole")
    public void onMenuChangeRole()  { showPanel(panelChangeRole,  menuChangeRole);  }

    @Listen("onClick = #menuResetPwd")
    public void onMenuResetPwd()    { showPanel(panelResetPwd,    menuResetPwd);    }

    @Listen("onClick = #menuAllUsers")
    public void onMenuAllUsers() {
        showPanel(panelAllUsers, menuAllUsers);
        loadAllUsersWithCount();
    }

    @Listen("onClick = #menuUserRequest")
    public void onMenuUserRequest() {
        showPanel(panelUserRequest, menuUserRequest);
        loadPendingRequests();
    }

    // ── 1. Add User — raises PENDING request ─────────────────────────

    @Listen("onClick = #btnAddUser")
    public void onAddUser(Event event) {
        String userId     = txtNewUserId.getValue().trim();
        String fullName   = txtNewFullName.getValue().trim();
        String branchCode = txtNewBranchCode.getValue().trim();
        String initial    = txtNewInitial.getValue().trim();
        String roleCode   = getSelectedRoleCode(cmbNewRole);

        if (userId.isEmpty() || fullName.isEmpty() || branchCode.isEmpty() || roleCode == null) {
            setMsg(lblAddUserMsg, "Please fill all required fields.", "error");
            return;
        }

        try {
            userMgmtService.addUser(userId, fullName, branchCode, initial, roleCode,
                    loggedInAdmin.getUserId());
            setMsg(lblAddUserMsg,
                "Request raised for adding user '" + userId + "'. Waiting for approval.", "success");

            txtNewUserId.setValue("");
            txtNewFullName.setValue("");
            txtNewBranchCode.setValue("");
            txtNewInitial.setValue("");
            cmbNewRole.setValue("");

        } catch (Exception e) {
            setMsg(lblAddUserMsg, "Error: " + e.getMessage(), "error");
        }
    }

    // ── 2. Delete User — raises PENDING request ───────────────────────

    @Listen("onClick = #btnDeleteUser")
    public void onDeleteUser(Event event) {
        String userId = txtDeleteUserId.getValue().trim();

        if (userId.isEmpty()) {
            setMsg(lblDeleteUserMsg, "Please enter a User ID.", "error");
            return;
        }

        Messagebox.show(
            "Raise a delete request for user '" + userId + "'?",
            "Confirm",
            Messagebox.YES | Messagebox.NO,
            Messagebox.QUESTION,
            (Event e) -> {
                if (Messagebox.ON_YES.equals(e.getName())) {
                    try {
                        userMgmtService.deleteUser(userId, loggedInAdmin.getUserId());
                        setMsg(lblDeleteUserMsg,
                            "Delete request raised for '" + userId + "'. Waiting for approval.",
                            "success");
                        txtDeleteUserId.setValue("");
                    } catch (Exception ex) {
                        setMsg(lblDeleteUserMsg, "Error: " + ex.getMessage(), "error");
                    }
                }
            }
        );
    }

    // ── 3. Enable User — raises PENDING request ───────────────────────

    @Listen("onClick = #btnEnableUser")
    public void onEnableUser(Event event) {
        String userId = txtEnableUserId.getValue().trim();

        if (userId.isEmpty()) {
            setMsg(lblEnableMsg, "Please enter a User ID.", "error");
            return;
        }

        try {
            userMgmtService.enableUser(userId, loggedInAdmin.getUserId());
            setMsg(lblEnableMsg,
                "Enable request raised for '" + userId + "'. Waiting for approval.", "success");
            txtEnableUserId.setValue("");
        } catch (Exception e) {
            setMsg(lblEnableMsg, "Error: " + e.getMessage(), "error");
        }
    }

    // ── 4. Disable User — raises PENDING request ──────────────────────

    @Listen("onClick = #btnDisableUser")
    public void onDisableUser(Event event) {
        String userId = txtDisableUserId.getValue().trim();

        if (userId.isEmpty()) {
            setMsg(lblDisableMsg, "Please enter a User ID.", "error");
            return;
        }

        if (userId.equals(loggedInAdmin.getUserId())) {
            setMsg(lblDisableMsg, "You cannot disable your own account.", "error");
            return;
        }

        try {
            userMgmtService.disableUser(userId, loggedInAdmin.getUserId());
            setMsg(lblDisableMsg,
                "Disable request raised for '" + userId + "'. Waiting for approval.", "success");
            txtDisableUserId.setValue("");
        } catch (Exception e) {
            setMsg(lblDisableMsg, "Error: " + e.getMessage(), "error");
        }
    }

    // ── 5. Update User Load ───────────────────────────────────────────

    @Listen("onClick = #btnLoadModifyUser")
    public void onLoadModifyUser(Event event) {
        String userId = txtModifyUserId.getValue().trim();

        if (userId.isEmpty()) {
            setMsg(lblModifyMsg, "Please enter a User ID.", "error");
            return;
        }

        SystemUser user = userMgmtService.findByUserId(userId);
        if (user == null) {
            setMsg(lblModifyMsg, "User not found: " + userId, "error");
            return;
        }

        txtModifyFullName.setValue(user.getFullName());
        txtModifyBranchCode.setValue(user.getBranchCode());
        txtModifyInitial.setValue(user.getInitial() != null ? user.getInitial() : "");
        setMsg(lblModifyMsg, "User loaded. Edit fields and click Save Changes.", "");
    }

    // ── 5. Update User Save — raises PENDING request ──────────────────

    @Listen("onClick = #btnSaveModifyUser")
    public void onSaveModifyUser(Event event) {
        String userId     = txtModifyUserId.getValue().trim();
        String fullName   = txtModifyFullName.getValue().trim();
        String branchCode = txtModifyBranchCode.getValue().trim();
        String initial    = txtModifyInitial.getValue().trim();

        if (userId.isEmpty() || fullName.isEmpty() || branchCode.isEmpty()) {
            setMsg(lblModifyMsg, "Please fill all required fields.", "error");
            return;
        }

        try {
            userMgmtService.updateUser(userId, fullName, branchCode, initial,
                    loggedInAdmin.getUserId());
            setMsg(lblModifyMsg,
                "Update request raised for '" + userId + "'. Waiting for approval.", "success");
        } catch (Exception e) {
            setMsg(lblModifyMsg, "Error: " + e.getMessage(), "error");
        }
    }

    // ── 6. Change Role — raises PENDING request ───────────────────────

    @Listen("onClick = #btnChangeRole")
    public void onChangeRole(Event event) {
        String userId   = txtRoleUserId.getValue().trim();
        String roleCode = getSelectedRoleCode(cmbRoleSelect);

        if (userId.isEmpty() || roleCode == null) {
            setMsg(lblRoleMsg, "Please enter User ID and select a role.", "error");
            return;
        }

        try {
            userMgmtService.changeUserRole(userId, roleCode, loggedInAdmin.getUserId());
            setMsg(lblRoleMsg,
                "Role change request raised for '" + userId + "'. Waiting for approval.",
                "success");
        } catch (Exception e) {
            setMsg(lblRoleMsg, "Error: " + e.getMessage(), "error");
        }
    }

    // ── 7. Reset Password — raises PENDING request ────────────────────

    @Listen("onClick = #btnResetPassword")
    public void onResetPassword(Event event) {
        String userId = txtResetUserId.getValue().trim();
        String newPwd = txtNewPassword.getValue().trim();

        if (userId.isEmpty() || newPwd.isEmpty()) {
            setMsg(lblResetMsg, "Please enter User ID and new password.", "error");
            return;
        }

        if (newPwd.length() < 6) {
            setMsg(lblResetMsg, "Password must be at least 6 characters.", "error");
            return;
        }

        try {
            userMgmtService.resetPassword(userId, newPwd, loggedInAdmin.getUserId());
            setMsg(lblResetMsg,
                "Password reset request raised for '" + userId + "'. Waiting for approval.",
                "success");
            txtNewPassword.setValue("");
        } catch (Exception e) {
            setMsg(lblResetMsg, "Error: " + e.getMessage(), "error");
        }
    }

    // ── Logout ────────────────────────────────────────────────────────

    @Listen("onClick = #btnLogout")
    public void onLogout(Event event) {
        Sessions.getCurrent().invalidate();
        Executions.sendRedirect("/login.zul");
    }

    // ── Private helpers ───────────────────────────────────────────────

    private void showPanel(Div targetPanel, Div activeMenu) {
        for (Div p : allPanels)    p.setVisible(false);
        for (Div m : allMenuItems) m.setSclass("cts-menu-item");
        targetPanel.setVisible(true);
        activeMenu.setSclass("cts-menu-item cts-menu-item-active");
    }

    private void loadRolesIntoCombobox(Combobox combobox) {
        combobox.getItems().clear();
        List<SystemRole> roles = userMgmtService.getAllRoles();
        for (SystemRole role : roles) {
            Comboitem item = combobox.appendItem(role.getRoleLabel());
            item.setValue(role.getRoleCode());
        }
    }

    private String getSelectedRoleCode(Combobox combobox) {
        Comboitem selected = combobox.getSelectedItem();
        if (selected == null) return null;
        return selected.getValue();
    }

    private void setMsg(Label label, String message, String type) {
        label.setValue(message);
        if ("success".equals(type))    label.setSclass("cts-msg-success");
        else if ("error".equals(type)) label.setSclass("cts-msg-error");
        else                           label.setSclass("");
    }

    private void loadEnabledUsers() {
        lstEnabledUsers.getItems().clear();
        for (SystemUser user : userMgmtService.getAllUsers()) {
            if (!Boolean.TRUE.equals(user.getActive())) continue;
            String roleCode = userMgmtService.getRoleCodeForUser(user.getUserId());
            Listitem item = new Listitem();
            item.appendChild(new Listcell(user.getUserId()));
            item.appendChild(new Listcell(user.getFullName()));
            item.appendChild(new Listcell(user.getBranchCode()));
            item.appendChild(new Listcell(roleCode != null ? roleCode.toUpperCase() : "NO ROLE"));
            lstEnabledUsers.appendChild(item);
        }
    }

    private void loadDisabledUsers() {
        lstDisabledUsers.getItems().clear();
        for (SystemUser user : userMgmtService.getAllUsers()) {
            if (Boolean.TRUE.equals(user.getActive())) continue;
            String roleCode = userMgmtService.getRoleCodeForUser(user.getUserId());
            Listitem item = new Listitem();
            item.appendChild(new Listcell(user.getUserId()));
            item.appendChild(new Listcell(user.getFullName()));
            item.appendChild(new Listcell(user.getBranchCode()));
            item.appendChild(new Listcell(roleCode != null ? roleCode.toUpperCase() : "NO ROLE"));
            lstDisabledUsers.appendChild(item);
        }
    }

    private void loadAllUsersWithCount() {
        lstAllUsers.getItems().clear();
        List<SystemUser> users = userMgmtService.getAllUsers();
        int total = users.size(), active = 0, inactive = 0;

        for (SystemUser user : users) {
            String roleCode = userMgmtService.getRoleCodeForUser(user.getUserId());
            boolean isActive = Boolean.TRUE.equals(user.getActive());
            if (isActive) active++; else inactive++;

            Listitem item = new Listitem();
            item.appendChild(new Listcell(user.getUserId()));
            item.appendChild(new Listcell(user.getFullName()));
            item.appendChild(new Listcell(user.getBranchCode()));
            item.appendChild(new Listcell(roleCode != null ? roleCode.toUpperCase() : "NO ROLE"));
            item.appendChild(new Listcell(isActive ? "Active" : "Inactive"));
            lstAllUsers.appendChild(item);
        }

        lblCountTotal.setValue(String.valueOf(total));
        lblCountActive.setValue(String.valueOf(active));
        lblCountInactive.setValue(String.valueOf(inactive));
    }

    // ── 9. Load pending requests with Accept/Decline buttons ──────────

    private void loadPendingRequests() {
        lstRequests.getItems().clear();

        List<UserRequest> requests = userMgmtService.getPendingRequests();

        if (requests.isEmpty()) {
            setMsg(lblRequestMsg, "No pending requests.", "");
            return;
        }

        setMsg(lblRequestMsg, "", "");

        for (UserRequest req : requests) {
            Listitem item = new Listitem();
            item.appendChild(new Listcell(req.getRequestId()));
            item.appendChild(new Listcell(formatRequestType(req.getRequestType())));
            item.appendChild(new Listcell(req.getTargetUserId()));
            item.appendChild(new Listcell(req.getRequestedBy()));
            item.appendChild(new Listcell(buildReadableDetails(req)));
            item.appendChild(new Listcell(req.getRequestDate()));

            // Accept button
            Button acceptBtn = new Button("Accept");
            acceptBtn.setSclass("cts-req-accept");
            String reqId = req.getRequestId();
            acceptBtn.addEventListener("onClick", e -> onAcceptRequest(reqId));

            // Decline button
            Button declineBtn = new Button("Decline");
            declineBtn.setSclass("cts-req-decline");
            declineBtn.addEventListener("onClick", e -> onDeclineRequest(reqId));

            Listcell actionCell = new Listcell();
            actionCell.appendChild(acceptBtn);
            actionCell.appendChild(declineBtn);
            item.appendChild(actionCell);

            lstRequests.appendChild(item);
        }
    }

    private void onAcceptRequest(String requestId) {
        try {
            userMgmtService.acceptRequest(requestId, loggedInAdmin.getUserId());
            loadPendingRequests();
            setMsg(lblRequestMsg, "Request " + requestId + " accepted and executed.", "success");
        } catch (Exception e) {
            setMsg(lblRequestMsg, "Error: " + e.getMessage(), "error");
        }
    }

    private void onDeclineRequest(String requestId) {
        try {
            userMgmtService.declineRequest(requestId, loggedInAdmin.getUserId());
            loadPendingRequests();
            setMsg(lblRequestMsg, "Request " + requestId + " declined.", "success");
        } catch (Exception e) {
            setMsg(lblRequestMsg, "Error: " + e.getMessage(), "error");
        }
    }

    // Convert request type to readable label
    private String formatRequestType(String type) {
        switch (type) {
            case "ADD_USER":       return "Add User";
            case "DELETE_USER":    return "Delete User";
            case "ENABLE_USER":    return "Enable User";
            case "DISABLE_USER":   return "Disable User";
            case "UPDATE_USER":    return "Update User";
            case "CHANGE_ROLE":    return "Change Role";
            case "RESET_PASSWORD": return "Reset Password";
            default:               return type;
        }
    }

    // Build a human-readable details string for each request type
    private String buildReadableDetails(UserRequest req) {
        String type    = req.getRequestType();
        String details = req.getDetails();

        switch (type) {
            case "ADD_USER":
                // details: "fullName|branchCode|initial|roleCode"
                String[] addParts = details.split("\\|");
                if (addParts.length >= 4) {
                    return "Name: " + addParts[0]
                        + " | Branch: " + addParts[1]
                        + " | Role: " + addParts[3];
                }
                return details;

            case "UPDATE_USER":
                // details: "fullName|branchCode|initial"
                String[] updParts = details.split("\\|");
                if (updParts.length >= 2) {
                    return "Name: " + updParts[0] + " | Branch: " + updParts[1];
                }
                return details;

            case "CHANGE_ROLE":
                return "New role: " + details;

            case "RESET_PASSWORD":
                // don't show hashed password
                return "Password reset requested";

            default:
                return details;
        }
    }
}
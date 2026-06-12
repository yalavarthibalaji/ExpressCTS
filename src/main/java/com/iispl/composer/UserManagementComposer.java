package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.entity.Role;
import com.iispl.entity.User;
import com.iispl.service.UserService;
import com.iispl.serviceImpl.UserServiceImpl;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;


import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * File    : com/iispl/composer/UserManagementComposer.java
 * Purpose : Admin User Management page composer.
 *           - Lists all users in a Grid table
 *           - Add User modal to create new users with role assignment
 *           - Toggle user status (Active / Inactive)
 *           - Admin-only access (redirects non-admin to their own dashboard)
 * ZUL     : admin/userManagement/userManagement.zul
 */
public class UserManagementComposer extends SelectorComposer<Component> {

    private final UserService userService = new UserServiceImpl();

   

    // ── User table wires ──
    @Wire private Rows userRows;

    // ── Add User Modal wires ──
    @Wire private Div      userModalOverlay;
    @Wire private Textbox  mLoginId;
    @Wire private Textbox  mFullName;
    @Wire private Textbox  mEmail;
    @Wire private Textbox  mMobile;
    @Wire private Textbox  mPassword;
    @Wire private Combobox mRole;
    @Wire private Label    mErrorLabel;

    /**
     * Entry point: wires user info into topbar, then loads roles + user list.
     */
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        // Admin-only check
        LoginDTO dto = SessionUtil.requireAdmin();
        if (dto == null) return;

        // Populate topbar
        

        // Initial load
        loadRolesIntoCombobox();
        renderUserTable();
    }

    // ════════════════════════════════════════════
    //  Top-level navigation
    // ════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    @Listen("onClick = #backToDashboardBtn")
    public void backToDashboard() {
        Executions.sendRedirect("/admin/adminDashboard.zul");
    }

    // ════════════════════════════════════════════
    //  Add User Modal
    // ════════════════════════════════════════════

    @Listen("onClick = #addUserBtn")
    public void openAddUserModal() {
        // Reset form fields
        mLoginId.setValue("");
        mFullName.setValue("");
        mEmail.setValue("");
        mMobile.setValue("");
        mPassword.setValue("");
        if (mRole.getItemCount() > 0) mRole.setSelectedIndex(0);
        mErrorLabel.setValue("");
        mErrorLabel.setVisible(false);

        // Show modal by adding the "open" class
        userModalOverlay.setSclass("modal-ov open");
    }

    @Listen("onClick = #modalCloseBtn; onClick = #modalCancelBtn")
    public void closeModal() {
        userModalOverlay.setSclass("modal-ov");
    }

    @Listen("onClick = #modalSaveBtn")
    public void saveUser() {
        String loginId  = mLoginId.getValue();
        String fullName = mFullName.getValue();
        String email    = mEmail.getValue();
        String mobile   = mMobile.getValue();
        String password = mPassword.getValue();

        // Read selected role id from combobox
        Comboitem item = mRole.getSelectedItem();
        Integer roleId = (item != null) ? (Integer) item.getValue() : null;

        // Delegate to service for validation + persistence
        String error = userService.createUser(loginId, fullName, email, mobile, password, roleId);

        if (error != null) {
            // Validation failure — show inline error in modal
            mErrorLabel.setValue("⚠ " + error);
            mErrorLabel.setVisible(true);
            return;
        }

        // Success: close modal, refresh table, show toast
        userModalOverlay.setSclass("modal-ov");
        renderUserTable();
        Clients.showNotification(
            "✓ User '" + loginId + "' created successfully",
            "info", null, "top_center", 3000
        );
    }

    // ════════════════════════════════════════════
    //  Helpers — load combobox + render table
    // ════════════════════════════════════════════

    /** Populates the Role combobox with all active roles. */
    private void loadRolesIntoCombobox() {
        mRole.getItems().clear();
        List<Role> roles = userService.getAllRoles();
        for (Role r : roles) {
            Comboitem item = new Comboitem(formatRoleName(r.getRoleCode()));
            item.setValue(r.getId());
            mRole.appendChild(item);
        }
        if (mRole.getItemCount() > 0) mRole.setSelectedIndex(0);
    }

    /** Re-renders the user table from current DB state. */
    private void renderUserTable() {
        userRows.getChildren().clear();
        List<User> users = userService.getAllUsers();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

        int idx = 1;
        for (User u : users) {
            Row row = new Row();

            row.appendChild(new Label(String.valueOf(idx++)));
            row.appendChild(new Label(u.getUserLoginId()));
            row.appendChild(new Label(u.getFullName()));
            row.appendChild(new Label(u.getEmail()));
            row.appendChild(new Label(u.getMobile() != null ? u.getMobile() : "-"));

            // Role badge
            String roleName = u.getRole() != null ? formatRoleName(u.getRole().getRoleCode()) : "-";
            Label roleBadge = new Label(roleName);
            roleBadge.setSclass("badge b-info");
            row.appendChild(roleBadge);

            // Status badge
            String status = u.getStatus();
            Label statusBadge = new Label(status);
            statusBadge.setSclass("ACTIVE".equals(status) ? "badge b-pass" : "badge b-fail");
            row.appendChild(statusBadge);

            // Created date
            row.appendChild(new Label(
                u.getCreatedAt() != null ? u.getCreatedAt().format(fmt) : "-"
            ));

            // Action button — toggle status
            Button toggleBtn = new Button("ACTIVE".equals(status) ? "Disable" : "Enable");
            toggleBtn.setSclass("btn " + ("ACTIVE".equals(status) ? "bw" : "bs") + " btn-sm");
            final Long   uid   = u.getId();
            final String uname = u.getUserLoginId();
            toggleBtn.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event event) {
                    if (userService.toggleUserStatus(uid)) {
                        renderUserTable();
                        Clients.showNotification(
                            "✓ User '" + uname + "' status updated",
                            "info", null, "top_center", 2000
                        );
                    } else {
                        Clients.showNotification(
                            "✗ Failed to update user '" + uname + "'",
                            "error", null, "top_center", 2000
                        );
                    }
                }
            });
            row.appendChild(toggleBtn);

            userRows.appendChild(row);
        }

        // Empty-state row: single cell spanning all 9 columns
        if (users.isEmpty()) {
            Row emptyRow = new Row();
            Cell cell = new Cell();
            cell.setColspan(9);
            cell.setStyle("text-align: center; padding: 20px; color: var(--tm);");
            cell.appendChild(new Label("No users found. Click '+ Add User' to create one."));
            emptyRow.appendChild(cell);
            userRows.appendChild(emptyRow);
        }
    }

    /** Maps internal role codes to display names. */
    private String formatRoleName(String code) {
        if (code == null) return "";
        switch (code) {
            case "ADMIN":           return "Administrator";
            case "MAKER_OUTWARD":   return "Maker — Outward";
            case "CHECKER_OUTWARD": return "Checker — Outward";
            case "MAKER_INWARD":    return "Maker — Inward";
            case "CHECKER_INWARD":  return "Checker — Inward";
            default:                return code;
        }
    }
}
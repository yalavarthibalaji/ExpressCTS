package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;

/**
 * Shared composer for all role dashboard pages.
 * Wires user info into the topbar and handles logout.
 */
public class DashboardComposer extends SelectorComposer<Component> {

    @Wire private Label userAvatar;
    @Wire private Label userName;
    @Wire private Label userRole;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.requireLogin();
        if (dto == null) return;

        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
        if (userName   != null) userName.setValue(dto.getFullName());
        if (userRole   != null) userRole.setValue(formatRole(dto.getRoleCode()));
    }

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }

    @Listen("onClick = #userMgmtBtn")
    public void goToUserManagement() {
        Executions.sendRedirect("/admin/userManagement/userManagement.zul");
    }

    @Listen("onClick = #backToDashboardBtn")
    public void backToDashboard() {
        Executions.sendRedirect("/admin/adminDashboard.zul");
    }

    private String formatRole(String code) {
        if (code == null) return "";
        switch (code) {
            case "ADMIN":            return "Administrator";
            case "MAKER_OUTWARD":    return "Maker — Outward";
            case "CHECKER_OUTWARD":  return "Checker — Outward";
            case "MAKER_INWARD":     return "Maker — Inward";
            case "CHECKER_INWARD":   return "Checker — Inward";
            default:                 return code;
        }
    }
}
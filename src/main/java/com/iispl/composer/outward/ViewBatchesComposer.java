package com.iispl.composer.outward;

import com.iispl.dto.LoginDTO;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;

/**
 * File    : com/iispl/composer/ViewBatchesComposer.java
 * ZUL     : admin/viewBatches/viewBatches.zul
 * Purpose : Displays all batches (admin view). Stub — to be implemented.
 *           Exists now to prevent ClassNotFoundException on startup.
 */
public class ViewBatchesComposer extends SelectorComposer<Component> {

    @Wire private Label userName;
    @Wire private Label userRole;
    @Wire private Label userAvatar;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.getCurrentUser();
        if (dto == null) {
            Executions.sendRedirect("/login/login.zul");
            return;
        }

        if (userName  != null) userName.setValue(dto.getFullName());
        if (userRole  != null) userRole.setValue(dto.getRoleCode());
        if (userAvatar != null) userAvatar.setValue(dto.getInitials());
    }

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
    }
}
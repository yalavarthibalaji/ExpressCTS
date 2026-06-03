package com.iispl.composer;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import com.iispl.dto.LoginDTO;
import com.iispl.util.SessionUtil;

public class TopbarComposer extends SelectorComposer<Component> {

    @Wire("#userName")
    private Label userName;

    @Wire("#userRole")
    private Label userRole;

    @Wire("#userAvatar")
    private Div userAvatar;

    @Wire("#logoutBtn")
    private Button logoutBtn;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = (LoginDTO) Sessions.getCurrent().getAttribute(SessionUtil.SESSION_KEY);
        if (dto == null) return;

        userName.setValue(dto.getFullName());
        userRole.setValue(dto.getRoleCode());
        userAvatar.appendChild(new Label(dto.getInitials()));
    }

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        Sessions.getCurrent().invalidate();
        Executions.sendRedirect("/login/login.zul");
    }
}
package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.service.UserService;
import com.iispl.serviceImpl.UserServiceImpl;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;

public class LoginComposer extends SelectorComposer<Component> {

    @Wire private Textbox userIdBox;
    @Wire private Textbox passwordBox;
    @Wire private Label   errorLabel;

    private final UserService userService = new UserServiceImpl();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        userIdBox.setValue("");
        passwordBox.setValue("");
        errorLabel.setValue("");
        errorLabel.setSclass("lerr");
    }

    @Listen("onClick = #loginBtn; onOK = #userIdBox, #passwordBox")
    public void doLogin() {
        String userId   = userIdBox.getValue().trim();
        String password = passwordBox.getValue();

        if (userId.isEmpty())  { showError("User ID is required.");   userIdBox.focus();  return; }
        if (password.isEmpty()){ showError("Password is required."); passwordBox.focus(); return; }

        LoginDTO dto = userService.validateLogin(userId, password);
        if (dto == null) {
            showError("⚠ Invalid credentials. Please try again.");
            passwordBox.setValue("");
            passwordBox.focus();
            return;
        }

        Sessions.getCurrent().setAttribute(SessionUtil.SESSION_KEY, dto);

        // Role-based redirect
        String dashboardUrl = SessionUtil.getDashboardUrlFor(dto.getRoleCode());
        Executions.sendRedirect(dashboardUrl);
    }

    private void showError(String message) {
        errorLabel.setValue(message);
        errorLabel.setSclass("lerr visible");
    }
}
/*login pages done by Ramana and it will detected role by using userId and Password*/
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

import com.iispl.dto.UserModel;
import com.iispl.service.AuthService;

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

    private final AuthService authService = new AuthService();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loginError.setVisible(false);
        detectedRoleWrap.setVisible(false);
        txtUserId.setFocus(true);
    }

    // Role auto-detection on every keystroke
    @Listen("onChanging = #txtUserId; onChanging = #txtPassword")
    public void onCredentialChange(Event event) {
        loginError.setVisible(false);

        InputEvent ie = (InputEvent) event;
        String typedNow = ie.getValue() == null ? "" : ie.getValue().trim();

        String userId, password;
        if (event.getTarget() == txtUserId) {
            userId   = typedNow;
            password = txtPassword.getValue().trim();
        } else {
            userId   = txtUserId.getValue().trim();
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

    @Listen("onClick = #btnSignIn; onOK = #txtPassword")
    public void doLogin(Event event) {
        String userId   = txtUserId.getValue().trim();
        String password = txtPassword.getValue().trim();

        System.out.println("DEBUG LoginController: userId = " + userId);
        System.out.println("DEBUG LoginController: password length = " + password.length());

        if (userId.isEmpty() || password.isEmpty()) {
            showError("Please enter your User ID and Password.");
            return;
        }

        UserModel user = authService.authenticate(userId, password);

        System.out.println("DEBUG LoginController: UserModel returned = " + user);

        if (user == null) {
            showError("Invalid User ID or Password. Please try again.");
            txtPassword.setValue("");
            txtPassword.setFocus(true);
            detectedRoleWrap.setVisible(false);
            return;
        }

        System.out.println("DEBUG LoginController: roleId = " + user.getRoleId());

        // Store user in ZK session
        Sessions.getCurrent().setAttribute("loggedInUser", user);

        String roleId = user.getRoleId();
        switch (roleId) {
            case "checker":
                Executions.sendRedirect("/zul/checkerDashboard.zul");
                break;
            case "admin":
                Executions.sendRedirect("/zul/adminDashboard.zul");
                break;
            case "micr_repair":
                Executions.sendRedirect("/zul/micrRepairDashboard.zul");
                break;
            case "maker":
                Executions.sendRedirect("/zul/makerDashboard.zul");
                break;
            default:
                System.out.println("DEBUG LoginController: roleId did not match any case → " + roleId);
                Executions.sendRedirect("/zul/makerDashboard.zul");
                break;
        }
    }    private void showError(String message) {
        loginErrorText.setValue(message);
        loginError.setVisible(true);
    }
}
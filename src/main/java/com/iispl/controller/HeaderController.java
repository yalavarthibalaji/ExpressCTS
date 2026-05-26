package com.iispl.controller;

import java.text.SimpleDateFormat;

import java.util.Date;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import com.iispl.dto.UserModel;

/**
 * HeaderController.java
 * ZK MVC Composer — controls components/header.zul
 * Populates avatar, user name, role badge, and datetime.
 */
public class HeaderController extends SelectorComposer<Component> {

    @Wire("#headerRoleBadge")
    private Div headerRoleBadge;

    @Wire("#headerAvatar")
    private Div headerAvatar;

    @Wire("#headerUserName")
    private Label headerUserName;

    @Wire("#headerDateTime")
    private Label headerDateTime;

    @Wire("#btnHeaderLogout")
    private org.zkoss.zul.Button btnHeaderLogout;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Object sessionObj = Sessions.getCurrent().getAttribute("loggedInUser");
        System.out.println("DEBUG HeaderController: session object = " + sessionObj);
        System.out.println("DEBUG HeaderController: session object class = "
                + (sessionObj == null ? "NULL" : sessionObj.getClass().getName()));

        UserModel user = (UserModel) Sessions.getCurrent().getAttribute("loggedInUser");
        System.out.println("DEBUG HeaderController: user after cast = " + user);

        if (user == null) {
            Executions.sendRedirect("/zul/login.zul");
            return;
        }

        // Avatar — initials circle
        headerAvatar.setStyle(
            "background:#1a3a6e;color:#fff;border-radius:50%;" +
            "width:32px;height:32px;display:flex;align-items:center;" +
            "justify-content:center;font-weight:700;font-size:13px;"
        );
        headerAvatar.appendChild(new org.zkoss.zul.Label(user.getRoleInitial()));

        // User ID and role label
        headerUserName.setValue(user.getUserId());
        headerRoleBadge.appendChild(new org.zkoss.zul.Label(user.getRoleLabel()));

        // Date-time display — e.g. "26 May 2026 10:46:39 am"
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm:ss a");
        headerDateTime.setValue(sdf.format(new Date()));
    }

    @Listen("onClick = #btnHeaderLogout")
    public void onLogout(Event e) {
        Sessions.getCurrent().invalidate();
        Executions.sendRedirect("/zul/login.zul");
    }
}
package com.iispl.composer;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Timer;

import com.iispl.dto.LoginDTO;
import com.iispl.dto.NotificationDto;
import com.iispl.service.NotificationService;
import com.iispl.serviceImpl.NotificationServiceImpl;
import com.iispl.util.SessionUtil;

public class TopbarComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    @Wire("#userName")   private Label userName;
    @Wire("#userRole")   private Label userRole;
    @Wire("#userAvatar") private Div   userAvatar;
    @Wire("#logoutBtn")  private Button logoutBtn;
    @Wire("#btnBell")    private Button btnBell;
    @Wire("#notifBadge") private Label  notifBadge;
    @Wire("#notifPopup") private Popup  notifPopup;
    @Wire("#notifList")  private Div    notifList;
    @Wire("#notifEmpty") private Div    notifEmpty;
    @Wire("#notifTimer") private Timer  notifTimer;

    private final NotificationService notifService = new NotificationServiceImpl();
    private List<NotificationDto> unreadNotifs = new ArrayList<>();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.getCurrentUser();
        if (dto == null) return;

        userName.setValue(dto.getFullName());
        userRole.setValue(dto.getRoleCode());
        userAvatar.appendChild(new Label(dto.getInitials()));

        refreshNotifications();
    }

    @Listen("onTimer = #notifTimer")
    public void onPoll(Event e) {
        refreshNotifications();
    }

    @Listen("onClick = #btnBell")
    public void onBellClick() {
        notifPopup.open(btnBell, "after_end");
    }

    @Listen("onClick = #btnMarkAllRead")
    public void onMarkAllRead() {
        notifService.markAllRead(SessionUtil.getCurrentUser().getUserId());
        unreadNotifs.clear();
        renderNotifList();
        updateBadge();
    }

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
        Executions.sendRedirect("/login/login.zul");
    }

    private void refreshNotifications() {
        LoginDTO dto = SessionUtil.getCurrentUser();
        if (dto == null) return;
        unreadNotifs = notifService.getUnreadByUser(dto.getUserId());
        renderNotifList();
        updateBadge();
    }

    private void updateBadge() {
        int count = unreadNotifs.size();
        if (count > 0) {
            notifBadge.setValue(count > 9 ? "9+" : String.valueOf(count));
            notifBadge.setVisible(true);
            btnBell.setSclass("bell-btn bell-btn--active");
        } else {
            notifBadge.setVisible(false);
            btnBell.setSclass("bell-btn");
        }
    }

    private void renderNotifList() {
        notifList.getChildren().clear();
        if (unreadNotifs == null || unreadNotifs.isEmpty()) {
            notifEmpty.setVisible(true);
            notifList.setVisible(false);
            return;
        }
        notifEmpty.setVisible(false);
        notifList.setVisible(true);
        for (NotificationDto n : unreadNotifs) {
            Div item = new Div();
            item.setSclass("notif-item");

            Div dot = new Div();
            dot.setSclass("notif-dot");

            Div content = new Div();
            content.setSclass("notif-content");

            Label msg = new Label(n.getMessage());
            msg.setSclass("notif-msg");

            Label time = new Label(n.getCreatedAt().format(FMT));
            time.setSclass("notif-time");

            content.appendChild(msg);
            content.appendChild(time);
            item.appendChild(dot);
            item.appendChild(content);
            notifList.appendChild(item);
        }
    }
}
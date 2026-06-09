package com.iispl.composer;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.event.Events;
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

/**
 * File    : com/iispl/composer/TopbarComposer.java
 * Purpose : Drives the shared topbar component.
 *           Handles user info display, logout, and notification bell.
 *
 * This composer runs on every page that includes /component/topbar.zul
 * It is a SHARED component — do not add page-specific logic here.
 *
 * Notification bell behaviour:
 *   - Page load   → loads unread count, shows red badge if count > 0
 *   - Every 15s   → timer re-checks count automatically
 *   - Bell click  → opens popup, renders notification list
 *   - Item click  → marks that notification read, navigates to its page
 *   - Mark all    → marks all read, clears badge, refreshes list
 */
public class TopbarComposer extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    // ── Wired from topbar.zul — these IDs already exist, no ZUL change needed ──
    @Wire("#userName")   private Label  userName;
    @Wire("#userRole")   private Label  userRole;
    @Wire("#userAvatar") private Div    userAvatar;
    @Wire("#logoutBtn")  private Button logoutBtn;
    @Wire("#btnBell")    private Button btnBell;
    @Wire("#notifBadge") private Label  notifBadge;
    @Wire("#notifPopup") private Popup  notifPopup;
    @Wire("#notifList")  private Div    notifList;
    @Wire("#notifEmpty") private Div    notifEmpty;
    @Wire("#notifTimer") private Timer  notifTimer;

    private final NotificationService notifService = new NotificationServiceImpl();

    // Logged-in user id — set once in doAfterCompose, used by all notif methods
    private Long currentUserId;

    // ════════════════════════════════════════════════════
    //  Page Init
    // ════════════════════════════════════════════════════

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.getCurrentUser();
        if (dto == null) return;

        currentUserId = dto.getUserId();

        // Populate user info — same as before, no change to this logic
        userName.setValue(dto.getFullName());
        userRole.setValue(dto.getRoleCode());
        userAvatar.appendChild(new Label(dto.getInitials()));

        // Load notification badge count on page load
        refreshBadge();
    }

    // ════════════════════════════════════════════════════
    //  Timer — auto refresh badge every 15 seconds
    //  (timer already exists in topbar.zul, just needed to uncomment)
    // ════════════════════════════════════════════════════

    @Listen("onTimer = #notifTimer")
    public void onPoll() {
        refreshBadge();
    }

    // ════════════════════════════════════════════════════
    //  Bell click — open popup and load list
    // ════════════════════════════════════════════════════

    @Listen("onClick = #btnBell")
    public void onBellClick() {
        renderNotifList();
        notifPopup.open(btnBell, "after_end");
    }

    // ════════════════════════════════════════════════════
    //  Mark all read
    // ════════════════════════════════════════════════════

    @Listen("onClick = #btnMarkAllRead")
    public void onMarkAllRead() {
        if (currentUserId == null) return;
        notifService.markAllRead(currentUserId);
        renderNotifList();
        refreshBadge();
    }

    // ════════════════════════════════════════════════════
    //  Logout — unchanged from before
    // ════════════════════════════════════════════════════

    @Listen("onClick = #logoutBtn")
    public void doLogout() {
        SessionUtil.logout();
        Executions.sendRedirect("/login/login.zul");
    }

    // ════════════════════════════════════════════════════
    //  Badge — updates red count bubble on bell icon
    // ════════════════════════════════════════════════════

    private void refreshBadge() {
        if (currentUserId == null) return;

        int count = notifService.countUnread(currentUserId);

        if (count > 0) {
            notifBadge.setValue(count > 9 ? "9+" : String.valueOf(count));
            notifBadge.setVisible(true);
            btnBell.setSclass("bell-btn bell-btn--active");
        } else {
            notifBadge.setVisible(false);
            btnBell.setSclass("bell-btn");
        }
    }

    // ════════════════════════════════════════════════════
    //  Render notification list inside popup
    // ════════════════════════════════════════════════════

    private void renderNotifList() {
        notifList.getChildren().clear();

        if (currentUserId == null) {
            showEmpty();
            return;
        }

        List<NotificationDto> unread = notifService.getUnreadForUser(currentUserId);

        if (unread == null || unread.isEmpty()) {
            showEmpty();
            return;
        }

        notifEmpty.setVisible(false);
        notifList.setVisible(true);

        for (NotificationDto n : unread) {
            notifList.appendChild(buildNotifItem(n));
        }
    }

    /**
     * Builds one clickable notification row.
     *
     * Layout (same CSS classes the other team already wrote):
     *   [blue dot]  [message text ]
     *               [time label   ]
     *
     * Click action:
     *   1. Mark this notification as read in DB
     *   2. Close the popup
     *   3. Navigate to the page linked to this notification
     */
    private Div buildNotifItem(final NotificationDto n) {
        Div item = new Div();
        item.setSclass("notif-item");
        item.setStyle("cursor:pointer");

        // Blue unread dot
        Div dot = new Div();
        dot.setSclass("notif-dot");
        item.appendChild(dot);

        // Content
        Div content = new Div();
        content.setSclass("notif-content");

        // Message text
        Label msg = new Label(n.getMessage());
        msg.setSclass("notif-msg");
        content.appendChild(msg);

        // Time
        String timeStr = n.getCreatedAt() != null
                ? n.getCreatedAt().format(TIME_FMT) : "";
        Label time = new Label(timeStr);
        time.setSclass("notif-time");
        content.appendChild(time);

        item.appendChild(content);

        // Click → mark read + navigate
        item.addEventListener(Events.ON_CLICK, event -> {
            notifService.markRead(n.getId());
            notifPopup.close();
            Executions.sendRedirect(n.getLinkUrl());
        });

        return item;
    }

    private void showEmpty() {
        notifEmpty.setVisible(true);
        notifList.setVisible(false);
    }
}
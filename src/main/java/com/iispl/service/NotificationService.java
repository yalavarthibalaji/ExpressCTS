package com.iispl.service;

import com.iispl.dto.NotificationDto;
import java.util.List;

/**
 * File    : com/iispl/service/NotificationService.java
 * Purpose : Service interface for in-app notifications.
 *
 * Two events create notifications:
 *
 *   notifyReferBack(batchDbId, batchIdStr, modules, makerId)
 *     → Called by CheckerServiceImpl after REFER_BACK is set.
 *     → Creates one notification for the maker who owns the batch.
 *     → modules is e.g. "MICR_REPAIR" or "MICR_REPAIR,DATA_ENTRY"
 *
 *   notifyResubmitted(batchIdStr, makerName)
 *     → Called by MakerOutwardServiceImpl after batch is re-submitted.
 *     → Creates one notification per active CHECKER_OUTWARD user.
 *
 * Read methods are used by TopbarComposer:
 *   getUnreadForUser(userId)  → list for popup
 *   countUnread(userId)       → badge number
 *   markRead(notifId)         → single click
 *   markAllRead(userId)       → "Mark all read" button
 */
public interface NotificationService {

    // ── Write (called from service layer) ──

    /**
     * Creates a REFER_BACK notification for the maker.
     *
     * @param batchDbId   outward_batch.id (PK) — used to look up owner
     * @param batchIdStr  human-readable batch ID, e.g. B-2026-0609-001
     * @param modules     comma-separated module list, e.g. "MICR_REPAIR,DATA_ENTRY"
     * @param makerId     recipient user id (the maker who owns this batch)
     */
    void notifyReferBack(Long batchDbId, String batchIdStr,
                          String modules, Long makerId);

    /**
     * Creates a RESUBMITTED notification for every active CHECKER_OUTWARD user.
     *
     * @param batchIdStr  human-readable batch ID
     * @param makerName   full name of the maker who re-submitted
     */
    void notifyResubmitted(String batchIdStr, String makerName);

    // ── Read (called from TopbarComposer) ──

    /** Returns up to 20 unread notifications for a user, newest first. */
    List<NotificationDto> getUnreadForUser(Long userId);

    /** Count of unread notifications. Used for the red badge. */
    int countUnread(Long userId);

    /** Mark one notification as read (user clicked it). */
    boolean markRead(Long notificationId);

    /** Mark all notifications as read (user clicked "Mark all read"). */
    boolean markAllRead(Long userId);
}
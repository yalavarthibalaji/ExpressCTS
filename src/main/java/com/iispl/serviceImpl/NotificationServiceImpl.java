package com.iispl.serviceImpl;

import com.iispl.dao.NotificationDao;
import com.iispl.daoImpl.NotificationDaoImpl;
import com.iispl.dto.NotificationDto;
import com.iispl.entity.outward.Notification;
import com.iispl.entity.User;
import com.iispl.service.NotificationService;

import java.util.ArrayList;
import java.util.List;

/**
 * File    : com/iispl/serviceImpl/NotificationServiceImpl.java
 * Purpose : Implements NotificationService.
 *
 * Design rules (same as AuditServiceImpl):
 *   - Notification writes must NEVER throw to the caller.
 *   - If a notification fails to save, we log to System.err and move on.
 *   - The business operation that triggered us must not be blocked.
 */
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notifDao = new NotificationDaoImpl();

    // ════════════════════════════════════════════════════
    //  REFER_BACK — notify maker
    // ════════════════════════════════════════════════════

    /**
     * Called by CheckerServiceImpl after finalizeBatchIfDone() returns REFER_BACK.
     *
     * Message format:
     *   "Batch B-2026-0609-001 referred back to you — fix: MICR Repair, Data Entry"
     *
     * Link:
     *   "/outward/viewBatches/viewBatches.zul"
     *   (maker opens View Batches → selects the REFER_BACK batch → navigates to fix)
     */
    @Override
    public void notifyReferBack(Long batchDbId, String batchIdStr,
                                 String modules, Long makerId) {
        if (makerId == null || batchIdStr == null) {
            System.err.println("NotificationService → notifyReferBack: "
                    + "makerId or batchIdStr is null — skipping");
            return;
        }

        try {
            String friendlyModules = buildFriendlyModuleNames(modules);

            String message = "Batch " + batchIdStr
                + " has been referred back to you — fix: " + friendlyModules;

            Notification n = new Notification();

            User recipient = new User();
            recipient.setId(makerId);
            n.setRecipient(recipient);

            n.setBatchIdStr(batchIdStr);
            n.setEventType("REFER_BACK");
            n.setModules(modules);
            n.setMessage(message);
            n.setLinkUrl("/outward/viewBatches/viewBatches.zul");

            notifDao.save(n);
            System.out.println("NotificationService → REFER_BACK notif saved: "
                    + "makerId=" + makerId + " batch=" + batchIdStr);

        } catch (Exception e) {
            System.err.println("NotificationService → notifyReferBack failed: "
                    + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════
    //  RESUBMITTED — notify all checkers
    // ════════════════════════════════════════════════════

    /**
     * Called by MakerOutwardServiceImpl after resubmitBatch() succeeds.
     *
     * Message format:
     *   "Batch B-2026-0609-001 has been re-submitted by Maker John Doe — ready for verification"
     *
     * Link:
     *   "/outward/checkerQueue/checkerQueue.zul"
     *   (checker opens Verification Queue and picks up the batch)
     *
     * One notification row is created per active CHECKER_OUTWARD user.
     */
    @Override
    public void notifyResubmitted(String batchIdStr, String makerName) {
        if (batchIdStr == null) {
            System.err.println("NotificationService → notifyResubmitted: "
                    + "batchIdStr is null — skipping");
            return;
        }

        try {
            List<Long> checkerIds = notifDao.findCheckerOutwardUserIds();

            if (checkerIds.isEmpty()) {
                System.out.println("NotificationService → notifyResubmitted: "
                        + "no active CHECKER_OUTWARD users found");
                return;
            }

            String name    = (makerName != null && !makerName.trim().isEmpty())
                             ? makerName.trim() : "Maker";
            String message = "Batch " + batchIdStr
                + " re-submitted by " + name
                + " — ready for verification";

            for (Long checkerId : checkerIds) {
                try {
                    Notification n = new Notification();

                    User recipient = new User();
                    recipient.setId(checkerId);
                    n.setRecipient(recipient);

                    n.setBatchIdStr(batchIdStr);
                    n.setEventType("RESUBMITTED");
                    n.setModules(null);
                    n.setMessage(message);
                    n.setLinkUrl("/outward/checkerQueue/checkerQueue.zul");

                    notifDao.save(n);
                } catch (Exception inner) {
                    // Don't stop for one failed checker notify
                    System.err.println("NotificationService → notifyResubmitted: "
                            + "failed for checkerId=" + checkerId
                            + ": " + inner.getMessage());
                }
            }
            System.out.println("NotificationService → RESUBMITTED notif sent to "
                    + checkerIds.size() + " checker(s) for batch=" + batchIdStr);

        } catch (Exception e) {
            System.err.println("NotificationService → notifyResubmitted failed: "
                    + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════
    //  Read methods — for TopbarComposer
    // ════════════════════════════════════════════════════

    @Override
    public List<NotificationDto> getUnreadForUser(Long userId) {
        if (userId == null) return new ArrayList<>();
        try {
            List<Notification> entities = notifDao.findUnreadByRecipient(userId);
            List<NotificationDto> dtos  = new ArrayList<>();
            for (Notification n : entities) {
                dtos.add(toDto(n));
            }
            return dtos;
        } catch (Exception e) {
            System.err.println("NotificationService → getUnreadForUser failed: "
                    + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public int countUnread(Long userId) {
        if (userId == null) return 0;
        return notifDao.countUnread(userId);
    }

    @Override
    public boolean markRead(Long notificationId) {
        if (notificationId == null) return false;
        return notifDao.markRead(notificationId);
    }

    @Override
    public boolean markAllRead(Long userId) {
        if (userId == null) return false;
        return notifDao.markAllRead(userId);
    }

    // ════════════════════════════════════════════════════
    //  Private helpers
    // ════════════════════════════════════════════════════

    /**
     * Converts internal module codes to readable names for the message.
     * "MICR_REPAIR"       → "MICR Repair"
     * "DATA_ENTRY"        → "Data Entry"
     * "MICR_REPAIR,DATA_ENTRY" → "MICR Repair, Data Entry"
     */
    private String buildFriendlyModuleNames(String modules) {
        if (modules == null || modules.trim().isEmpty()) {
            return "correction";
        }
        String[] parts = modules.split(",");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(", ");
            String code = part.trim();
            if ("MICR_REPAIR".equals(code))  sb.append("MICR Repair");
            else if ("DATA_ENTRY".equals(code)) sb.append("Data Entry");
            else sb.append(code);
        }
        return sb.toString();
    }

    /** Maps Notification entity → NotificationDto (no lazy loading risk). */
    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setBatchIdStr(n.getBatchIdStr());
        dto.setEventType(n.getEventType());
        dto.setMessage(n.getMessage());
        dto.setLinkUrl(n.getLinkUrl());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}
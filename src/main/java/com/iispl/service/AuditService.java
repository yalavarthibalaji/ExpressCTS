package com.iispl.service;

/**
 * File    : com/iispl/service/AuditService.java
 * Purpose : Single facade for writing audit_log rows.
 *
 * Module constants (use these — don't pass raw strings):
 *   AUTH           — login / logout / lockout
 *   BATCH_UPLOAD   — XML/zip upload, parsing
 *   MICR_REPAIR    — maker fixes a MICR error
 *   ACCOUNT_ENTRY  — maker fills account & amount
 *   CHECKER_QUEUE  — checker pass / reject / refer
 *   DEM_EXPORT     — CXF + CIGF generation, transmission
 *   USER_MGMT      — admin creates / locks user
 *
 * Action constants — use the existing strings consistently:
 *   LOGIN_SUCCESS, LOGIN_FAILED, LOGIN_LOCKED, LOGOUT
 *   BATCH_UPLOADED, BATCH_SUBMITTED, BATCH_RESUBMITTED
 *   BATCH_APPROVED, BATCH_REFERRED_BACK
 *   CHEQUE_PASSED, CHEQUE_REJECTED, CHEQUE_REFERRED
 *   MICR_REPAIRED, EXPORT_GENERATED, EXPORT_TRANSMITTED
 *   USER_LOCKED, USER_UNLOCKED, USER_CREATED
 *
 * IP address is auto-detected from the current HTTP request via
 * SessionUtil.getCurrentIp() — callers don't supply it.
 */
public interface AuditService {

    // ── Module constants ──
    String M_AUTH          = "AUTH";
    String M_BATCH_UPLOAD  = "BATCH_UPLOAD";
    String M_MICR_REPAIR   = "MICR_REPAIR";
    String M_ACCOUNT_ENTRY = "ACCOUNT_ENTRY";
    String M_CHECKER_QUEUE = "CHECKER_QUEUE";
    String M_DEM_EXPORT    = "DEM_EXPORT";
    String M_USER_MGMT     = "USER_MGMT";

    // ── Action constants ──
    String A_LOGIN_SUCCESS      = "LOGIN_SUCCESS";
    String A_LOGIN_FAILED       = "LOGIN_FAILED";
    String A_LOGIN_LOCKED       = "LOGIN_LOCKED";
    String A_LOGOUT             = "LOGOUT";
    String A_BATCH_UPLOADED     = "BATCH_UPLOADED";
    String A_BATCH_SUBMITTED    = "BATCH_SUBMITTED";
    String A_BATCH_RESUBMITTED  = "BATCH_RESUBMITTED";
    String A_BATCH_APPROVED     = "BATCH_APPROVED";
    String A_BATCH_REFERRED     = "BATCH_REFERRED_BACK";
    String A_CHEQUE_PASSED      = "CHEQUE_PASSED";
    String A_CHEQUE_REJECTED    = "CHEQUE_REJECTED";
    String A_CHEQUE_REFERRED    = "CHEQUE_REFERRED";
    String A_MICR_REPAIRED      = "MICR_REPAIRED";
    String A_EXPORT_GENERATED   = "EXPORT_GENERATED";
    String A_EXPORT_TRANSMITTED = "EXPORT_TRANSMITTED";
    String A_USER_LOCKED        = "USER_LOCKED";
    String A_USER_CREATED       = "USER_CREATED";

    // ── Entity type constants ──
    String E_OUTWARD_BATCH  = "OUTWARD_BATCH";
    String E_OUTWARD_CHEQUE = "OUTWARD_CHEQUE";
    String E_USER           = "USER";
    String E_EXPORT_FILE    = "OUTWARD_EXPORT";

    /**
     * Primary log method. Most callers use this.
     */
    void log(Long   userId,
             String module,
             String action,
             String entityType,
             Long   entityId,
             String oldData,
             String newData);

    /**
     * Convenience overload for events with no before/after payload
     * (logins, simple actions). Pass null userId for system events.
     */
    void log(Long userId, String module, String action);
}
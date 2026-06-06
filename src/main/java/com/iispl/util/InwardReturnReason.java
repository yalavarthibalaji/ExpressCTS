package com.iispl.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NPCI-standard inward return reason codes.
 *
 * Used by the checker when returning a cheque during
 * inward verification. The reasonCode is stored in
 * InwardCheckerAction.reasonCode and the reasonText
 * in InwardCheckerAction.reasonText.
 *
 * Source: NPCI CTS-2010 Return Reason Code List
 */
public class InwardReturnReason {

    // ── Reason Codes ──────────────────────────────────────────────────────────

    public static final String CODE_FUNDS_INSUFFICIENT        = "01";
    public static final String CODE_SIGNATURE_MISMATCH        = "02";
    public static final String CODE_ACCOUNT_CLOSED            = "03";
    public static final String CODE_PAYMENT_STOPPED           = "04";
    public static final String CODE_CHEQUE_ALTERED            = "05";
    public static final String CODE_STALE_CHEQUE              = "06";
    public static final String CODE_POST_DATED                = "07";
    public static final String CODE_DRAWEE_DECEASED           = "08";
    public static final String CODE_ACCOUNT_FROZEN            = "09";
    public static final String CODE_REFER_TO_DRAWER           = "10";
    public static final String CODE_MICR_ERROR                = "11";
    public static final String CODE_AMOUNT_IN_WORDS_MISMATCH  = "12";
    public static final String CODE_IMAGE_NOT_LEGIBLE         = "13";
    public static final String CODE_INSTRUMENT_MUTILATED      = "14";

    // ── Reason Texts ─────────────────────────────────────────────────────────

    public static final String TEXT_FUNDS_INSUFFICIENT        = "Funds Insufficient";
    public static final String TEXT_SIGNATURE_MISMATCH        = "Signature Mismatch";
    public static final String TEXT_ACCOUNT_CLOSED            = "Account Closed";
    public static final String TEXT_PAYMENT_STOPPED           = "Payment Stopped by Drawer";
    public static final String TEXT_CHEQUE_ALTERED            = "Cheque Altered / Tampered";
    public static final String TEXT_STALE_CHEQUE              = "Stale Cheque";
    public static final String TEXT_POST_DATED                = "Post Dated Cheque";
    public static final String TEXT_DRAWEE_DECEASED           = "Drawee Deceased";
    public static final String TEXT_ACCOUNT_FROZEN            = "Account Frozen / Under Lien";
    public static final String TEXT_REFER_TO_DRAWER           = "Refer to Drawer";
    public static final String TEXT_MICR_ERROR                = "MICR Defect / Unreadable";
    public static final String TEXT_AMOUNT_IN_WORDS_MISMATCH  = "Amount in Words and Figures Differ";
    public static final String TEXT_IMAGE_NOT_LEGIBLE         = "Cheque Image Not Legible";
    public static final String TEXT_INSTRUMENT_MUTILATED      = "Instrument Mutilated";

    // ── Lookup Map: code → display label ─────────────────────────────────────

    /**
     * Returns a map of reasonCode → "code - reasonText"
     * Used to populate the Return Reason dropdown in the UI.
     *
     * Example entry: "01" → "01 - Funds Insufficient"
     *
     * LinkedHashMap preserves insertion order so dropdown
     * appears in code order (01, 02, 03...).
     */
    public static Map<String, String> getReasonDropdownMap() {

        Map<String, String> map = new LinkedHashMap<>();

        map.put(CODE_FUNDS_INSUFFICIENT,       CODE_FUNDS_INSUFFICIENT       + " - " + TEXT_FUNDS_INSUFFICIENT);
        map.put(CODE_SIGNATURE_MISMATCH,       CODE_SIGNATURE_MISMATCH       + " - " + TEXT_SIGNATURE_MISMATCH);
        map.put(CODE_ACCOUNT_CLOSED,           CODE_ACCOUNT_CLOSED           + " - " + TEXT_ACCOUNT_CLOSED);
        map.put(CODE_PAYMENT_STOPPED,          CODE_PAYMENT_STOPPED          + " - " + TEXT_PAYMENT_STOPPED);
        map.put(CODE_CHEQUE_ALTERED,           CODE_CHEQUE_ALTERED           + " - " + TEXT_CHEQUE_ALTERED);
        map.put(CODE_STALE_CHEQUE,             CODE_STALE_CHEQUE             + " - " + TEXT_STALE_CHEQUE);
        map.put(CODE_POST_DATED,               CODE_POST_DATED               + " - " + TEXT_POST_DATED);
        map.put(CODE_DRAWEE_DECEASED,          CODE_DRAWEE_DECEASED          + " - " + TEXT_DRAWEE_DECEASED);
        map.put(CODE_ACCOUNT_FROZEN,           CODE_ACCOUNT_FROZEN           + " - " + TEXT_ACCOUNT_FROZEN);
        map.put(CODE_REFER_TO_DRAWER,          CODE_REFER_TO_DRAWER          + " - " + TEXT_REFER_TO_DRAWER);
        map.put(CODE_MICR_ERROR,               CODE_MICR_ERROR               + " - " + TEXT_MICR_ERROR);
        map.put(CODE_AMOUNT_IN_WORDS_MISMATCH, CODE_AMOUNT_IN_WORDS_MISMATCH + " - " + TEXT_AMOUNT_IN_WORDS_MISMATCH);
        map.put(CODE_IMAGE_NOT_LEGIBLE,        CODE_IMAGE_NOT_LEGIBLE        + " - " + TEXT_IMAGE_NOT_LEGIBLE);
        map.put(CODE_INSTRUMENT_MUTILATED,     CODE_INSTRUMENT_MUTILATED     + " - " + TEXT_INSTRUMENT_MUTILATED);

        return map;
    }

    /**
     * Given a reason code, returns the full reason text.
     * Returns empty string if code is not found.
     *
     * Example: getReasonText("01") → "Funds Insufficient"
     */
    public static String getReasonText(String code) {

        if (code == null || code.isEmpty()) {
            return "";
        }

        Map<String, String> map = getReasonDropdownMap();
        String entry = map.get(code);

        if (entry == null) {
            return "";
        }

        // entry is "01 - Funds Insufficient", we only want text after " - "
        int separatorIndex = entry.indexOf(" - ");
        if (separatorIndex == -1) {
            return entry;
        }

        return entry.substring(separatorIndex + 3);
    }

    // Private constructor — this is a utility/constants class
    // No one should create an object of this class
    private InwardReturnReason() {
    }
}
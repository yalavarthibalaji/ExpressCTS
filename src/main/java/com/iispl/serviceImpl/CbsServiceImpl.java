package com.iispl.serviceImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.iispl.dto.CbsValidationResult;
import com.iispl.service.CbsService;

/**
 * File    : com/iispl/serviceImpl/CbsServiceImpl.java
 * Purpose : Real CBS validation via Firebase Realtime Database.
 *
 * Flow:
 *   1. Build URL:  {FIREBASE_BASE}/accounts/{accountNo}.json
 *   2. HTTP GET    (uses Java built-in HttpURLConnection, no extra library)
 *   3. Firebase returns JSON object or literal "null" if not found
 *   4. Parse fields: accountHolderName, accountStatus, isActive,
 *                    balance, accountType, ifscCode, bankCode,
 *                    branchCode, cityCode, chequeBookIssued
 *   5. Return CbsValidationResult
 *
 * Firebase field names (exact, case-sensitive):
 *   "accountHolderName"  → String
 *   "accountStatus"      → String  (ACTIVE / INACTIVE / CLOSED / FROZEN)
 *   "isActive"           → boolean (true / false)
 *   "balance"            → Number
 *   "accountType"        → String  (SAVINGS / CURRENT)
 *   "ifscCode"           → String
 *   "bankCode"           → String
 *   "branchCode"         → String
 *   "cityCode"           → String
 *   "chequeBookIssued"   → boolean
 */


//
public class CbsServiceImpl implements CbsService {

    private static final String FIREBASE_BASE_URL =
        "https://outward-cbsvalidation-api-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int CONNECT_TIMEOUT_MS = 6000;
    private static final int READ_TIMEOUT_MS    = 10000;

    // ════════════════════════════════════════════════════
    //  Main Entry Point
    // ════════════════════════════════════════════════════

    @Override
    public CbsValidationResult validateAccount(String accountNo) {

        if (accountNo == null || accountNo.trim().isEmpty()) {
            return CbsValidationResult.serviceError("Account number is blank.");
        }

        String acc = accountNo.trim();
        System.out.println("CbsService → Validating account: " + acc);

        // Step 1: HTTP GET from Firebase
        String json;
        try {
            json = fetchFromFirebase(acc);
        } catch (Exception e) {
            System.err.println("CbsService → Firebase call failed: "
                    + e.getMessage());
            return CbsValidationResult.serviceError(
                "CBS service is temporarily unavailable. "
                + "Please try again. (" + e.getMessage() + ")");
        }

        // Step 2: Firebase returns literal "null" when account not found
        if (json == null
                || json.trim().equals("null")
                || json.trim().isEmpty()) {
            System.out.println("CbsService → Account NOT found: " + acc);
            return CbsValidationResult.notFound(acc);
        }

        // Step 3: Parse the JSON response
        return parseAccountJson(acc, json);
    }

    // ════════════════════════════════════════════════════
    //  HTTP GET — fetch account JSON from Firebase
    // ════════════════════════════════════════════════════

    private String fetchFromFirebase(String accountNo) throws Exception {

        String endpoint = FIREBASE_BASE_URL
            + "/accounts/" + accountNo + ".json";

        System.out.println("CbsService → GET " + endpoint);

        URL               url = new URL(endpoint);
        HttpURLConnection con = null;

        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setRequestProperty("Accept", "application/json");

            int code = con.getResponseCode();
            System.out.println("CbsService → HTTP response: " + code);

            // Firebase security rules not allowing public read
            if (code == 401 || code == 403) {
                throw new Exception(
                    "Firebase access denied (HTTP " + code + "). "
                    + "Set Firebase rules: accounts/.read = true");
            }

            if (code != 200) {
                throw new Exception("Firebase returned HTTP " + code);
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString().trim();

        } catch (java.net.UnknownHostException e) {
            // DNS cannot resolve the Firebase hostname
            throw new Exception(
                "Cannot reach CBS Firebase server. "
                + "Check: (1) internet connection, "
                + "(2) Firebase security rules allow public read.");

        } catch (java.net.SocketTimeoutException e) {
            // Connection established but no response in time
            throw new Exception(
                "CBS Firebase server timed out after "
                + (READ_TIMEOUT_MS / 1000) + " seconds. Please try again.");

        } catch (java.net.ConnectException e) {
            // TCP connection refused
            throw new Exception(
                "Cannot connect to CBS Firebase server. "
                + "Check network/firewall settings.");

        } finally {
            if (con != null) con.disconnect();
        }
    }

    // ════════════════════════════════════════════════════
    //  Parse Firebase JSON response
    //
    //  Expects a flat JSON object.
    //  Firebase field names are case-sensitive.
    // ════════════════════════════════════════════════════

    private CbsValidationResult parseAccountJson(String accountNo,
                                                   String json) {
        try {
            // ── Extract all relevant fields ──
            String  holderName       = extractString(json, "accountHolderName");
            String  accountStatus    = extractString(json, "accountStatus");
            String  isActiveStr      = extractRaw(json,    "isActive");
            String  balanceStr       = extractRaw(json,    "balance");
            String  accountType      = extractString(json, "accountType");
            String  ifscCode         = extractString(json, "ifscCode");
            String  bankCode         = extractString(json, "bankCode");
            String  branchCode       = extractString(json, "branchCode");
            String  cityCode         = extractString(json, "cityCode");
            String  chequeBookStr    = extractRaw(json,    "chequeBookIssued");

            // ── Apply defaults for missing fields ──
            if (holderName    == null || holderName.isEmpty())
                holderName = "Account Holder";
            if (accountStatus == null || accountStatus.isEmpty())
                accountStatus = "UNKNOWN";
            if (accountType   == null) accountType   = "—";
            if (ifscCode      == null) ifscCode      = "—";
            if (bankCode      == null) bankCode      = "—";
            if (branchCode    == null) branchCode    = "—";
            if (cityCode      == null) cityCode      = "—";

            // ── Parse balance ──
            double balance = 0.0;
            if (balanceStr != null && !balanceStr.isEmpty()) {
                try {
                    balance = Double.parseDouble(balanceStr);
                } catch (NumberFormatException ignored) { }
            }

            // ── Parse boolean flags ──
            boolean isActive        = Boolean.parseBoolean(isActiveStr);
            boolean chequeBookIssued = Boolean.parseBoolean(chequeBookStr);

            System.out.println("CbsService → Parsed: "
                    + "holder=" + holderName
                    + " | status=" + accountStatus
                    + " | isActive=" + isActive
                    + " | balance=" + balance);

            // ── Determine if account is usable ──
            // Both isActive flag AND accountStatus must say ACTIVE
            boolean fullyActive = isActive
                    && "ACTIVE".equalsIgnoreCase(accountStatus);

            if (fullyActive) {
                return CbsValidationResult.active(
                    accountNo, holderName,
                    accountType, ifscCode,
                    bankCode, branchCode, cityCode,
                    balance, chequeBookIssued);
            } else {
                // Found but not active — accountStatus tells us why
                return CbsValidationResult.inactive(
                    accountNo, holderName, accountStatus);
            }

        } catch (Exception e) {
            System.err.println("CbsService → JSON parse error: "
                    + e.getMessage());
            return CbsValidationResult.serviceError(
                "Failed to read CBS response. Please try again.");
        }
    }

    // ════════════════════════════════════════════════════
    //  Simple JSON Parsers
    //  Works for FLAT JSON objects only (no nested, no arrays).
    //  No external library required.
    // ════════════════════════════════════════════════════

    /**
     * Extracts a quoted STRING value from flat JSON.
     * e.g. "accountHolderName":"B. Saraswathi" → "B. Saraswathi"
     */
    private String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int    keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;

        // Skip whitespace after ':'
        int start = colonIdx + 1;
        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length() || json.charAt(start) != '"') {
            return null; // value is not a string
        }

        // Find closing quote — handle escaped quotes (\")
        int end = start + 1;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') { end += 2; continue; }
            if (c == '"')  { break;               }
            end++;
        }

        if (end >= json.length()) return null;
        return json.substring(start + 1, end);
    }

    /**
     * Extracts a RAW (unquoted) value from flat JSON.
     * Works for numbers and booleans.
     * e.g. "balance":0.00        → "0.0"
     *      "isActive":true       → "true"
     *      "chequeBookIssued":false → "false"
     */
    private String extractRaw(String json, String key) {
        String search = "\"" + key + "\"";
        int    keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;

        int start = colonIdx + 1;
        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (start >= json.length()) return null;

        // If it's a quoted string, extract string value
        if (json.charAt(start) == '"') {
            return extractString(json, key);
        }

        // Number, boolean, or null — read until , or } or whitespace
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == ',' || c == '}' || c == '\n'
                    || c == '\r' || c == ' ') {
                break;
            }
            end++;
        }

        return json.substring(start, end).trim();
    }
}
package com.iispl.service;

import com.iispl.dto.CbsValidationResult;

/**
 * File    : com/iispl/service/CbsService.java
 * Purpose : CBS (Core Banking System) account validation.
 *           Currently backed by Firebase Realtime Database.
 *
 * Firebase DB URL:
 *   https://cbs-validation-api-default-rtdb.asia-southeast1.firebasedatabase.app
 *
 * Endpoint called:
 *   GET /accounts/{accountNumber}.json
 *
 * Returns account details or null if not found.
 */
public interface CbsService {

    /**
     * Validates an account number against CBS (Firebase RTDB).
     *
     * @param accountNo the account number to validate
     * @return CbsValidationResult with found/active status and full details
     */
    CbsValidationResult validateAccount(String accountNo);
}
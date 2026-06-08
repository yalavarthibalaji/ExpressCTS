package com.iispl.service;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.iispl.dto.CbsValidationResult;
import com.iispl.util.FirebaseConfig;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * File : com/iispl/service/CbsFirebaseService.java
 *
 * Looks up account details from Firebase Realtime Database.
 * Used by ProcessBatchComposer to fill CBS Validation section
 * when checker selects a cheque row.
 *
 * Firebase path: cbs_accounts/<account_number>
 */
public class CbsFirebaseService {

    // Our bank code — cheques drawn on CSB Bank
    private static final String OUR_BANK_CODE = "700";

    public CbsFirebaseService() {
        // Ensure Firebase is initialised before any call
        FirebaseConfig.init();
    }

    /**
     * Looks up account from Firebase.
     *
     * @param accountNumber  drawee_account_number from inward_cheque
     * @param chequesBankCode  bank_code from inward_cheque (should match ours)
     * @return CbsValidationResult — found=false if account does not exist
     */
    public CbsValidationResult validate(String accountNumber, String chequesBankCode) {

        CbsValidationResult result = new CbsValidationResult();

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            return result; // found = false
        }

        String cleanAcct = accountNumber.trim();
        CountDownLatch latch = new CountDownLatch(1);

        DatabaseReference ref = FirebaseDatabase.getInstance()
            .getReference("cbs_accounts")
            .child(cleanAcct);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    result.setFound(true);
                    result.setAccountHolder(
                        getStr(snapshot, "account_holder"));
                    result.setBankCode(
                        getStr(snapshot, "bank_code"));
                    result.setBranchCode(
                        getStr(snapshot, "branch_code"));
                    result.setActive(
                        Boolean.TRUE.equals(snapshot.child("is_active")
                            .getValue(Boolean.class)));
                    result.setAccountType(
                        getStr(snapshot, "account_type"));

                    Double bal = snapshot.child("balance")
                        .getValue(Double.class);
                    result.setBalance(
                        bal != null ? BigDecimal.valueOf(bal) : BigDecimal.ZERO);
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("CbsFirebaseService → Firebase error: "
                    + error.getMessage());
                latch.countDown();
            }
        });

        try {
            // Wait max 5 seconds for Firebase response
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result;
    }

    /**
     * Convenience method — checks if bank code on the cheque
     * matches our bank code (700 = CSB Bank).
     */
    public boolean isBankMatched(String chequesBankCode) {
        return OUR_BANK_CODE.equals(chequesBankCode);
    }

    private String getStr(DataSnapshot snap, String key) {
        Object val = snap.child(key).getValue();
        return val != null ? val.toString() : "—";
    }
}
package com.iispl.dto;

/**
 * File    : com/iispl/dto/CbsValidationResult.java
 * Purpose : Holds the result of a CBS Firebase account lookup.
 *
 * Firebase structure:
 *  /accounts/{accountNo}
 *  {
 *    "accountNumber"      : "8896532164956784",
 *    "accountHolderName"  : "B. Saraswathi",
 *    "accountType"        : "SAVINGS",
 *    "ifscCode"           : "KKBK0000654",
 *    "balance"            : 0.00,
 *    "isActive"           : true,
 *    "accountStatus"      : "ACTIVE",
 *    "chequeBookIssued"   : true,
 *    "bankCode"           : "321",
 *    "branchCode"         : "098",
 *    "cityCode"           : "654"
 *  }
 *
 * Outcomes:
 *   found=true,  active=true  → ACTIVE account — save allowed
 *   found=true,  active=false → INACTIVE/CLOSED/FROZEN — must reject
 *   found=false               → account not found — must reject
 *   errorMessage != null      → CBS service error — retry or reject
 */
public class CbsValidationResult {

    private final boolean found;
    private final boolean active;

    // Account details (from Firebase)
    private final String  accountNo;
    private final String  accountHolderName;
    private final String  accountType;
    private final String  ifscCode;
    private final String  accountStatus;     // ACTIVE / INACTIVE / CLOSED / FROZEN
    private final String  bankCode;
    private final String  branchCode;
    private final String  cityCode;
    private final double  balance;
    private final boolean chequeBookIssued;

    // Error (null when no error)
    private final String  errorMessage;

    // ── private constructor, use static factories ──
    private CbsValidationResult(boolean found,
                                  boolean active,
                                  String  accountNo,
                                  String  accountHolderName,
                                  String  accountType,
                                  String  ifscCode,
                                  String  accountStatus,
                                  String  bankCode,
                                  String  branchCode,
                                  String  cityCode,
                                  double  balance,
                                  boolean chequeBookIssued,
                                  String  errorMessage) {
        this.found              = found;
        this.active             = active;
        this.accountNo          = accountNo;
        this.accountHolderName  = accountHolderName;
        this.accountType        = accountType;
        this.ifscCode           = ifscCode;
        this.accountStatus      = accountStatus;
        this.bankCode           = bankCode;
        this.branchCode         = branchCode;
        this.cityCode           = cityCode;
        this.balance            = balance;
        this.chequeBookIssued   = chequeBookIssued;
        this.errorMessage       = errorMessage;
    }

    // ════════════════════════════════════════════════════
    //  Static Factories
    // ════════════════════════════════════════════════════

    /** Account found and status = ACTIVE and isActive = true. */
    public static CbsValidationResult active(String accountNo,
                                              String holderName,
                                              String accountType,
                                              String ifscCode,
                                              String bankCode,
                                              String branchCode,
                                              String cityCode,
                                              double balance,
                                              boolean chequeBookIssued) {
        return new CbsValidationResult(
            true, true,
            accountNo, holderName, accountType,
            ifscCode, "ACTIVE",
            bankCode, branchCode, cityCode,
            balance, chequeBookIssued,
            null);
    }

    /** Account found but NOT active (INACTIVE / CLOSED / FROZEN). */
    public static CbsValidationResult inactive(String accountNo,
                                                String holderName,
                                                String cbsStatus) {
        return new CbsValidationResult(
            true, false,
            accountNo, holderName, null,
            null, cbsStatus != null ? cbsStatus.toUpperCase() : "INACTIVE",
            null, null, null,
            0.0, false,
            null);
    }

    /** Account number does not exist in Firebase CBS. */
    public static CbsValidationResult notFound(String accountNo) {
        return new CbsValidationResult(
            false, false,
            accountNo, null, null,
            null, null,
            null, null, null,
            0.0, false,
            "Account number not found in CBS records.");
    }

    /** CBS Firebase call failed (network/timeout/parse error). */
    public static CbsValidationResult serviceError(String message) {
        return new CbsValidationResult(
            false, false,
            null, null, null,
            null, null,
            null, null, null,
            0.0, false,
            message);
    }

    // ════════════════════════════════════════════════════
    //  Getters
    // ════════════════════════════════════════════════════

    public boolean isFound()           { return found;             }
    public boolean isActive()          { return active;            }
    public String  getAccountNo()      { return accountNo;         }
    public String  getAccountHolderName() { return accountHolderName; }
    public String  getAccountType()    { return accountType;       }
    public String  getIfscCode()       { return ifscCode;          }
    public String  getAccountStatus()  { return accountStatus;     }
    public String  getBankCode()       { return bankCode;          }
    public String  getBranchCode()     { return branchCode;        }
    public String  getCityCode()       { return cityCode;          }
    public double  getBalance()        { return balance;           }
    public boolean isChequeBookIssued(){ return chequeBookIssued;  }
    public String  getErrorMessage()   { return errorMessage;      }

    /** Human-readable status label for UI display. */
    public String getStatusLabel() {
        if (accountStatus == null) return "Unknown";
        switch (accountStatus.toUpperCase()) {
            case "ACTIVE":   return "Active";
            case "INACTIVE": return "Inactive";
            case "CLOSED":   return "Closed";
            case "FROZEN":   return "Frozen";
            default:         return accountStatus;
        }
    }

    @Override
    public String toString() {
        return "CbsValidationResult{"
            + "found="        + found
            + ", active="     + active
            + ", accountNo='" + accountNo + '\''
            + ", holder='"    + accountHolderName + '\''
            + ", status='"    + accountStatus + '\''
            + ", balance="    + balance
            + ", error='"     + errorMessage + '\''
            + '}';
    }
}
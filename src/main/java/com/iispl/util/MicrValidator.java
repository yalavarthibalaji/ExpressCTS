package com.iispl.util;

/**
 * File    : com/iispl/util/MicrValidator.java
 * Purpose : Reads the isMicrError flag from the XML cheque record.
 *           PASS → cheque MICR is correct, no repair needed.
 *           FAIL → cheque MICR has errors in sub-fields,
 *                  maker must correct manually by viewing cheque image.
 *
 * Note    : The XML file intentionally contains wrong values in
 *           2 of the MICR sub-fields (e.g. cityCode, branchCode)
 *           when isMicrError = FAIL. This simulates real scanner
 *           MICR read errors for demonstration purposes.
 */
public class MicrValidator {

    /**
     * Checks if the cheque has a MICR error based on the XML flag.
     *
     * @param isMicrErrorFlag  value from XML <isMicrError> tag ("PASS" or "FAIL")
     * @return true  → MICR has error   (repair_status = NEEDS_REPAIR)
     *         false → MICR is correct  (repair_status = NOT_REQUIRED)
     */
    public static boolean hasMicrError(String isMicrErrorFlag) {
        if (isMicrErrorFlag == null || isMicrErrorFlag.trim().isEmpty()) {
            // If flag missing in XML, treat as PASS (no error)
            System.out.println("MicrValidator → isMicrError flag missing, treating as PASS");
            return false;
        }
        boolean isError = "FAIL".equalsIgnoreCase(isMicrErrorFlag.trim());
        System.out.println("MicrValidator → isMicrError = "
                + isMicrErrorFlag.trim().toUpperCase()
                + " → " + (isError ? "MICR ERROR" : "MICR OK"));
        return isError;
    }
}
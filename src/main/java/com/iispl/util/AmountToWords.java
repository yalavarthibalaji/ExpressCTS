package com.iispl.util;

/**
 * File    : com/iispl/util/AmountToWords.java
 * Purpose : Converts a numeric amount to Indian currency words.
 *           Used on the Account & Amount Entry screen to auto-fill
 *           the "Amount in Words" field.
 *
 * Examples:
 *   50000.00  → "Fifty Thousand Rupees Only"
 *   100000.00 → "One Lakh Rupees Only"
 *   201000.50 → "Two Lakhs One Thousand Rupees and Fifty Paise Only"
 *   10000000  → "One Crore Rupees Only"
 */
public class AmountToWords {

    private static final String[] UNITS = {
        "", "One", "Two", "Three", "Four", "Five",
        "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
        "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"
    };

    /**
     * Converts a double amount to Indian currency words.
     *
     * @param amount the cheque amount (e.g. 50000.00)
     * @return words string (e.g. "Fifty Thousand Rupees Only")
     */
    public static String convert(double amount) {
        if (amount < 0) return "Invalid Amount";
        if (amount == 0) return "Zero Rupees Only";

        // Split into rupees and paise
        long totalPaise = Math.round(amount * 100);
        long rupees     = totalPaise / 100;
        long paise      = totalPaise % 100;

        StringBuilder result = new StringBuilder();

        if (rupees > 0) {
            result.append(numberToWords(rupees));
            result.append(rupees == 1 ? " Rupee" : " Rupees");
        }

        if (paise > 0) {
            if (rupees > 0) result.append(" and ");
            result.append(numberToWords(paise));
            result.append(paise == 1 ? " Paisa" : " Paise");
        }

        result.append(" Only");
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Converts a long number to words using the Indian system.
     * Indian system: Hundreds → Thousands → Lakhs → Crores
     */
    private static String numberToWords(long n) {
        if (n == 0) return "";

        StringBuilder words = new StringBuilder();

        // Crores (10,000,000)
        if (n >= 10000000) {
            words.append(numberToWords(n / 10000000)).append(" Crore ");
            n %= 10000000;
        }

        // Lakhs (100,000)
        if (n >= 100000) {
            words.append(numberToWords(n / 100000)).append(" Lakh ");
            n %= 100000;
        }

        // Thousands (1,000)
        if (n >= 1000) {
            words.append(numberToWords(n / 1000)).append(" Thousand ");
            n %= 1000;
        }

        // Hundreds
        if (n >= 100) {
            words.append(UNITS[(int)(n / 100)]).append(" Hundred ");
            n %= 100;
        }

        // Tens and Units
        if (n >= 20) {
            words.append(TENS[(int)(n / 10)]);
            if (n % 10 != 0) {
                words.append(" ").append(UNITS[(int)(n % 10)]);
            }
        } else if (n > 0) {
            words.append(UNITS[(int) n]);
        }

        return words.toString().trim();
    }
}
package com.iispl.test;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * GenerateBcryptHashes.java
 * Run this class ONCE to generate BCrypt hashes for each user's password.
 *
 * Steps:
 *   1. Run this main() in Eclipse
 *   2. Copy the printed hashes
 *   3. Replace the hash values in seed_users.sql with the printed ones
 *   4. Run seed_users.sql in your Supabase SQL editor
 *
 * Package : com.iispl.test
 */
public class GenerateBcryptHashes {

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("  BCrypt Hash Generator for CTS Users ");
        System.out.println("======================================");
        System.out.println();

        // Each line: userId → plain-text password
        String[][] users = {
            { "maker1",        "maker123"  },
            { "checker1",      "check123"  },
            { "supervisor1",   "super123"  },
            { "cts_admin",     "cts123"    },
            { "microperator1", "microp123" }
        };

        for (String[] entry : users) {
            String userId   = entry[0];
            String password = entry[1];

            // Generate BCrypt hash with cost factor 12
            String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

            System.out.println("User     : " + userId);
            System.out.println("Password : " + password);
            System.out.println("Hash     : " + hash);
            System.out.println();
        }

        System.out.println("======================================");
        System.out.println("  Copy these hashes into seed_users.sql");
        System.out.println("======================================");
    }
}
package com.iispl.test;

import com.iispl.entity.Role;
import com.iispl.entity.User;
import com.iispl.util.HibernateUtil;
import com.iispl.util.PasswordUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;

/**
 * One-time data seeder — creates default roles and test users.
 * Run this ONCE as a Java Application before testing the login page.
 *
 * Test Credentials (after seeding):
 *   admin       / admin
 *   maker.out   / pass
 *   checker.out / pass
 *   maker.in    / pass
 *   checker.in  / pass
 */
public class SeedTestUsers {

    public static void main(String[] args) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;

        try {
            tx = session.beginTransaction();

            // ── Create 5 Roles ──
            Role adminRole       = createRole("ADMIN",            "Administrator");
            Role makerOutRole    = createRole("MAKER_OUTWARD",    "Maker - Outward");
            Role checkerOutRole  = createRole("CHECKER_OUTWARD",  "Checker - Outward");
            Role makerInRole     = createRole("MAKER_INWARD",     "Maker - Inward");
            Role checkerInRole   = createRole("CHECKER_INWARD",   "Checker - Inward");

            session.save(adminRole);
            session.save(makerOutRole);
            session.save(checkerOutRole);
            session.save(makerInRole);
            session.save(checkerInRole);

            // ── Create 5 Users ──
            session.save(createUser("admin",       "Admin User",       "admin@iispl.com",       "admin", adminRole));
            session.save(createUser("maker.out",   "Rajesh Kumar",     "rajesh@iispl.com",      "pass",  makerOutRole));
            session.save(createUser("checker.out", "Priya Sharma",     "priya@iispl.com",       "pass",  checkerOutRole));
            session.save(createUser("maker.in",    "Suresh Babu",      "suresh@iispl.com",      "pass",  makerInRole));
            session.save(createUser("checker.in",  "Meera Pillai",     "meera@iispl.com",       "pass",  checkerInRole));

            tx.commit();

            System.out.println("\n✓ Seed complete!");
            System.out.println("Test credentials:");
            System.out.println("  admin       / admin");
            System.out.println("  maker.out   / pass");
            System.out.println("  checker.out / pass");
            System.out.println("  maker.in    / pass");
            System.out.println("  checker.in  / pass");

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("Seed failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
            HibernateUtil.shutdown();
        }
    }

    private static Role createRole(String code, String name) {
        Role r = new Role();
        r.setRoleCode(code);
        r.setRoleName(name);
        r.setActive(true);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private static User createUser(String loginId, String fullName, String email,
                                   String plainPassword, Role role) {
        User u = new User();
        u.setUserLoginId(loginId);
        u.setFullName(fullName);
        u.setEmail(email);
        u.setPasswordHash(PasswordUtil.hash(plainPassword));
        u.setRole(role);
        u.setStatus("ACTIVE");
        u.setLocked(false);
        u.setFailedAttempts(0);
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }
}
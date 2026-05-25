package com.iispl.db;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.iispl.entity.OutwardBatch;
import com.iispl.entity.OutwardCheque;
import com.iispl.entity.OutwardChequeStaging;
import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRole;

public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = build();

    private static SessionFactory build() {
        try {
            Configuration cfg = new Configuration();

            // PostgreSQL Driver
            cfg.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");

            // Supabase Direct Connection
            cfg.setProperty(
                "hibernate.connection.url",
                "jdbc:postgresql://db.canypbqhtnhehwnuodnt.supabase.co:5432/postgres?sslmode=require"
            );

            cfg.setProperty("hibernate.connection.username", "postgres");
            cfg.setProperty("hibernate.connection.password", "Expresscts*7674962623");

            // Dialect
            cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

            // Show SQL in console - helpful for debugging
            cfg.setProperty("hibernate.show_sql", "false"); //Changed by giri
            cfg.setProperty("hibernate.format_sql", "true");

            // Tables already created in Supabase manually
            cfg.setProperty("hibernate.hbm2ddl.auto", "none");

            // ── Outward entities ──────────────────────────────────
            cfg.addAnnotatedClass(OutwardBatch.class);
            cfg.addAnnotatedClass(OutwardCheque.class);
            cfg.addAnnotatedClass(OutwardChequeStaging.class);

            // ── User management entities (Phase 1) ────────────────
            cfg.addAnnotatedClass(SystemUser.class);
            cfg.addAnnotatedClass(SystemRole.class);
            cfg.addAnnotatedClass(UserRole.class);

            return cfg.buildSessionFactory();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("SessionFactory build failed: " + e.getMessage());
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        if (SESSION_FACTORY != null) {
            SESSION_FACTORY.close();
        }
    }
}
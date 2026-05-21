package com.iispl.db;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.iispl.entity.OutwardBatch;
import com.iispl.entity.OutwardChequeStaging;

public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = build();

    private static SessionFactory build() {

        try {

            Configuration cfg = new Configuration();

            // PostgreSQL Driver
            cfg.setProperty(
                "hibernate.connection.driver_class",
                "org.postgresql.Driver"
            );

            // Supabase Direct Connection (IPv6 compatible)
            cfg.setProperty(
                "hibernate.connection.url",
                "jdbc:postgresql://db.canypbqhtnhehwnuodnt.supabase.co:5432/postgres?sslmode=require"
            );

            // Username
            cfg.setProperty(
                "hibernate.connection.username",
                "postgres"
            );

            // Password
            cfg.setProperty(
                "hibernate.connection.password",
                "Expresscts*7674962623"
            );

            // Dialect
            cfg.setProperty(
                "hibernate.dialect",
                "org.hibernate.dialect.PostgreSQLDialect"
            );

            // Show SQL in console - helpful for debugging
            cfg.setProperty("hibernate.show_sql", "true");
            cfg.setProperty("hibernate.format_sql", "true");

            // Tables already created in Supabase manually
            cfg.setProperty("hibernate.hbm2ddl.auto", "none");

            // Entity Classes
            cfg.addAnnotatedClass(OutwardBatch.class);
            cfg.addAnnotatedClass(OutwardChequeStaging.class);

            return cfg.buildSessionFactory();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("SessionFactory build failed");
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
package com.iispl.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.iispl.entity.Notification;
import com.iispl.entity.Role;
import com.iispl.entity.User;
import com.iispl.entity.UserSession;
import com.iispl.entity.inward.InwardBatch;
import com.iispl.entity.inward.InwardCheckerAction;
import com.iispl.entity.inward.InwardCheque;
import com.iispl.entity.inward.InwardExport;
import com.iispl.entity.inward.InwardMicrRepair;
import com.iispl.entity.outward.OutwardBatch;
import com.iispl.entity.outward.OutwardCheckerAction;
import com.iispl.entity.outward.OutwardCheque;
import com.iispl.entity.outward.OutwardExport;
import com.iispl.entity.outward.OutwardMicrRepair;

public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = build();

    private static SessionFactory build() {
        try {
            Configuration cfg = new Configuration();

            // Database connection
            cfg.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            cfg.setProperty("hibernate.connection.url", "jdbc:postgresql://localhost:5432/expressCTS");
            cfg.setProperty("hibernate.connection.username", "postgres");
            cfg.setProperty("hibernate.connection.password", "password");
<<<<<<< Updated upstream
=======

>>>>>>> Stashed changes

            // Hibernate settings
            cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            cfg.setProperty("hibernate.show_sql", "true");
            cfg.setProperty("hibernate.format_sql", "false");
            cfg.setProperty("hibernate.hbm2ddl.auto", "update");

            // User Management
            cfg.addAnnotatedClass(Role.class);
            cfg.addAnnotatedClass(User.class);
            cfg.addAnnotatedClass(UserSession.class);
            
            cfg.addAnnotatedClass(Notification.class);

            // Outward
            cfg.addAnnotatedClass(OutwardBatch.class);
            cfg.addAnnotatedClass(OutwardCheque.class);
            cfg.addAnnotatedClass(OutwardExport.class);
            cfg.addAnnotatedClass(OutwardMicrRepair.class);
            cfg.addAnnotatedClass(OutwardCheckerAction.class);

            // Inward
            cfg.addAnnotatedClass(InwardBatch.class);
            cfg.addAnnotatedClass(InwardCheque.class);
            cfg.addAnnotatedClass(InwardExport.class);
            cfg.addAnnotatedClass(InwardMicrRepair.class);
            cfg.addAnnotatedClass(InwardCheckerAction.class);

            return cfg.buildSessionFactory();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to build SessionFactory: " + e.getMessage());
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

package com.iispl.daoImpl;

import com.iispl.dao.AuditLogDao;
import com.iispl.entity.AuditLog;
import com.iispl.util.HibernateUtil;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * File    : com/iispl/daoImpl/AuditLogDaoImpl.java
 * Purpose : Hibernate persistence for audit_log rows.
 */
public class AuditLogDaoImpl implements AuditLogDao {

    @Override
    public boolean save(AuditLog log) {
        if (log == null) return false;

        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            session.persist(log);
            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            // Audit must NEVER break business logic. Log and move on.
            System.err.println("AuditLogDao → save failed (non-critical): "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }
}
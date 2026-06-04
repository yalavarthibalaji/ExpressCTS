package com.iispl.daoImpl;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.iispl.dao.OutwardMicrRepairDao;
import com.iispl.entity.outward.OutwardMicrRepair;
import com.iispl.util.HibernateUtil;

/**
 * File    : com/iispl/daoImpl/OutwardMicrRepairDaoImpl.java
 * Purpose : Saves MICR repair audit log records.
 *           One record per changed MICR sub-field.
 */
public class OutwardMicrRepairDaoImpl implements OutwardMicrRepairDao {

    @Override
    public boolean save(OutwardMicrRepair repair) {
        Session     session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx      = null;
        try {
            tx = session.beginTransaction();
            session.persist(repair);
            tx.commit();
            System.out.println("OutwardMicrRepairDao → Saved repair log: "
                    + repair.getFieldName()
                    + " [" + repair.getOldValue()
                    + " → " + repair.getNewValue() + "]");
            return true;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("OutwardMicrRepairDao → save failed: "
                    + e.getMessage());
            return false;
        } finally {
            session.close();
        }
    }
}
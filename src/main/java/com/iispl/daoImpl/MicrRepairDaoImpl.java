package com.iispl.daoImpl;

import com.iispl.dao.MicrRepairDao;
import com.iispl.db.HibernateUtil;
import com.iispl.entity.MicrRepairEntry;
import com.iispl.entity.OutwardBatch;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class MicrRepairDaoImpl implements MicrRepairDao {

    @Override
    public List<OutwardBatch> getMicrRepairBatches() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // Only fetch batches that are flagged as MICR repair batches
            Query<OutwardBatch> query = session.createQuery(
                "FROM OutwardBatch WHERE isMicrRepairBatch = true ORDER BY scannedAt DESC",
                OutwardBatch.class
            );
            return query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load MICR repair batches: " + e.getMessage());
        }
    }

    @Override
    public List<MicrRepairEntry> getRepairEntriesByBatchId(Long batchId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // Join through outwardCheque → batch to filter by batch
            Query<MicrRepairEntry> query = session.createQuery(
                "FROM MicrRepairEntry m WHERE m.outwardCheque.batch.id = :batchId " +
                "ORDER BY m.id ASC",
                MicrRepairEntry.class
            );
            query.setParameter("batchId", batchId);
            return query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load repair entries: " + e.getMessage());
        }
    }

    @Override
    public MicrRepairEntry getRepairEntryById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            return session.get(MicrRepairEntry.class, id);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load repair entry: " + e.getMessage());
        }
    }

    @Override
    public void saveRepairEntry(MicrRepairEntry entry) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            tx = session.beginTransaction();
            session.persist(entry);
            tx.commit();

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save repair entry: " + e.getMessage());
        }
    }

    @Override
    public void updateRepairEntry(MicrRepairEntry entry) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            tx = session.beginTransaction();
            session.merge(entry);
            tx.commit();

        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to update repair entry: " + e.getMessage());
        }
    }
}
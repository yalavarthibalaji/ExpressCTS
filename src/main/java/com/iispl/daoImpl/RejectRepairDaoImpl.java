package com.iispl.daoImpl;


import com.iispl.dao.RejectRepairDao;
import com.iispl.entity.inward.InwardBatch;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RejectRepairDaoImpl
 *
 * JDBC DAO for Reject & Repair Step 1.
 *
 * Table: inward_batch
 *   Columns used: batch_id, batch_date, source_file_name,
 *                 total_cheques, total_amount, micr_error_count,
 *                 status, repair_status, created_at
 */
public class RejectRepairDaoImpl implements RejectRepairDao {

    private static final Logger LOG =
            Logger.getLogger(RejectRepairDaoImpl.class.getName());

    private static final String JNDI_DS = "java:comp/env/jdbc/expressclearDS";

    // ── SQL ───────────────────────────────────────────────────────────────

    /**
     * Fetch batches eligible for repair:
     *   - status is RECEIVED or PARSED (uploaded but not yet fully processed)
     *   - has at least one MICR error
     */
    private static final String SQL_REPAIR_ELIGIBLE =
            "SELECT batch_id, batch_date, source_file_name, " +
            "       total_cheques, total_amount, micr_error_count, " +
            "       status, repair_status, created_at " +
            "FROM   inward_batch " +
            "WHERE  status IN ('RECEIVED', 'PARSED') " +
            "AND    micr_error_count > 0 " +
            "ORDER  BY created_at DESC";

    private static final String SQL_FIND_BY_BATCH_ID =
            "SELECT batch_id, batch_date, source_file_name, " +
            "       total_cheques, total_amount, micr_error_count, " +
            "       status, repair_status, created_at " +
            "FROM   inward_batch " +
            "WHERE  batch_id = ?";

    // ── DataSource ────────────────────────────────────────────────────────

    private DataSource getDataSource() throws Exception {
        Context ctx = new InitialContext();
        return (DataSource) ctx.lookup(JNDI_DS);
    }

    // ── DAO implementation ────────────────────────────────────────────────

    @Override
    public List<InwardBatch> findRepairEligibleBatches() {
        List<InwardBatch> list = new ArrayList<>();
        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_REPAIR_ELIGIBLE);
            rs   = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findRepairEligibleBatches failed", e);
            throw new RuntimeException("DB error in findRepairEligibleBatches", e);
        } finally {
            close(rs, ps, conn);
        }
        return list;
    }

    @Override
    public InwardBatch findBatchById(String batchId) {
        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_FIND_BY_BATCH_ID);
            ps.setString(1, batchId);
            rs   = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findBatchById failed, batchId=" + batchId, e);
            throw new RuntimeException("DB error in findBatchById", e);
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    // ── Row mapper ────────────────────────────────────────────────────────

    /**
     * Maps ResultSet → InwardBatch entity fields.
     * Uses entity's actual setter names (from @Column annotations):
     *   batchId, batchDate (LocalDate), sourceFileName,
     *   totalCheques, totalAmount (BigDecimal),
     *   micrErrorCount, status, repairStatus
     */
    private InwardBatch mapRow(ResultSet rs) throws SQLException {
        InwardBatch batch = new InwardBatch();

        batch.setBatchId(rs.getString("batch_id"));
        batch.setSourceFileName(rs.getString("source_file_name"));
        batch.setStatus(rs.getString("status"));
        batch.setRepairStatus(rs.getString("repair_status"));
        batch.setTotalCheques(rs.getInt("total_cheques"));
        batch.setMicrErrorCount(rs.getInt("micr_error_count"));

        BigDecimal amount = rs.getBigDecimal("total_amount");
        batch.setTotalAmount(amount != null ? amount : BigDecimal.ZERO);

        // java.sql.Date → LocalDate
        Date batchDateSql = rs.getDate("batch_date");
        if (batchDateSql != null) {
            batch.setBatchDate(batchDateSql.toLocalDate());
        }

        return batch;
    }

    // ── Resource cleanup ──────────────────────────────────────────────────

    private void close(ResultSet rs, PreparedStatement ps, Connection conn) {
        if (rs   != null) try { rs.close();   } catch (SQLException e) { LOG.warning("RS: "   + e.getMessage()); }
        if (ps   != null) try { ps.close();   } catch (SQLException e) { LOG.warning("PS: "   + e.getMessage()); }
        if (conn != null) try { conn.close(); } catch (SQLException e) { LOG.warning("Conn: " + e.getMessage()); }
    }
}
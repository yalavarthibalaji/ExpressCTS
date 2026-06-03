package com.iispl.daoImpl;

import com.iispl.dao.MakerInwardDashboardDao;
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
 * MakerInwardDashboardDaoImpl
 *
 * JDBC DAO — column names match the @Column annotations
 * in com.iispl.entity.inward.InwardBatch exactly.
 *
 * Table: inward_batch
 *   id, batch_id, batch_date, source_file_name, source_file_path,
 *   total_cheques, total_amount, micr_error_count,
 *   status, repair_status, parsed_at, received_by,
 *   created_at, updated_at
 */
public class MakerInwardDashboardDaoImpl implements MakerInwardDashboardDao {

    private static final Logger LOG =
            Logger.getLogger(MakerInwardDashboardDaoImpl.class.getName());

    private static final String JNDI_DS = "java:comp/env/jdbc/expressclearDS";

    // ── SQL ───────────────────────────────────────────────────────────────

    private static final String SQL_FIND_ALL =
            "SELECT batch_id, batch_date, source_file_name, " +
            "       total_cheques, total_amount, micr_error_count, " +
            "       status, repair_status, created_at " +
            "FROM   inward_batch " +
            "ORDER  BY created_at DESC";

    private static final String SQL_FIND_BY_STATUS =
            "SELECT batch_id, batch_date, source_file_name, " +
            "       total_cheques, total_amount, micr_error_count, " +
            "       status, repair_status, created_at " +
            "FROM   inward_batch " +
            "WHERE  status = ? " +
            "ORDER  BY created_at DESC";

    // ── DataSource ────────────────────────────────────────────────────────

    private DataSource getDataSource() throws Exception {
        Context ctx = new InitialContext();
        return (DataSource) ctx.lookup(JNDI_DS);
    }

    // ── DAO implementation ────────────────────────────────────────────────

    @Override
    public List<InwardBatch> findAllInwardBatches() {
        List<InwardBatch> list = new ArrayList<>();
        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_FIND_ALL);
            rs   = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findAllInwardBatches failed", e);
            throw new RuntimeException("DB error in findAllInwardBatches", e);
        } finally {
            close(rs, ps, conn);
        }
        return list;
    }

    @Override
    public List<InwardBatch> findInwardBatchesByStatus(String status) {
        List<InwardBatch> list = new ArrayList<>();
        Connection        conn = null;
        PreparedStatement ps   = null;
        ResultSet         rs   = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_FIND_BY_STATUS);
            ps.setString(1, status);
            rs   = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findInwardBatchesByStatus failed, status=" + status, e);
            throw new RuntimeException("DB error in findInwardBatchesByStatus", e);
        } finally {
            close(rs, ps, conn);
        }
        return list;
    }

    // ── Row mapper ────────────────────────────────────────────────────────

    /**
     * Maps a ResultSet row to InwardBatch using the entity's actual
     * field names and types:
     *
     *   batchId        ← batch_id        (String)
     *   batchDate      ← batch_date      (LocalDate)   FIX: was setUploadedDate()
     *   sourceFileName ← source_file_name (String)     FIX: was setFileName()
     *   totalCheques   ← total_cheques   (int)
     *   totalAmount    ← total_amount    (BigDecimal)
     *   micrErrorCount ← micr_error_count (int)
     *   status         ← status          (String)
     *   repairStatus   ← repair_status   (String)
     */
    private InwardBatch mapRow(ResultSet rs) throws SQLException {
        InwardBatch batch = new InwardBatch();

        // String fields
        batch.setBatchId(rs.getString("batch_id"));
        batch.setSourceFileName(rs.getString("source_file_name")); // FIX: was "FILE_NAME"
        batch.setStatus(rs.getString("status"));
        batch.setRepairStatus(rs.getString("repair_status"));

        // LocalDate — convert java.sql.Date → LocalDate           FIX: was setUploadedDate()
        Date batchDateSql = rs.getDate("batch_date");
        if (batchDateSql != null) {
            batch.setBatchDate(batchDateSql.toLocalDate());
        }

        // Numeric fields
        batch.setTotalCheques(rs.getInt("total_cheques"));

        BigDecimal amount = rs.getBigDecimal("total_amount");
        batch.setTotalAmount(amount != null ? amount : BigDecimal.ZERO);

        batch.setMicrErrorCount(rs.getInt("micr_error_count"));

        return batch;
    }

    // ── Resource cleanup ──────────────────────────────────────────────────

    private void close(ResultSet rs, PreparedStatement ps, Connection conn) {
        if (rs   != null) try { rs.close();   } catch (SQLException e) { LOG.warning("RS close: "   + e.getMessage()); }
        if (ps   != null) try { ps.close();   } catch (SQLException e) { LOG.warning("PS close: "   + e.getMessage()); }
        if (conn != null) try { conn.close(); } catch (SQLException e) { LOG.warning("Conn close: " + e.getMessage()); }
    }
}
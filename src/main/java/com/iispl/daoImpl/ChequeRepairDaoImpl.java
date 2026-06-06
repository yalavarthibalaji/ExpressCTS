package com.iispl.daoImpl;

import com.iispl.dao.ChequeRepairDao;
import com.iispl.entity.inward.InwardCheque;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ChequeRepairDaoImpl
 *
 * JDBC DAO for Step 2 (Date & Amount) and Step 3 (Payee & Account) operations.
 *
 * Table: inward_cheque
 */
public class ChequeRepairDaoImpl implements ChequeRepairDao {

    private static final Logger LOG =
            Logger.getLogger(ChequeRepairDaoImpl.class.getName());

    private static final String JNDI_DS = "java:comp/env/jdbc/expressclearDS";

    // ── SQL ───────────────────────────────────────────────────────────────

    /** All cheques for a given batch, ordered by seq_no. */
    private static final String SQL_BY_BATCH =
            "SELECT id, seq_no, cheque_no, cheque_date, amount, " +
            "       cheque_date_ocr, amount_ocr, " +
            "       presenting_bank_name, " +
            "       drawee_account_number, payee_name, " +
            "       status, repair_status " +
            "FROM   inward_cheque " +
            "WHERE  batch_id = ? " +
            "ORDER  BY seq_no ASC";

    /** Single cheque by PK. */
    private static final String SQL_BY_ID =
            "SELECT id, seq_no, cheque_no, cheque_date, amount, " +
            "       cheque_date_ocr, amount_ocr, " +
            "       presenting_bank_name, " +
            "       drawee_account_number, payee_name, " +
            "       status, repair_status " +
            "FROM   inward_cheque " +
            "WHERE  id = ?";

    /** Step 2 — update OCR date, OCR amount, repair_status. */
    private static final String SQL_UPDATE_DATE_AMOUNT =
            "UPDATE inward_cheque " +
            "SET    cheque_date_ocr = ?, amount_ocr = ?, " +
            "       repair_status = ?, updated_at = ? " +
            "WHERE  id = ?";

    /** Step 3 — update payee name, account number, repair_status. */
    private static final String SQL_UPDATE_PAYEE_ACCOUNT =
            "UPDATE inward_cheque " +
            "SET    payee_name = ?, drawee_account_number = ?, " +
            "       repair_status = ?, updated_at = ? " +
            "WHERE  id = ?";

    /** Count cheques still needing repair in a batch. */
    private static final String SQL_COUNT_PENDING =
            "SELECT COUNT(*) FROM inward_cheque " +
            "WHERE  batch_id = ? AND repair_status = 'NEEDS_REPAIR'";

    // ── DataSource ────────────────────────────────────────────────────────

    private DataSource getDataSource() throws Exception {
        Context ctx = new InitialContext();
        return (DataSource) ctx.lookup(JNDI_DS);
    }

    // ── DAO implementation ────────────────────────────────────────────────

    @Override
    public List<InwardCheque> findChequesByBatchId(String batchId) {
        List<InwardCheque> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_BY_BATCH);
            ps.setString(1, batchId);
            rs   = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findChequesByBatchId failed, batchId=" + batchId, e);
            throw new RuntimeException("DB error in findChequesByBatchId", e);
        } finally {
            close(rs, ps, conn);
        }
        return list;
    }

    @Override
    public InwardCheque findChequeById(Long chequeId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_BY_ID);
            ps.setLong(1, chequeId);
            rs   = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "findChequeById failed, id=" + chequeId, e);
            throw new RuntimeException("DB error in findChequeById", e);
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    @Override
    public boolean updateDateAmount(InwardCheque cheque) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_UPDATE_DATE_AMOUNT);

            // cheque_date_ocr
            if (cheque.getChequeDateOcr() != null) {
                ps.setDate(1, Date.valueOf(cheque.getChequeDateOcr()));
            } else {
                ps.setNull(1, java.sql.Types.DATE);
            }

            // amount_ocr
            BigDecimal amt = cheque.getAmountOcr();
            ps.setBigDecimal(2, amt != null ? amt : BigDecimal.ZERO);

            // repair_status
            ps.setString(3, cheque.getRepairStatus() != null ? cheque.getRepairStatus() : "REPAIRED");

            // updated_at
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

            // WHERE id
            ps.setLong(5, cheque.getChequeId());

            return ps.executeUpdate() == 1;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "updateDateAmount failed, id=" + cheque.getChequeId(), e);
            throw new RuntimeException("DB error in updateDateAmount", e);
        } finally {
            close(null, ps, conn);
        }
    }

    @Override
    public boolean updatePayeeAccount(InwardCheque cheque) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_UPDATE_PAYEE_ACCOUNT);

            // payee_name
            ps.setString(1, cheque.getPayeeName());

            // drawee_account_number
            ps.setString(2, cheque.getDraweeAccountNumber());

            // repair_status
            ps.setString(3, cheque.getRepairStatus() != null ? cheque.getRepairStatus() : "REPAIRED");

            // updated_at
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

            // WHERE id
            ps.setLong(5, cheque.getChequeId());

            return ps.executeUpdate() == 1;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "updatePayeeAccount failed, id=" + cheque.getChequeId(), e);
            throw new RuntimeException("DB error in updatePayeeAccount", e);
        } finally {
            close(null, ps, conn);
        }
    }

    @Override
    public int countPendingRepairs(String batchId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getDataSource().getConnection();
            ps   = conn.prepareStatement(SQL_COUNT_PENDING);
            ps.setString(1, batchId);
            rs   = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "countPendingRepairs failed, batchId=" + batchId, e);
        } finally {
            close(rs, ps, conn);
        }
        return 0;
    }

    // ── Row mapper ────────────────────────────────────────────────────────

    private InwardCheque mapRow(ResultSet rs) throws SQLException {
        InwardCheque c = new InwardCheque();

        c.setChequeId(rs.getLong("id"));
        c.setSeqNo(rs.getInt("seq_no"));
        c.setChequeNo(rs.getString("cheque_no"));
        c.setPresentingBankName(rs.getString("presenting_bank_name"));
        c.setStatus(rs.getString("status"));
        c.setRepairStatus(rs.getString("repair_status"));
        c.setPayeeName(rs.getString("payee_name"));
        c.setDraweeAccountNumber(rs.getString("drawee_account_number"));

        BigDecimal amount = rs.getBigDecimal("amount");
        c.setAmount(amount != null ? amount : BigDecimal.ZERO);

        BigDecimal amountOcr = rs.getBigDecimal("amount_ocr");
        c.setAmountOcr(amountOcr);

        Date chequeDateSql = rs.getDate("cheque_date");
        if (chequeDateSql != null) {
            c.setChequeDate(chequeDateSql.toLocalDate());
        }

        Date chequeDateOcrSql = rs.getDate("cheque_date_ocr");
        if (chequeDateOcrSql != null) {
            c.setChequeDateOcr(chequeDateOcrSql.toLocalDate());
        }

        return c;
    }

    // ── Resource cleanup ──────────────────────────────────────────────────

    private void close(ResultSet rs, PreparedStatement ps, Connection conn) {
        if (rs   != null) try { rs.close();   } catch (SQLException e) { LOG.warning("RS: "   + e.getMessage()); }
        if (ps   != null) try { ps.close();   } catch (SQLException e) { LOG.warning("PS: "   + e.getMessage()); }
        if (conn != null) try { conn.close(); } catch (SQLException e) { LOG.warning("Conn: " + e.getMessage()); }
    }
}
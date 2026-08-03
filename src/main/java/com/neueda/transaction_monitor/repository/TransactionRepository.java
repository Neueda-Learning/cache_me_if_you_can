package com.neueda.transaction_monitor.repository;

import com.neueda.transaction_monitor.model.AccountSummary;
import com.neueda.transaction_monitor.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbcTemplate;

    public TransactionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── RowMapper ──────────────────────────────────────────────────────────────
    // Shared across all query methods — maps SQL columns to Transaction POJO fields.
    private final RowMapper<Transaction> transactionRowMapper = (rs, rowNum) -> {
        Transaction t = new Transaction();
        t.setTransactionId(rs.getInt("Transaction_ID"));
        t.setAccountId(rs.getInt("Account_ID"));
        t.setPayeeId(rs.getInt("Payee_ID"));
        t.setAmount(rs.getBigDecimal("Amount"));
        t.setTransactionType(rs.getString("Transaction_Type"));
        Timestamp ts = rs.getTimestamp("Time_Stamp");
        if (ts != null) {
            t.setTimeStamp(ts.toLocalDateTime());
        }
        return t;
    };

    // ── Section A: PreparedStatement CRUD ─────────────────────────────────────

    /**
     * Inserts a new transaction using an explicit PreparedStatementCreator.
     * The generated primary key is set back on the returned object.
     */
    public Transaction save(Transaction t) {
        String sql = "INSERT INTO TRANSACTION_TABLE (Account_ID, Payee_ID, Amount, Transaction_Type) "
                   + "VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, t.getAccountId());
            ps.setInt(2, t.getPayeeId());
            ps.setBigDecimal(3, t.getAmount());
            ps.setString(4, t.getTransactionType());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            t.setTransactionId(key.intValue());
        }
        return t;
    }

    /**
     * Finds a single transaction by primary key.
     * JdbcTemplate uses a PreparedStatement internally for the '?' parameter.
     */
    public Optional<Transaction> findById(int id) {
        String sql = "SELECT * FROM TRANSACTION_TABLE WHERE Transaction_ID = ?";
        List<Transaction> results = jdbcTemplate.query(sql, transactionRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Returns all transactions for a given account, most recent first.
     */
    public List<Transaction> findByAccountId(int accountId) {
        String sql = "SELECT * FROM TRANSACTION_TABLE WHERE Account_ID = ? ORDER BY Time_Stamp DESC";
        return jdbcTemplate.query(sql, transactionRowMapper, accountId);
    }

    /**
     * Returns all transactions, most recent first.
     */
    public List<Transaction> findAll() {
        String sql = "SELECT * FROM TRANSACTION_TABLE ORDER BY Time_Stamp DESC";
        return jdbcTemplate.query(sql, transactionRowMapper);
    }

    /**
     * Returns transactions whose Time_Stamp falls within the given range (inclusive).
     */
    public List<Transaction> findByTimeWindow(LocalDateTime from, LocalDateTime to) {
        String sql = "SELECT * FROM TRANSACTION_TABLE WHERE Time_Stamp BETWEEN ? AND ? ORDER BY Time_Stamp DESC";
        return jdbcTemplate.query(sql, transactionRowMapper,
                Timestamp.valueOf(from), Timestamp.valueOf(to));
    }

    // ── Section B: CallableStatement Reporting ─────────────────────────────────

    /**
     * Calls the stored procedure GetAccountSummary(accountId).
     * This is the ONLY place a CallableStatement is used in the codebase.
     *
     * Prerequisite — run once manually in MySQL Workbench:
     *
     *   DELIMITER $$
     *   CREATE PROCEDURE GetAccountSummary(IN p_account_id INT)
     *   BEGIN
     *       SELECT Account_ID,
     *              COUNT(*)   AS total_transactions,
     *              SUM(Amount) AS total_amount
     *       FROM TRANSACTION_TABLE
     *       WHERE Account_ID = p_account_id;
     *   END$$
     *   DELIMITER ;
     */
    public AccountSummary getAccountTransactionSummary(int accountId) {
        return jdbcTemplate.execute(
            (Connection conn) -> conn.prepareCall("{CALL GetAccountSummary(?)}"),
            (CallableStatement cs) -> {
                cs.setInt(1, accountId);
                try (ResultSet rs = cs.executeQuery()) {
                    if (rs.next()) {
                        return new AccountSummary(
                            rs.getInt("Account_ID"),
                            rs.getInt("total_transactions"),
                            rs.getBigDecimal("total_amount")
                        );
                    }
                }
                return new AccountSummary(accountId, 0, java.math.BigDecimal.ZERO);
            }
        );
    }
}

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

    /**
     * Calls the stored procedure GetTransactionList(p_filter_by, p_value) which
     * returns transactions together with the payer and payee account numbers.
     * filterBy values expected by the procedure: 'ALL', 'TXN', 'ACCOUNT', 'PAYEE'
     */
    public java.util.List<com.neueda.transaction_monitor.model.TransactionView> getTransactionList(String filterBy, String value) {
        try {
            final String fb = (filterBy == null || filterBy.isBlank()) ? "ALL" : filterBy.toUpperCase();
            final String val = (value == null) ? "" : value;

            org.springframework.jdbc.core.CallableStatementCreator csc = new org.springframework.jdbc.core.CallableStatementCreator() {
                @Override
                public java.sql.CallableStatement createCallableStatement(java.sql.Connection con) throws java.sql.SQLException {
                    java.sql.CallableStatement cs = con.prepareCall("{CALL GetTransactionList(?, ?)}");
                    cs.setString(1, fb);
                    cs.setString(2, val);
                    return cs;
                }
            };

            org.springframework.jdbc.core.CallableStatementCallback<java.util.List<com.neueda.transaction_monitor.model.TransactionView>> callback =
                    new org.springframework.jdbc.core.CallableStatementCallback<java.util.List<com.neueda.transaction_monitor.model.TransactionView>>() {
                        @Override
                        public java.util.List<com.neueda.transaction_monitor.model.TransactionView> doInCallableStatement(java.sql.CallableStatement cs) throws java.sql.SQLException, org.springframework.dao.DataAccessException {
                            java.util.List<com.neueda.transaction_monitor.model.TransactionView> out = new java.util.ArrayList<>();
                            try (java.sql.ResultSet rs = cs.executeQuery()) {
                                while (rs.next()) {
                                    com.neueda.transaction_monitor.model.TransactionView v = new com.neueda.transaction_monitor.model.TransactionView();
                                    v.setTransactionId(rs.getInt("Transaction_ID"));
                                    v.setAccountNumber(rs.getString("Account_Number"));
                                    v.setPayeeAccountNumber(rs.getString("Payee_Account_Number"));
                                    v.setAmount(rs.getBigDecimal("Amount"));
                                    v.setTransactionType(rs.getString("Transaction_Type"));
                                    java.sql.Timestamp ts = rs.getTimestamp("Time_Stamp");
                                    if (ts != null) v.setTimeStamp(ts.toLocalDateTime());
                                    out.add(v);
                                }
                            }
                            return out;
                        }
                    };

            try {
                return jdbcTemplate.execute(csc, callback);
            } catch (Exception e) {
                log.warn("Stored-proc call failed (GetTransactionList), falling back to inline query: {}", e.getMessage());
                // Fallback: run an equivalent SELECT using JdbcTemplate so UI still gets results
                String sqlBase = "SELECT t.Transaction_ID, a.Account_Number AS Account_Number, p.Payee_Account_Number AS Payee_Account_Number, t.Amount, t.Transaction_Type, t.Time_Stamp " +
                        "FROM TRANSACTION_TABLE t JOIN ACCOUNT a ON t.Account_ID = a.Account_ID JOIN PAYEE p ON t.Payee_ID = p.Payee_ID ";

                java.util.List<com.neueda.transaction_monitor.model.TransactionView> out = new java.util.ArrayList<>();
                try {
                    switch (fb) {
                        case "TXN": {
                            String q = sqlBase + "WHERE t.Transaction_ID = ? ORDER BY t.Time_Stamp DESC";
                            out = jdbcTemplate.query(q, (rs, rowNum) -> {
                                com.neueda.transaction_monitor.model.TransactionView v = new com.neueda.transaction_monitor.model.TransactionView();
                                v.setTransactionId(rs.getInt("Transaction_ID"));
                                v.setAccountNumber(rs.getString("Account_Number"));
                                v.setPayeeAccountNumber(rs.getString("Payee_Account_Number"));
                                v.setAmount(rs.getBigDecimal("Amount"));
                                v.setTransactionType(rs.getString("Transaction_Type"));
                                java.sql.Timestamp ts = rs.getTimestamp("Time_Stamp");
                                if (ts != null) v.setTimeStamp(ts.toLocalDateTime());
                                return v;
                            }, Integer.valueOf(val));
                            break;
                        }
                        case "ACCOUNT": {
                            String q = sqlBase + "WHERE a.Account_Number = ? ORDER BY t.Time_Stamp DESC";
                            out = jdbcTemplate.query(q, (rs, rowNum) -> {
                                com.neueda.transaction_monitor.model.TransactionView v = new com.neueda.transaction_monitor.model.TransactionView();
                                v.setTransactionId(rs.getInt("Transaction_ID"));
                                v.setAccountNumber(rs.getString("Account_Number"));
                                v.setPayeeAccountNumber(rs.getString("Payee_Account_Number"));
                                v.setAmount(rs.getBigDecimal("Amount"));
                                v.setTransactionType(rs.getString("Transaction_Type"));
                                java.sql.Timestamp ts = rs.getTimestamp("Time_Stamp");
                                if (ts != null) v.setTimeStamp(ts.toLocalDateTime());
                                return v;
                            }, val);
                            break;
                        }
                        case "PAYEE": {
                            String q = sqlBase + "WHERE p.Payee_Account_Number = ? ORDER BY t.Time_Stamp DESC";
                            out = jdbcTemplate.query(q, (rs, rowNum) -> {
                                com.neueda.transaction_monitor.model.TransactionView v = new com.neueda.transaction_monitor.model.TransactionView();
                                v.setTransactionId(rs.getInt("Transaction_ID"));
                                v.setAccountNumber(rs.getString("Account_Number"));
                                v.setPayeeAccountNumber(rs.getString("Payee_Account_Number"));
                                v.setAmount(rs.getBigDecimal("Amount"));
                                v.setTransactionType(rs.getString("Transaction_Type"));
                                java.sql.Timestamp ts = rs.getTimestamp("Time_Stamp");
                                if (ts != null) v.setTimeStamp(ts.toLocalDateTime());
                                return v;
                            }, val);
                            break;
                        }
                        default: {
                            String q = sqlBase + "ORDER BY t.Time_Stamp DESC";
                            out = jdbcTemplate.query(q, (rs, rowNum) -> {
                                com.neueda.transaction_monitor.model.TransactionView v = new com.neueda.transaction_monitor.model.TransactionView();
                                v.setTransactionId(rs.getInt("Transaction_ID"));
                                v.setAccountNumber(rs.getString("Account_Number"));
                                v.setPayeeAccountNumber(rs.getString("Payee_Account_Number"));
                                v.setAmount(rs.getBigDecimal("Amount"));
                                v.setTransactionType(rs.getString("Transaction_Type"));
                                java.sql.Timestamp ts = rs.getTimestamp("Time_Stamp");
                                if (ts != null) v.setTimeStamp(ts.toLocalDateTime());
                                return v;
                            });
                        }
                    }
                } catch (Exception ex2) {
                    log.error("Fallback inline query also failed for GetTransactionList: {}", ex2.getMessage(), ex2);
                    return java.util.Collections.emptyList();
                }
                return out;
            }
        } catch (Exception e) {
            log.error("Error calling GetTransactionList: {}", e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
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
        // If caller provided a timestamp, include it in the INSERT so the DB
        // preserves the supplied value. Otherwise let the DB default (CURRENT_TIMESTAMP).
        boolean hasTs = t.getTimeStamp() != null;
        String sql;
        if (hasTs) {
            sql = "INSERT INTO TRANSACTION_TABLE (Account_ID, Payee_ID, Amount, Transaction_Type, Time_Stamp) VALUES (?, ?, ?, ?, ?)";
        } else {
            sql = "INSERT INTO TRANSACTION_TABLE (Account_ID, Payee_ID, Amount, Transaction_Type) VALUES (?, ?, ?, ?)";
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, t.getAccountId());
            ps.setInt(2, t.getPayeeId());
            ps.setBigDecimal(3, t.getAmount());
            ps.setString(4, t.getTransactionType());
            if (hasTs) {
                ps.setTimestamp(5, Timestamp.valueOf(t.getTimeStamp()));
            }
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

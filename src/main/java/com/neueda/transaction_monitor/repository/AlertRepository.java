package com.neueda.transaction_monitor.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.neueda.transaction_monitor.model.Alert;
import com.neueda.transaction_monitor.model.Alert.AlertSeverity;
import com.neueda.transaction_monitor.model.Alert.AlertStatus;

@Repository
public class AlertRepository {

    private static final String BASE_SELECT = """
        SELECT a.Alert_ID, a.Rule_ID, r.Rule_Name, a.Transaction_ID,
               acct.Account_Number AS Account_Number, p.Payee_Account_Number AS Payee_Account_Number,
               a.Status, a.Severity, a.Created_At, a.Closed_At
        FROM ALERT a
        LEFT JOIN RULE r ON a.Rule_ID = r.Rule_ID
        LEFT JOIN TRANSACTION_TABLE t ON a.Transaction_ID = t.Transaction_ID
        LEFT JOIN ACCOUNT acct ON t.Account_ID = acct.Account_ID
        LEFT JOIN PAYEE p ON t.Payee_ID = p.Payee_ID
        
        """;

    private final JdbcTemplate jdbcTemplate;

    public AlertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Alert create(Long ruleId, Long transactionId, AlertSeverity severity) {
        String sql = "INSERT INTO ALERT (Rule_ID, Transaction_ID, Status, Severity) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, ruleId);
            ps.setLong(2, transactionId);
            ps.setString(3, AlertStatus.OPEN.name());
            ps.setString(4, severity.name());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Unable to create alert ID");

        return findById(key.longValue())
            .orElseThrow(() -> new IllegalStateException("Created alert could not be read back"));
    }

    public Optional<Alert> findById(Long alertId) {
        String sql = BASE_SELECT + " WHERE a.Alert_ID = ?";
        return jdbcTemplate.query(sql, alertRowMapper(), alertId).stream().findFirst();
    }

    public List<Alert> findAll(AlertStatus status, AlertSeverity severity) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null) { sql.append(" AND a.Status = ?"); params.add(status.name()); }
        if (severity != null) { sql.append(" AND a.Severity = ?"); params.add(severity.name()); }

        sql.append(" ORDER BY a.Created_At DESC");
        return jdbcTemplate.query(sql.toString(), alertRowMapper(), params.toArray());
    }

    // Group alerts by rule and severity in the last `minutes` minutes. Returns a simple summary record.
    public record GroupedAlert(Long ruleId, AlertSeverity severity, Integer count, java.time.LocalDateTime firstCreated) {}

    public List<GroupedAlert> findGroupedAlerts(int minutes, AlertSeverity severity) {
        StringBuilder sql = new StringBuilder(
            "SELECT a.Rule_ID, a.Severity, COUNT(*) AS cnt, MIN(a.Created_At) AS first_created " +
            "FROM ALERT a WHERE a.Created_At >= ? ");

        List<Object> params = new ArrayList<>();
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusMinutes(minutes);
        params.add(java.sql.Timestamp.valueOf(cutoff));

        if (severity != null) {
            sql.append(" AND a.Severity = ?");
            params.add(severity.name());
        }

        sql.append(" GROUP BY a.Rule_ID, a.Severity ORDER BY cnt DESC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Long ruleId = rs.getLong("Rule_ID");
            AlertSeverity sev = AlertSeverity.valueOf(rs.getString("Severity"));
            int cnt = rs.getInt("cnt");
            java.util.Calendar utc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            java.sql.Timestamp ts = rs.getTimestamp("first_created", utc);
            java.time.LocalDateTime first = ts == null ? null : ts.toLocalDateTime();
            return new GroupedAlert(ruleId, sev, cnt, first);
        }, params.toArray());
    }

    public void updateStatus(Long alertId, AlertStatus status, LocalDateTime closedAt) {
        Timestamp closedTs = closedAt == null ? null : Timestamp.valueOf(closedAt);
        int updated = jdbcTemplate.update(
            "UPDATE ALERT SET Status = ?, Closed_At = ? WHERE Alert_ID = ?",
            status.name(), closedTs, alertId
        );
        if (updated == 0) throw new IllegalStateException("Alert status update failed");
    }

    private RowMapper<Alert> alertRowMapper() {
        return (rs, rowNum) -> {
            Alert alert = new Alert();
            alert.setAlertId(rs.getLong("Alert_ID"));
            alert.setRuleId(rs.getLong("Rule_ID"));
            // optional joined fields
            try { alert.setRuleName(rs.getString("Rule_Name")); } catch (Exception e) { /* ignore */ }
            alert.setTransactionId(rs.getLong("Transaction_ID"));
            alert.setStatus(AlertStatus.valueOf(rs.getString("Status")));
            alert.setSeverity(AlertSeverity.valueOf(rs.getString("Severity")));

            // joined account/payee
            try { alert.setAccountNumber(rs.getString("Account_Number")); } catch (Exception e) { /* ignore */ }
            try { alert.setPayeeAccountNumber(rs.getString("Payee_Account_Number")); } catch (Exception e) { /* ignore */ }

            // Read TIMESTAMP columns using an explicit UTC Calendar to avoid implicit timezone shifts
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            java.sql.Timestamp createdTs = rs.getTimestamp("Created_At", utc);
            if (createdTs != null) {
                alert.setCreatedAt(createdTs.toLocalDateTime());
            }

            java.sql.Timestamp closedTs = rs.getTimestamp("Closed_At", utc);
            if (closedTs != null) {
                alert.setClosedAt(closedTs.toLocalDateTime());
            }

            return alert;
        };
    }
}

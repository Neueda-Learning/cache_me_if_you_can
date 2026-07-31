package com.neueda.transaction_monitor.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.neueda.transaction_monitor.model.Rule;
import com.neueda.transaction_monitor.model.Rule.RuleSeverity;
import com.neueda.transaction_monitor.model.Rule.RuleType;

@Repository
public class RuleRepository {

    private static final String BASE_SELECT = """
        SELECT Rule_ID, Rule_Name, Rule_Type, Severity, Threshold, Time_Window, Active_Status
        FROM RULE
        """;

    private final JdbcTemplate jdbcTemplate;

    public RuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Rule create(String ruleName, RuleType ruleType, RuleSeverity severity,
                       java.math.BigDecimal threshold, Integer timeWindow) {
        String sql = "INSERT INTO RULE (Rule_Name, Rule_Type, Severity, Threshold, Time_Window) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, ruleName);
            ps.setString(2, ruleType.name());
            ps.setString(3, severity.name());
            if (threshold != null) ps.setBigDecimal(4, threshold); else ps.setNull(4, java.sql.Types.DECIMAL);
            if (timeWindow != null) ps.setInt(5, timeWindow); else ps.setNull(5, java.sql.Types.INTEGER);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Unable to create rule ID");

        return findById(key.longValue())
            .orElseThrow(() -> new IllegalStateException("Created rule could not be read back"));
    }

    public Optional<Rule> findById(Long ruleId) {
        String sql = BASE_SELECT + " WHERE Rule_ID = ?";
        return jdbcTemplate.query(sql, ruleRowMapper(), ruleId).stream().findFirst();
    }

    public List<Rule> findAll(RuleType ruleType, Boolean activeStatus) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (ruleType != null) { sql.append(" AND Rule_Type = ?"); params.add(ruleType.name()); }
        if (activeStatus != null) { sql.append(" AND Active_Status = ?"); params.add(activeStatus); }

        sql.append(" ORDER BY Rule_ID ASC");
        return jdbcTemplate.query(sql.toString(), ruleRowMapper(), params.toArray());
    }

    public Rule update(Long ruleId, String ruleName, RuleSeverity severity,
                       java.math.BigDecimal threshold, Integer timeWindow, Boolean activeStatus) {
        StringBuilder sql = new StringBuilder("UPDATE RULE SET ");
        List<Object> params = new ArrayList<>();

        if (ruleName != null)     { sql.append("Rule_Name = ?, ");    params.add(ruleName); }
        if (severity != null)     { sql.append("Severity = ?, ");     params.add(severity.name()); }
        if (threshold != null)    { sql.append("Threshold = ?, ");    params.add(threshold); }
        if (timeWindow != null)   { sql.append("Time_Window = ?, ");  params.add(timeWindow); }
        if (activeStatus != null) { sql.append("Active_Status = ?, "); params.add(activeStatus); }

        if (params.isEmpty()) throw new IllegalArgumentException("No fields to update");

        // Remove trailing ", "
        String query = sql.substring(0, sql.length() - 2) + " WHERE Rule_ID = ?";
        params.add(ruleId);

        int updated = jdbcTemplate.update(query, params.toArray());
        if (updated == 0) throw new java.util.NoSuchElementException("Rule not found: " + ruleId);

        return findById(ruleId)
            .orElseThrow(() -> new IllegalStateException("Updated rule could not be read back"));
    }

    public void delete(Long ruleId) {
        int deleted = jdbcTemplate.update("DELETE FROM RULE WHERE Rule_ID = ?", ruleId);
        if (deleted == 0) throw new java.util.NoSuchElementException("Rule not found: " + ruleId);
    }

    public void toggleActive(Long ruleId, boolean activeStatus) {
        int updated = jdbcTemplate.update(
            "UPDATE RULE SET Active_Status = ? WHERE Rule_ID = ?", activeStatus, ruleId);
        if (updated == 0) throw new java.util.NoSuchElementException("Rule not found: " + ruleId);
    }

    private RowMapper<Rule> ruleRowMapper() {
        return (rs, rowNum) -> {
            Rule rule = new Rule();
            rule.setRuleId(rs.getLong("Rule_ID"));
            rule.setRuleName(rs.getString("Rule_Name"));
            rule.setRuleType(RuleType.valueOf(rs.getString("Rule_Type")));
            rule.setSeverity(RuleSeverity.valueOf(rs.getString("Severity")));
            java.math.BigDecimal threshold = rs.getBigDecimal("Threshold");
            rule.setThreshold(threshold);
            int tw = rs.getInt("Time_Window");
            rule.setTimeWindow(rs.wasNull() ? null : tw);
            rule.setActiveStatus(rs.getBoolean("Active_Status"));
            return rule;
        };
    }
}

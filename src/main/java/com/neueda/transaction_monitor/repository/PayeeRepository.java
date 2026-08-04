package com.neueda.transaction_monitor.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import com.neueda.transaction_monitor.model.Payee;

@Repository
public class PayeeRepository {

    private final JdbcTemplate jdbcTemplate;

    public PayeeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Payee> findAll() {
        String sql = "SELECT Payee_ID, Payee_Name, Payee_Account_Number, Bank_Name FROM PAYEE ORDER BY Payee_ID ASC";
        return jdbcTemplate.query(sql, rowMapper());
    }

    public Optional<Payee> findById(Integer id) {
        String sql = "SELECT Payee_ID, Payee_Name, Payee_Account_Number, Bank_Name FROM PAYEE WHERE Payee_ID = ?";
        return jdbcTemplate.query(sql, rowMapper(), id).stream().findFirst();
    }

    public Payee save(Payee payee) {
        String sql = "INSERT INTO PAYEE (Payee_Name, Payee_Account_Number, Bank_Name) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, payee.getPayeeName());
            ps.setString(2, payee.getPayeeAccountNumber());
            ps.setString(3, payee.getBankName());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to create payee");
        return findById(key.intValue()).orElseThrow();
    }

    private RowMapper<Payee> rowMapper() {
        return (rs, rowNum) -> {
            Payee p = new Payee();
            p.setPayeeId(rs.getInt("Payee_ID"));
            p.setPayeeName(rs.getString("Payee_Name"));
            p.setPayeeAccountNumber(rs.getString("Payee_Account_Number"));
            p.setBankName(rs.getString("Bank_Name"));
            return p;
        };
    }
}


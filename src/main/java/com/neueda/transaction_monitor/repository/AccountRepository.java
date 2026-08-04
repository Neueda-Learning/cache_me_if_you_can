package com.neueda.transaction_monitor.repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import com.neueda.transaction_monitor.entity.Account;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Account> findAll() {
        String sql = "SELECT Account_ID, Account_Number, Account_Holder_Name, Balance, Created_At FROM ACCOUNT ORDER BY Account_ID ASC";
        return jdbcTemplate.query(sql, rowMapper());
    }

    public Optional<Account> findById(Integer id) {
        String sql = "SELECT Account_ID, Account_Number, Account_Holder_Name, Balance, Created_At FROM ACCOUNT WHERE Account_ID = ?";
        return jdbcTemplate.query(sql, rowMapper(), id).stream().findFirst();
    }

    public Account save(Account account) {
        String sql = "INSERT INTO ACCOUNT (Account_Number, Account_Holder_Name, Balance) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, account.getAccountNumber());
            ps.setString(2, account.getAccountHolderName());
            ps.setBigDecimal(3, account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to create account");
        return findById(key.intValue()).orElseThrow();
    }

    private RowMapper<Account> rowMapper() {
        return (rs, rowNum) -> {
            Account a = new Account();
            a.setAccountId(rs.getInt("Account_ID"));
            a.setAccountNumber(rs.getString("Account_Number"));
            a.setAccountHolderName(rs.getString("Account_Holder_Name"));
            a.setBalance(rs.getBigDecimal("Balance"));
            java.sql.Timestamp ts = rs.getTimestamp("Created_At");
            if (ts != null) a.setCreatedAt(ts.toLocalDateTime());
            return a;
        };
    }
}


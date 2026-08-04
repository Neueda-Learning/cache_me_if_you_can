package com.neueda.transaction_monitor.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.neueda.transaction_monitor.model.AppUser;

@Repository
public class AppUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public AppUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AppUser> findByUsername(String username) {
        String sql = "SELECT User_ID, Username, Password_Hash, Full_Name, Email, Role FROM APP_USER WHERE Username = ?";
        return jdbcTemplate.query(sql, rowMapper(), username).stream().findFirst();
    }

    public void save(String username, String passwordHash, String fullName, String email, String role) {
        jdbcTemplate.update(
            "INSERT INTO APP_USER (Username, Password_Hash, Full_Name, Email, Role) VALUES (?, ?, ?, ?, ?)",
            username, passwordHash, fullName, email, role
        );
    }

    public void updatePassword(String username, String newHash) {
        jdbcTemplate.update(
            "UPDATE APP_USER SET Password_Hash = ? WHERE Username = ?",
            newHash, username
        );
    }

    public int countAdmins() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM APP_USER WHERE Role = 'ADMIN'", Integer.class);
        return count == null ? 0 : count;
    }

    private RowMapper<AppUser> rowMapper() {
        return (rs, rowNum) -> {
            AppUser u = new AppUser();
            u.setUserId(rs.getLong("User_ID"));
            u.setUsername(rs.getString("Username"));
            u.setPasswordHash(rs.getString("Password_Hash"));
            u.setFullName(rs.getString("Full_Name"));
            u.setEmail(rs.getString("Email"));
            u.setRole(rs.getString("Role"));
            return u;
        };
    }
}


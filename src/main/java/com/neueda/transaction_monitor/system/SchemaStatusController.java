package com.neueda.transaction_monitor.system;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SchemaStatusController {

    private static final List<String> REQUIRED_TABLES = List.of(
        "ACCOUNT",
        "PAYEE",
        "RULE",
        "TRANSACTION_TABLE",
        "ALERT"
    );

    private final JdbcTemplate jdbcTemplate;

    public SchemaStatusController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/schema-status")
    public ResponseEntity<Map<String, Object>> schemaStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Boolean> tables = new LinkedHashMap<>();

        for (String table : REQUIRED_TABLES) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                table
            );
            tables.put(table, count != null && count > 0);
        }

        boolean allPresent = tables.values().stream().allMatch(Boolean::booleanValue);
        response.put("database", jdbcTemplate.queryForObject("SELECT DATABASE()", String.class));
        response.put("allRequiredTablesPresent", allPresent);
        response.put("tables", tables);

        return ResponseEntity.ok(response);
    }
}


package com.neueda.transaction_monitor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.neueda.transaction_monitor.service.AuthService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class TransactionMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionMonitorApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAdmin(
			AuthService authService,
			@Value("${hawk.admin.username:admin}") String username,
			@Value("${hawk.admin.password:Admin@1234}") String password,
			@Value("${hawk.admin.fullname:System Administrator}") String fullName) {
		return args -> authService.seedAdminIfAbsent(username, password, fullName);
	}

	/**
	 * Create / update stored procedures which cannot be executed via schema.sql
	 * because the JDBC script runner does not understand DELIMITER directives.
	 */
	@Bean
	CommandLineRunner createStoredProcedures(DataSource dataSource) {
		Logger log = LoggerFactory.getLogger(TransactionMonitorApplication.class);
		return args -> {
			try (Connection con = dataSource.getConnection(); Statement st = con.createStatement()) {
				st.execute("DROP PROCEDURE IF EXISTS GetTransactionList");
				String proc = "CREATE PROCEDURE GetTransactionList(IN p_filter_by VARCHAR(20), IN p_value VARCHAR(64)) BEGIN " +
					"SELECT t.Transaction_ID, a.Account_Number AS Account_Number, p.Payee_Account_Number AS Payee_Account_Number, t.Amount, t.Transaction_Type, t.Time_Stamp " +
					"FROM TRANSACTION_TABLE t JOIN ACCOUNT a ON t.Account_ID = a.Account_ID JOIN PAYEE p ON t.Payee_ID = p.Payee_ID " +
					"WHERE ( p_filter_by = 'ALL' OR (p_filter_by = 'TXN' AND t.Transaction_ID = CAST(p_value AS UNSIGNED)) OR (p_filter_by = 'ACCOUNT' AND a.Account_Number = p_value) OR (p_filter_by = 'PAYEE' AND p.Payee_Account_Number = p_value) ) " +
					"ORDER BY t.Time_Stamp DESC; END";
				st.execute(proc);

				st.execute("DROP PROCEDURE IF EXISTS GetAccountSummary");
				String accountSummaryProc = "CREATE PROCEDURE GetAccountSummary(IN p_account_id INT) BEGIN " +
					"SELECT p_account_id AS Account_ID, COUNT(*) AS total_transactions, COALESCE(SUM(Amount), 0) AS total_amount " +
					"FROM TRANSACTION_TABLE WHERE Account_ID = p_account_id; END";
				st.execute(accountSummaryProc);

				log.info("Stored procedure GetTransactionList created/updated");
				log.info("Stored procedure GetAccountSummary created/updated");
			} catch (SQLException e) {
				log.warn("Could not create stored procedures: {}", e.getMessage());
			}
		};
	}
}

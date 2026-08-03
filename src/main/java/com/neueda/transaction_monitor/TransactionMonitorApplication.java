package com.neueda.transaction_monitor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import com.neueda.transaction_monitor.service.AuthService;

@SpringBootApplication
@EnableAsync
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
}

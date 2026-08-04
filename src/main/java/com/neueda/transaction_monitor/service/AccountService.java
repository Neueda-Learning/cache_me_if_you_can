package com.neueda.transaction_monitor.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.transaction_monitor.entity.Account;
import com.neueda.transaction_monitor.repository.AccountRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<Account> getAll() {
        return accountRepository.findAll();
    }

    public Account getById(Integer id) {
        return accountRepository.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Account not found: " + id));
    }

    public Account create(Account account) {
        if (account.getAccountHolderName() == null || account.getAccountHolderName().isBlank()) {
            throw new IllegalArgumentException("Account holder name is required");
        }
        if (account.getAccountNumber() == null || account.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        return accountRepository.save(account);
    }
}


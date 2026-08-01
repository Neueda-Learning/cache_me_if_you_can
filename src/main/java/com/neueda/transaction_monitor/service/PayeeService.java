package com.neueda.transaction_monitor.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.transaction_monitor.model.Payee;
import com.neueda.transaction_monitor.repository.PayeeRepository;

@Service
public class PayeeService {

    private final PayeeRepository payeeRepository;

    public PayeeService(PayeeRepository payeeRepository) {
        this.payeeRepository = payeeRepository;
    }

    public List<Payee> getAll() {
        return payeeRepository.findAll();
    }

    public Payee getById(Integer id) {
        return payeeRepository.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Payee not found: " + id));
    }

    public Payee create(Payee payee) {
        if (payee.getPayeeName() == null || payee.getPayeeName().isBlank()) {
            throw new IllegalArgumentException("Payee name is required");
        }
        if (payee.getPayeeAccountNumber() == null || payee.getPayeeAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Payee account number is required");
        }
        if (payee.getBankName() == null || payee.getBankName().isBlank()) {
            throw new IllegalArgumentException("Bank name is required");
        }
        return payeeRepository.save(payee);
    }
}


package com.neueda.transaction_monitor.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.neueda.transaction_monitor.common.ApiResponse;
import com.neueda.transaction_monitor.model.Payee;
import com.neueda.transaction_monitor.service.PayeeService;

@RestController
@RequestMapping("/api/payees")
public class PayeeController {

    private final PayeeService payeeService;

    public PayeeController(PayeeService payeeService) {
        this.payeeService = payeeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Payee>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Payees retrieved", payeeService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Payee>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success("Payee retrieved", payeeService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Payee>> create(@RequestBody Payee payee) {
        Payee created = payeeService.create(payee);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Payee created", created));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> notFound(java.util.NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.ErrorResponse.of(ex.getMessage()));
    }
}


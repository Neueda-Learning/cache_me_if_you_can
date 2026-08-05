package com.neueda.transaction_monitor.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    private Integer accountId;
    private String accountNumber;
    private String accountHolderName;
    private String email;
    private BigDecimal balance;
    private LocalDateTime createdAt;


}

package com.neueda.transaction_monitor.model;

public class Payee {
    private Integer payeeId;
    private String payeeName;
    private String payeeAccountNumber;
    private String bankName;

    public Integer getPayeeId()               { return payeeId; }
    public void setPayeeId(Integer v)         { this.payeeId = v; }
    public String getPayeeName()              { return payeeName; }
    public void setPayeeName(String v)        { this.payeeName = v; }
    public String getPayeeAccountNumber()     { return payeeAccountNumber; }
    public void setPayeeAccountNumber(String v){ this.payeeAccountNumber = v; }
    public String getBankName()               { return bankName; }
    public void setBankName(String v)         { this.bankName = v; }
}


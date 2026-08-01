package com.neueda.transaction_monitor.model;

public class AppUser {
    private Long userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String role; // "ADMIN" or "USER"

    public Long getUserId()            { return userId; }
    public void setUserId(Long v)      { this.userId = v; }
    public String getUsername()        { return username; }
    public void setUsername(String v)  { this.username = v; }
    public String getPasswordHash()    { return passwordHash; }
    public void setPasswordHash(String v){ this.passwordHash = v; }
    public String getFullName()        { return fullName; }
    public void setFullName(String v)  { this.fullName = v; }
    public String getEmail()           { return email; }
    public void setEmail(String v)     { this.email = v; }
    public String getRole()            { return role; }
    public void setRole(String v)      { this.role = v; }
}


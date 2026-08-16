package com.wallet.wallet.dto;

import com.wallet.wallet.model.Wallet;
import com.wallet.wallet.model.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private WalletStatus status;
    private LocalDateTime updatedAt;

    public WalletResponse() {
    }

    public WalletResponse(Wallet wallet) {
        this.id = wallet.getId();
        this.userId = wallet.getUserId();
        this.balance = wallet.getBalance();
        this.status = wallet.getStatus();
        this.updatedAt = wallet.getUpdatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

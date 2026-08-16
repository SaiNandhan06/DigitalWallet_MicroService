package com.wallet.transaction.dto;

import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private String message;

    public TransactionResponse() {
    }

    public TransactionResponse(Transaction transaction) {
        this.id = transaction.getId();
        this.senderId = transaction.getSenderId();
        this.receiverId = transaction.getReceiverId();
        this.amount = transaction.getAmount();
        this.status = transaction.getStatus();
        this.createdAt = transaction.getCreatedAt();
    }

    public TransactionResponse(Transaction transaction, String message) {
        this(transaction);
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

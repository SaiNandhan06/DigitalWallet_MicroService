package com.wallet.transaction.dto;

import java.math.BigDecimal;

public class AmountDto {
    private BigDecimal amount;

    public AmountDto() {
    }

    public AmountDto(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

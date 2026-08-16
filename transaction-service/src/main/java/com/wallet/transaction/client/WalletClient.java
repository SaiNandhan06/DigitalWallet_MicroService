package com.wallet.transaction.client;

import com.wallet.transaction.dto.AmountDto;
import com.wallet.transaction.dto.WalletDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @GetMapping("/wallets/{userId}")
    WalletDto getWallet(@PathVariable("userId") Long userId);

    @PutMapping("/wallets/{userId}/debit")
    WalletDto debit(@PathVariable("userId") Long userId, @RequestBody AmountDto amountDto);

    @PutMapping("/wallets/{userId}/credit")
    WalletDto credit(@PathVariable("userId") Long userId, @RequestBody AmountDto amountDto);
}

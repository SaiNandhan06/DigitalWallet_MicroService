package com.wallet.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @PostMapping("/wallets/{userId}/init")
    Object initWallet(@PathVariable Long userId);
}

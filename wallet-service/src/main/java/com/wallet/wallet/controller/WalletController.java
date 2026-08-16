package com.wallet.wallet.controller;

import com.wallet.wallet.dto.AmountRequest;
import com.wallet.wallet.dto.WalletResponse;
import com.wallet.wallet.model.Wallet;
import com.wallet.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{userId}/init")
    public ResponseEntity<WalletResponse> initWallet(@PathVariable Long userId) {
        Wallet wallet = walletService.initWallet(userId);
        return ResponseEntity.ok(new WalletResponse(wallet));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(new WalletResponse(wallet));
    }

    @PostMapping("/{userId}/topup")
    public ResponseEntity<WalletResponse> topup(@PathVariable Long userId, @RequestBody AmountRequest request) {
        Wallet wallet = walletService.topup(userId, request.getAmount());
        return ResponseEntity.ok(new WalletResponse(wallet));
    }

    @PutMapping("/{userId}/debit")
    public ResponseEntity<WalletResponse> debit(@PathVariable Long userId, @RequestBody AmountRequest request) {
        Wallet wallet = walletService.debit(userId, request.getAmount());
        return ResponseEntity.ok(new WalletResponse(wallet));
    }

    @PutMapping("/{userId}/credit")
    public ResponseEntity<WalletResponse> credit(@PathVariable Long userId, @RequestBody AmountRequest request) {
        Wallet wallet = walletService.credit(userId, request.getAmount());
        return ResponseEntity.ok(new WalletResponse(wallet));
    }

    @PutMapping("/{userId}/freeze")
    public ResponseEntity<WalletResponse> freeze(@PathVariable Long userId) {
        Wallet wallet = walletService.freezeWallet(userId);
        return ResponseEntity.ok(new WalletResponse(wallet));
    }

    @PutMapping("/{userId}/unfreeze")
    public ResponseEntity<WalletResponse> unfreeze(@PathVariable Long userId) {
        Wallet wallet = walletService.unfreezeWallet(userId);
        return ResponseEntity.ok(new WalletResponse(wallet));
    }
}

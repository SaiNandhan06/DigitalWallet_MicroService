package com.wallet.wallet.service;

import com.wallet.wallet.model.Wallet;
import com.wallet.wallet.model.WalletStatus;
import com.wallet.wallet.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public Wallet initWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> walletRepository.save(new Wallet(userId)));
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found for userId: " + userId));
    }

    @Transactional
    public Wallet topup(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Top-up amount must be positive");
        }
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet is frozen");
        }
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet debit(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit amount must be positive");
        }
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet is frozen");
        }
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet credit(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit amount must be positive");
        }
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet is frozen");
        }
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet freezeWallet(Long userId) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setStatus(WalletStatus.FROZEN);
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet unfreezeWallet(Long userId) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setStatus(WalletStatus.ACTIVE);
        return walletRepository.save(wallet);
    }
}

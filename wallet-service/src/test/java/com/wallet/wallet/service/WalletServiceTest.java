package com.wallet.wallet.service;

import com.wallet.wallet.model.Wallet;
import com.wallet.wallet.model.WalletStatus;
import com.wallet.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private WalletService walletService;

    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        testWallet = new Wallet(1L);
        testWallet.setId(10L);
        testWallet.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void initWallet_createsNewWalletIfNotExist() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet wallet = walletService.initWallet(1L);

        assertNotNull(wallet);
        assertEquals(1L, wallet.getUserId());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void topup_increasesBalance() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet updated = walletService.topup(1L, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("700.00"), updated.getBalance());
    }

    @Test
    void debit_decreasesBalance() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet updated = walletService.debit(1L, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("300.00"), updated.getBalance());
    }

    @Test
    void debit_throwsExceptionWhenInsufficientBalance() {
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));

        assertThrows(ResponseStatusException.class, () -> walletService.debit(1L, new BigDecimal("1000.00")));
    }

    @Test
    void debit_throwsExceptionWhenWalletFrozen() {
        testWallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(testWallet));

        assertThrows(ResponseStatusException.class, () -> walletService.debit(1L, new BigDecimal("100.00")));
    }
}

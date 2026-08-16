package com.wallet.transaction.service;

import com.wallet.transaction.client.UserClient;
import com.wallet.transaction.client.WalletClient;
import com.wallet.transaction.dto.AmountDto;
import com.wallet.transaction.dto.TransactionResponse;
import com.wallet.transaction.dto.TransferRequest;
import com.wallet.transaction.dto.WalletDto;
import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TransactionStatus;
import com.wallet.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletClient walletClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private TransactionService transactionService;

    private WalletDto senderWallet;

    @BeforeEach
    void setUp() {
        senderWallet = new WalletDto();
        senderWallet.setId(10L);
        senderWallet.setUserId(1L);
        senderWallet.setBalance(new BigDecimal("500.00"));
        senderWallet.setStatus("ACTIVE");
    }

    @Test
    void transfer_successfullyTransfersMoney() {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("200.00"));

        when(walletClient.getWallet(1L)).thenReturn(senderWallet);
        when(walletClient.debit(eq(1L), any(AmountDto.class))).thenReturn(senderWallet);
        when(walletClient.credit(eq(2L), any(AmountDto.class))).thenReturn(new WalletDto());

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            tx.setId(100L);
            return tx;
        });

        TransactionResponse response = transactionService.transfer(request);

        assertNotNull(response);
        assertEquals(TransactionStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        verify(walletClient, times(1)).debit(eq(1L), any(AmountDto.class));
        verify(walletClient, times(1)).credit(eq(2L), any(AmountDto.class));
    }

    @Test
    void transfer_throwsWhenSameSenderAndReceiver() {
        TransferRequest request = new TransferRequest(1L, 1L, new BigDecimal("100.00"));

        assertThrows(ResponseStatusException.class, () -> transactionService.transfer(request));
    }

    @Test
    void transfer_throwsWhenInsufficientBalance() {
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("1000.00"));
        when(walletClient.getWallet(1L)).thenReturn(senderWallet);

        assertThrows(ResponseStatusException.class, () -> transactionService.transfer(request));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void transfer_throwsWhenSenderWalletFrozen() {
        senderWallet.setStatus("FROZEN");
        TransferRequest request = new TransferRequest(1L, 2L, new BigDecimal("100.00"));
        when(walletClient.getWallet(1L)).thenReturn(senderWallet);

        assertThrows(ResponseStatusException.class, () -> transactionService.transfer(request));
    }
}

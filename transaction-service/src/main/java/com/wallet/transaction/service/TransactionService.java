package com.wallet.transaction.service;

import com.wallet.transaction.client.UserClient;
import com.wallet.transaction.client.WalletClient;
import com.wallet.transaction.dto.*;
import com.wallet.transaction.model.Transaction;
import com.wallet.transaction.model.TransactionStatus;
import com.wallet.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final WalletClient walletClient;
    private final UserClient userClient;

    public TransactionService(TransactionRepository transactionRepository, WalletClient walletClient, UserClient userClient) {
        this.transactionRepository = transactionRepository;
        this.walletClient = walletClient;
        this.userClient = userClient;
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        if (request.getSenderId() == null || request.getReceiverId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender and receiver IDs are required");
        }
        if (request.getSenderId().equals(request.getReceiverId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender and receiver cannot be the same user");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be greater than 0");
        }

        Long senderId = request.getSenderId();
        Long receiverId = request.getReceiverId();
        BigDecimal amount = request.getAmount();

        // 1. Verify user profiles via User Service (Inter-service validation)
        try {
            userClient.getUserProfile(senderId);
            userClient.getUserProfile(receiverId);
        } catch (Exception e) {
            log.warn("User validation failed via User Service: {}", e.getMessage());
        }

        // 2. Check sender's balance via Wallet Service
        WalletDto senderWallet;
        try {
            senderWallet = walletClient.getWallet(senderId);
        } catch (Exception e) {
            transactionRepository.save(new Transaction(senderId, receiverId, amount, TransactionStatus.FAILED));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to retrieve sender wallet: " + e.getMessage());
        }

        if ("FROZEN".equalsIgnoreCase(senderWallet.getStatus())) {
            transactionRepository.save(new Transaction(senderId, receiverId, amount, TransactionStatus.FAILED));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender wallet is frozen");
        }

        if (senderWallet.getBalance() == null || senderWallet.getBalance().compareTo(amount) < 0) {
            transactionRepository.save(new Transaction(senderId, receiverId, amount, TransactionStatus.FAILED));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds in sender wallet");
        }

        // 2. Perform Debit on Sender Wallet
        try {
            walletClient.debit(senderId, new AmountDto(amount));
        } catch (Exception e) {
            transactionRepository.save(new Transaction(senderId, receiverId, amount, TransactionStatus.FAILED));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debit failed: " + e.getMessage());
        }

        // 3. Perform Credit on Receiver Wallet
        /*
         * Note on Distributed Transaction Safety / Saga Pattern:
         * In a microservices architecture, if the debit succeeds above but the credit call fails below
         * (e.g. network partition or receiver wallet frozen/not found), sender funds have been deducted.
         * For MVP simplicity, we log this clearly as a MANUAL RECONCILIATION CASE.
         * In a production system, a Saga orchestrator or compensating transaction (crediting back the sender)
         * or two-phase commit / event-driven architecture with outbox pattern should be implemented.
         */
        try {
            walletClient.credit(receiverId, new AmountDto(amount));
        } catch (Exception e) {
            log.error("CRITICAL MANUAL RECONCILIATION REQUIRED: Debit succeeded for sender {} (amount: {}), but credit failed for receiver {}: {}",
                    senderId, amount, receiverId, e.getMessage());
            Transaction failedTx = transactionRepository.save(new Transaction(senderId, receiverId, amount, TransactionStatus.FAILED));
            return new TransactionResponse(failedTx, "Debit succeeded but credit failed. Flagged for manual reconciliation.");
        }

        // 4. Record Success Transaction
        Transaction successTx = transactionRepository.save(new Transaction(senderId, receiverId, amount, TransactionStatus.SUCCESS));
        return new TransactionResponse(successTx, "Transfer completed successfully");
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getUserTransactions(Long userId) {
        return transactionRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(TransactionResponse::new)
                .collect(Collectors.toList());
    }
}

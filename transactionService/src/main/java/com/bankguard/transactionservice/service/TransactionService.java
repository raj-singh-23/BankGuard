package com.bankguard.transactionservice.service;

import com.bankguard.transactionservice.entity.Transaction;
import com.bankguard.transactionservice.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionById(Long transactionId) {
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);
        return transaction.orElse(null);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public Transaction updateTransaction(Long transactionId, Transaction transactionDetails) {
        Optional<Transaction> existingTransaction = transactionRepository.findById(transactionId);
        if (existingTransaction.isPresent()) {
            Transaction transaction = existingTransaction.get();
            transaction.setAmount(transactionDetails.getAmount());
            transaction.setCity(transactionDetails.getCity());
            transaction.setState(transactionDetails.getState());
            transaction.setIpAddress(transactionDetails.getIpAddress());
            transaction.setTime(transactionDetails.getTime());
            transaction.setRiskScore(transactionDetails.getRiskScore());
            transaction.setReceiverAccountNumber(transactionDetails.getReceiverAccountNumber());
            transaction.setCustomerId(transactionDetails.getCustomerId());
            return transactionRepository.save(transaction);
        }
        return null;
    }

    public boolean deleteTransaction(Long transactionId) {
        if (transactionRepository.existsById(transactionId)) {
            transactionRepository.deleteById(transactionId);
            return true;
        }
        return false;
    }

    public List<Transaction> getTransactionsByCustomerId(Long customerId) {
        return transactionRepository.findByCustomerId(customerId);
    }

    public List<Transaction> getTransactionsByReceiverAccount(String receiverAccountNumber) {
        return transactionRepository.findByReceiverAccountNumber(receiverAccountNumber);
    }
}

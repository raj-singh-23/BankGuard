package com.bankguard.transactionservice.controller;

import com.bankguard.transactionservice.dto.TransactionCreationRequest;
import com.bankguard.transactionservice.entity.Customer;
import com.bankguard.transactionservice.entity.Transaction;
import com.bankguard.transactionservice.repository.CustomerRepository;
import com.bankguard.transactionservice.service.TransactionEnrichmentIntegrationService;
import com.bankguard.transactionservice.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@Slf4j
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionEnrichmentIntegrationService enrichmentIntegrationService;

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTransaction(@RequestBody TransactionCreationRequest request) {
        try {
            // 1. Validate sender customer exists
            Customer senderCustomer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Sender customer not found with ID: " + request.getCustomerId()));

            // 2. Map Request to a transient (unsaved) Transaction Entity ( current transaction )
            Transaction transaction = new Transaction();
            transaction.setAmount(request.getAmount());
            transaction.setCity(request.getCity());
            transaction.setState(request.getState());
            transaction.setIpAddress(request.getIpAddress());
            transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
            transaction.setCustomerId(request.getCustomerId());
            transaction.setTime(LocalDateTime.now());
            transaction.setRiskScore(0.0);

            // 3. SAVE transaction FIRST to get the transactionId for enrichment service
            // The enrichment service MUST have a valid transactionId to send to AlertCaseService
            Transaction savedTransaction = transactionService.saveTransaction(transaction);

            log.info("✓ Transaction saved to database with ID: {}", savedTransaction.getTransactionId());

            // 4. Get enrichment analysis from enrichment service (now with valid transactionId)
            Object enrichedResponse = enrichmentIntegrationService.enrichTransactionWithService(savedTransaction, senderCustomer);

            // 5. Process enrichment response - handles account balance updates if status is "genuine"
            // This will throw ReceiverAccountNotFoundException if receiver's account doesn't exist
            // Or TransactionProcessingException for other processing errors
            Map<String, Object> processedResponse = enrichmentIntegrationService.processEnrichmentResponse(
                    enrichedResponse,
                    savedTransaction,
                    senderCustomer
            );

            return new ResponseEntity<>(
                    Map.of(
                            "status", "SUCCESS",
                            "transaction", savedTransaction,
                            "processedResponse", processedResponse
                    ),
                    HttpStatus.CREATED
            );

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", "INVALID_REQUEST",
                            "message", e.getMessage()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        } catch (com.bankguard.transactionservice.exception.ReceiverAccountNotFoundException e) {
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", "RECEIVER_NOT_FOUND",
                            "message", e.getMessage(),
                            "receiverAccountNumber", e.getAccountNumber()
                    ),
                    HttpStatus.NOT_FOUND
            );
        } catch (com.bankguard.transactionservice.exception.TransactionProcessingException e) {
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", e.getErrorCode(),
                            "message", e.getMessage()
                    ),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    Map.of(
                            "status", "ERROR",
                            "errorCode", "INTERNAL_ERROR",
                            "message", e.getMessage()
                    ),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }



    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        List<Transaction> transactions = transactionService.getAllTransactions();
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long transactionId) {
        Transaction transaction = transactionService.getTransactionById(transactionId);
        if (transaction != null) {
            return new ResponseEntity<>(transaction, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<Transaction> updateTransaction(@PathVariable Long transactionId, @RequestBody Transaction transactionDetails) {
        Transaction updatedTransaction = transactionService.updateTransaction(transactionId, transactionDetails);
        if (updatedTransaction != null) {
            return new ResponseEntity<>(updatedTransaction, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<String> deleteTransaction(@PathVariable Long transactionId) {
        boolean deleted = transactionService.deleteTransaction(transactionId);
        if (deleted) {
            return new ResponseEntity<>("Transaction deleted successfully", HttpStatus.OK);
        }
        return new ResponseEntity<>("Transaction not found", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Transaction>> getTransactionsByCustomerId(@PathVariable Long customerId) {
        List<Transaction> transactions = transactionService.getTransactionsByCustomerId(customerId);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @GetMapping("/receiver/{receiverAccountNumber}")
    public ResponseEntity<List<Transaction>> getTransactionsByReceiverAccount(@PathVariable String receiverAccountNumber) {
        List<Transaction> transactions = transactionService.getTransactionsByReceiverAccount(receiverAccountNumber);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }
}

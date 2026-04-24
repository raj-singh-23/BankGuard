package com.bankguard.enrichmentservice.controller;

import com.bankguard.enrichmentservice.dto.*;
import com.bankguard.enrichmentservice.service.EnrichmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enrich")
public class EnrichmentController {

    @Autowired
    private EnrichmentService enrichmentService;

    @PostMapping("/transaction/with-decision-and-alert")
    public ResponseEntity<TransactionDecisionResponse> enrichTransactionWithDecisionAndAlert(@RequestBody EnrichmentRequest request) {
        try {
            // Validate input
            if (request == null || request.getCurrentTransaction() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Complete enrichment and decision flow with conditional alert
            TransactionDecisionResponse response = enrichmentService.enrichAndDecideWithConditionalAlert(request);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/transaction")
    public ResponseEntity<EnrichedTransactionDTO> enrichTransaction(@RequestBody EnrichmentRequest request) {
        try {
            // Validate input
            if (request == null || request.getCurrentTransaction() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Enrich transaction
            EnrichedTransactionDTO enrichedTransaction = enrichmentService.enrichTransaction(request);
            return new ResponseEntity<>(enrichedTransaction, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/transaction/with-decision")
    public ResponseEntity<TransactionDecisionResponse> enrichTransactionWithDecision(@RequestBody EnrichmentRequest request) {
        try {
            // Validate input
            if (request == null || request.getCurrentTransaction() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Enrich transaction
            EnrichedTransactionDTO enrichedTransaction = enrichmentService.enrichTransaction(request);
            
            // Convert to decision request
            DecisionRequest decisionRequest = enrichmentService.convertToDecisionRequest(enrichedTransaction);
            
            // Get Gemini decision
            GeminiDecisionResponse geminiDecision = enrichmentService.getGeminiDecision(decisionRequest);
            
            // Combine results
            TransactionDecisionResponse response = new TransactionDecisionResponse();

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @PostMapping("/validate/amount")
    public ResponseEntity<Boolean> validateAmount(@RequestParam Double amount) {
        try {
            boolean isValid = enrichmentService.validateTransactionAmount(amount);
            return new ResponseEntity<>(isValid, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/validate/balance")
    public ResponseEntity<Boolean> validateBalance(
            @RequestParam Double customerBalance,
            @RequestParam Double transactionAmount) {
        try {
            boolean isValid = enrichmentService.validateSufficientBalance(customerBalance, transactionAmount);
            return new ResponseEntity<>(isValid, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/validate/ip")
    public ResponseEntity<Boolean> validateIpAddress(@RequestParam String ipAddress) {
        try {
            boolean isValid = enrichmentService.validateIpAddress(ipAddress);
            return new ResponseEntity<>(isValid, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

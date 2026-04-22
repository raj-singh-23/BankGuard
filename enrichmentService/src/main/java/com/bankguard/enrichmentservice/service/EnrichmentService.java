package com.bankguard.enrichmentservice.service;

import com.bankguard.enrichmentservice.dto.*;
import com.bankguard.enrichmentservice.client.AlertCaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EnrichmentService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AlertCaseClient alertCaseClient;

    private static final String DECISION_ENGINE_URL = "http://localhost:7002/api/gemini/analyze-transaction";


    /**
     * Complete enrichment and decision flow with conditional AlertCase routing
     * 1. Enrich transaction (add customer info and previous transactions)
     * 2. Convert to decision request
     * 3. Get Gemini decision
     * 4. If decision is "flagged" or "terminated" → send to AlertCaseService
     * 5. If decision is "genuine" → return response without alerting
     */

    public TransactionDecisionResponse enrichAndDecideWithConditionalAlert(EnrichmentRequest request) {
        // Step 1: Enrich transaction
        log.info("Step 1: Enriching transaction for customer: {}", request.getCurrentTransaction().getCustomerId());
        EnrichedTransactionDTO enrichedTransaction = enrichTransaction(request);

        // Step 2: Convert to decision request
        log.info("Step 2: Converting enriched transaction to decision request");
        DecisionRequest decisionRequest = convertToDecisionRequest(enrichedTransaction);

        // Step 3: Get Gemini decision
        log.info("Step 3: Getting Gemini decision for transaction amount: {}", enrichedTransaction.getAmount());
        GeminiDecisionResponse geminiDecision = getGeminiDecision(decisionRequest);
        log.info("Step 3 Result: Gemini Decision = {}, Risk Score = {}", geminiDecision.getDecision(), geminiDecision.getRiskScore());

        // Step 4: Create response with explicit alertSent = false by default
        TransactionDecisionResponse response = new TransactionDecisionResponse();
        response.setEnrichedTransaction(enrichedTransaction);
        response.setGeminiDecision(geminiDecision);
        response.setAlertSent(false);  // EXPLICITLY SET TO FALSE BY DEFAULT

        // Step 5: Conditional AlertCase routing based on EXACT decision value
        String decision = geminiDecision.getDecision();
        String trimmedDecision = decision != null ? decision.trim().toLowerCase() : "";
        log.info("Step 5: Decision received: '{}' → Trimmed & Lowercase: '{}'", decision, trimmedDecision);

        // ONLY send alert if decision is EXACTLY "flagged" or "terminated"
        if ("flagged".equals(trimmedDecision) || "terminated".equals(trimmedDecision)) {
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("FRAUD DETECTED - SENDING ALERT TO ALERTCASE SERVICE");
            log.warn("Decision: {}, Risk Score: {}, Transaction ID: {}",
                    trimmedDecision, geminiDecision.getRiskScore(), enrichedTransaction.getTransactionId());
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // Create alert payload and send to AlertCaseService
            AlertCasePayload alertPayload = new AlertCasePayload();
            alertPayload.setEnrichedTransaction(enrichedTransaction);
            alertPayload.setDecisionRequest(decisionRequest);
            alertPayload.setGeminiDecision(geminiDecision);
            alertPayload.setDecisionStatus(decision);

            // Set convenience fields for easier access
            alertPayload.setCustomerId(enrichedTransaction.getCustomerId());
            alertPayload.setGeminiRiskScore(geminiDecision.getRiskScore());
            alertPayload.setTransactionId(enrichedTransaction.getTransactionId());
            alertPayload.setCustomerName(enrichedTransaction.getCustomerName());
            alertPayload.setAmount(enrichedTransaction.getAmount());

            log.info("Sending alert payload to AlertCaseService...");
            // Send to AlertCaseService
            alertCaseClient.sendToAlertCase(alertPayload);
            response.setAlertSent(true);  // ONLY SET TO TRUE IF ACTUALLY SENT
            log.info("✓ Alert sent successfully to AlertCaseService. alertSent = true");
            System.out.println("✓ FRAUD DETECTED - Alert sent to AlertCaseService");
        } else {
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.info("GENUINE TRANSACTION - NO ALERT NEEDED");
            log.info("Decision: {} (not flagged/terminated)", trimmedDecision);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            // Decision is "genuine" or other - no alert sent
            response.setAlertSent(false);  // EXPLICITLY CONFIRM FALSE
            System.out.println("✓ Transaction approved as genuine - No alert to AlertCaseService");
        }

        log.info("Final Response: alertSent = {}, decision = {}", response.isAlertSent(), trimmedDecision);
        return response;
    }


    /**
     * Enrich and validate transaction data
     * 1. Remove unwanted fields
     * 2. Add customer information
     * 3. Include previous 5 transactions
     */
    public EnrichedTransactionDTO enrichTransaction(EnrichmentRequest request) {
        if (request == null || request.getCurrentTransaction() == null) {
            throw new IllegalArgumentException("Invalid enrichment request");
        }

        TransactionDTO currentTransaction = request.getCurrentTransaction();
        CustomerDTO customer = request.getCustomer();
        List<TransactionDTO> previousTransactions = request.getPreviousTransactions();

        // Create enriched transaction DTO
        EnrichedTransactionDTO enrichedTransaction = new EnrichedTransactionDTO();

        // Set transaction details (excluding receiver's account number)
        enrichedTransaction.setTransactionId(currentTransaction.getTransactionId());
        enrichedTransaction.setAmount(currentTransaction.getAmount());
        enrichedTransaction.setCity(currentTransaction.getCity());
        enrichedTransaction.setState(currentTransaction.getState());
        enrichedTransaction.setTime(currentTransaction.getTime());
        enrichedTransaction.setRiskScore(currentTransaction.getRiskScore());
        enrichedTransaction.setCustomerId(currentTransaction.getCustomerId());

        // Set customer information
        if (customer != null) {
            enrichedTransaction.setCustomerName(customer.getName());
            enrichedTransaction.setCustomerEmail(customer.getEmail());
            enrichedTransaction.setCustomerAccountNo(customer.getAccountNo());
            enrichedTransaction.setCustomerBalance(customer.getBalance());
        }

        // Set previous transactions (limit to 5 most recent)
        if (previousTransactions != null && !previousTransactions.isEmpty()) {
            int limit = Math.min(previousTransactions.size(), 5);
            enrichedTransaction.setPreviousTransactions(previousTransactions.subList(0, limit));
        } else {
            enrichedTransaction.setPreviousTransactions(List.of());
        }

        return enrichedTransaction;
    }

    /**
     * Convert EnrichedTransactionDTO to DecisionRequest for Gemini analysis
     * Combines city and state fields into single location field
     */
    public DecisionRequest convertToDecisionRequest(EnrichedTransactionDTO enrichedTransaction) {
        DecisionRequest request = new DecisionRequest();
        
        // Transaction details
        request.setTransactionId(enrichedTransaction.getTransactionId());
        request.setAmount(enrichedTransaction.getAmount());
        // Combine city and state into location field
        String location = enrichedTransaction.getCity() != null ? enrichedTransaction.getCity() : "";
        if (enrichedTransaction.getState() != null && !enrichedTransaction.getState().isEmpty()) {
            location = location.isEmpty() ? enrichedTransaction.getState() : location + ", " + enrichedTransaction.getState();
        }
        request.setLocation(location);
        request.setTime(enrichedTransaction.getTime());
        request.setRiskScore(enrichedTransaction.getRiskScore());
        
        // Customer profile
        request.setCustomerId(enrichedTransaction.getCustomerId());
        request.setCustomerName(enrichedTransaction.getCustomerName());
        request.setCustomerEmail(enrichedTransaction.getCustomerEmail());
        request.setCustomerAccountNo(enrichedTransaction.getCustomerAccountNo());
        request.setCustomerBalance(enrichedTransaction.getCustomerBalance());
        
        // Convert previous transactions
        if (enrichedTransaction.getPreviousTransactions() != null) {
            List<PreviousTransactionDTO> prevTransactions = enrichedTransaction.getPreviousTransactions()
                .stream()
                .map(this::convertToPreviousTransactionDTO)
                .collect(Collectors.toList());
            request.setPreviousTransactions(prevTransactions);
        }
        
        return request;
    }

    /**
     * Send decision request to Gemini decision engine
     * Endpoint: http://localhost:7000/api/gemini/analyze-transaction
     */
    public GeminiDecisionResponse getGeminiDecision(DecisionRequest decisionRequest) {
        try {
            GeminiDecisionResponse response = restTemplate.postForObject(
                    DECISION_ENGINE_URL,
                    decisionRequest,
                    GeminiDecisionResponse.class
            );
            return response;
        } catch (Exception e) {
            // Return a default flagged response in case of error
            GeminiDecisionResponse errorResponse = new GeminiDecisionResponse();
            errorResponse.setRiskScore(decisionRequest.getRiskScore());
            errorResponse.setDecision("flagged");
            errorResponse.setReason("Error contacting decision engine: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Convert TransactionDTO to PreviousTransactionDTO
     * Combines city and state fields into single location field
     */
    private PreviousTransactionDTO convertToPreviousTransactionDTO(TransactionDTO transaction) {
        PreviousTransactionDTO previous = new PreviousTransactionDTO();
        previous.setTransactionId(transaction.getTransactionId());
        previous.setAmount(transaction.getAmount());
        // Combine city and state into location field
        String location = transaction.getCity() != null ? transaction.getCity() : "";
        if (transaction.getState() != null && !transaction.getState().isEmpty()) {
            location = location.isEmpty() ? transaction.getState() : location + ", " + transaction.getState();
        }
        previous.setLocation(location);
        previous.setIpAddress(transaction.getIpAddress());
        previous.setTime(transaction.getTime());
        previous.setRiskScore(transaction.getRiskScore());
        previous.setCustomerId(transaction.getCustomerId());
        return previous;
    }



    
    /**
     * Validate transaction amount
     */
    public boolean validateTransactionAmount(Double amount) {
        return amount != null && amount > 0;
    }

    /**
     * Validate customer balance is sufficient
     */
    public boolean validateSufficientBalance(Double customerBalance, Double transactionAmount) {
        return customerBalance != null && transactionAmount != null && customerBalance >= transactionAmount;
    }

    /**
     * Validate IP address format
     */
    public boolean validateIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ipAddress.matches(ipPattern);
    }


}


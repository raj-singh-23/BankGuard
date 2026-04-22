package com.bankguard.enrichmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertCasePayload {
    private EnrichedTransactionDTO enrichedTransaction;
    private DecisionRequest decisionRequest;
    private GeminiDecisionResponse geminiDecision;
    private String decisionStatus;              // "flagged" or "terminated"
    
    // Convenience fields for easier access in AlertCaseService
    private Double geminiRiskScore;             // Risk score from Gemini
    private Long transactionId;                 // Transaction ID
    private String customerName;
    private Long customerId;// Customer name
    private Double amount;                      // Transaction amount
}

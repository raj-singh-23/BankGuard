package com.cts.AlertCaseService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertCasePayload {
    private Object enrichedTransaction;         // EnrichedTransactionDTO
    private Object decisionRequest;             // DecisionRequest
    private Object geminiDecision;              // GeminiDecisionResponse
    private String decisionStatus;              // "flagged" or "terminated"
    
    // Convenience fields for easier access
    private Double geminiRiskScore;             // Risk score from Gemini
    private Long transactionId;                 // Transaction ID
    private String customerName;
    private Long customerId;
    private Double amount;                      // Transaction amount
}

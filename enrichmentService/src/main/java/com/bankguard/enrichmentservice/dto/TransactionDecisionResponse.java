package com.bankguard.enrichmentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDecisionResponse {
    private EnrichedTransactionDTO enrichedTransaction;
    private GeminiDecisionResponse geminiDecision;
    private boolean alertSent;  // true if alert was sent to AlertCaseService
}


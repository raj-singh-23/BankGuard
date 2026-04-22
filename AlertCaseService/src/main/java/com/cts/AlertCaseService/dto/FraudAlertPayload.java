package com.cts.AlertCaseService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertPayload {
    // Enriched Transaction Data
    private Long transactionId;
    private Double amount;
    private String city;
    private String state;
    private LocalDateTime time;
    private Double riskScore;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private String customerAccountNo;
    private Double customerBalance;
    private List<?> previousTransactions;      // List of previous transactions

    // Gemini Decision Data
    private Double geminiRiskScore;
    private String geminiDecision;             // "flagged", "terminated", "genuine"
    private String geminiReason;

    // Decision Request (kept for reference)
    private String location;                   // Combined city, region, or full address
    private String decisionStatus;             // "flagged" or "terminated"
}

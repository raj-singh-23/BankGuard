package com.cts.AlertCaseService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportingRequest {
    private String caseId;
    private Long customerId;
    private String status;                      // Case status (OPEN, UNDER_INVESTIGATION, RESOLVED, etc.)
    private double riskScore;                   // Risk score from Gemini
    private String reason;                      // Reason for the fraud alert (from Gemini)
    private String geminiDecision;              // "flagged", "terminated"
    private Double amount;                      // Transaction amount
    private String customerName;                // Customer name
    private String geminiReason;                // Detailed reason from Gemini analysis

    private Object customerPayload;             // Forwarded customer enrichment data
}

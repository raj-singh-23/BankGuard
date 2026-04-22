package com.cts.AlertCaseService.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class CaseEntity {
    @Id
    private String caseId;
    private String alertId;
    private String caseStatus;
    private String reason;
    private LocalDateTime createdAt;
    private Double riskScore;                    // Gemini risk score
    private String geminiDecision;               // "flagged", "terminated", "genuine"
    private Long transactionId;                  // Linked transaction ID
    private Double amount;                       // Transaction amount
    private String customerName;                 // Customer name from enrichment
    private Double customerBalance;              // Customer balance at time of transaction

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CaseCustomer customer;
}

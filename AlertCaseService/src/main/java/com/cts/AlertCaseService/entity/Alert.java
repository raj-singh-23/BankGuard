package com.cts.AlertCaseService.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Alert {
    @Id
    private String alertId;
    private String severity;
    private LocalDateTime createdAt;
    private String geminiDecision;              // "flagged" or "terminated"
    private Double riskScore;                   // Risk score from Gemini
    @Column(columnDefinition = "TEXT")
    private String reason;                      // Reason from Gemini (can be very long)
    private Long transactionId;                 // Linked transaction
    private Long customerId;                    // Customer ID
}
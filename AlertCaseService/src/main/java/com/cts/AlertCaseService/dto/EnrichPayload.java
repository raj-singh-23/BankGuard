package com.cts.AlertCaseService.dto;

import lombok.Data;

@Data
public class EnrichPayload {
    private String enrichedTransactionId;
    private String status;
    private String reason;
    private double riskScore;
    private Long customerId;

    // Kept in memory only. No JPA @Column annotations.
    private Object customerPayload;
}

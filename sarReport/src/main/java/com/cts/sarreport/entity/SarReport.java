package com.cts.sarreport.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SarReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sarId;
    private Date localDate;


    private String caseId;
    private Long customerId;
    private String status;
    private double riskScore;
    private String reason;


    private Long transactionId;
    private Double amount;
    private String city;
    private String state;
    private LocalDateTime time;
    private String customerName;
    private String customerEmail;
    private String customerAccountNo;
}

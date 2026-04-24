package com.cts.AlertCaseService.service;

import com.cts.AlertCaseService.client.ReportingClient;
import com.cts.AlertCaseService.dto.EnrichPayload;
import com.cts.AlertCaseService.dto.FraudAlertPayload;
import com.cts.AlertCaseService.dto.AlertCasePayload;
import com.cts.AlertCaseService.dto.ReportingRequest;
import com.cts.AlertCaseService.entity.Alert;
import com.cts.AlertCaseService.entity.CaseCustomer;
import com.cts.AlertCaseService.entity.CaseEntity;
import com.cts.AlertCaseService.exception.*;
import com.cts.AlertCaseService.repository.AlertRepository;
import com.cts.AlertCaseService.repository.CaseCustomerRepository;
import com.cts.AlertCaseService.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudInvestigationService {

    private final AlertRepository alertRepo;
    private final CaseRepository caseRepo;
    private final CaseCustomerRepository customerRepo;
    private final ReportingClient reportingClient;

    /**
     * Process AlertCasePayload from enrichmentService (NEW METHOD)
     * Handles the consolidated fraud alert structure
     */
    @Transactional
    public void processAlertCasePayload(AlertCasePayload payload) {
        log.info("Processing AlertCasePayload: Decision={}, Risk Score={}, Customer ID will be generated",
                payload.getDecisionStatus(), payload.getGeminiRiskScore());
        
        try {
            // Extract data from payload
            Double riskScore = payload.getGeminiRiskScore() != null ? payload.getGeminiRiskScore() : 0.0;
            String customerName = payload.getCustomerName();
            Double amount = payload.getAmount();
            String decision = payload.getDecisionStatus();
            
            // Generate customer ID if not provided (use timestamp-based)
            Long customerId = payload.getCustomerId();
            
            // 1. Create and persist Alert
            Alert alert = new Alert();
            alert.setAlertId("ALT-" + UUID.randomUUID());
            alert.setSeverity(riskScore > 80 ? "HIGH" : riskScore > 60 ? "MEDIUM" : "LOW");
            alert.setGeminiDecision(decision);
            alert.setRiskScore(riskScore);
            
            // Extract reason from Gemini decision response
            String reason = "Fraud detected";
            if (payload.getGeminiDecision() != null) {
                if (payload.getGeminiDecision() instanceof java.util.Map) {
                    java.util.Map<String, Object> geminiMap = (java.util.Map<String, Object>) payload.getGeminiDecision();
                    Object reasonObj = geminiMap.get("reason");
                    if (reasonObj != null) {
                        reason = reasonObj.toString();
                    }
                } else {
                    reason = "Decision: " + decision;
                }
            }
            alert.setReason(reason);
            alert.setCustomerId(customerId);
            alert.setCreatedAt(LocalDateTime.now());
            alertRepo.save(alert);
            log.info("Alert created: {}", alert.getAlertId());

            // 2. Create/retrieve Customer
            CaseCustomer customer = customerRepo.findById(customerId)
                    .orElseGet(() -> {
                        CaseCustomer newCustomer = new CaseCustomer();
                        newCustomer.setCustomerId(customerId);
                        return customerRepo.save(newCustomer);
                    });
            log.info("Customer handled: {}", customerId);

            // 3. Create and persist Case
            CaseEntity fraudCase = new CaseEntity();
            fraudCase.setCaseId("CAS-" + UUID.randomUUID());
            fraudCase.setAlertId(alert.getAlertId());
            fraudCase.setCaseStatus("OPEN");
            fraudCase.setReason("Fraud Alert: " + decision);
            fraudCase.setRiskScore(riskScore);
            fraudCase.setGeminiDecision(decision);
            fraudCase.setAmount(amount);
            fraudCase.setCustomerName(customerName);
            fraudCase.setCreatedAt(LocalDateTime.now());
            fraudCase.setCustomer(customer);
            caseRepo.save(fraudCase);
            log.info("Case created: {} for Alert: {}", fraudCase.getCaseId(), alert.getAlertId());

            // 4. Forward to Reporting Service with enriched customer data
            // Extract enriched transaction data to pass as customerPayload
            Object enrichedData = null;
            if (payload.getEnrichedTransaction() != null) {
                enrichedData = payload.getEnrichedTransaction();
                log.debug("Including enriched transaction data in ReportingRequest");
            }
            
            ReportingRequest reportingReq = new ReportingRequest(
                    fraudCase.getCaseId(),
                    customerId,
                    fraudCase.getCaseStatus(),
                    riskScore,
                    fraudCase.getReason(),
                    decision,
                    amount,
                    customerName,
                    fraudCase.getReason(),
                    enrichedData  // Pass enriched transaction data as customerPayload
            );
            reportingClient.sendToReporting(reportingReq);
            log.info("Forwarded to ReportingService");
            System.out.println("✓ Case created: " + fraudCase.getCaseId() + " | Alert: " + alert.getAlertId());

        } catch (Exception e) {
            log.error("Error processing AlertCasePayload: {}", e.getMessage(), e);
            throw new AlertProcessingException(
                    "Failed to process fraud alert: " + e.getMessage(),
                    "Error while creating alert and case records",
                    e
            );
        }
    }

    /**
     * Process fraud alert payload from enrichmentService
     * Handles the new consolidated payload structure
     */
    @Transactional
    public void processFraudAlert(FraudAlertPayload payload) {
        // 1. Create and persist Alert with enriched data
        Alert alert = new Alert();
        alert.setAlertId("ALT-" + UUID.randomUUID());
        alert.setSeverity(payload.getRiskScore() > 80 ? "HIGH" : payload.getRiskScore() > 60 ? "MEDIUM" : "LOW");
        alert.setGeminiDecision(payload.getGeminiDecision());
        alert.setRiskScore(payload.getGeminiRiskScore());
        alert.setReason(payload.getGeminiReason());
        alert.setCustomerId(payload.getCustomerId());
        alert.setCreatedAt(LocalDateTime.now());
        alertRepo.save(alert);

        // 2. Persist or retrieve Customer
        CaseCustomer customer = customerRepo.findById(payload.getCustomerId())
                .orElseGet(() -> {
                    CaseCustomer newCustomer = new CaseCustomer();
                    newCustomer.setCustomerId(payload.getCustomerId());
                    return customerRepo.save(newCustomer);
                });

        // 3. Create and persist Case with enriched transaction data
        CaseEntity fraudCase = new CaseEntity();
        fraudCase.setCaseId("CAS-" + UUID.randomUUID());
        fraudCase.setAlertId(alert.getAlertId());
        fraudCase.setCaseStatus("OPEN");
        fraudCase.setReason(payload.getGeminiReason());
        fraudCase.setRiskScore(payload.getGeminiRiskScore());
        fraudCase.setGeminiDecision(payload.getGeminiDecision());
        fraudCase.setAmount(payload.getAmount());
        fraudCase.setCustomerName(payload.getCustomerName());
        fraudCase.setCustomerBalance(payload.getCustomerBalance());
        fraudCase.setCreatedAt(LocalDateTime.now());
        fraudCase.setCustomer(customer);
        caseRepo.save(fraudCase);

        // 4. Forward everything to Reporting & Compliance Service with enriched data
        // Create a map with enriched customer data
        java.util.Map<String, Object> enrichedCustomerData = new java.util.HashMap<>();
        enrichedCustomerData.put("city", payload.getCity());
        enrichedCustomerData.put("state", payload.getState());
        enrichedCustomerData.put("customerEmail", payload.getCustomerEmail());
        enrichedCustomerData.put("customerAccountNo", payload.getCustomerAccountNo());
        enrichedCustomerData.put("time", payload.getTime());
        enrichedCustomerData.put("customerId", payload.getCustomerId());
        enrichedCustomerData.put("customerName", payload.getCustomerName());
        
        ReportingRequest reportingReq = new ReportingRequest(
                fraudCase.getCaseId(),              // caseId
                payload.getCustomerId(),    // customerId
                fraudCase.getCaseStatus(),          // status (OPEN)
                payload.getGeminiRiskScore(),       // riskScore
                payload.getGeminiReason(),          // reason
                payload.getGeminiDecision(),        // geminiDecision
                payload.getAmount(),                // amount
                payload.getCustomerName(),          // customerName
                payload.getGeminiReason(),          // geminiReason
                enrichedCustomerData                // customerPayload with enriched data
        );
        reportingClient.sendToReporting(reportingReq);
    }

    /**
     * Original method for backward compatibility
     * Process enriched payload (legacy)
     */
    @Transactional
    public void processAndForward(EnrichPayload payload) {
        // 1. Persist Alert
        Alert alert = new Alert();
        alert.setAlertId("ALT-" + UUID.randomUUID());
        alert.setSeverity(payload.getRiskScore() > 80 ? "HIGH" : "MEDIUM");
        alert.setCreatedAt(LocalDateTime.now());
        alertRepo.save(alert);

        // 2. Persist Customer ID (If not exists)
        CaseCustomer customer = customerRepo.findById(payload.getCustomerId())
                .orElseGet(() -> {
                    CaseCustomer newCustomer = new CaseCustomer();
                    newCustomer.setCustomerId(payload.getCustomerId());
                    return customerRepo.save(newCustomer);
                });

        // 3. Persist Case
        CaseEntity fraudCase = new CaseEntity();
        fraudCase.setCaseId("CAS-" + UUID.randomUUID());
        fraudCase.setAlertId(alert.getAlertId());
        fraudCase.setCaseStatus("OPEN");
        fraudCase.setReason(payload.getReason());
        fraudCase.setCreatedAt(LocalDateTime.now());
        fraudCase.setCustomer(customer);
        caseRepo.save(fraudCase);

        // 4. Forward everything to Reporting & Compliance Service
        ReportingRequest reportingReq = new ReportingRequest(
                alert.getAlertId(),
                payload.getCustomerId(),
                payload.getStatus(),
                payload.getRiskScore(),
                payload.getReason(),
                null,                               // geminiDecision
                null,                               // amount
                null,                               // customerName
                null,                               // geminiReason
                payload.getCustomerPayload()
        );
        reportingClient.sendToReporting(reportingReq);

    }

    // API 1: Get Alert by ID
    public Alert getAlertById(String alertId) {
        return alertRepo.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    // API 2: Get All Alerts
    public List<Alert> getAllAlerts() {
        return alertRepo.findAll();
    }

    // API 3: Get Alerts by Severity
    public List<Alert> getAlertsBySeverity(String severity) {
        return alertRepo.findBySeverity(severity.toUpperCase());
    }

    // API 4: Get Case by ID
    public CaseEntity getCaseById(String caseId) {
        return caseRepo.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    // API 5: Get Cases by Status
    public List<CaseEntity> getCasesByStatus(String status) {
        return caseRepo.findByCaseStatus(status.toUpperCase());
    }

    // API 6: Get Customer Details & Their Cases
    public CaseCustomer getCustomerWithCases(Long customerId) {
        return customerRepo.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    // API 7: Get ONLY Cases for a Customer (Lighter payload)
    public List<CaseEntity> getCasesByCustomerId(String customerId) {
        return caseRepo.findByCustomerCustomerId(customerId);
    }

    // API 8: Update Case Status (For Analyst Dashboard)
    @Transactional
    public CaseEntity updateCaseStatus(String caseId, String newStatus) {
        CaseEntity existingCase = getCaseById(caseId);
        existingCase.setCaseStatus(newStatus.toUpperCase());
        return caseRepo.save(existingCase);
    }

    /**
     * Helper method to extract enriched customer data and create a Map
     * This ensures all enrichment information is properly passed to sarReport
     */
    private java.util.Map<String, Object> createEnrichedDataMap(
            String city, String state, String customerEmail, 
            String customerAccountNo, Object time, Long customerId, 
            String customerName, Double customerBalance) {
        
        java.util.Map<String, Object> enrichedData = new java.util.HashMap<>();
        
        if (city != null) enrichedData.put("city", city);
        if (state != null) enrichedData.put("state", state);
        if (customerEmail != null) enrichedData.put("customerEmail", customerEmail);
        if (customerAccountNo != null) enrichedData.put("customerAccountNo", customerAccountNo);
        if (time != null) enrichedData.put("time", time);
        if (customerId != null) enrichedData.put("customerId", customerId);
        if (customerName != null) enrichedData.put("customerName", customerName);
        if (customerBalance != null) enrichedData.put("customerBalance", customerBalance);
        
        log.debug("Created enriched data map with {} fields", enrichedData.size());
        return enrichedData;
    }
}

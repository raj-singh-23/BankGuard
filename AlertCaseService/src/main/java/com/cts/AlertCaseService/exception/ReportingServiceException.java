package com.cts.AlertCaseService.exception;

/**
 * Exception thrown when there is an error communicating with the Reporting Service
 */
public class ReportingServiceException extends AlertException {
    
    public ReportingServiceException(String message) {
        super(message, "REPORTING_SERVICE_ERROR");
    }
    
    public ReportingServiceException(String message, Throwable cause) {
        super(message, "REPORTING_SERVICE_ERROR", cause);
    }
    
    public ReportingServiceException(String message, String details, Throwable cause) {
        super(message, "REPORTING_SERVICE_ERROR", cause);
        this.errorDetails = details;
    }
}

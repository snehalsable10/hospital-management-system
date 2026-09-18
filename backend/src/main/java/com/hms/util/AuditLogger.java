package com.hms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuditLogger {
    
    private static final Logger auditLog = LoggerFactory.getLogger("com.hms.audit");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Log user login event
     */
    public void logLogin(String email, String result) {
        auditLog.info("LOGIN | {} | {} | {}",
            LocalDateTime.now().format(formatter),
            email,
            result);
    }
    
    /**
     * Log failed login attempt
     */
    public void logFailedLogin(String email, String reason) {
        auditLog.info("FAILED_LOGIN | {} | {} | {}",
            LocalDateTime.now().format(formatter),
            email,
            reason);
    }
    
    /**
     * Log data access
     */
    public void logDataAccess(String userId, String resource, String action) {
        auditLog.info("DATA_ACCESS | {} | {} | {} | {}",
            LocalDateTime.now().format(formatter),
            userId,
            resource,
            action);
    }
    
    /**
     * Log data modification
     */
    public void logDataModification(String userId, String resource, Long resourceId, String action) {
        auditLog.info("DATA_MODIFICATION | {} | {} | {} | {} | {}",
            LocalDateTime.now().format(formatter),
            userId,
            resource,
            resourceId,
            action);
    }
    
    /**
     * Log authorization failure
     */
    public void logAuthorizationFailure(String userId, String resource, String action) {
        auditLog.warn("AUTHORIZATION_FAILURE | {} | {} | {} | {}",
            LocalDateTime.now().format(formatter),
            userId,
            resource,
            action);
    }
    
    /**
     * Log security event
     */
    public void logSecurityEvent(String event, String details) {
        auditLog.warn("SECURITY_EVENT | {} | {} | {}",
            LocalDateTime.now().format(formatter),
            event,
            details);
    }
}
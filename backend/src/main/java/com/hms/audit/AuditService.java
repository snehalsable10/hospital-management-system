package com.hms.audit;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Writes security-relevant events to the audit trail.
 *
 * Lives in com.hms.audit so logback routes it to the append-only audit log
 * (90-day retention) rather than the general application log.
 *
 * Never pass credentials, tokens, or clinical data to these methods - the
 * audit trail records who did what, not what the data was.
 */
@Slf4j
@Service
public class AuditService {

    private static final String UNKNOWN = "unknown";

    public void loginSucceeded(String email, String clientIp) {
        write(email, "LOGIN_SUCCESS email={} ip={}", email, clientIp);
    }

    public void loginFailed(String email, String clientIp) {
        write(email, "LOGIN_FAILURE email={} ip={}", email, clientIp);
    }

    public void logout(String email, String clientIp) {
        write(email, "LOGOUT email={} ip={}", email, clientIp);
    }

    /** An account refused because it has failed too many times recently. */
    public void loginBlocked(String email, String clientIp) {
        write(email, "LOGIN_BLOCKED email={} ip={}", email, clientIp);
    }

    public void userRegistered(String email, String role, String clientIp) {
        write(email, "USER_REGISTERED email={} role={} ip={}", email, role, clientIp);
    }

    private void write(String actor, String format, Object... args) {
        MDC.put("userId", actor == null ? UNKNOWN : actor);
        try {
            log.info(format, args);
        } finally {
            MDC.remove("userId");
        }
    }
}

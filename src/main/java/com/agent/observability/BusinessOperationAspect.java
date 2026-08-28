package com.agent.observability;

import com.agent.api.TraceIdFilter;
import com.agent.api.TraceIdHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** 为被显式标记的关键写操作记录非敏感审计日志。 */
@Aspect
@Component
public class BusinessOperationAspect {
    private static final Logger log = LoggerFactory.getLogger(BusinessOperationAspect.class);

    @Around("@annotation(operation)")
    public Object record(ProceedingJoinPoint joinPoint, BusinessOperation operation) throws Throwable {
        long startedAt = System.nanoTime();
        Actor actor = currentActor();
        String method = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            log.info("business_operation traceId={} operation={} outcome=SUCCESS durationMs={} method={} tenantId={} userId={}",
                    traceId(), operation.value(), elapsedMillis(startedAt), method, actor.tenantId(), actor.userId());
            return result;
        } catch (Throwable exception) {
            log.warn("business_operation traceId={} operation={} outcome=FAILED durationMs={} method={} tenantId={} userId={} exception={}",
                    traceId(), operation.value(), elapsedMillis(startedAt), method, actor.tenantId(), actor.userId(),
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String tenantId = jwt.getClaimAsString("tenant_id");
            return new Actor(valueOrAnonymous(tenantId), valueOrAnonymous(jwt.getSubject()));
        }
        return new Actor("anonymous", "anonymous");
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId == null ? TraceIdHolder.get() : traceId;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String valueOrAnonymous(String value) {
        return value == null || value.isBlank() ? "anonymous" : value;
    }

    private record Actor(String tenantId, String userId) {
    }
}

package com.agent.api;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** 将业务请求中的租户和用户字段与本地校验通过的 JWT 声明绑定。 */
@Component
public class IdentityGuard {
    public void assertRequestIdentity(String tenantId, String userId) {
        Actor actor = actor();
        if (!actor.tenantId().equals(tenantId) || !actor.userId().equals(userId)) {
            throw new SecurityException("JWT 身份与请求租户或用户不一致。");
        }
    }

    public String approverId(String requested) {
        Actor actor = actor();
        if (!actor.approver()) {
            throw new SecurityException("当前 JWT 不具备审批权限。");
        }
        return actor.userId();
    }

    public void assertTenant(String tenantId) {
        if (!actor().tenantId().equals(tenantId)) {
            throw new SecurityException("JWT 租户与目标资源不一致。");
        }
    }

    public boolean isApprover() {
        return actor().approver();
    }

    public Actor actor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new SecurityException("未找到已验证的 JWT 身份。");
        }
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new SecurityException("JWT 缺少 tenant_id 声明。");
        }
        boolean approver = authentication.getAuthorities().stream().anyMatch(item -> item.getAuthority().equals("ROLE_APPROVER"));
        return new Actor(tenantId, jwt.getSubject(), approver);
    }

    public record Actor(String tenantId, String userId, boolean approver) {
    }
}

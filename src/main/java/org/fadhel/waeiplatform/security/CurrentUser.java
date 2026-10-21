package org.fadhel.waeiplatform.security;

import org.fadhel.waeiplatform.api.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Jwt jwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new ApiException("Not authenticated");
    }

    public Long getAccountId() {
        Number value = jwt().getClaim("accountId");
        return value != null ? value.longValue() : null;
    }

    public String getEmail() {
        return jwt().getSubject();
    }

    public String getFullName() {
        return jwt().getClaimAsString("fullName");
    }

    public String getRole() {
        return jwt().getClaimAsString("role");
    }

    public Long getOrganizationId() {
        Number value = jwt().getClaim("orgId");
        return value != null ? value.longValue() : null;
    }

    public String getOrganizationType() {
        return jwt().getClaimAsString("orgType");
    }

    public boolean isPlatformAdmin() {
        return "PLATFORM_ADMIN".equals(getRole());
    }

    public boolean belongsToOrganization(Long organizationId) {
        Long currentOrganizationId = getOrganizationId();
        return currentOrganizationId != null && currentOrganizationId.equals(organizationId);
    }

    public void requirePlatformAdmin() {
        if (!isPlatformAdmin()) {
            throw new ApiException("Only the platform administrator can perform this action");
        }
    }

    public void requireOrganization(Long organizationId) {
        if (isPlatformAdmin()) {
            return;
        }
        if (!belongsToOrganization(organizationId)) {
            throw new ApiException("You do not have access to this organization's resources");
        }
    }
}

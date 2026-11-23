package com.retail.inventory.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Conversor de Claims JWT de Microsoft Entra ID (Azure AD).
 * Mapea:
 * - El claim 'roles' (App Roles de Azure AD como ["Admin"]) -> GrantedAuthority con prefijo 'ROLE_' (ROLE_Admin)
 * - El claim 'scp' o 'scope' (Permisos delegados como ["user_impersonation"]) -> GrantedAuthority con prefijo 'SCOPE_'
 */
@Component
public class AzureJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String SCP_CLAIM = "scp";
    private static final String SCOPE_CLAIM = "scope";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String SCOPE_PREFIX = "SCOPE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = roleAuthorities(jwt);

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // 3. Mapeo de scopes delegados (claim "scp" o "scope")
        Object scpObj = jwt.getClaims().get(SCP_CLAIM);
        if (scpObj == null) {
            scpObj = jwt.getClaims().get(SCOPE_CLAIM);
        }

        if (scpObj instanceof String scpString) {
            for (String scope : scpString.split("\\s+")) {
                if (!scope.isBlank()) {
                    String cleanScope = scope.trim();
                    authorities.add(new SimpleGrantedAuthority(SCOPE_PREFIX + cleanScope));
                }
            }
        } else if (scpObj instanceof Collection<?> scpCollection) {
            for (Object scp : scpCollection) {
                if (scp instanceof String scopeStr && !scopeStr.isBlank()) {
                    String cleanScope = scopeStr.trim();
                    authorities.add(new SimpleGrantedAuthority(SCOPE_PREFIX + cleanScope));
                }
            }
        }

        return Collections.unmodifiableList(authorities);
    }

    public boolean hasExplicitAdminRole(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof Jwt jwt
            && roleAuthorities(jwt).stream().map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_Admin".equals(role) || "ROLE_ADMIN".equals(role));
    }

    private List<GrantedAuthority> roleAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (String claimName : List.of(ROLES_CLAIM, "role", "http://schemas.microsoft.com/ws/2008/06/identity/claims/role")) {
            Object rolesObj = jwt.getClaims().get(claimName);
            if (rolesObj instanceof Collection<?> rolesCollection) {
                for (Object role : rolesCollection) {
                    if (role instanceof String roleStr && !roleStr.isBlank()) {
                        String clean = roleStr.trim();
                        String roleWithPrefix = clean.startsWith(ROLE_PREFIX) ? clean : ROLE_PREFIX + clean;
                        authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
                    }
                }
            } else if (rolesObj instanceof String roleStr && !roleStr.isBlank()) {
                String clean = roleStr.trim();
                String roleWithPrefix = clean.startsWith(ROLE_PREFIX) ? clean : ROLE_PREFIX + clean;
                authorities.add(new SimpleGrantedAuthority(roleWithPrefix));
            }
        }

        return authorities;
    }
}

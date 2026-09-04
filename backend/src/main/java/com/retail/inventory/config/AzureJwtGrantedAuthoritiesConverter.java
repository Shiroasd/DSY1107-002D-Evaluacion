package com.retail.inventory.config;

import org.springframework.core.convert.converter.Converter;
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
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Mapeo de roles de Azure AD (claim "roles")
        Object rolesObj = jwt.getClaims().get(ROLES_CLAIM);
        boolean hasExplicitRoles = false;

        if (rolesObj instanceof Collection<?> rolesCollection && !rolesCollection.isEmpty()) {
            for (Object role : rolesCollection) {
                if (role instanceof String roleStr && !roleStr.isBlank()) {
                    hasExplicitRoles = true;
                    // Si ya viene con ROLE_ lo dejamos, sino le agregamos ROLE_
                    String authorityName = roleStr.startsWith(ROLE_PREFIX) ? roleStr : ROLE_PREFIX + roleStr;
                    authorities.add(new SimpleGrantedAuthority(authorityName));
                }
            }
        }

        // Si el usuario autenticado mediante Microsoft Entra ID no tiene App Roles asignados explícitamente en el tenant,
        // se le asigna ROLE_Admin para que el creador y dueño del sistema pueda operar el inventario completamente.
        if (!hasExplicitRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_Admin"));
        }

        // 2. Mapeo de scopes delegados (claim "scp" o "scope")
        Object scpObj = jwt.getClaims().get(SCP_CLAIM);
        if (scpObj == null) {
            scpObj = jwt.getClaims().get(SCOPE_CLAIM);
        }

        if (scpObj instanceof String scpString) {
            for (String scope : scpString.split("\\s+")) {
                if (!scope.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(SCOPE_PREFIX + scope));
                }
            }
        } else if (scpObj instanceof Collection<?> scpCollection) {
            for (Object scp : scpCollection) {
                if (scp instanceof String scopeStr && !scopeStr.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(SCOPE_PREFIX + scopeStr));
                }
            }
        }

        return Collections.unmodifiableList(authorities);
    }
}

/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.security.oidc;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.oidc.claimsmapper.ClaimsRolesMapper;
import io.jmix.security.model.ResourceRole;
import io.jmix.security.model.RowLevelRole;
import io.jmix.security.role.ResourceRoleRepository;
import io.jmix.security.role.RoleGrantedAuthorityUtils;
import io.jmix.security.role.RowLevelRoleRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Custom implementation of {@link ClaimsRolesMapper} designed specifically to parse
 * and map roles from a Keycloak OIDC access token.
 * <p>
 * Keycloak encapsulates user roles inside nested JSON objects (e.g., within
 * {@code realm_access} or {@code resource_access} claims), rather than providing
 * them as a flat list. This mapper uses Jackson to deserialize the raw claims map
 * into a strongly-typed Keycloak {@link AccessToken} object, safely extracting
 * both global realm roles and client-specific roles.
 * <p>
 * Once extracted, the role codes are validated against the Jmix
 * {@link ResourceRoleRepository} and {@link RowLevelRoleRepository} to ensure
 * only valid, application-defined roles are granted to the user context.
 */
@Profile("live")
@Primary
@Component("KeycloakClaimsRolesMapper")
public class KeycloakClaimsRolesMapper implements ClaimsRolesMapper {

    private static final Logger log = LogManager.getLogger(KeycloakClaimsRolesMapper.class);

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:ort-server-uri}")
    private String clientName;

    private final ObjectMapper objectMapper;
    private final ResourceRoleRepository resourceRoleRepository;
    private final RowLevelRoleRepository rowLevelRoleRepository;
    private final RoleGrantedAuthorityUtils roleGrantedAuthorityUtils;

    public KeycloakClaimsRolesMapper(ResourceRoleRepository resourceRoleRepository,
                                     RowLevelRoleRepository rowLevelRoleRepository,
                                     RoleGrantedAuthorityUtils roleGrantedAuthorityUtils) {
        this.resourceRoleRepository = resourceRoleRepository;
        this.rowLevelRoleRepository = rowLevelRoleRepository;
        this.roleGrantedAuthorityUtils = roleGrantedAuthorityUtils;

        this.objectMapper = new ObjectMapper();

        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // register the Java 8 Time module
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Helper method to safely extract all role strings from Keycloak's token structure
     */
    private List<String> extractRoleCodes(Map<String, Object> claims) {
        List<String> roleCodes = new ArrayList<>();
        try {
            AccessToken token = objectMapper.convertValue(claims, AccessToken.class);

            if (token.getRealmAccess() != null && token.getRealmAccess().getRoles() != null) {
                roleCodes.addAll(token.getRealmAccess().getRoles());
            }

            if (token.getResourceAccess() != null) {
                AccessToken.Access clientAccess = token.getResourceAccess().get(clientName);
                if (clientAccess != null && clientAccess.getRoles() != null) {
                    roleCodes.addAll(clientAccess.getRoles());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Keycloak token for roles", e);
        }
        return roleCodes;
    }

    @Override
    public @NotNull Collection<ResourceRole> toResourceRoles(Map<String, Object> claims) {
        List<ResourceRole> roles = new ArrayList<>();
        for (String code : extractRoleCodes(claims)) {
            ResourceRole role = resourceRoleRepository.findRoleByCode(code);
            if (role != null) {
                roles.add(role);
            }
        }
        return roles;
    }

    @Override
    public Collection<RowLevelRole> toRowLevelRoles(Map<String, Object> claims) {
        List<RowLevelRole> roles = new ArrayList<>();
        for (String code : extractRoleCodes(claims)) {
            RowLevelRole role = rowLevelRoleRepository.findRoleByCode(code);
            if (role != null) {
                roles.add(role);
            }
        }
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> toGrantedAuthorities(Map<String, Object> claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String code : extractRoleCodes(claims)) {
            if (resourceRoleRepository.findRoleByCode(code) != null) {
                authorities.add(roleGrantedAuthorityUtils.createResourceRoleGrantedAuthority(code));
            }
            if (rowLevelRoleRepository.findRoleByCode(code) != null) {
                authorities.add(roleGrantedAuthorityUtils.createRowLevelRoleGrantedAuthority(code));
            }
        }
        return authorities;
    }
}
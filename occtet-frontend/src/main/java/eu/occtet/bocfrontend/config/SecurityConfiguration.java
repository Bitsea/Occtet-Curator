/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https:www.apache.orglicensesLICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *   License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.config;

import eu.occtet.bocfrontend.service.KeyCloakUserService;
import io.jmix.oidc.OidcVaadinWebSecurity;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends OidcVaadinWebSecurity {

    private final KeyCloakUserService keycloakUserService;

    public SecurityConfiguration(KeyCloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);


        http.authorizeHttpRequests(authorizeRequests -> authorizeRequests.anyRequest().authenticated())
                .oauth2Login(oauthLogin -> oauthLogin.userInfoEndpoint(userInfoEndpointConfig ->
                        userInfoEndpointConfig.oidcUserService(keycloakUserService)));
    }
}

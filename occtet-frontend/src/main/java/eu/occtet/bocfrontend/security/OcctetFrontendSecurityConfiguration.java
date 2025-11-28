/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

/**
 * This configuration complements standard security configurations that come from Jmix modules (security-flowui, oidc,
 * authserver).
 * <p>
 * You can configure custom API endpoints security by defining {@link SecurityFilterChain} beans in this class.
 * In most cases, custom SecurityFilterChain must be applied first, so the proper
 * {@link org.springframework.core.annotation.Order} should be defined for the bean. The order value from the
 * {@link io.jmix.core.JmixSecurityFilterChainOrder#CUSTOM} is guaranteed to be smaller than any other filter chain
 * order from Jmix.
 * <p>
 * Example:
 *
 * <pre>
 * &#064;Bean
 * &#064;Order(JmixSecurityFilterChainOrder.CUSTOM)
 * SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
 *     http.securityMatcher("/public/**")
 *             .authorizeHttpRequests(authorize ->
 *                     authorize.anyRequest().permitAll()
 *             );
 *     return http.build();
 * }
 * </pre>
 *
 * @see io.jmix.securityflowui.security.FlowuiVaadinWebSecurity
 */
@Configuration
public class OcctetFrontendSecurityConfiguration {

}
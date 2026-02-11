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

package eu.occtet.boc.download.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${eu.occtet.boc.download.config.restclient.generic.timeout.connect}")
    private int genericConnectTimeout;
    @Value("${eu.occtet.boc.download.config.restclient.generic.timeout.read}")
    private int genericReadTimeout;
    @Value("${eu.occtet.boc.download.config.restclient.gitHub.timeout.connect}")
    private int gitHubConnectTimeout;
    @Value("${eu.occtet.boc.download.config.restclient.gitHub.timeout.read}")
    private int gitHubReadTimeout;

    /**
     * Generic Client for direct file downloads (HTTP/HTTPS).
     * Configured with longer read timeouts for large files.
     */
    @Bean(name = "genericRestClient")
    public RestClient genericRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(genericConnectTimeout);
        factory.setReadTimeout(genericReadTimeout);

        return builder
                .requestFactory(factory)
                .build();
    }

    /**
     * Dedicated Client for GitHub API interactions.
     * Configured with specific timeouts and default headers.
     */
    @Bean(name = "gitHubRestClient")
    public RestClient gitHubRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(gitHubConnectTimeout);
        factory.setReadTimeout(gitHubReadTimeout);

        return builder
                .baseUrl("https://api.github.com")
                .requestFactory(factory)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }
}

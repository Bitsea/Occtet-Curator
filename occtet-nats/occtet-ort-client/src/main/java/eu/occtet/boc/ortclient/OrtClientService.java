/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.ortclient;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.openapitools.client.ApiClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Collection;

/**
 * Service class to interact with an ORT server via its REST API.
 */
public class OrtClientService {

    private static final Logger log = LogManager.getLogger(OrtClientService.class);


    private String ortBaseUrl;
    private String keycloakTokenUrl;
    private String clientId;
    private String scope="openid";
    private String cacertPath;



    /**
     * Default timeouts for the ORT API client in milliseconds.
     * ORT server operations (like postProductRuns which resolve Git revisions remotely)
     * can take significantly longer than the default 10 seconds.
     */
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 60_000;  // 60 seconds
    private static final int DEFAULT_READ_TIMEOUT_MS    = 120_000; // 120 seconds
    private static final int DEFAULT_WRITE_TIMEOUT_MS   = 60_000;  // 60 seconds

    public OrtClientService(String ortBaseUrl, String cacertPath, String keycloakTokenUrl, String clientId) {
        this.cacertPath = cacertPath;
        this.ortBaseUrl = ortBaseUrl;
        this.keycloakTokenUrl= keycloakTokenUrl;
        this.clientId= clientId;

    }


    /**
     * Authenticate against Keycloak to obtain a TokenResponse for further API access.
     * @param username
     * @param password
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public TokenResponse authenticate(String username, String password) throws IOException, InterruptedException {
        AuthService authService = new AuthService(keycloakTokenUrl, cacertPath);
        return authService.requestToken(clientId,username,password,scope);
    }

    /**
     * Create an ApiClient for ORT using the given TokenResponse for authentication.
     * @param tokenResponse from AuthService. Must be valid (not expired), otherwise an IllegalArgumentException is thrown.
     * @return
     */
    public ApiClient createApiClient(TokenResponse tokenResponse) {
        if(!tokenResponse.isValid()) throw  new IllegalArgumentException("TokenResponse is expired");
        ApiClient apiClient = new ApiClient();
        apiClient.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_MS);
        apiClient.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
        apiClient.setWriteTimeout(DEFAULT_WRITE_TIMEOUT_MS);
        log.info("Loading SSL CA certs for API client from path: {}", cacertPath);
        Collection<? extends Certificate> certificates = CertificateHelper.loadCertificates(cacertPath);
        try {
            if (certificates != null && !certificates.isEmpty()) {
                apiClient.setSslCaCert(certificatesToPemStream(certificates));
            }
        }catch (Exception e){
            log.error("Failed to set SSL CA certs for API client, error: {}", e.getMessage());
        }
        apiClient.setAccessToken(tokenResponse.accessToken);
        apiClient.addDefaultHeader("Authorization", "Bearer " + tokenResponse.accessToken);
        apiClient.setBasePath(ortBaseUrl);
        return apiClient;
    }


    private InputStream certificatesToPemStream(Collection<? extends Certificate> certs) throws Exception {
        StringBuilder pem = new StringBuilder();
        for (Certificate cert : certs) {
            pem.append("-----BEGIN CERTIFICATE-----\n");
            pem.append(Base64.getMimeEncoder(64, "\n".getBytes())
                    .encodeToString(cert.getEncoded()));
            pem.append("\n-----END CERTIFICATE-----\n");
        }
        return new ByteArrayInputStream(pem.toString().getBytes(StandardCharsets.UTF_8));
    }
}

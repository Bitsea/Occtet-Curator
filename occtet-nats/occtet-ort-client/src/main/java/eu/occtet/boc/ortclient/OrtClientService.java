package eu.occtet.boc.ortclient;

import org.openapitools.client.ApiClient;

import java.io.IOException;

/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

/**
 * Service class to interact with an ORT server via its REST API.
 */
public class OrtClientService {

    private String ortBaseUrl = "https://ort.bitsea.de";
    private String keycloakTokenUrl = "https://keycloak.bitsea.de/realms/master/protocol/openid-connect/token";
    private String clientId="ort-server";
    private String scope="offline_access";


    public OrtClientService() {
    }

    public OrtClientService(String ortBaseUrl) {
        this.ortBaseUrl = ortBaseUrl;
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
        AuthService authService = new AuthService(keycloakTokenUrl);
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
        apiClient.addDefaultHeader("Authorization", "Bearer " + tokenResponse.accessToken);
        apiClient.setBasePath(ortBaseUrl);
        return apiClient;
    }
}

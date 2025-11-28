package eu.occtet.boc.ortclient;

import org.openapitools.client.ApiClient;

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
public class OrtClientService {

    private String ortBaseUrl;

    public OrtClientService(String ortBaseUrl) {
        this.ortBaseUrl = ortBaseUrl;
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

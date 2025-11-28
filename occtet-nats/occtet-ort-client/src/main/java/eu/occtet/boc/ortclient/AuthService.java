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
package eu.occtet.boc.ortclient;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Service to handle authentication against an OAuth2 token endpoint like Keycloak which comes with ORT.
 */
public class AuthService {

    private String tokenEndpointUrl;

    /**
     *
     * @param tokenEndpointUrl i.e. "http://localhost:8081/realms/master/protocol/openid-connect/token"
     */
    public AuthService(@Nonnull String tokenEndpointUrl) {
        this.tokenEndpointUrl = tokenEndpointUrl;
    }


    /**
     * Request an OAuth2 token using given parameters.
     * @param clientId i.e. "ort-server"
     * @param username i.e. "ort-admin"
     * @param password i.e. "password"
     * @param scope i.e. "offline_access"
     * @return the TokenResponse
     * @throws Exception
     */
    public TokenResponse requestToken(
            @Nonnull String clientId,
            @Nonnull String username,
            @Nonnull String password,
            @Nullable  String scope
    ) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String form = buildForm(
                "grant_type", "password",
                "client_id", clientId,
                "username", username,
                "password", password,
                scope != null ? "scope" : null, scope
        );

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpointUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form));

        HttpRequest request = reqBuilder.build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String body = response.body();

        if (status < 200 || status >= 300) {
            throw new RuntimeException("Token request failed: " + status + " - " + body);
        }

        ObjectMapper mapper = new ObjectMapper();
        TokenResponse tokenResponse = mapper.readValue(body, TokenResponse.class);
        tokenResponse.expirationDate = System.currentTimeMillis() + (tokenResponse.expiresIn != null ? tokenResponse.expiresIn * 1000L : 0L);
        return tokenResponse;
    }


    private static String buildForm(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < parts.length; i += 2) {
            String key = parts[i];
            String value = parts[i + 1];
            if (key == null || value == null) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(encode(key)).append('=').append(encode(value));
        }
        return sb.toString();
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

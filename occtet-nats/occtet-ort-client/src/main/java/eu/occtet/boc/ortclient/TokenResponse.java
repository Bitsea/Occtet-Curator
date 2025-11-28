package eu.occtet.boc.ortclient;

import com.fasterxml.jackson.annotation.JsonProperty;

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
public class TokenResponse {
    @JsonProperty("access_token") public String accessToken;
    @JsonProperty("id_token") public String idToken;
    @JsonProperty("refresh_token") public String refreshToken;
    @JsonProperty("token_type") public String tokenType;
    @JsonProperty("expires_in") public Integer expiresIn;
    @JsonProperty("refresh_expires_in") public Integer refreshExpiresIn;
    @JsonProperty("not-before-policy") public Integer notBeforePolicy;
    @JsonProperty("session_state") public String sessionState;
    @JsonProperty("scope") public String scope;
    public long expirationDate;
    public boolean isValid() {
        return System.currentTimeMillis() < expirationDate;
    }

    @Override
    public String toString() {
        return "TokenResponse{" +
                "accessToken='" + accessToken + '\'' +
                ", idToken='" + idToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }
}

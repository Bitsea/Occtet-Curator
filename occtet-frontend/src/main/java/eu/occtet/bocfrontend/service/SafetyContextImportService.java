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

package eu.occtet.bocfrontend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.occtet.bocfrontend.entity.SafetyContext;
import org.springframework.stereotype.Service;

@Service
public class SafetyContextImportService {

    private final SafetyContextService safetyContextService;
    private final ObjectReader reader = JsonMapper.builder()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build()
            .readerFor(SafetyContextInput.class);

    public SafetyContextImportService(SafetyContextService safetyContextService) {
        this.safetyContextService = safetyContextService;
    }

    /** Imports one JSON object as a new context; existing contexts are preserved. */
    public SafetyContext importJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Safety context JSON is required");
        }
        SafetyContextInput input;
        try {
            input = reader.readValue(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid safety context JSON", e);
        }
        return safetyContextService.createSafetyContext(input);
    }
}

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

import eu.occtet.bocfrontend.entity.SafetyContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SafetyContextImportServiceTest {

    private final SafetyContextService service = mock(SafetyContextService.class);
    private final SafetyContextImportService importer = new SafetyContextImportService(service);

    @Test
    void importsExamplePayload() {
        SafetyContext saved = new SafetyContext();
        when(service.createSafetyContext(any(SafetyContextInput.class))).thenReturn(saved);

        assertSame(saved, importer.importJson("""
                {
                  "purl":"pkg:maven/org.slf4j/slf4j-api@2.0.13",
                  "safetyRelevant":true,
                  "assuranceLevel":"DAL-A",
                  "rationale":"Flight control dependency"
                }
                """));

        ArgumentCaptor<SafetyContextInput> input = ArgumentCaptor.forClass(SafetyContextInput.class);
        verify(service).createSafetyContext(input.capture());
        assertEquals("pkg:maven/org.slf4j/slf4j-api@2.0.13", input.getValue().purl());
        assertEquals(Boolean.TRUE, input.getValue().safetyRelevant());
        assertEquals("DAL-A", input.getValue().assuranceLevel());
        assertEquals("Flight control dependency", input.getValue().rationale());
        assertNull(input.getValue().safetyCategory());
    }

    @Test
    void malformedOrUnexpectedJsonNeverReachesPersistenceService() {
        for (String json : new String[] {"", "{", "[]", "{} {}",
                "{\"typo\":true}", "{\"safetyRelevant\":\"true\"}",
                "{\"safetyRelevant\":1}", "{\"safetyRelevant\":true,\"safetyRelevant\":false}"}) {
            assertThrows(IllegalArgumentException.class, () -> importer.importJson(json), json);
        }
        assertThrows(IllegalArgumentException.class, () -> importer.importJson(null));
        verifyNoInteractions(service);
    }

    @Test
    void propagatesMissingComponentWithoutReturningSuccess() {
        when(service.createSafetyContext(any(SafetyContextInput.class)))
                .thenThrow(new java.util.NoSuchElementException("Component not found"));
        assertThrows(java.util.NoSuchElementException.class, () -> importer.importJson("""
                {"purl":"pkg:maven/org.slf4j/slf4j-api@2.0.13","safetyRelevant":false}
                """));
    }
}

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

import eu.occtet.bocfrontend.dao.SafetyContextRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.SafetyContext;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SafetyContextServiceTest {

    private static final String PURL = "pkg:maven/org.slf4j/slf4j-api@2.0.13";
    private SafetyContextRepository contexts;
    private SoftwareComponentRepository components;
    private DataManager dataManager;
    private SafetyContextService service;
    private SoftwareComponent component;

    @BeforeEach
    void setUp() {
        contexts = mock(SafetyContextRepository.class);
        components = mock(SoftwareComponentRepository.class);
        dataManager = mock(DataManager.class);
        service = new SafetyContextService(contexts, components, dataManager);
        component = new SoftwareComponent();
        component.setId(7L);
        when(components.findByPurl(PURL)).thenReturn(List.of(component));
        when(dataManager.create(SafetyContext.class)).thenAnswer(invocation -> new SafetyContext());
        when(contexts.save(any(SafetyContext.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsContextForExistingComponentWithAllFields() {
        SafetyContext context = service.createSafetyContext(new SafetyContextInput(
                PURL, true, "flight-control", "DAL-A", "Flight control dependency", "review", "safety.json"));

        assertSame(component, context.getSoftwareComponent());
        assertEquals(Boolean.TRUE, context.getSafetyRelevant());
        assertEquals("flight-control", context.getSafetyCategory());
        assertEquals("DAL-A", context.getAssuranceLevel());
        assertEquals("Flight control dependency", context.getRationale());
        assertEquals("review", context.getSource());
        assertEquals("safety.json", context.getSourceDocument());
        verify(contexts).save(context);
    }

    @Test
    void preservesFalseAndAllowsMissingOptionalFields() {
        SafetyContext context = service.createSafetyContext(input(false));
        assertEquals(Boolean.FALSE, context.getSafetyRelevant());
        assertNull(context.getAssuranceLevel());
    }

    @Test
    void findsEveryContextForPurlWithoutCreatingAnything() {
        List<SafetyContext> results = List.of(new SafetyContext(), new SafetyContext());
        when(contexts.findByPurl(PURL)).thenReturn(results);
        assertEquals(results, service.findByPurl(" " + PURL + " "));
        verifyNoInteractions(dataManager);
        verify(contexts, never()).save(any(SafetyContext.class));
    }

    @Test
    void missingPurlReturnsEmptyResults() {
        when(contexts.findByPurl(PURL)).thenReturn(List.of());
        assertTrue(service.findByPurl(PURL).isEmpty());
    }

    @Test
    void rejectsUnknownComponentWithoutSaving() {
        when(components.findByPurl(PURL)).thenReturn(List.of());
        assertThrows(NoSuchElementException.class, () -> service.createSafetyContext(input(true)));
        verifyNoInteractions(dataManager);
        verify(contexts, never()).save(any(SafetyContext.class));
    }

    @Test
    void rejectsAmbiguousPurlWithoutSaving() {
        when(components.findByPurl(PURL)).thenReturn(List.of(component, new SoftwareComponent()));
        assertThrows(IllegalArgumentException.class, () -> service.createSafetyContext(input(true)));
        verify(contexts, never()).save(any(SafetyContext.class));
    }

    @Test
    void updatePreservesIdAndReplacesOptionalFields() {
        SafetyContext context = new SafetyContext();
        context.setId(42L);
        context.setAssuranceLevel("DAL-A");
        context.setRationale("Old rationale");
        context.setSourceDocument("old.json");
        when(contexts.findById(42L)).thenReturn(Optional.of(context));

        assertSame(context, service.updateSafetyContext(42L, input(false)));
        assertEquals(42L, context.getId());
        assertSame(component, context.getSoftwareComponent());
        assertEquals(Boolean.FALSE, context.getSafetyRelevant());
        assertNull(context.getAssuranceLevel());
        assertNull(context.getRationale());
        assertNull(context.getSourceDocument());
        verifyNoInteractions(dataManager);
    }

    @Test
    void updateDoesNotInsertMissingContext() {
        when(contexts.findById(42L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.updateSafetyContext(42L, input(true)));
        verify(contexts, never()).save(any(SafetyContext.class));
    }

    @Test
    void invalidUpdateDoesNotMutateExistingContext() {
        SafetyContext context = new SafetyContext();
        context.setRationale("Keep this");
        when(contexts.findById(42L)).thenReturn(Optional.of(context));
        when(components.findByPurl(PURL)).thenReturn(List.of());
        assertThrows(NoSuchElementException.class, () -> service.updateSafetyContext(42L, input(true)));
        assertEquals("Keep this", context.getRationale());
        verify(contexts, never()).save(any(SafetyContext.class));
    }

    @Test
    void rejectsMissingRequiredInputAndOversizedColumns() {
        assertThrows(IllegalArgumentException.class, () -> service.createSafetyContext(null));
        assertThrows(IllegalArgumentException.class, () -> service.createSafetyContext(input(null)));
        assertThrows(IllegalArgumentException.class, () -> service.findByPurl(" "));
        assertThrows(IllegalArgumentException.class, () -> service.updateSafetyContext(null, input(true)));
        assertThrows(IllegalArgumentException.class, () -> service.createSafetyContext(
                new SafetyContextInput(PURL, true, null, "A".repeat(256), null, null, null)));
        verify(contexts, never()).save(any(SafetyContext.class));
    }

    private static SafetyContextInput input(Boolean relevant) {
        return new SafetyContextInput(PURL, relevant, null, null, null, null, null);
    }
}

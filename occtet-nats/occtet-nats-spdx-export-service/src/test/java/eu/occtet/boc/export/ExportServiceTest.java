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

package eu.occtet.boc.export;

import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.dao.SpdxDocumentRootRepository;
import eu.occtet.boc.entity.Organization;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.spdxV2.CreationInfoEntity;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.export.service.AnswerService;
import eu.occtet.boc.export.service.ExportService;
import eu.occtet.boc.export.service.MergeService;
import eu.occtet.boc.model.SpdxExportWorkData;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExportServiceTest {

    @InjectMocks
    private ExportService exportService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SpdxDocumentRootRepository spdxDocumentRootRepository;

    @Mock
    private AnswerService answerService;

    @Mock
    private MergeService mergeService;

    @Test
    void process_ValidExportData_GeneratesAndPushesSbom() {
        ReflectionTestUtils.setField(exportService, "toolName", "TestTool-1.0");

        SpdxExportWorkData workData = new SpdxExportWorkData();
        workData.setProjectId(100L);
        workData.setSpdxDocumentId("https://test.uri/doc");
        workData.setObjectStoreKey("sbom-export.json");

        Project project = new Project("Export Project");
        project.setId(100L);
        project.setVersion("1.0.0");
        project.setProjectContact("Jane Doe");
        project.setCreatedAt(LocalDateTime.now());

        Organization org = new Organization();
        org.setOrganizationEmail("test@test.com");
        org.setOrganizationName("Test Org");

        project.setOrganization(org);
        org.getProjects().add(project);

        SpdxDocumentRoot documentRoot = getSpdxDocumentRoot();

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(spdxDocumentRootRepository.findByDocumentUri("https://test.uri/doc")).thenReturn(Optional.of(documentRoot));

        doNothing().when(mergeService).mergeChangesToDocumentEntities(any(), any());

        boolean result = exportService.process(workData);

        assertTrue(result, "Export process should return true on success");

        verify(mergeService, times(1)).mergeChangesToDocumentEntities(documentRoot, project);

        // capture the byte[] created in the service
        ArgumentCaptor<byte[]> byteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(answerService, times(1)).putIntoBucket(eq("sbom-export.json"), byteCaptor.capture());

        byte[] generatedPayload = byteCaptor.getValue();
        assertTrue(generatedPayload.length > 0, "Generated JSON payload should not be empty");

        String jsonString = new String(generatedPayload);
        assertTrue(jsonString.contains("SPDXRef-DOCUMENT"), "JSON should contain the document SPDX reference");
        assertTrue(jsonString.contains("Test Org"), "JSON should contain project organization");
    }

    @NotNull
    private static SpdxDocumentRoot getSpdxDocumentRoot() {
        CreationInfoEntity creationInfo = new CreationInfoEntity();
        creationInfo.setCreated("2026-03-09T10:00:00Z");
        creationInfo.setCreators("Tool: TestTool");

        SpdxDocumentRoot documentRoot = new SpdxDocumentRoot();
        documentRoot.setSpdxId("SPDXRef-DOCUMENT");
        documentRoot.setSpdxVersion("SPDX-2.3");
        documentRoot.setDataLicense("CC0-1.0");
        documentRoot.setName("Export Doc");
        documentRoot.setDocumentUri("https://test.uri/doc");
        documentRoot.setCreationInfo(creationInfo);
        return documentRoot;
    }

    @Test
    void process_ProjectNotFound_ReturnsFalse() {
        SpdxExportWorkData workData = new SpdxExportWorkData();
        workData.setProjectId(999L); // Non-existent ID
        workData.setSpdxDocumentId("https://test.uri/doc");

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = exportService.process(workData);

        assertFalse(result, "Export process should return false if the project is missing.");
        verify(spdxDocumentRootRepository, never()).findByDocumentUri(anyString());
        verify(mergeService, never()).mergeChangesToDocumentEntities(any(), any());
    }

    @Test
    void process_DocumentNotFound_ReturnsFalse() {
        SpdxExportWorkData workData = new SpdxExportWorkData();
        workData.setProjectId(100L);
        workData.setSpdxDocumentId("https://missing.uri/doc");

        Project project = new Project("Test Project");

        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(spdxDocumentRootRepository.findByDocumentUri("https://missing.uri/doc")).thenReturn(Optional.empty());

        boolean result = exportService.process(workData);

        assertFalse(result, "Export process should return false if the document root is missing.");
        verify(mergeService, never()).mergeChangesToDocumentEntities(any(), any());
    }
}

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

package eu.occtet.boc.spdx.service;


import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.service.ProgressReportingService;
import eu.occtet.boc.spdx.context.SpdxImportContext;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.service.handler.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.*;
import org.spdx.library.model.v2.license.*;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpdxService extends ProgressReportingService  {

    private static final Logger log = LogManager.getLogger(SpdxService.class);

    @Autowired
    private SpdxConverter spdxConverter;
    @Autowired
    private PackageHandler packageHandler;
    @Autowired
    private RelationshipHandler relationshipHandler;
    @Autowired
    private SnippetHandler snippetHandler;
    @Autowired
    private OrphanHandler orphanHandler;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private SpdxDocumentRootRepository spdxDocumentRootRepository;
    @Autowired
    private CleanUpService cleanUpService;

    public boolean process(SpdxWorkData workData) {
        log.debug("SpdxService: reads spdx and creates entities to curate {}", workData.toString());
        return parseDocument(workData);
    }

    /**
     * Takes spdxWorkData, extracts contained JSON and creates entities based on the deserialized spdxDocument created from the JSON.
     * If the entities are already present then no new ones will be created, however some of their attributes may change.
     *
     * @param spdxWorkData
     * @return true if the entities where created successfully, false is any error occurred
     */
    public boolean parseDocument(SpdxWorkData spdxWorkData) {
        try {
            log.info("now processing spdx for project id: {}", spdxWorkData.getProjectId());
            notifyProgress(1, "init");
            // setup for spdx library need to be called once before any spdx model objects are accessed

            SpdxDocument spdxDocument = loadSpdxDocument(spdxWorkData.getJsonBytes());
            Project project = loadProject(spdxWorkData.getProjectId(), spdxDocument);
            if (project == null) {
                return false;
            }

            cleanUpService.cleanUpFileTree(project);
            SpdxDocumentRoot spdxDocumentRoot = spdxConverter.convertSpdxV2DocumentInformation(spdxDocument);


            SpdxImportContext context = new SpdxImportContext(project, spdxDocument, spdxDocumentRoot);
            context.setExtractedLicenseInfos(spdxDocument.getExtractedLicenseInfos());
            initDocumentDescribes(context);

            notifyProgress(10, "converting spdx");

            packageHandler.processAllPackages(context, (percent) -> notifyProgress(20 + percent, "processing packages"));

            orphanHandler.processOrphanFiles(context);

            refreshInventoryCache(context);
            relationshipHandler.processAllRelationships(context, (percent) -> notifyProgress(60 + percent, "converting relationships"));

            snippetHandler.processAllSnippets(context);

            spdxDocumentRootRepository.save(spdxDocumentRoot);
            scheduleAnswerService(context, spdxWorkData);

            notifyProgress(100, "completed");
            return true;

        } catch (Exception e) {
            log.error("Error in SPDX orchestration", e);
            return false;
        }
    }

    private SpdxDocument loadSpdxDocument(byte[] jsonBytes) throws InvalidSPDXAnalysisException, IOException {
        SpdxModelFactory.init();
        MultiFormatStore inputStore = new MultiFormatStore(new InMemSpdxStore(), MultiFormatStore.Format.JSON);
        return inputStore.deSerialize(new ByteArrayInputStream(jsonBytes), false);
    }

    private Project loadProject(long projectId, SpdxDocument spdxDocument){
        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if(projectOptional.isEmpty()) {
            log.error("failed to find the project");
            return null;
        }
        Project project = projectOptional.get();
        project.setDocumentID(spdxDocument.getDocumentUri());
        projectRepository.save(project);
        return project;
    }

    private void initDocumentDescribes(SpdxImportContext context) {
        try {
            context.getSpdxDocument().getDocumentDescribes().stream()
                    .map(SpdxElement::getId)
                    .forEach(context.getMainPackageIds()::add);
        } catch (InvalidSPDXAnalysisException e) {
            log.warn("Could not read DocumentDescribes: {}", e.getMessage());
        }
    }

    private void refreshInventoryCache(SpdxImportContext context) {
        log.debug("Refreshing inventory cache for project {}", context.getProject().getId());
        List<InventoryItem> allItems = inventoryItemRepository.findAllByProject(context.getProject());

        Map<String, InventoryItem> inventoryCache = allItems.stream()
                .filter(item -> item.getSpdxId() != null)
                .collect(Collectors.toMap(
                        InventoryItem::getSpdxId,
                        item -> item,
                        (existing, replacement) -> existing
                ));

        context.setInventoryCache(inventoryCache);

        log.debug("Inventory cache refreshed. Mapped {} items.", inventoryCache.size());
    }

    private void scheduleAnswerService(SpdxImportContext context, SpdxWorkData workData) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    answerService.prepareAnswers(
                            context.getInventoryItems(),
                            workData.isUseCopyrightAi(),
                            workData.isUseLicenseMatcher(),
                            context.getMainInventoryItems()
                    );
                } catch (Exception e) {
                    log.error("Error sending answers", e);
                }
            }
        });
}

}
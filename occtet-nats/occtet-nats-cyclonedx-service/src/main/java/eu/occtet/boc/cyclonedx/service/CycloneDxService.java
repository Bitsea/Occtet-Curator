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

package eu.occtet.boc.cyclonedx.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.cyclonedx.service.handler.ComponentHandler;
import eu.occtet.boc.cyclonedx.service.handler.VulnerabilityHandler;
import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.model.CycloneDxWorkData;
import eu.occtet.boc.service.ProgressReportingService;
import eu.occtet.boc.cyclonedx.context.CycloneDxImportContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.Bom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CycloneDxService extends ProgressReportingService {

    private static final Logger log = LogManager.getLogger(CycloneDxService.class);

    @Autowired
    private ComponentHandler componentHandler;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @Autowired
    private AnswerService answerService;
    @Autowired
    private VulnerabilityHandler vulnerabilityHandler;
    @Autowired
    private CleanUpService cleanUpService;


    /**
     * using spdxWorkdata because the same data is used for CycloneDx, easier to mantain one data model
     * @param workData
     * @return
     */
    public boolean process(CycloneDxWorkData workData)  {
        log.debug("CycloneDxService: reads CycloneDx and creates entities to curate {}", workData.toString());
        return parseDocument(workData);
    }

    /**
     * Takes cycloneDxworkData, extracts contained JSON and creates entities based on the deserialized CycloneDx document created from the JSON.
     * If the entities are already present then no new ones will be created, however some of their attributes may change.
     *
     * @param cycloneDxWorkData
     * @return true if the entities where created successfully, false is any error occurred
     */
    public boolean parseDocument(CycloneDxWorkData cycloneDxWorkData) {
        try {
            log.info("now processing CycloneDx for project id: {}", cycloneDxWorkData.getProjectId());
            notifyProgress(1, "init");
            // setup for spdx library need to be called once before any spdx model objects are accessed

            Bom bom = loadCycloneDxDocument(cycloneDxWorkData.getJsonBytes());
            Project project = loadProject(cycloneDxWorkData.getProjectId());
            if (project == null) {
                return false;
            }

            cleanUpService.cleanUpFileTree(project);

            CycloneDxImportContext context = new CycloneDxImportContext(project);


            notifyProgress(10, "converting CycloneDx");
            refreshInventoryCache(context);
            refreshComponentCache(context);
            componentHandler.processAllPackages(context, (percent) -> notifyProgress(20 + percent, "processing packages")
                    ,bom, cycloneDxWorkData.isWithTestLibraries());
            vulnerabilityHandler.handleVulnerabilities(bom, context);


            scheduleAnswerService(context, cycloneDxWorkData);

            notifyProgress(100, "completed");
            return true;

        } catch (Exception e) {
            log.error("Handler caused CycloneDx Analysis to fail: {}", e.getMessage());
            return false;
        }
    }

    private Bom loadCycloneDxDocument(byte[] jsonBytes){
        try {
            ObjectMapper mapper = new ObjectMapper();

            Bom bom = mapper.readValue(jsonBytes, Bom.class);

            return bom;
        } catch (Exception e) {
            log.error("Error parsing CycloneDX JSON", e);
            throw new RuntimeException("Fehler beim Parsen des CycloneDX-JSON-Strings", e);
        }
    }

    private Project loadProject(long projectId) {
        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isEmpty()) {
            log.error("failed to find the project");
            return null;
        }

        return projectOptional.get();
    }

    /**
     * we have a inventorycahce for curated inventoryitems so we can easily check if an item already exists,
     * so we can reuse
     *
     * @param context
     */
    private void refreshInventoryCache(CycloneDxImportContext context) {
        log.debug("Refreshing inventory cache for project {}", context.getProject().getId());
        List<InventoryItem> allItems = inventoryItemRepository.findAllByProjectAndCurated(context.getProject(), true);

        Map<String, InventoryItem> inventoryCache = allItems.stream()
                .collect(Collectors.toMap(
                        InventoryItem::getInventoryName,
                        item -> item,
                        (existing, replacement) -> existing
                ));

        context.setInventoryCache(inventoryCache);

        log.debug("Inventory cache refreshed. Mapped {} items.", inventoryCache.size());
    }

    private void refreshComponentCache(CycloneDxImportContext context) {
        log.debug("Refreshing component cache for project {}", context.getProject().getId());
        List<SoftwareComponent> allItems = softwareComponentRepository.findComponentsByProject(context.getProject());

        Map<String, SoftwareComponent> componentCache = allItems.stream()
                .collect(Collectors.toMap(
                        SoftwareComponent::getPurl,
                        item -> item,
                        (existing, replacement) -> existing
                ));

        context.setComponentCache(componentCache);

        log.debug("Component cache refreshed. Mapped {} components.", componentCache.size());
    }

    private void scheduleAnswerService(CycloneDxImportContext context, CycloneDxWorkData workData) {
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
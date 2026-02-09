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
import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.service.ProgressReportingService;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.service.handler.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.TypedValue;
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
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc = new ArrayList<>();

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
    public boolean parseDocument(SpdxWorkData spdxWorkData){
        try {
            log.info("now processing spdx for project id: {}", spdxWorkData.getProjectId());
            notifyProgress(1,"init");
            // setup for spdx library need to be called once before any spdx model objects are accessed
            SpdxModelFactory.init();
            InMemSpdxStore modelStore = new InMemSpdxStore();
            MultiFormatStore inputStore = new MultiFormatStore(modelStore, MultiFormatStore.Format.JSON);

            byte[] spdxJsonBytes = spdxWorkData.getJsonBytes();

            InputStream inputStream = new ByteArrayInputStream(spdxJsonBytes);
            SpdxDocument spdxDocument = inputStore.deSerialize(inputStream, false);

            SpdxDocumentRoot spdxDocumentRoot = spdxConverter.convertSpdxV2DocumentInformation(spdxDocument);
            notifyProgress(10,"converting spdx");
            long projectId = spdxWorkData.getProjectId();
            Optional<Project> projectOptional = projectRepository.findById(projectId);
            if(projectOptional.isEmpty()) {
               log.error("failed to find the project");
                return false;
            }
            Project project = projectOptional.get();
            project.setDocumentID(spdxDocument.getDocumentUri());
            projectRepository.save(project);

            List<InventoryItem> inventoryItems = new ArrayList<>();
            List<TypedValue> packageUri = spdxDocument.getModelStore().getAllItems(null, "Package").toList();
            List<SpdxPackage> spdxPackages = new ArrayList<>();
            //avoid looking at packages multiple times
            HashSet<String> seenPackages = new HashSet<>();
            licenseInfosExtractedSpdxDoc = spdxDocument.getExtractedLicenseInfos();
            Set<String> processedFileIds = new HashSet<>();

            //get the list of described packages for the download-service to differentiate between them and dependencies
            Set<String> mainPackageIds = new HashSet<>();
            Set<Long> mainInventoryItems = new HashSet<>();
            notifyProgress(20,"collecting document describes");
            try {
                Set<String> ids = spdxDocument.getDocumentDescribes().stream()
                        .map(SpdxElement::getId)
                        .collect(Collectors.toSet());
                mainPackageIds.addAll(ids);
            } catch (InvalidSPDXAnalysisException e) {
                log.warn("Could not read DocumentDescribes: {}", e.getMessage());
            }
            //before entities are created and saved a cleanup for file entities connected to the project is done
            cleanUpService.cleanUpFileTree(project);

            Map<String, SpdxPackageEntity> packageLookupMap = new HashMap<>();
            if (spdxDocumentRoot.getPackages() != null) {
                for (SpdxPackageEntity entity : spdxDocumentRoot.getPackages()) {
                    if (entity.getSpdxId() != null) {
                        packageLookupMap.put(entity.getSpdxId(), entity);
                    }
                }
            }

            //Caches for component and license
            Map<String, SoftwareComponent> componentCache = new HashMap<>();
            Map<String, License> licenseCache = new HashMap<>();
            Map<String, InventoryItem> fileToInventoryItemMap = new HashMap<>();
            int count=0;
            for (TypedValue uri : packageUri) {
                SpdxModelFactory.getSpdxObjects(spdxDocument.getModelStore(), null, "Package", uri.getObjectUri(), null).forEach(
                        spdxPackage -> {
                            try {
                                if (!seenPackages.contains(spdxPackage.toString())) {
                                    log.debug("Processing unseen package: {}", spdxPackage.toString());
                                    inventoryItems.add(packageHandler.parsePackages((SpdxPackage) spdxPackage, project,
                                            spdxDocumentRoot, packageLookupMap, componentCache, licenseCache,
                                            mainInventoryItems, mainPackageIds, fileToInventoryItemMap,
                                            processedFileIds,  licenseInfosExtractedSpdxDoc));
                                    spdxPackages.add((SpdxPackage) spdxPackage);
                                    spdxPackages.add((SpdxPackage) spdxPackage);
                                    seenPackages.add((spdxPackage).toString());
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
                count++;
                int progress = 20 + (int) ( (40*count) / packageUri.size());
                if(progress%5 ==0)
                    notifyProgress(progress, "processing packages");
            }

            List<InventoryItem> allItems = inventoryItemRepository.findAllByProject(project);
            Map<String, InventoryItem> inventoryCache = allItems.stream()
                    .filter(i -> i.getSpdxId() != null)
                    .collect(Collectors.toMap(InventoryItem::getSpdxId, item -> item, (p1, p2) -> p1));

            try {
                orphanHandler.processOrphanFiles(spdxDocument, project, spdxDocumentRoot,
                        licenseCache, fileToInventoryItemMap, processedFileIds, inventoryItems, licenseInfosExtractedSpdxDoc);
            } catch (Exception e) {
                log.error("Error processing orphan files", e);
            }

            count=0;
            for (SpdxPackage spdxPackage : spdxPackages) {
                relationshipHandler.parseRelationships(spdxPackage, spdxPackage.getRelationships().stream().toList(), inventoryCache);
                for(Relationship relationship: spdxPackage.getRelationships()) {
                    spdxConverter.convertRelationShip(relationship, spdxDocumentRoot, spdxPackage);
                }
                log.debug("Converted {} relationships for package {}", spdxPackage.getRelationships().size(), spdxPackage.getId());
                count++;
                int progress = 60 + (int) (((double) count / packageUri.size()) * 40);
                if(progress%5 ==0)
                    notifyProgress(progress, "converting relationships");
            }

            Stream<?> rawStream = SpdxModelFactory.getSpdxObjects(
                    spdxDocument.getModelStore(),
                    spdxDocument.getCopyManager(),
                    SpdxConstantsCompatV2.CLASS_SPDX_SNIPPET,
                    spdxDocument.getDocumentUri(),
                    null
            );

            Stream<SpdxSnippet> snippetStream = rawStream
                    .filter(obj -> obj instanceof SpdxSnippet)
                    .map(obj -> (SpdxSnippet) obj);

            snippetStream.forEach(snippet -> {
                //create conversion objects
                spdxConverter.convertSnippets(snippet, spdxDocumentRoot);

                try {
                    snippetHandler.enrichComponentFromSnippet(snippet, fileToInventoryItemMap, licenseCache, licenseInfosExtractedSpdxDoc);
                } catch (Exception e) {
                    log.error("Failed to enrich component from snippet: {}", snippet.getId(), e);
                }
            });

            spdxDocumentRootRepository.save(spdxDocumentRoot);

            log.info("processed spdx with {} items", inventoryItems.size());



            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                        log.debug("Transaction committed, sending answers service");
                    try {
                        answerService.prepareAnswers(
                                inventoryItems,
                                spdxWorkData.isUseCopyrightAi(),
                                spdxWorkData.isUseLicenseMatcher(),
                                mainInventoryItems
                        );
                    } catch (Exception e) {
                        log.error("Error sending answers to the answers service", e);
                    }
                    log.debug(("SENT"));
                    }
            });

            notifyProgress(100, "completed");
            return true;

        }catch (InvalidSPDXAnalysisException | IOException e ){
            log.error("An error occurred while trying to deserialize spdx: {}", e.toString());
            return false;
        }
    }



}
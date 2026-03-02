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

package eu.occtet.boc.spdx.service.handler;

import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.spdx.context.SpdxImportContext;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.service.InventoryItemService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.TypedValue;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxElement;
import org.spdx.library.model.v2.SpdxPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
public class RelationshipHandler {

    private static final Logger log = LogManager.getLogger(RelationshipHandler.class);

    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    SpdxConverter spdxConverter;

    /**
     * Orchestrates the processing of all relationships in the document.
     * Iterates through all packages, parses their relationships, and updates the entities.
     *
     * @param context The shared import context containing the document, root, and caches.
     * @param progressCallback A callback to report progress
     */
    public void processAllRelationships(SpdxImportContext context, Consumer<Integer> progressCallback) {
        try {
            List<TypedValue> packageUris = context.getSpdxDocument().getModelStore()
                    .getAllItems(null, "Package").toList();

            int total = packageUris.size();
            int count = 0;

            Set<String> seenPackages = new HashSet<>();

            for (TypedValue uri : packageUris) {
                SpdxModelFactory.getSpdxObjects(context.getSpdxDocument().getModelStore(), null, "Package", uri.getObjectUri(), null)
                        .forEach(obj -> {
                            if (obj instanceof SpdxPackage spdxPackage && !seenPackages.contains(spdxPackage.getId())) {
                                processSinglePackageRelationships(spdxPackage, context);
                                seenPackages.add(spdxPackage.getId());
                            }
                        });

                count++;
                int currentProgress = (int) (((double) count / total) * 39);
                if (count % 5 == 0 || count == total) {
                    progressCallback.accept(currentProgress);
                }
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error("Error retrieving packages for relationship processing", e);
        }
    }

    /**
     * Processes relationships for a single package.
     */
    private void processSinglePackageRelationships(SpdxPackage spdxPackage, SpdxImportContext context)  {
        try {
            List<Relationship> relationships = spdxPackage.getRelationships().stream().toList();
            parseRelationshipsLogic(spdxPackage, relationships, context.getInventoryCache());


            for (Relationship relationship : relationships) {
                spdxConverter.convertRelationShip(relationship, context.getSpdxDocumentRoot(), spdxPackage);
            }

            log.debug("Converted {} relationships for package {}", relationships.size(), spdxPackage.getId());
        }catch (InvalidSPDXAnalysisException e) {
            log.debug("Failed to process relationships from package {}", spdxPackage.getId(), e);
        }
    }

    /**
     * Core logic to interpret relationships and update InventoryItems.
     */
    private void parseRelationshipsLogic(SpdxPackage spdxPackage, List<Relationship> relationships,
                                         Map<String, InventoryItem> inventoryCache) {

        InventoryItem sourceItem = inventoryCache.get(spdxPackage.getId());

        if (sourceItem == null) {
            log.debug("Relationship source package not found in inventory: {}. Skipping...", spdxPackage.getId());
            return;
        }

        for (Relationship relationship : relationships) {
            try {
                Optional<SpdxElement> targetOpt = relationship.getRelatedSpdxElement();
                if (targetOpt.isEmpty()) continue;

                SpdxElement targetElement = targetOpt.get();
                InventoryItem targetItem = inventoryCache.get(targetElement.getId());

                if (targetItem == null) continue;

                switch (relationship.getRelationshipType()) {
                    case CONTAINS, DEPENDS_ON, ANCESTOR_OF -> {
                        if (targetElement instanceof SpdxPackage) {
                            targetItem.setParent(sourceItem);
                            inventoryItemService.update(targetItem);
                            log.info("identified {} as parent of {}", sourceItem.getInventoryName(), targetItem.getInventoryName());
                        }
                    }
                    case CONTAINED_BY, DEPENDENCY_OF, DESCENDANT_OF -> {
                        if (targetElement instanceof SpdxPackage) {
                            sourceItem.setParent(targetItem);
                            inventoryItemService.update(sourceItem);
                            log.info("identified {} as child of {}", sourceItem.getInventoryName(), targetItem.getInventoryName());
                        }
                    }
                    case STATIC_LINK -> {
                        if (targetElement instanceof SpdxPackage) {
                            targetItem.setLinking("Static");
                            inventoryItemService.update(targetItem);
                        }
                    }
                    case DYNAMIC_LINK -> {
                        if (targetElement instanceof SpdxPackage) {
                            targetItem.setLinking("Dynamic");
                            inventoryItemService.update(targetItem);
                        }
                    }
                    case null, default -> {
                    }
                }
            }catch (InvalidSPDXAnalysisException e) {
                log.warn("Malformed relationship in package {}. Skipping.", spdxPackage.getId());
            }
        }
    }
}

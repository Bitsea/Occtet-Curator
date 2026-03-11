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

package eu.occtet.boc.spdx.service;


import eu.occtet.boc.dao.OrtIssueRepository;
import eu.occtet.boc.dao.OrtViolationRepository;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.spdx.factory.InventoryItemFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryItemService {

    @Autowired
    private OrtIssueRepository ortIssueRepository;
    @Autowired
    private OrtViolationRepository ortViolationRepository;

    private static final Logger log = LogManager.getLogger(InventoryItemService.class);


    @Autowired
    private InventoryItemFactory inventoryItemFactory;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    public InventoryItem getOrCreateInventoryItem(String inventoryName, SoftwareComponent sc,
                                                  Project project) {
        List<InventoryItem> inventoryItemList = inventoryItemRepository.findByProjectAndInventoryName(
                project, inventoryName
        );
        if (inventoryItemList.isEmpty()) {
            return inventoryItemFactory.create(inventoryName, project, sc);
        }
        return inventoryItemList.getFirst(); // Return the first inventory of the inventories found
    }



    public void update(InventoryItem inventoryItem){
        inventoryItemRepository.save(inventoryItem);
    }


    /**
     * Sorts the given lists of OrtIssues and OrtViolations by the purl of the given InventoryItem's SoftwareComponent.
     * putting this here, because it is needed multiple times
     * @param ortIssues
     * @param ortViolations
     * @param inventoryItem
     */
    public void sortViolationsAndIssues(List<OrtIssue> ortIssues, List<OrtViolation> ortViolations, InventoryItem inventoryItem){
        log.debug("sorting violations and issues for inventory item {}", inventoryItem.getInventoryName());

        String purl = inventoryItem.getSoftwareComponent().getPurl();
        log.debug("sorting for purl {}", purl);
        if (purl != null) {
            ortIssues.removeIf(issue -> {
                if (purl.equals(issue.getPurl())) {
                    issue.setInventoryItem(inventoryItem);
                    ortIssueRepository.save(issue);
                    log.debug("issue found for purl {}", purl);
                    return true;
                }
                return false;
            });

            ortViolations.removeIf(vio -> {
                if (purl.equals(vio.getPurl())) {
                    vio.setInventoryItem(inventoryItem);
                    ortViolationRepository.save(vio);
                    log.debug("violation found for purl {}", purl);
                    return true;
                }
                return false;
            });

        }
    }
}

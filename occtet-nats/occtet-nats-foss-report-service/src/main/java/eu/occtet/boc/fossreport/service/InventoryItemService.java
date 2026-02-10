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

package eu.occtet.boc.fossreport.service;

import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.SoftwareComponentRepository;
import eu.occtet.boc.fossreport.factory.InventoryItemFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
public class InventoryItemService {

    @Autowired
    private InventoryItemFactory inventoryItemFactory;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

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

    public InventoryItem getOrCreateInventoryItemWithAllAttributes(
            Project project,
            String inventoryName,
            Integer size,
            String linking,
            String externalNotes,
            InventoryItem parentItem,
            SoftwareComponent sc,
            List<Copyright> copyrights,
            Integer priority
    ) {
        List<InventoryItem> inventoryItemList =
                inventoryItemRepository.findByProjectAndSoftwareComponentAndInventoryName(
                        project, sc, inventoryName
                );
        InventoryItem inventoryItem;

        if(inventoryItemList.isEmpty()){
            inventoryItem = inventoryItemFactory.create(
                    inventoryName, size, linking, externalNotes, parentItem, sc, copyrights,
                    project, priority);
        } else {
            inventoryItem = inventoryItemList.getFirst();
            updateInventoryItem(inventoryItem,
                    linking,  externalNotes, parentItem, sc,  copyrights
            );
        }

        return inventoryItem;
    }

    private void updateInventoryItem(
            InventoryItem inventoryItem,
            String linking,
            String externalNotes,
            InventoryItem parentItem,
            SoftwareComponent component,
            List<Copyright> copyrights
    ) {
        if(!inventoryItem.getLinking().equals(linking) && linking != null && !linking.isEmpty()){
            inventoryItem.setLinking(linking);
        }

        if(externalNotes != null && !externalNotes.isEmpty()){
            if(inventoryItem.getExternalNotes() == null || inventoryItem.getExternalNotes().isEmpty()){
                inventoryItem.setExternalNotes(externalNotes);
            } else if(!inventoryItem.getExternalNotes().equals(externalNotes)){
                inventoryItem.setExternalNotes(inventoryItem.getExternalNotes() + "\n" + externalNotes);
            }
        }if(parentItem != null){
            inventoryItem.setParent(parentItem);
        }if(component != null) {
            inventoryItem.setSoftwareComponent(component);
        }if(copyrights != null && !copyrights.isEmpty()){
            HashSet<Copyright> updatedCopyrights = new HashSet<>(inventoryItem.getSoftwareComponent().getCopyrights());
            updatedCopyrights.addAll(copyrights);
            inventoryItem.getSoftwareComponent().setCopyrights(List.copyOf(updatedCopyrights));
            softwareComponentRepository.save(inventoryItem.getSoftwareComponent());
        }

        inventoryItemRepository.save(inventoryItem);
    }
}

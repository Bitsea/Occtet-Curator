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

package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InventoryItemService {

    private static final Logger log = LogManager.getLogger(InventoryItemService.class);

    @Autowired
    private SoftwareComponentService softwareComponentService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private DataManager dataManager;

    public List<InventoryItem> findInventoryItemsOfProject(Project project){
        return inventoryItemRepository.findByProject(project);
    }

    public List<InventoryItem> findInventoryItemsOfSoftwareComponent(SoftwareComponent softwareComponent){
        return inventoryItemRepository.findBySoftwareComponent(softwareComponent);
    }

    public List<InventoryItem> findInventoryItemsByCurated(Boolean curated){
        return inventoryItemRepository.findInventoryItemsByCurated(curated);
    }
    
    public List<InventoryItem> findInventoryItemsByIsVulnerable(Boolean isVulnerable){
        List<InventoryItem> inventoryItems = new ArrayList<>();
        softwareComponentService.findSoftwareComponentsByIsVulnerable(isVulnerable)
                .forEach(sc->inventoryItems.addAll(
                        inventoryItemRepository.findInventoryItemBySoftwareComponentOrderByCreatedAtDesc(sc)));
        return inventoryItems;
    }

    public List<InventoryItem> findInventoryItemsByLicense(License license){
        return inventoryItemRepository.searchInventoryItemsBySoftwareComponent_Licenses(license);
    }

    public void controlInventoryItem(InventoryItem item){

        if(item != null){

            List<InventoryItem> sameItems = inventoryItemRepository
                    .findInventoryItemsByInventoryNameAndSoftwareComponent(item.getInventoryName(),item.getSoftwareComponent());

            if(!sameItems.isEmpty() && sameItems.size()>1) {
                if(item.getParent() == null){

                    InventoryItem latestItem = sameItems.get(0);
                    for(InventoryItem item1 : sameItems){
                        if(item1.getCreatedAt().isAfter(latestItem.getCreatedAt())){
                            latestItem = item1;
                        }
                    }
                    item.setParent(latestItem.getParent());
                    dataManager.save(item);

                    List<InventoryItem> childItems = inventoryItemRepository.findInventoryItemsByParent(latestItem);
                    if(childItems != null && !childItems.isEmpty()){
                        for(InventoryItem child : childItems){
                            child.setParent(item);
                            dataManager.save(child);
                        }
                    }
                }
            }else if(!sameItems.isEmpty()){
                if(item.getParent() == null){
                    InventoryItem latest = sameItems.get(0);
                    item.setParent(latest.getParent());
                    List<InventoryItem> childItems = inventoryItemRepository.findInventoryItemsByParent(latest);

                    if(childItems != null && !childItems.isEmpty()){
                        for(InventoryItem child : childItems){
                            child.setParent(item);
                            dataManager.save(child);
                        }
                    }
                }
            }
        }
    }

    public List<InventoryItem> filterIventoryItems(List<InventoryItem> items){

        items.sort(Comparator.comparing(InventoryItem::getCreatedAt).reversed());
        List<InventoryItem> filteredItems = new ArrayList<>();
        Set<String> findItems = new HashSet<>();

        for(InventoryItem item : items){
            if(!findItems.contains(item.getInventoryName())){
                findItems.add(item.getInventoryName());
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }
}

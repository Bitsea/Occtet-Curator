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

import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.DataManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.OrtIssue;
import eu.occtet.bocfrontend.entity.OrtViolation;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class InventoryItemService {

    private static final Logger log = LogManager.getLogger(InventoryItemService.class);

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private DataManager dataManager;

    public List<InventoryItem> findInventoryItemsOfProject(Project project){
        return inventoryItemRepository.findByProject(project);
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

    /**
     * Safely deletes an InventoryItem by removing all associations:
     * - Unlinks child inventory items
     * - Removes from dependencies of other inventory items
     * - Removes from files' inventoryItems association
     * - Clears foreign key references in ORT issues and violations
     * - Deletes the inventory item entity
     *
     * @param item the inventory item to delete
     */
    @Transactional
    public void deleteInventoryItem(InventoryItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        deleteInventoryItemById(item.getId(), item.getInventoryName());
    }

    /**
     * Safely deletes an InventoryItem by its ID.
     *
     * @param itemId the ID of the inventory item to delete
     * @param itemName the name for logging (optional)
     */
    @Transactional
    public void deleteInventoryItemById(Long itemId, String itemName) {
        if (itemId == null) {
            return;
        }
        log.info("Deleting inventory item with id: {} ({})", itemId, itemName);

        // 1. Unlink child inventory items
        List<InventoryItem> children = dataManager.load(InventoryItem.class)
                .query("select i from InventoryItem i where i.parent.id = :itemId")
                .parameter("itemId", itemId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("parent"))
                .list();
        for (InventoryItem child : children) {
            child.setParent(null);
            dataManager.save(child);
        }

        // 2. Unlink dependencies in other inventory items
        List<InventoryItem> dependingItems = dataManager.load(InventoryItem.class)
                .query("select distinct i from InventoryItem i join i.dependencies d where d.id = :itemId")
                .parameter("itemId", itemId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("dependencies"))
                .list();
        for (InventoryItem dep : dependingItems) {
            if (dep.getDependencies() != null) {
                dep.getDependencies().removeIf(depItem -> depItem != null && Objects.equals(depItem.getId(), itemId));
                dataManager.save(dep);
            }
        }

        // 3. Unlink files
        List<File> files = dataManager.load(File.class)
                .query("select distinct f from File f join f.inventoryItems i where i.id = :itemId")
                .parameter("itemId", itemId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("inventoryItems"))
                .list();
        for (File file : files) {
            if (file.getInventoryItems() != null) {
                file.getInventoryItems().removeIf(invItem -> invItem != null && Objects.equals(invItem.getId(), itemId));
                dataManager.save(file);
            }
        }

        // 4. Unlink ORT issues
        List<OrtIssue> ortIssues = dataManager.load(OrtIssue.class)
                .query("select o from OrtIssue o where o.inventoryItem.id = :itemId")
                .parameter("itemId", itemId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("inventoryItem"))
                .list();
        for (OrtIssue issue : ortIssues) {
            issue.setInventoryItem(null);
            dataManager.save(issue);
        }

        // 5. Unlink ORT violations
        List<OrtViolation> ortViolations = dataManager.load(OrtViolation.class)
                .query("select o from OrtViolation o where o.inventoryItem.id = :itemId")
                .parameter("itemId", itemId)
                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("inventoryItem"))
                .list();
        for (OrtViolation violation : ortViolations) {
            violation.setInventoryItem(null);
            dataManager.save(violation);
        }

        // 6. Delete the item itself using ID
        dataManager.remove(Id.of(itemId, InventoryItem.class));
    }

}

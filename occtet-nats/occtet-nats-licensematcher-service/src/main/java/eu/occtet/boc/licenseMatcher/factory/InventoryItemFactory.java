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

package eu.occtet.boc.licenseMatcher.factory;


import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.licenseMatcher.dao.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryItemFactory {

    private final InventoryItemRepository inventoryItemRepository;

    @Autowired
    public InventoryItemFactory(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public InventoryItem create(String inventoryName, int size, String linking,String externalNotes, InventoryItem parent,
                                SoftwareComponent component, boolean wasCombined, List<Copyright> copyrights,
                                Project project, String basePath, String spdxId) {

        InventoryItem inventoryItem = new InventoryItem(
                inventoryName, size, linking, copyrights,  externalNotes,
                parent, component, wasCombined, false, project, basePath, spdxId);

        return inventoryItemRepository.save(inventoryItem);
    }


    public InventoryItem create(String inventoryName, Project project, SoftwareComponent sc) {
        InventoryItem inventoryItem = new InventoryItem(inventoryName, project, sc);
        return inventoryItemRepository.save(inventoryItem);
    }

    public void update(InventoryItem item) {
        inventoryItemRepository.save(item);
    }
}

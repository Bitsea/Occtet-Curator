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

package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InventoryItemFactory {

    @Autowired
    protected DataManager dataManager;


    public InventoryItem create(@Nonnull String inventoryName, int size, @Nonnull String linking, @Nonnull String externalNotes,
                                @Nonnull InventoryItem parent, @Nonnull SoftwareComponent softwareComponent,
                                 boolean curated, @Nonnull Project project){
        InventoryItem inventoryItem = dataManager.create(InventoryItem.class);
        inventoryItem.setInventoryName(inventoryName);
        inventoryItem.setSize(size);
        inventoryItem.setLinking(linking);
        inventoryItem.setExternalNotes(externalNotes);
        inventoryItem.setParent(parent);
        inventoryItem.setSoftwareComponent(softwareComponent);
        inventoryItem.setCurated(curated);
        inventoryItem.setProject(project);

        return dataManager.save(inventoryItem);
    }


    public InventoryItem create(@Nonnull String inventoryName, SoftwareComponent softwareComponent, Project project){
        return create(inventoryName, 0, "", "", null, softwareComponent, false, project);
    }
}

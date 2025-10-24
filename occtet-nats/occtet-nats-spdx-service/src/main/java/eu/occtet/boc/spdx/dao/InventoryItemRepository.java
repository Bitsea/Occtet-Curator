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

package eu.occtet.boc.spdx.dao;



import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByInventoryNameAndSoftwareComponent(String inventoryName, SoftwareComponent sc);

    List<InventoryItem> findBySoftwareComponentAndProject(SoftwareComponent softwareComponent, Project project);


    List<InventoryItem> findBySoftwareComponent(SoftwareComponent sc);

    List<InventoryItem> findByProjectAndSoftwareComponent(Project project, SoftwareComponent sc);

    List<InventoryItem> findByProjectAndInventoryName(Project project, String inventoryName);

    List<InventoryItem> findByProjectAndSoftwareComponentAndInventoryName(Project project, SoftwareComponent sc, String inventoryName);

    List<InventoryItem> findBySpdxIdAndProject(String spdxID, Project project);
    Optional<InventoryItem> findById(UUID uid);
}

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

package eu.occtet.boc.dao;

import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByProjectAndInventoryName(Project project, String inventoryName);

    List<InventoryItem> findByProjectAndSoftwareComponentAndInventoryName(Project project, SoftwareComponent sc, String inventoryName);

    List<InventoryItem> findBySpdxIdAndProject(String spdxID, Project project);

    List<InventoryItem> findAllByProject(Project project);

//    @Query("SELECT DISTINCT ii.*\n" +
//            "FROM inventory_item ii\n" +
//            "JOIN project p \n" +
//            "    ON ii.project_id = p.id\n" +
//            "JOIN inventory_item_software_component iisc \n" +
//            "    ON iisc.inventory_item_id = ii.id\n" +
//            "JOIN software_component sc \n" +
//            "    ON sc.id = iisc.software_component_id\n" +
//            "WHERE p.id = :projectId\n" +
//            "  AND sc.purl = :purl;")
//    List<InventoryItem> findByProjectIdAndSoftwareComponentPurl(Long projectId, String purl);

    @Query("select distinct i from InventoryItem i join i.project p join i.softwareComponent sc where p.id = :projectId and sc.purl = :purl")
    List<InventoryItem> findByProjectIdAndSoftwareComponentPurl(Long projectId, String purl);
}

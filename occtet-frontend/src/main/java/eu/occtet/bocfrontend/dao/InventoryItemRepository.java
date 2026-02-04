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

package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.*;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;

import java.util.List;


public interface InventoryItemRepository extends JmixDataRepository<InventoryItem, Long> {

    List<InventoryItem> findAll();
    List<InventoryItem> findBySoftwareComponent(SoftwareComponent softwareComponent);
    List<InventoryItem> findBySoftwareComponentIn(List<SoftwareComponent> softwareComponents);
    List<InventoryItem> findInventoryItemBySoftwareComponentOrderByCreatedAtDesc(SoftwareComponent softwareComponent);
    List<InventoryItem> findByProject(Project project);
    List<InventoryItem> findInventoryItemsByCurated(Boolean curated);
    List<InventoryItem> searchInventoryItemsBySoftwareComponentIsNotNull();
    List<InventoryItem> searchInventoryItemsBySoftwareComponent_Licenses(License license);
    List<InventoryItem> findInventoryItemsByInventoryNameAndSoftwareComponent(String name,SoftwareComponent softwareComponent);
    List<InventoryItem> findInventoryItemsByParent(InventoryItem item);
    List<InventoryItem> findInventoryItemsByProjectAndParent(Project project, InventoryItem item);
    @Query("select distinct i from InventoryItem i join i.softwareComponent sc join i.project p where sc in :softwareComponents and p = :project")
    List<InventoryItem> findBySoftwareComponentInAndProject(List<SoftwareComponent> softwareComponents, Project project);
    @Query("select distinct i from InventoryItem i join i.softwareComponent sc join sc.licenses l join i.project p where l = :license and p = :project")
    List<InventoryItem> findByLicenseAndProject(License license, Project project);
    @Query("select distinct i from InventoryItem i join i.softwareComponent sc join sc.vulnerabilities v join i.project p where v = :vulnerability and p = :project")
    List<InventoryItem> findByVulnerabilityAndProject(Vulnerability vulnerability, Project project);
}

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

import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findAllByProject(Project project);
    List<File> findByInventoryItemsContaining(InventoryItem inventoryItem);


    @Modifying
    @Query(value = "DELETE FROM copyright_file_link WHERE file_id IN (SELECT id FROM file WHERE project_id = ?1)", nativeQuery = true)
    void deleteCopyrightFileLinksByProject(@Param("projectId") Long projectId);

    @Modifying
    @Query(value = "DELETE FROM file_inventory_item_link WHERE file_id IN (SELECT id FROM file WHERE project_id = ?1)", nativeQuery = true)
    void deleteInventoryItemFileLinksByProject(@Param("projectId") Long projectId);

    @Modifying
    @Query(value = "UPDATE file SET parent_id = NULL WHERE project_id = ?1", nativeQuery = true)
    void unlinkParentsByProject(@Param("projectId") Long projectId);


    boolean existsByProject(Project project);

    @Modifying
    @Query(value = "DELETE FROM copyright_file_link WHERE file_id IN (" +
            "  SELECT link.file_id FROM copyright_file_link link " +
            "  JOIN file f ON link.file_id = f.id " +
            "  WHERE f.project_id = ?1 LIMIT ?2" +
            ")", nativeQuery = true)
    int deleteCopyrightFileLinksBatchByProject(Long projectId, int limit);

    // 2. Inventory Item Links
    @Modifying
    @Query(value = "DELETE FROM file_inventory_item_link WHERE file_id IN (" +
            "  SELECT link.file_id FROM file_inventory_item_link link " +
            "  JOIN file f ON link.file_id = f.id " +
            "  WHERE f.project_id = ?1 LIMIT ?2" +
            ")", nativeQuery = true)
    int deleteInventoryItemFileLinksBatchByProject(Long projectId, int limit);

    // 3. Parent-Child Hierarchien auflösen
    @Modifying
    @Query(value = "UPDATE file SET parent_id = NULL WHERE id IN (" +
            "  SELECT id FROM file " +
            "  WHERE project_id = ?1 AND parent_id IS NOT NULL LIMIT ?2" +
            ")", nativeQuery = true)
    int unlinkParentsBatchByProject(Long projectId, int limit);

    // 4. File-Hauptdatensätze löschen
    @Modifying
    @Query(value = "DELETE FROM file WHERE id IN (" +
            "  SELECT id FROM file " +
            "  WHERE project_id = ?1 LIMIT ?2" +
            ")", nativeQuery = true)
    int deleteBatchByProject(Long projectId, int limit);

}
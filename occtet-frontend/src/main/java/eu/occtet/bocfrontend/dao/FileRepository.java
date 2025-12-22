/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FileRepository extends JmixDataRepository<File, UUID> {
    final String DEPENDENCY_FOLDER_NAME = "dependencies";

    // Root Nodes (Reviewed Filter)
    @Query("select f from File f " +
            "where f.project = :project and f.parent is null " +
            "order by " +
            "   CASE WHEN lower(f.fileName) = '" + DEPENDENCY_FOLDER_NAME + "' THEN 1 ELSE 0 END ASC, " +
            "   f.isDirectory desc, " +
            "   f.fileName asc")
    List<File> findRootsSorted(@Param("project") Project project,
                               @Param("targetStatus") Boolean targetStatus,
                               Pageable pageable);

    // Child Nodes (Reviewed Filter)
    @Query("select f from File f " +
            "where f.parent = :parent " +
            "order by " +
            "   CASE WHEN lower(f.fileName) = '" + DEPENDENCY_FOLDER_NAME + "' THEN 1 ELSE 0 END ASC, " +
            "   f.isDirectory desc, " +
            "   f.fileName asc")
    List<File> findChildrenSorted(@Param("parent") File parent,
                                  @Param("targetStatus") Boolean targetStatus,
                                  Pageable pageable);

    long countByProjectAndParentIsNull(Project project);
    long countByParent(File parent);
    long countByProjectAndParentIsNullAndReviewed(Project project, Boolean reviewed);
    long countByParentAndReviewed(File parent, Boolean reviewed);

    // Methods for FileContentService & DownloadService
    File findByCodeLocation(CodeLocation codeLocation);

    @Query("select f from File f where f.project = :project and f.fileName = :fileName")
    List<File> findCandidates(@Param("project") Project project, @Param("fileName") String fileName);

    // Methods related to the file tree search mechanism
    // TODO
    long countAllByFileNameContainingIgnoreCase(String fileName);


    @Query("""
            select f.id from file f
            where f.project = :project 
            and lower(f.fileName) like lower(concat('%', :term, '%'))
            order by f.relativePath asc
            """)
    List<UUID> searchIdsByFileName(@Param("term") String fileName, @Param("project") Project project, Pageable pageable);

    @Query("select f.id from File f " +
            "where f.project = :project " +
            "and lower(f.relativePath) like lower(concat('%', :term, '%')) " +
            "order by f.relativePath asc")
    List<UUID> searchIdsByPath(@Param("project") Project project,
                               @Param("term") String term,
                               Pageable pageable);
    // Method for searching for full/relative pathes (e.g. a/b.txt)

}
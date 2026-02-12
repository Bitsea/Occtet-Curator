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

import eu.occtet.boc.util.FileConstants;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;



public interface FileRepository extends JmixDataRepository<File, Long> {
    final static String DEPENDENCY_FOLDER_NAME = FileConstants.DEPENDENCIES_FOLDER_NAME;
    List<File> findByProject(Project project);

    // Root Nodes (Reviewed Filter)
    @Query("select f from File f " +
            "where f.project = :project and f.parent is null " +
            "and (:targetStatus is null or f.reviewed = :targetStatus) " +
            "and f.physicalPath is not null and f.physicalPath <> '' " +
            "order by " +
            "   CASE WHEN lower(f.fileName) = '" + DEPENDENCY_FOLDER_NAME + "' THEN 1 ELSE 0 END ASC, " +
            "   f.isDirectory desc, " +
            "   f.fileName asc")
    List<File> findRootsSorted(@Param("project") Project project,
                               @Param("targetStatus") Boolean targetStatus,
                               Pageable pageable);

    @Query("select count(f) from File f " +
            "where f.project = :project and f.parent is null " +
            "and (:targetStatus is null or f.reviewed = :targetStatus)"+
            "and f.physicalPath is not null and f.physicalPath <> '' "
    )
    long countRoots(@Param("project") Project project,
                    @Param("targetStatus") Boolean targetStatus);

    // Child Nodes (Reviewed Filter)
    @Query("select f from File f " +
            "where f.parent = :parent " +
            "and (:targetStatus is null or f.reviewed = :targetStatus) " +
            "and f.physicalPath is not null and f.physicalPath <> '' " +
            "order by " +
            "   CASE WHEN lower(f.fileName) = '" + DEPENDENCY_FOLDER_NAME + "' THEN 1 ELSE 0 END ASC, " +
            "   f.isDirectory desc, " +
            "   f.fileName asc")
    List<File> findChildrenSorted(@Param("parent") File parent,
                                  @Param("targetStatus") Boolean targetStatus,
                                  Pageable pageable);

    @Query("select count(f) from File f " +
            "where f.parent = :parent " +
            "and (:targetStatus is null or f.reviewed = :targetStatus)"+
            "and f.physicalPath is not null and f.physicalPath <> '' ")
    long countChildren(@Param("parent") File parent,
                       @Param("targetStatus") Boolean targetStatus);


    @Query("select f from File f where f.project = :project and f.fileName = :fileName")
    List<File> findCandidates(@Param("project") Project project, @Param("fileName") String fileName);

    @Query("select f from File f " +
            "left join fetch f.parent " +
            "where f.project = :project " +
            "and lower(f.fileName) like lower(concat('%', :searchText, '%')) " +
            "and f.physicalPath is not null and f.physicalPath <> '' " +
            "and (:targetStatus is null or f.reviewed = :targetStatus)")
    List<File> findCandidates(@Param("project") Project project,
                              @Param("searchText") String searchText,
                              @Param("targetStatus") Boolean targetStatus);

    @Query("select count(f) from File f " +
            "where f.project = :project " +
            "and lower(f.fileName) like lower(concat('%', :searchText, '%')) " +
            "and f.physicalPath is not null and f.physicalPath <> '' " +
            "and (:targetStatus is null or f.reviewed = :targetStatus)")
    long countCandidates(@Param("project") Project project,
                         @Param("searchText") String searchText,
                         @Param("targetStatus") Boolean targetStatus);

}
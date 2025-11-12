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

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.repository.JmixDataRepository;
import io.jmix.core.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodeLocationRepository  extends JmixDataRepository<CodeLocation, UUID> {
    @Query("SELECT c " + "FROM CodeLocation c " + "WHERE :filePath LIKE CONCAT('%', c.filePath) " + "AND c.inventoryItem.project = :project")
    Optional<CodeLocation> findByFilePathEndingWith(@Param("filePath") String filePath, @Param("project") Project project);

    List<CodeLocation> findByInventoryItem_Project(Project project);
}

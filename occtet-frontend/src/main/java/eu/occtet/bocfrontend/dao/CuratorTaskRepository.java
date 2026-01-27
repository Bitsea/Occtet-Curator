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

import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.TaskStatus;
import io.jmix.core.repository.JmixDataRepository;

import java.time.LocalDateTime;
import java.util.List;


public interface CuratorTaskRepository extends JmixDataRepository<CuratorTask, Long> {

    List<CuratorTask> findByStatus(TaskStatus status);
    long countByStatus(TaskStatus status);
    List<CuratorTask> findByTaskType(String type);
    List<CuratorTask> findAllByStatusAndLastUpdateAfter(TaskStatus status, LocalDateTime threshold);
    List<CuratorTask> findAllByLastUpdateAfter(LocalDateTime threshold);
}

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

package eu.occtet.bocfrontend.dao;

import eu.occtet.bocfrontend.entity.ScannerInitializer;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ScannerInitializerRepository extends JmixDataRepository<ScannerInitializer, UUID> {

    List<ScannerInitializer> findByStatus(String status);
    long countByStatus(String status);
    Optional<ScannerInitializer>  findById(UUID id);
}

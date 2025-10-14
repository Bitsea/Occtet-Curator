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

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Copyright;
import io.jmix.core.repository.JmixDataRepository;

import java.util.List;
import java.util.UUID;


public interface CopyrightRepository extends JmixDataRepository<Copyright, UUID> {

   List<Copyright> findAll();
   List<Copyright> findByCopyrightText(String copyrightText);
   List<Copyright> findCopyrightsByCurated(Boolean curated);
   List<Copyright> findCopyrightsByGarbage(Boolean garbage);
   List<Copyright> findCopyrightsByCodeLocation(CodeLocation codeLocation);
}
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

package eu.occtet.boc.spdx.service;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.dao.CodeLocationRepository;
import eu.occtet.boc.spdx.factory.CodeLocationFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CodeLocationService {

    private static final Logger log = LogManager.getLogger(CodeLocationService.class);


    @Autowired
    private CodeLocationFactory codeLocationFactory;

    @Autowired
    private CodeLocationRepository codeLocationRepository;

    public CodeLocation findOrCreateCodeLocationWithInventory(String filePath, InventoryItem inventoryItem) {
        return codeLocationFactory.createWithInventory(filePath, inventoryItem);
    }

    public CodeLocation update(CodeLocation codeLocation){
        return codeLocationRepository.save(codeLocation);
    }

    /**
     * Finds or creates a batch of {@link CodeLocation} objects based on the provided file paths and associated
     * {@link InventoryItem}. If a code location corresponding to a file path already exists in the database for
     * the given inventory item, it is reused. Otherwise, new code locations are created and persisted.
     *
     * @param filePaths a list of file paths for which code locations need to be found or created
     * @param inventoryItem the inventory item associated with the code locations
     * @return a map where the keys are the file paths and the values are the corresponding {@link CodeLocation} objects
     */
    @Transactional
    public Map<String, CodeLocation> findOrCreateBatch(List<String> filePaths, InventoryItem inventoryItem) {

        List<CodeLocation> existingList = codeLocationRepository.findByInventoryItem(inventoryItem);

        Map<String, CodeLocation> cache = existingList.stream()
                .collect(Collectors.toMap(
                        CodeLocation::getFilePath,
                        Function.identity(),
                        (p1, p2) -> p1
                ));
        List<CodeLocation> toSave = new ArrayList<>();

        Set<String> uniquePaths = new HashSet<>(filePaths);

        for (String path : uniquePaths) {
            if (!cache.containsKey(path)) {
                CodeLocation newLoc = new CodeLocation(inventoryItem, path);
                toSave.add(newLoc);
                cache.put(path, newLoc);
            }
        }

        if (!toSave.isEmpty()) {
            codeLocationRepository.saveAll(toSave);
        }
        return cache;
    }
}

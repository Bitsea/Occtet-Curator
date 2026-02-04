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


import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.spdx.factory.FileFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LogManager.getLogger(FileService.class);


    @Autowired
    private FileRepository fileRepository;


    /**
     * Finds or creates a batch of {@link File} objects based on the provided file paths and associated
     * {@link InventoryItem}. If a code location corresponding to a file path already exists in the database for
     * the given inventory item, it is reused. Otherwise, new code locations are created and persisted.
     *
     * @param filePaths a list of file paths for which code locations need to be found or created
     * @param inventoryItem the inventory item associated with the code locations
     * @return a map where the keys are the file paths and the values are the corresponding {@link File} objects
     */
    @Transactional
    public Map<String, File> findOrCreateBatch(List<String> filePaths, InventoryItem inventoryItem) {

        List<File> existingList = fileRepository.findByInventoryItem(inventoryItem);

        Map<String, File> cache = existingList.stream()
                .collect(Collectors.toMap(
                        File::getProjectPath,
                        Function.identity(),
                        (p1, p2) -> p1
                ));
        List<File> toSave = new ArrayList<>();

        Set<String> uniquePaths = new HashSet<>(filePaths);

        for (String path : uniquePaths) {
            if (!cache.containsKey(path)) {
                int p=path.lastIndexOf(java.io.File.pathSeparator);
                String name =path.substring(p+1);
                File newLoc = new File(inventoryItem, path, name, inventoryItem.getProject());
                toSave.add(newLoc);
                cache.put(path, newLoc);
            }
        }

        if (!toSave.isEmpty()) {
            fileRepository.saveAll(toSave);
        }
        return cache;
    }
}

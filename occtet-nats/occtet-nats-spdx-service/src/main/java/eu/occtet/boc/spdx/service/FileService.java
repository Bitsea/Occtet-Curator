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
    @Autowired
    private FileFactory filefactory;

    /**
     * Resolves existing file entities or instantiates new ones to persist the structural file tree.
     * Binds the original SPDX identifier to the domain entity to prevent metadata loss during downstream exports.
     *
     * @param fileToSpdxIdMap A mapping of the artifact path to its assigned SPDX document identifier.
     * @param inventoryItem   The audited inventory node grouping these files.
     * @return A map correlating the artifact path to the fully persisted file entity.
     */
    @Transactional
    public Map<String, File> findOrCreateBatch(Map<String, String> fileToSpdxIdMap, InventoryItem inventoryItem) {
        log.debug("Create Batch of File entities for InventoryItem id={} with {} paths",
                inventoryItem.getId(), fileToSpdxIdMap.size());

        List<File> toSave = new ArrayList<>();
        Map<String, File> cache = new HashMap<>();

        List<File> existingFiles = fileRepository.findAllByProject(inventoryItem.getProject());
        Map<String, File> existingMap = existingFiles.stream()
                .filter(f -> f.getArtifactPath() != null)
                .collect(Collectors.toMap(File::getArtifactPath, f -> f, (f1, f2) -> f1));

        for (Map.Entry<String, String> entry : fileToSpdxIdMap.entrySet()) {
            String path = entry.getKey();
            String spdxId = entry.getValue();

            File fileEntity = existingMap.get(path);

            if (fileEntity != null) {
                log.debug("Linking existing File entity {} to InventoryItem {}", path, inventoryItem.getInventoryName());
                fileEntity.addInventoryItem(inventoryItem);

                if (fileEntity.getDocumentId() == null) {
                    fileEntity.setDocumentId(spdxId);
                }

                toSave.add(fileEntity);
                cache.put(path, fileEntity);
            } else {
                int p = path.lastIndexOf("/");
                if (p == -1) {
                    p = path.lastIndexOf("\\");
                }
                String name = path.substring(p + 1);

                log.debug("Creating new File entity for path {} with name {} for InventoryItem id={}",
                        path, name, inventoryItem.getInventoryName());

                File newLoc = filefactory.create(path, name, inventoryItem.getProject(), inventoryItem);
                newLoc.setDocumentId(spdxId);
                toSave.add(newLoc);
                cache.put(path, newLoc);
            }
        }

        if (!toSave.isEmpty()) {
            fileRepository.saveAll(toSave);
            fileRepository.flush();
        }
        return cache;
    }
}

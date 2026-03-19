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

package eu.occtet.boc.fossreport.service;


import eu.occtet.boc.dao.CopyrightRepository;
import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.fossreport.factory.FileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);


    @Autowired
    private FileFactory fileFactory;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private CopyrightRepository copyrightRepository;


    public File findOrCreateFileWithInventory(String filePath, InventoryItem inventoryItem, Project project) {
        return fileFactory.createWithInventory(filePath, inventoryItem, project);
    }

    public Set<File> createFilesWithInventory(List<String> filePaths, InventoryItem inventoryItem) {
        Set<File> files= new HashSet<>();
        filePaths.forEach(filePath -> files.add(fileFactory.createWithInventory(filePath, inventoryItem, inventoryItem.getProject())));
        return files;
    }

    public void deleteOldFilesOfInventoryItem(InventoryItem inventoryItem, File basePathFile){
        List<File> toBeDeletedCls = fileRepository.findByInventoryItemsContaining(inventoryItem);
        if (toBeDeletedCls.isEmpty()) return;

        toBeDeletedCls.remove(basePathFile);

        for (File f : toBeDeletedCls) {
            List<Copyright> copyrights = copyrightRepository.findByFilesIn(List.of(f));
            for (Copyright c : copyrights) {
                c.getFiles().remove(f);
                log.debug("File {} has been removed from copyright {}", f.getProjectPath(), c.getCopyrightText());
            }
            copyrightRepository.saveAll(copyrights);
            copyrightRepository.flush();
        }
        fileRepository.deleteAll(toBeDeletedCls);
        fileRepository.flush();
        log.debug("Deleted {} old files of inventory item: {}", toBeDeletedCls.size(),
                inventoryItem.getInventoryName());
    }
}

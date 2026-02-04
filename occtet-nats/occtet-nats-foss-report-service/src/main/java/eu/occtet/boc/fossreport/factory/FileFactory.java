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

package eu.occtet.boc.fossreport.factory;


import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileFactory {

    private static final Logger log = LoggerFactory.getLogger(FileFactory.class);

    @Autowired
    private FileRepository fileRepository;

    public File createWithInventory(String filePath, InventoryItem inventoryItem, Project project) {
        log.debug("Creating Files with filePath: {}, with inventory item: {}", filePath, inventoryItem);
        return fileRepository.save(new File(inventoryItem, filePath, project));
    }
}

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

package eu.occtet.boc.fossreport.service;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.fossreport.dao.CodeLocationRepository;
import eu.occtet.boc.fossreport.dao.CopyrightRepository;
import eu.occtet.boc.fossreport.dao.InventoryItemRepository;
import eu.occtet.boc.fossreport.factory.CodeLocationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeLocationService {

    private static final Logger log = LoggerFactory.getLogger(CodeLocationService.class);


    @Autowired
    private CodeLocationFactory codeLocationFactory;

    @Autowired
    private CodeLocationRepository codeLocationRepository;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    public CodeLocation findOrCreateCodeLocationWithInventory(String filePath, InventoryItem inventoryItem) {
        return codeLocationFactory.createWithInventory(filePath, inventoryItem);
    }

    public void CreateCodeLocationsWithInventory(List<String> filePaths, InventoryItem inventoryItem) {
        filePaths.forEach(filePath -> codeLocationFactory.createWithInventory(filePath, inventoryItem));
    }

    public void deleteOldCodeLocationsOfInventoryItem(InventoryItem inventoryItem, CodeLocation basePathCodeLocation){
        List<CodeLocation> toBeDeletedCls = codeLocationRepository.findByInventoryItem(inventoryItem);
        if (toBeDeletedCls.isEmpty()) return;

        toBeDeletedCls.remove(basePathCodeLocation);

        for (CodeLocation cl : toBeDeletedCls) {
            List<Copyright> copyrights = copyrightRepository.findByCodeLocationsIn(List.of(cl));
            for (Copyright c : copyrights) {
                c.getCodeLocations().remove(cl);
                log.debug("CodeLocation {} has been removed from copyright {}", cl.getFilePath(), c.getCopyrightText());
            }
            copyrightRepository.saveAll(copyrights);
            copyrightRepository.flush();
        }
        codeLocationRepository.deleteAll(toBeDeletedCls);
        codeLocationRepository.flush();
        log.debug("Deleted {} old code locations of inventory item: {}", toBeDeletedCls.size(),
                inventoryItem.getInventoryName());
    }
}

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

package eu.occtet.boc.licenseMatcher.service;

import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.licenseMatcher.dao.CodeLocationRepository;
import eu.occtet.boc.licenseMatcher.factory.CodeLocationFactory;
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


    public CodeLocation findOrCreateCodeLocation(String filePath) {
        List<CodeLocation> codeLocation = codeLocationRepository.findByFilePath(filePath);

        if (!codeLocation.isEmpty() && codeLocation.getFirst() != null) {
            log.debug("Found existing CodeLocation for filePath: {}", filePath);
            return codeLocation.getFirst();
        } else {
            return codeLocationFactory.create(filePath);
        }
    }

    public CodeLocation findOrCreateCodeLocationByFileName(String filePath) {
        List<CodeLocation> codeLocation = codeLocationRepository.findByFilePath(filePath);

        if (!codeLocation.isEmpty() && codeLocation.getFirst() != null) {
            return codeLocationRepository.save(codeLocation.getFirst());
        } else {
            return codeLocationFactory.create(filePath);
        }
    }

    public CodeLocation createCodeLocationWithLineNumber(String filePath,
                                                         Integer lineNumberOne, Integer lineNumberTwo
    ) {
        return codeLocationFactory.createCodeLocationWithLineNumbers(filePath, lineNumberOne,
                lineNumberTwo);
    }

    public CodeLocation createCodeLocation(String filePath
    ) {
        return codeLocationFactory.create(filePath);
    }

    public void update(CodeLocation codeLocation){
        codeLocationRepository.save(codeLocation);
    }

    public CodeLocation findOrCreateCodeLocationWithInventory(String filePath, InventoryItem inventoryItem) {
        return codeLocationFactory.createWithInventory(filePath, inventoryItem);
    }
}

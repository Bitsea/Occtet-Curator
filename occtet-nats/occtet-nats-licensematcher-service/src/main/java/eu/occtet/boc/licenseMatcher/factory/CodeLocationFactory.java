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

package eu.occtet.boc.licenseMatcher.factory;


import eu.occtet.boc.entity.CodeLocation;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.licenseMatcher.dao.CodeLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CodeLocationFactory {

    private static final Logger log = LoggerFactory.getLogger(CodeLocationFactory.class);


    @Autowired
    private CodeLocationRepository codeLocationRepository;

    public CodeLocation createCodeLocationWithLineNumbers(String filePath,
                               Integer lineNumberOne, Integer lineNumberTwo
    ) {
         return codeLocationRepository.save(new CodeLocation(filePath,  lineNumberOne,
                 lineNumberTwo));
    }

    public CodeLocation create(String filePath) {
        log.debug("Creating CodeLocation with filePath: {}", filePath);
        return codeLocationRepository.save(new CodeLocation(filePath));
    }


    public CodeLocation createWithInventory(String filePath, InventoryItem originInventoryItem) {
        log.debug("Creating CodeLocation with filePath: {}", filePath);
        return codeLocationRepository.save(new CodeLocation( originInventoryItem, filePath));
    }
}

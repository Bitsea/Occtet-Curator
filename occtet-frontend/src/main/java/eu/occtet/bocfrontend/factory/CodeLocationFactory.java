/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.InventoryItem;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class CodeLocationFactory {

    @Autowired
    private DataManager dataManager;

    public CodeLocation create(@Nonnull InventoryItem inventoryItem, @Nonnull String filePath,
                               @Nonnull Integer lineNumber,
                               @Nonnull Integer lineNumberTo) {
        CodeLocation codeLocation = dataManager.create(CodeLocation.class);
        codeLocation.setInventoryItem(inventoryItem);
        codeLocation.setFilePath(filePath);
        codeLocation.setLineNumber(lineNumber);
        codeLocation.setLineNumberTo(lineNumberTo);
        return dataManager.save(codeLocation);
    }

    public CodeLocation create(@Nonnull InventoryItem inventoryItem, @Nonnull String filePath){
        return create(inventoryItem, filePath, 0, 0);
    }
}

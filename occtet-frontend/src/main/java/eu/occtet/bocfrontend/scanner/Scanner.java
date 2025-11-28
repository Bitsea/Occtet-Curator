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

package eu.occtet.bocfrontend.scanner;

import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class Scanner {


    private final String name;


    protected Scanner(String name) {
        this.name = name;
    }

    /**
     *
     * @return the name of this scanner
     */
    public String getName() {
        return name;
    }

    /**
     * Process the given scanning task. This may happen in background.
     * @param scannerInitializer the scanning task to be processed
     * @param completionCallback callback to be called when the processing is done. The callback receives the same ScannerInitializer instance as given to this method.
     ** @return true on success, false if something went wrong.
     */
    public abstract boolean processTask(@Nonnull ScannerInitializer scannerInitializer, @NotNull Consumer<ScannerInitializer> completionCallback);

    /**
     *
     * @return list of supported settings for this scanner
     */
    public List<String> getSupportedConfigurationKeys() {return Collections.emptyList();
    }

    /**
     *
     * @return list of required settings for this scanner
     */
    public List<String> getRequiredConfigurationKeys() {
        return Collections.emptyList();
    }

    public boolean isConfigurationRequired(String key) {
        return getRequiredConfigurationKeys().contains(key);
    }

    public String getDefaultConfigurationValue(String k) {
        return "";
    };

    public Configuration.Type getTypeOfConfiguration(String key) {
        return Configuration.Type.STRING;
    }


}

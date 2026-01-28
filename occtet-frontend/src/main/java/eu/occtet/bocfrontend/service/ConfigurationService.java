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

package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.factory.ConfigurationFactory;
import eu.occtet.bocfrontend.importer.ImportManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {

    private static final Logger log = LogManager.getLogger(ConfigurationService.class);

    @Autowired
    private ConfigurationFactory configurationFactory;
    @Autowired
    private ImportManager importManager;

    public Configuration create(String name, String value){
        return configurationFactory.create(name, value);
    }

    public Configuration.Type getTypeOfConfiguration(String key, CuratorTask curatorTask) {
        return importManager.findImportByName(curatorTask.getTaskType()).getTypeOfConfiguration(key);
    }

    /**
     * Processes a given configuration object by applying the needed handler functions to it.
     * True is returned if the configuration was processed successfully, false otherwise.
     *
     * @param config the configuration object to be processed by the chain of handler functions
     */
    public boolean handleConfig(
            Configuration config,
            String nameField,
            byte[] uploadFieldValue,
            String uploadFileName,
            Boolean booleanField,
            CuratorTask curatorTask
    ) {
        if (getTypeOfConfiguration(nameField, curatorTask) == Configuration.Type.FILE_UPLOAD) {
            return handleFileUploadConfig(config, nameField, uploadFieldValue, uploadFileName);
        } else if (getTypeOfConfiguration(nameField, curatorTask) == Configuration.Type.BOOLEAN) {
            return handleValueOnly(config, nameField, booleanField.toString());
        } else {
            return handleValueOnly(config, nameField, config.getValue());
        }
    }


    private Boolean handleFileUploadConfig(
            Configuration config,
            String nameField,
            byte[] uploadFieldValue,
            String uploadFileName
    ) {
        log.debug("handle file upload config called with parameters nameField: {} and uploadField length: {}",
                nameField,
                uploadFieldValue.length);

        if (uploadFieldValue.length == 0) {
            log.error("upload invalid");
            return false;
        }

        config.setValue(uploadFileName);
        config.setUpload(uploadFieldValue);
        return true;
    }

    private Boolean handleValueOnly(
            Configuration config,
            String nameField,
            String valueField
    ) {
        log.debug("handle value only called with parameters nameField: {} and valueField: {}", nameField, valueField);
        config.setValue(valueField);
        return true;
    }
}

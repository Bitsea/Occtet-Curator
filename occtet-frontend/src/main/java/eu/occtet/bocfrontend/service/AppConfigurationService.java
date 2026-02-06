/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.service;

import eu.occtet.bocfrontend.entity.appconfigurations.AppConfigKey;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfiguration;
import io.jmix.core.DataManager;
import io.jmix.core.querycondition.PropertyCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AppConfigurationService {

    @Autowired
    private DataManager dataManager;

    public Optional<AppConfiguration> findByConfigKey(AppConfigKey configKey) {
        return dataManager.load(AppConfiguration.class)
                .condition(PropertyCondition.equal("configKey", configKey))
                .optional();
    }
}

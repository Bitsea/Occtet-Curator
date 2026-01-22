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

package eu.occtet.bocfrontend.config;

import eu.occtet.bocfrontend.dao.AppConfigurationRepository;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfigKey;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfiguration;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.security.SystemAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SystemConfigInitializer {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigInitializer.class);

    @Autowired
    private AppConfigurationRepository repository;
    @Autowired
    private UnconstrainedDataManager dataManager;

    @Autowired
    private SystemAuthenticator systemAuthenticator;

    @EventListener
    public void init(ContextRefreshedEvent event){
        log.info("Initializing missing system configurations...");
        systemAuthenticator.runWithSystem(() -> {
            Set<AppConfigKey> existingKeys = repository.findAll()
                    .stream()
                    .map(AppConfiguration::getConfigKey)
                    .collect(Collectors.toSet());
            for (AppConfigKey key : AppConfigKey.values()) {
                if (!existingKeys.contains(key)) {
                    log.info("Creating missing config: {}", key.getId());

                    AppConfiguration config = dataManager.create(AppConfiguration.class);
                    config.setConfigKey(key);
                    config.setValue(key.getDefaultValue());
                    config.setDataType(key.getType());
                    config.setDescription("Auto-generated config");

                    dataManager.save(config);
                    log.info("Created config: {}", key.getId());
                }
            }
        });
    }
}

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

package eu.occtet.bocfrontend.entity.settings.configurations;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

import java.util.UUID;

public class SettingConfiguration {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Column(name = "CONFIG_KEY", nullable = false, unique = true)
    private ConfigKey configKey;

    @Column(name = "VALUE", columnDefinition = "TEXT")
    private String value;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DATA_TYPE")
    private ConfigType dataType;

    // TODO last update

    public SettingConfiguration() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ConfigKey getConfigKey() {
        return configKey;
    }

    public void setConfigKey(ConfigKey configKey) {
        this.configKey = configKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConfigType getDataType() {
        return dataType;
    }

    public void setDataType(ConfigType dataType) {
        this.dataType = dataType;
    }
}

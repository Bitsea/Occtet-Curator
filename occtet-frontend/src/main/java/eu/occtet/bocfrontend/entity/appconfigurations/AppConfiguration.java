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

package eu.occtet.bocfrontend.entity.appconfigurations;

import eu.occtet.bocfrontend.converter.AppConfigKeyConverter;
import eu.occtet.bocfrontend.converter.AppConfigTypeConverter;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;

@JmixEntity
@Table(name = "APP_CONFIGURATION", uniqueConstraints = {
        @UniqueConstraint(name = "IDX_APP_CONFIG_UNQ_KEY", columnNames = {"CONFIG_KEY"})
})
@Entity
public class AppConfiguration {

    @JmixGeneratedValue
    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "CONFIG_KEY", nullable = false, unique = true, columnDefinition = "TEXT")
    @Convert(converter = AppConfigKeyConverter.class)
    private AppConfigKey configKey;

    @Column(name = "DATA_TYPE", columnDefinition = "TEXT")
    @Convert(converter = AppConfigTypeConverter.class)
    private AppConfigType dataType;

    @Column(name = "CONFIG_VALUE", columnDefinition = "TEXT")
    private String value;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;


    public AppConfiguration() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppConfigKey getConfigKey() {
        return configKey;
    }

    public void setConfigKey(AppConfigKey configKey) {
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

    public AppConfigType getDataType() {
        return dataType;
    }

    public void setDataType(AppConfigType dataType) {
        this.dataType = dataType;
    }
}
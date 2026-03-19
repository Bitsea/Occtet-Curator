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

import jakarta.annotation.Nullable;

public enum AppConfigKey implements EnumClass<String>{

    GENERAL_BASE_PATH(
            AppConfigGroup.GENERAL + ".base_path",
            "",
            AppConfigType.STRING,
            "The base path to which project files will be downloaded."
    ),
    ;


    private final String id;
    private final String defaultValue;
    private final AppConfigType type;
    private final String description;

    AppConfigKey(String id, String defaultValue, AppConfigType type, String description) {
        this.id = id;
        this.defaultValue = defaultValue;
        this.type = type;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public AppConfigType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    @Nullable
    public static AppConfigKey fromId(String id) {
        for (AppConfigKey at : AppConfigKey.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }

    /**
     * Acts as a prefix for the config key
     */
    public static class AppConfigGroup {
        public static final String GENERAL = "general";
        public static final String ORT = "ort";
        public static final String OPENSEARCH = "opensearch";
    }
}

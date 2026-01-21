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

import io.jmix.core.metamodel.datatype.EnumClass;
import jakarta.annotation.Nullable;

public enum AppConfigKey implements EnumClass<String> {
    // DB Value: "general.base_path"
    GENERAL_BASE_PATH(AppConfigGroup.GENERAL + ".base_path"),

    // Examples: (DELETE after)
    // DB Value: "ort.server_url"
//    ORT_SERVER_URL(AppConfigGroup.ORT + ".server_url"),

    // DB Value: "opensearch.host"
//    OPENSEARCH_HOST(AppConfigGroup.OPENSEARCH + ".host"),

    // DB Value: "opensearch.port"
//    OPENSEARCH_PORT(AppConfigGroup.OPENSEARCH + ".port"),

    ;
    private final String id;

    AppConfigKey(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
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
}

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

public enum AppConfigType implements EnumClass<String> {
    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    JSON_LIST("json_list");

    private final String id;

    AppConfigType(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static AppConfigType fromId(String id) {
        for (AppConfigType at : AppConfigType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}

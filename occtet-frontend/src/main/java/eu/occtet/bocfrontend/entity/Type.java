
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

package eu.occtet.bocfrontend.entity;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum Type implements EnumClass<String> {

    STRING("STRING"),NUMERIC("NUMERIC"),FILE_UPLOAD("FILE_UPLOAD"),BASE_PATH("BASE_PATH"),
    COMMA_SEPARATED_STRING("COMMA_SEPARATED_STRINGS"),BOOLEAN("BOOLEAN");

    private final String id;

    Type(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static Type fromId(String id) {
        for (Type at : Type.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}
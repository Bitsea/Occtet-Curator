
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

import org.springframework.lang.Nullable;


public enum ImportStatus {
    CREATING("CREATING"),APPROVE("APPROVE"),IN_PROGRESS("IN_PROGRESS"),WAITING("WAITING"), STOPPED("STOPPED"), COMPLETED("COMPLETED");

    private final String id;

    ImportStatus(String value) {
        this.id = value;
    }


    public String getId() {
        return id;
    }

    @Nullable
    public static ImportStatus fromId(String id) {
        for (ImportStatus at : ImportStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}



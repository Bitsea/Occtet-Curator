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

package eu.occtet.bocfrontend.model;

public enum FileReviewedFilterMode {
    SHOW_ALL,
    REVIEWED_ONLY,
    NOT_REVIEWED_ONLY;

    /**
     * Converts the enum to a nullable boolean for database queries.
     * @return true for reviewed, false for not reviewed, null for all.
     */
    public Boolean asBoolean() {
        return switch (this) {
            case REVIEWED_ONLY -> true;
            case NOT_REVIEWED_ONLY -> false;
            case SHOW_ALL -> null;
        };
    }
}
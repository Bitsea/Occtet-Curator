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

package eu.occtet.bocfrontend.service;

import org.springframework.stereotype.Service;

@Service
public class Utilities {

    public String handleCasing(String arg) {
        if (arg == null || arg.isEmpty()) {
            return "";
        }
        String separated = arg.replaceAll("([a-z])([A-Z])", "$1 $2");
        String firstLetter = separated.substring(0, 1).toUpperCase();
        return firstLetter + separated.substring(1);
    }
}

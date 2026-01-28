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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Converts a text to list using breakers for seperation
     */
    public String convertListToText(List<String> list, String breaker) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(breaker, list);
    }

    /**
     * Converts a list to text using breakers for seperation
     */
    public List<String> convertTextToList(String text, String breaker) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(text.split(breaker))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}

/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converts a list of strings to a single string, with each element separated by a newline.
 * Used for storing lists of strings in database columns.
 */
@Converter(autoApply = true)
public class ListStringConverter implements AttributeConverter<List<String>,String> {

    private static final String SEPARATOR = "\n";

    /**
     * Converts a list of strings to a single string, with each element separated by a newline.
     *
     * @return a single string representing the joined and trimmed elements of the list,
     *         or an empty string if the input list is null or empty
     */
    @Override
    public String convertToDatabaseColumn(List<String> attribute){
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(String::trim)
                .collect(Collectors.joining(SEPARATOR));
    }

    /**
     * Converts a single string to a list of strings, by splitting the string at the newline character.
     *
     * @param dbData  the data from the database column to be
     *                converted
     * @return empty list if the input string is null or blank,
     * or a list of strings with each element trimmed from the input string.
     */
    @Override
    public List<String> convertToEntityAttribute(String dbData){
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(dbData.split(SEPARATOR)));
    }
}


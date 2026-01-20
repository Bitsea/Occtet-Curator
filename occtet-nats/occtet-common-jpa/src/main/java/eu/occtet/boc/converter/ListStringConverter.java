package eu.occtet.boc.converter;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
public class ListStringConverter {
    private static final String DELIMITER = "|";

    /**
     *
     * @param s
     * @return List of strings (empty list if input is null)
     */
    @NonNull
    public static List<String> nullableStringToList(@Nullable  String s) {
        return (s==null) ? Collections.emptyList() : Arrays.asList(StringUtils.split(s, DELIMITER));
    }

    /**
     *
     * @param s
     * @return String representation of the list or null if the list is null (empty string if list is empty)
     */
    public static String toStringOrNull(@Nullable  List<String> s) {
        if(s==null) return null;
        return StringUtils.collectionToDelimitedString(s, DELIMITER);
    }
}

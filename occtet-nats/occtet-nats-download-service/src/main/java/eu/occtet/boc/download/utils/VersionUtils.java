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

package eu.occtet.boc.download.utils;

import org.springframework.stereotype.Service;

@Service
public class VersionUtils {

    public boolean isMatch(String tagName, String version) {
        if (tagName == null || version == null) return false;
        if (tagName.equalsIgnoreCase(version)) return true;

        String normalizedTag = normalizeVersion(tagName);
        String normalizedVersion = normalizeVersion(version);

        if (normalizedTag.equals(normalizedVersion)) return true;
        if (isPrefixWithBoundary(normalizedTag, normalizedVersion)) return true;
        return isPrefixWithBoundary(normalizedVersion, normalizedTag);
    }

    private String normalizeVersion(String input) {
        String i = input.toLowerCase();
        i = i.replaceAll("^[^0-9]+", "");
        // Treat underscore, hyphen, and plus as dots
        i = i.replaceAll("[_\\-+]", ".");
        // Insert dots at letter/digit boundaries
        i = i.replaceAll("(?<=\\d)(?=[a-zA-Z])", ".");
        i = i.replaceAll("(?<=[a-zA-Z])(?=\\d)", ".");
        return i;
    }

    private boolean isPrefixWithBoundary(String text, String prefix) {
        if (!text.startsWith(prefix)) return false;
        if (text.length() == prefix.length()) return true;
        char nextChar = text.charAt(prefix.length());
        return !Character.isDigit(nextChar);
    }
}

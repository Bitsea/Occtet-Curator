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

package eu.occtet.boc.fossreport.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PathUtilitiesTest {

    @Test
    void testCleanAndSplit() {
        String input = "react-dom/LICENSE_x000D_\n react-dom/README.md\n react-dom/package.json\n" +
                "chromium/third_party/android_protobuf/OWNERS_x000D_";

        List<String> expected = List.of(
                "react-dom/LICENSE",
                "react-dom/README.md",
                "react-dom/package.json",
                "chromium/third_party/android_protobuf/OWNERS"
        );

        List<String> actual = PathUtilities.cleanAndSplits(input);

        assertEquals(expected, actual);
    }
}

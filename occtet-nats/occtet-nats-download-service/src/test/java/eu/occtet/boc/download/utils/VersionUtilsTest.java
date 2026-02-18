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

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration()
class VersionUtilsTest {

    private final VersionUtils versionUtils = new VersionUtils();

    @Test
    void isMatch_ExactMatches() {
        assertTrue(versionUtils.isMatch("1.0.0", "1.0.0"));
        assertTrue(versionUtils.isMatch("v1.0.0", "1.0.0"));
        assertTrue(versionUtils.isMatch("release-2.5", "2.5"));
    }

    @Test
    void isMatch_Normalization() {
        assertTrue(versionUtils.isMatch("1.2.0-rc1", "1.2.0.rc.1"));
        assertTrue(versionUtils.isMatch("1.2.0_rc1", "1.2.0-rc1"));
        assertTrue(versionUtils.isMatch("1.0a1", "1.0.a.1"));
    }

    @Test
    void isMatch_PrefixLogic() {
        assertTrue(versionUtils.isMatch("v1.2.5", "1.2"));
        // Distro specific case: 13.8+deb... matches 13.8
        assertTrue(versionUtils.isMatch("13.8", "13.8+deb13u3"));
    }

    @Test
    void isMatch_BoundariesAndMismatches() {
        // 1.50 != 1.5
        assertFalse(versionUtils.isMatch("1.50", "1.5"));
        assertFalse(versionUtils.isMatch("2.0", "1.0"));
        assertFalse(versionUtils.isMatch(null, "1.0"));
    }
}

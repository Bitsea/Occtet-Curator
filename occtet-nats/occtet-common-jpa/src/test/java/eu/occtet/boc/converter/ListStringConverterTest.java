package eu.occtet.boc.converter;

import org.junit.jupiter.api.Test;

import java.util.Collections;

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

public class ListStringConverterTest {

    @Test
    public void testConvertToDatabaseColumn() {


        String result = ListStringConverter.toStringOrNull (java.util.Arrays.asList("one", "two", "three"));
        assert "one|two|three".equals(result);
        result = ListStringConverter.toStringOrNull(java.util.Arrays.asList("Tool: ort-74.0.0"));
        assert "Tool: ort-74.0.0".equals(result);
        result = ListStringConverter.toStringOrNull(Collections.emptyList());
        assert "".equals(result);
        result = ListStringConverter.toStringOrNull(null);
        assert result==null;


    }
}
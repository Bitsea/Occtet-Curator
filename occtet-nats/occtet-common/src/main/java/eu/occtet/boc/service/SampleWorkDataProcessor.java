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

package eu.occtet.boc.service;

import eu.occtet.boc.model.SampleWorkData;
import eu.occtet.boc.model.ScannerSendWorkData;

public class SampleWorkDataProcessor extends BaseWorkDataProcessor {
    @Override
    public boolean process(SampleWorkData workData) {
        System.out.println("Processing SampleWorkData: " + workData.toString());
        return true;
    }

    @Override
    public boolean process(ScannerSendWorkData workData) {
        System.out.println("Processing ScannerSendWorkData: " + workData.toString());
        return true;
    }
}

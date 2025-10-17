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

import eu.occtet.boc.entity.ScannerInitializer;
import eu.occtet.boc.fossreport.dao.ScannerInitializerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;


@Service
public class ScannerInitializerService {

    @Autowired
    private ScannerInitializerRepository scannerInitializerRepository;

    public void updateScannerFeedback(ScannerInitializer scannerInitializer, String msg){
        if (scannerInitializer == null) return;
        if (scannerInitializer.getFeedback() == null)
            scannerInitializer.setFeedback(new ArrayList<>());

        scannerInitializer.getFeedback().add(msg);
        scannerInitializerRepository.save(scannerInitializer);
    }
}

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

import eu.occtet.bocfrontend.dao.ScannerInitializerRepository;
import eu.occtet.bocfrontend.entity.ScannerInitializer;
import eu.occtet.bocfrontend.entity.ScannerInitializerStatus;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScannerInitializerService {

    @Autowired
    private ScannerInitializerRepository scannerInitializerRepository;

    @Autowired
    private DataManager dataManager;

    public List<ScannerInitializer> getScannerByStatus(ScannerInitializerStatus status){
        return scannerInitializerRepository.findByStatus(status.getId());
    }

    public long countScannerByStatus(ScannerInitializerStatus status){
        return scannerInitializerRepository.countByStatus(status.getId());
    }

    public void updateScannerFeedback(String feedback, ScannerInitializer scannerInitializer){
        if (scannerInitializer.getFeedback() == null) {
            scannerInitializer.setFeedback(new ArrayList<>());
        }
        scannerInitializer.getFeedback().add(feedback);
        dataManager.save(scannerInitializer);
    }

    public void updateScannerStatus(ScannerInitializerStatus status, ScannerInitializer scannerInitializer){
        scannerInitializer.updateStatus(status.getId());
        dataManager.save(scannerInitializer);
    }

}

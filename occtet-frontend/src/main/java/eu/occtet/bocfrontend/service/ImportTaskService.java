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

import eu.occtet.bocfrontend.dao.ImportTaskRepository;
import eu.occtet.bocfrontend.entity.ImportStatus;
import eu.occtet.bocfrontend.entity.ImportTask;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImportTaskService {

    @Autowired
    private ImportTaskRepository importTaskRepository;

    @Autowired
    private DataManager dataManager;

    public List<ImportTask> getImportByStatus(ImportStatus status){
        return importTaskRepository.findByStatus(status.getId());
    }

    public long countImportByStatus(ImportStatus status){
        return importTaskRepository.countByStatus(status.getId());
    }

    public void updateImportFeedback(String feedback, ImportTask importer){
        if (importer.getFeedback() == null) {
            importer.setFeedback(new ArrayList<>());
        }
        importer.getFeedback().add(feedback);
        dataManager.save(importer);
    }

    public void updateImportStatus(ImportStatus status, ImportTask importer){
        importer.updateStatus(status.getId());
        dataManager.save(importer);
    }

    public List<ImportTask> getStoppedImporters(){
        return importTaskRepository.findByStatus(ImportStatus.STOPPED.getId());
    }

}

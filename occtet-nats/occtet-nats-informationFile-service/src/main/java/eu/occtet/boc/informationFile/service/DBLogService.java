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

package eu.occtet.boc.informationFile.service;

import java.util.List;
import java.util.Optional;

import eu.occtet.boc.informationFile.dao.LogRepository;
import eu.occtet.boc.entity.DBLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DBLogService {

    private static final Logger log = LogManager.getLogger(DBLogService.class);

    private final LogRepository logRepository;

    @Autowired
    public DBLogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void saveLog(String component, String message){
        DBLog dbLog = new DBLog(component, message);
        logRepository.save(dbLog);
        log.info("saved {} log to DB with ID: {}", dbLog.getComponent(), dbLog.getId());
    }

    public List<DBLog> getAllLogs() {
        return logRepository.findAll();
    }

    public Optional<DBLog> getLogById(Long id) {
        return logRepository.findById(id);
    }

    public void emptyTable(){
        logRepository.deleteAll();
        log.info("Deleted all logs in the DB");
    }

}
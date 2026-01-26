package eu.occtet.bocfrontend.service;

import eu.occtet.boc.model.ProgressSystemMessage;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
@Service
public class CuratorTaskProgressMonitor {

    @Autowired
    NatsService natsService;

    private static final Logger log = LogManager.getLogger(CuratorTaskProgressMonitor.class);

    private Map<Long,Integer> taskProgressMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        natsService.addProgressListener(this::onProgressMessage);
    }

    public void onProgressMessage(ProgressSystemMessage progressSystemMessage) {
        log.debug("Received progress update for task {}: {}%",progressSystemMessage.getTaskId(),progressSystemMessage.getProgressPercent());
        taskProgressMap.put(progressSystemMessage.getTaskId(), progressSystemMessage.getProgressPercent());
    }

    public int getProgressForTask(long taskId) {
        if(!taskProgressMap.containsKey(taskId)) { return 0;}
        return taskProgressMap.get(taskId);
    }


}

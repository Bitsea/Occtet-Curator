package eu.occtet.bocfrontend.service;

import eu.occtet.boc.model.ProgressSystemMessage;
import eu.occtet.boc.model.WorkTaskProgress;
import eu.occtet.boc.model.WorkTaskStatus;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
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
public class WorkTaskProgressMonitor {

    @Autowired
    NatsService natsService;

    private static final Logger log = LogManager.getLogger(WorkTaskProgressMonitor.class);

    private Map<String, WorkTaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        natsService.addProgressListener(this::onProgressMessage);
    }

    public void onProgressMessage(ProgressSystemMessage progressSystemMessage) {
        log.debug("Received progress update for task {}: {}%",progressSystemMessage.getTaskId(),progressSystemMessage.getProgressPercent());
        taskProgressMap.put(progressSystemMessage.getTaskId(), systemMessageToWorkTaskProgress(progressSystemMessage));
    }

    private WorkTaskProgress systemMessageToWorkTaskProgress(ProgressSystemMessage m) {
        return new WorkTaskProgress(m.getName(), m.getProgressPercent(), m.getStatus(), m.getDetails());
    }

    public List<WorkTaskProgress> getAllProgress() {
        return taskProgressMap.values().stream().toList();
    }

    public WorkTaskProgress getProgressForTask(String taskId) {
        return taskProgressMap.get(taskId);
    }


}

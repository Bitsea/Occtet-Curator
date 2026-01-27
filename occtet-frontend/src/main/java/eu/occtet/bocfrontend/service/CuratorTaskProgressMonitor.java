package eu.occtet.bocfrontend.service;

import eu.occtet.boc.model.ProgressSystemMessage;
import eu.occtet.bocfrontend.dao.CuratorTaskRepository;
import eu.occtet.bocfrontend.entity.TaskStatus;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private NatsService natsService;

    @Autowired
    private CuratorTaskRepository curatorTaskRepository;

    private static final Logger log = LogManager.getLogger(CuratorTaskProgressMonitor.class);

    @PostConstruct
    public void init() {
        natsService.addProgressListener(this::onProgressMessage);
    }

    @Transactional
    public void onProgressMessage(ProgressSystemMessage progressSystemMessage) {
        log.debug("Received progress update for task {}: {}%",progressSystemMessage.getTaskId(),progressSystemMessage.getProgressPercent());
        curatorTaskRepository.findById(progressSystemMessage.getTaskId()).ifPresent(curatorTask -> {
            curatorTask.setCurrentProgress(progressSystemMessage.getProgressPercent());
        });
    }


}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.occtet.boc.model.BaseWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.dao.CuratorTaskRepository;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.TaskStatus;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.reflections.Reflections.log;

@Service
public class CuratorTaskService {

    @Autowired
    private CuratorTaskRepository curatorTaskRepository;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private NatsService natsService;

    /**
     * Save the task and send it to the NATS work queue for processing.
     * @param curatorTask
     * @param workData
     * @param optDetails optional details about the task for information/metadata purposes
     * @return true on success
     */
    public boolean saveAndRunTask(CuratorTask curatorTask, BaseWorkData workData, String optDetails)  {
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        curatorTask.notifyStarted();
        dataManager.save(curatorTask);
        WorkTask workTask = new WorkTask(curatorTask.getId(), optDetails, actualTimestamp, workData);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        String message = null;
        try {
            message = mapper.writeValueAsString(workTask);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize work task to JSON", e);
            curatorTask.setStatus(TaskStatus.CANCELLED);
            return false;
        }
        log.debug("sending message to spdx service: {}", message);
        try {
            natsService.sendWorkMessageToStream("work.spdx", message.getBytes(Charset.defaultCharset()));
        } catch (Exception e) {
            log.error("Could not send work message", e);
            curatorTask.setStatus(TaskStatus.CANCELLED);
            return false;
        }
        return true;

    }

    public List<CuratorTask> getTasksByStatus(TaskStatus status){
        return curatorTaskRepository.findByStatus(status);
    }

    public long countTasksByStatus(TaskStatus status){
        return curatorTaskRepository.countByStatus(status);
    }

    public void updateTaskFeedback(String feedback, CuratorTask task){
        if (task.getFeedback() == null) {
            task.setFeedback(new ArrayList<>());
        }
        task.getFeedback().add(feedback);
        dataManager.save(task);
    }

    public void updateTaskStatus(CuratorTask curatorTask, TaskStatus status){
        curatorTask.setStatus(status);
        dataManager.save(curatorTask);
    }

    public void updateTaskProgress(CuratorTask curatorTask, int progress){
        curatorTask.setCurrentProgress(progress);
        dataManager.save(curatorTask);
    }

    public List<CuratorTask> getCancelledTasks(){
        return curatorTaskRepository.findByStatus(TaskStatus.CANCELLED);
    }

    public void removeOutdatedCompletedTasks(int updatedBeforeHours){
        LocalDateTime before = LocalDateTime.now().minusHours(updatedBeforeHours);
        List<CuratorTask> tasks = curatorTaskRepository.findAllByStatusAndLastUpdateAfter(TaskStatus.COMPLETED, before);
        dataManager.remove(tasks);
    }

    public List<CuratorTask> getCurrentTasks(int updatedBeforeMinutes) {
        LocalDateTime before = LocalDateTime.now().minusMinutes(updatedBeforeMinutes);
        List<CuratorTask> inProgress = curatorTaskRepository.findByStatus(TaskStatus.IN_PROGRESS);
        List<CuratorTask> cancelled = curatorTaskRepository.findAllByStatusAndLastUpdateAfter(TaskStatus.CANCELLED, before);
        List<CuratorTask> completed = curatorTaskRepository.findAllByStatusAndLastUpdateAfter(TaskStatus.COMPLETED, before);
        List<CuratorTask> result = new ArrayList<>();
        result.addAll(inProgress);
        result.addAll(cancelled);
        result.addAll(completed);
        return result;
    }

    public CuratorTask saveWithFeedBack(CuratorTask curatorTask, List<String> feedback, TaskStatus status){
        List<String> newFeedbacks= new ArrayList<>();
        List<String> oldFeedbacks= curatorTask.getFeedback(); // get preexisting feedbacks
        if (oldFeedbacks!=null && !oldFeedbacks.isEmpty()) newFeedbacks.addAll(oldFeedbacks);

        // Add new feedback
        newFeedbacks.addAll(feedback);

        curatorTask.setFeedback(newFeedbacks);
        curatorTask.setStatus(status);
        return dataManager.save(curatorTask);
    }
}

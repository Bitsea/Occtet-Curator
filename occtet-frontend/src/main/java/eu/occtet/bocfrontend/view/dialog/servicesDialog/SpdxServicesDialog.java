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

package eu.occtet.bocfrontend.view.dialog.servicesDialog;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.service.NatsService;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.upload.FileStorageUploadField;
import io.jmix.flowui.component.upload.receiver.FileTemporaryStorageBuffer;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.upload.TemporaryStorage;
import io.jmix.flowui.view.*;
import io.nats.client.api.ObjectInfo;
import io.nats.client.api.ObjectMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;


@ViewController("spdxMicroServiceDialog")
@ViewDescriptor(value = "spdx-microService-dialog.xml")
@DialogMode(width = "1000px", height = "650px")
public class SpdxServicesDialog extends AbstractServicesDialog{

    private static final Logger log = LogManager.getLogger(SpdxServicesDialog.class);


    @ViewComponent
    private ComboBox<Project> projectComboBox;

    @ViewComponent
    private ComboBox<InventoryItem> inventoryItemComboBox;

    @ViewComponent
    private Checkbox useCopyrigthAIBox;

    @ViewComponent
    private Checkbox licenseMatcherBox;

    @Autowired
    private NatsService natsService;

    @Autowired
    private Notifications notifications;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private TemporaryStorage temporaryStorage;

    private SpdxWorkData workData;

    @Subscribe
    protected void onInit(InitEvent event) {

        workData = new SpdxWorkData();
        projectComboBox.setItems(projectRepository.findAll());
        inventoryItemComboBox.setItems(inventoryItemRepository.findAll());
        inventoryItemComboBox.setItemLabelGenerator(InventoryItem::getInventoryName);
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
        useCopyrigthAIBox.setValue(false);
        licenseMatcherBox.setValue(false);
    }

    @Subscribe("projectComboBox")
    public void setProject(final AbstractField.ComponentValueChangeEvent event){
        if(event != null){
            Project project = (Project) event.getValue();
            workData.setProjectId(project.getId().toString());
        }
    }

    @Override
    @Subscribe("sendWorkdataButton")
    public void sendWorkDataButton(ClickEvent<Button> event) {

        if(processworkData()){
            close(StandardOutcome.CLOSE);
        }else{
            notifications.show("Something went wrong, please check your input");
        }
    }

    @Subscribe("jsonSpdxUpload")
    public void uploadSpdxFile(final FileUploadSucceededEvent<FileStorageUploadField> event){

        if(event.getReceiver() instanceof FileTemporaryStorageBuffer storageBuffer){

            UUID fileId = storageBuffer.getFileData().getFileInfo().getId();
            File file = temporaryStorage.getFile(fileId);

            if(file != null){
                ObjectMeta objectMeta = ObjectMeta.builder(file.getName())
                        .description("Spdxdocument for use by spdx-microservice")
                        .chunkSize(32 * 1024)
                        .build();
                ByteArrayInputStream objectStoreInput = new ByteArrayInputStream(file.getAbsolutePath().getBytes());
                ObjectInfo objectInfo = natsService.putDataIntoObjectStore(objectStoreInput, objectMeta);

                workData.setJsonSpdx(objectInfo.getObjectName());
                workData.setBucketName(objectInfo.getBucket());
            }
        }
    }

    @Subscribe(id="cancelButton")
    public void cancelWorkDataButton(ClickEvent<Button> event){cancelButton(event);}

    private boolean processworkData(){

        workData.setUseCopyrightAi(useCopyrigthAIBox.getValue());
        workData.setUseLicenseMatcher(licenseMatcherBox.getValue());

        log.info("Project {}",workData.getProjectId());
        log.info("JsonSPDX {}",workData.getJsonSpdx());

        if(checkData()){
            LocalDateTime now = LocalDateTime.now();
            long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
            WorkTask workTask = new WorkTask("processing_spdx", "question", actualTimestamp, workData);
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String message = objectMapper.writeValueAsString(workTask);
                natsService.sendWorkMessageToStream("work.spdx", message.getBytes());
                log.debug("sending message to spdx service: {}", message);
                return true;
            } catch (Exception e) {
                log.debug("Error with spdx service connection: " + e.getMessage());
            }
            return false;
        }else{
            return false;
        }
    }

    private boolean checkData() {
        return workData.getJsonSpdx() != null && !workData.getProjectId().isEmpty();
    }
}

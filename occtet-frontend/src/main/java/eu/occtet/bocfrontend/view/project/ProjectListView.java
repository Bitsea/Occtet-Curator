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

package eu.occtet.bocfrontend.view.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.boc.model.SpdxExportWorkData;
import eu.occtet.boc.model.WorkTask;

import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;


@Route(value = "projects", layout = MainView.class)
@ViewController(id = "Project.list")
@ViewDescriptor(path = "project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
public class ProjectListView extends StandardListView<Project> {

    private static final Logger log = LogManager.getLogger(ProjectListView.class);

    @ViewComponent
    private DataGrid<Project> projectsDataGrid;

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private NatsService natsService;
    @Autowired
    private Downloader downloader;

    @Subscribe
    public void onInit(final InitEvent event) {
        DataGrid.Column<Project> exportColumn = projectsDataGrid.getColumnByKey("exportBtn");

        exportColumn.setRenderer(new ComponentRenderer<>(project -> {

            JmixButton exportButton = uiComponents.create(JmixButton.class);
            exportButton.setIcon(VaadinIcon.DOWNLOAD.create());
            exportButton.setText("Export SBOM");
            exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            exportButton.setTooltipText("Export Project");
            exportButton.addClickListener(clickEvent -> {
                handleExport(project);
            });

            return exportButton;
        }));
    }

    private void handleExport(Project project){
        SpdxExportWorkData spdxExportWorkData = new SpdxExportWorkData(project.getDocumentID(), String.valueOf(project.getId()));
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("processing_spdx", "send project data to microservice to create new spdx", actualTimestamp, spdxExportWorkData);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            String message = mapper.writeValueAsString(workTask);
            log.debug("sending message to export service: {}", message);
            natsService.sendWorkMessageToStream("work.export", message.getBytes(Charset.defaultCharset()));

            UI ui = UI.getCurrent();
            String fileId = project.getDocumentID(); // Assuming this is the key in the bucket

            CompletableFuture.runAsync(() -> {
                waitForFileAndDownload(ui, fileId);
            });

        }catch(Exception e){
            log.error("Error with microservice connection: {}", e.getMessage());
        }
    }

    private void waitForFileAndDownload(UI ui, String fileId) {
        int timeoutSeconds = 300000;
        int pollIntervalMillis = 1000;
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);

        byte[] fileData = null;

        while (System.currentTimeMillis() < endTime) {
            fileData = natsService.getFileFromBucket(fileId);

            if (fileData != null) {
                break;
            }

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        byte[] finalFileData = fileData;

        byte[] finalFileData1 = fileData;
        ui.access(() -> {
            if (finalFileData != null) {
                downloader.download(finalFileData1
                        , "spdx_export.json"
                );
        }
    });
    }

}

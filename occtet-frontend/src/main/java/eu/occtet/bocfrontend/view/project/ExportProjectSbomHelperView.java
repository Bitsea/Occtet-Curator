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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import eu.occtet.boc.model.SpdxExportWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.model.WorkTaskProgress;
import eu.occtet.boc.model.WorkTaskStatus;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.service.WorkTaskProgressMonitor;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.facet.Timer;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ViewController(id = "ExportProjectSbomHelperView")
@ViewDescriptor(path = "export-project-sbom-helper-view.xml")
@DialogMode(width = "90%", height = "90%")
public class ExportProjectSbomHelperView extends StandardView {

    // TODO refresh bahaviour
    // TODO notify user

    private final Logger log = LogManager.getLogger(this.getClass());

    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final String KV_ENTITY_OBJECT_STORE_KEY = "objectStoreKey";

    private Project project;
    private String projectTaskPrefix;

    private Map<String, String> activeTasks = new ConcurrentHashMap<>();

    @Value("${nats.send-subject-export}")
    private String sendSubjectExport;

    @Value("${nats.object-store-ttl}")
    private String natsObjectStoreTtl;

    @Autowired protected UiComponents uiComponents;
    @Autowired private NatsService natsService;
    @Autowired protected DataManager dataManager;
    @Autowired private Downloader downloader;
    @Autowired private Messages messages;
    @Autowired private Notifications notifications;
    @Autowired private WorkTaskProgressMonitor workTaskProgressMonitor;

    @ViewComponent private KeyValueCollectionContainer prevExportsDc;
    @ViewComponent private DataGrid<KeyValueEntity> prevExportsDataGrid;
    @ViewComponent private JmixButton generateSbomButton;
    @ViewComponent private JmixComboBox<String> sbomFormatComboBox;
    @ViewComponent private Timer progressTimer;
    @ViewComponent private VerticalLayout activeTasksBox;
    @ViewComponent private Span infoSpan;
    @ViewComponent private Span currentlyInQueue;

    public void setProject(Project project) {
        this.project = project;
    }

    @Subscribe
    protected void onInit(final InitEvent event) {
        sbomFormatComboBox.setItems("SPDX 2.3");
        generateSbomButton.addClickListener(e -> handleExport(project, sbomFormatComboBox.getValue()));

        // TODO message
        String template = "Note: Generated SBOMs are automatically deleted from the server after %s hours.";
        infoSpan.setText(String.format(template, natsObjectStoreTtl));
    }

    @Subscribe
    protected void beforeShow(final BeforeShowEvent event) {
        projectTaskPrefix = "SPDX_Export_" + project.getProjectName() + "_";

        Map<String, WorkTaskProgress> globalProgress = workTaskProgressMonitor.getAllProgressMap();
        for (Map.Entry<String, WorkTaskProgress> entry : globalProgress.entrySet()) {
            String taskId = entry.getKey();
            WorkTaskProgress progress = entry.getValue();

            if (progress.getName() != null && progress.getName().startsWith(projectTaskPrefix)) {
                if (progress.getStatus() == WorkTaskStatus.IN_PROGRESS) {
                    activeTasks.put(taskId, progress.getName() + " - Resuming: " + progress.getPercent() + "%");
                }
            }
        }

        if (!activeTasks.isEmpty()) {
            progressTimer.start();
            updateActiveTasksUI();
        }

        // Load available downloads
        refreshDownloadsList();
        updateQueueSpan();
    }

    private void handleExport(Project project, String sbomFormat) {
        if (sbomFormat == null) {
            log.warn("No SBOM format selected");
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.project/exportProjectSbomHelperView" +
                    ".generate.sbom.combobox.format.notSelected.message")).withThemeVariant(NotificationVariant.LUMO_WARNING).show();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String timeStamp = now.format(TIME_FORMATTER);
        String dynamicTaskName = projectTaskPrefix + timeStamp;

        String formatSuffix = sbomFormat.replace(" ", "").toLowerCase();

        // "MyProject_1_2026-02-26_10-25-00_spdx2.3"
        String expectedObjectStoreKey = project.getProjectName() + "_" + project.getId() + "_" + timeStamp + "_" + formatSuffix;

        String taskId = UUID.randomUUID().toString();
        long actualTimestamp = Instant.now().getEpochSecond();

        SpdxExportWorkData spdxExportWorkData = new SpdxExportWorkData(
                project.getDocumentID(),
                project.getId(),
                expectedObjectStoreKey
        );

        WorkTask workTask = new WorkTask(taskId, dynamicTaskName, "Export data to microservice to create new SPDX", actualTimestamp, spdxExportWorkData);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        try {
            String message = mapper.writeValueAsString(workTask);
            natsService.sendWorkMessageToStream(sendSubjectExport, message.getBytes());

            // Add to local tracking and update UI
            // TODO message
            activeTasks.put(taskId, dynamicTaskName + " - Queued...");
            updateActiveTasksUI();

            updateQueueSpan();

            progressTimer.start();

            // TODO message
            notifications.create("Export task added to queue.")
                    .withThemeVariant(NotificationVariant.LUMO_PRIMARY).show();

        } catch (Exception e) {
            log.error("Could not send work message", e);
            // TODO message
            notifications.create("Failed to send export task.").withThemeVariant(NotificationVariant.LUMO_ERROR).show();
        }
    }

    @Subscribe("progressTimer")
    protected void onProgressTimerTimerAction(final Timer.TimerActionEvent event) {
        updateQueueSpan();

        if (activeTasks.isEmpty()) {
            progressTimer.stop();
            return;
        }

        boolean changed = false;
        boolean refreshFiles = false;

        Iterator<Map.Entry<String, String>> iterator = activeTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String taskId = entry.getKey();

            WorkTaskProgress progress = workTaskProgressMonitor.getProgressForTask(taskId);

            if (progress != null) {
                if (progress.getStatus() == WorkTaskStatus.COMPLETED) {
                    iterator.remove();
                    changed = true;
                    refreshFiles = true;
                    // TODO message
                    notifications.create(progress.getName() + " Completed!")
                            .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                            .withPosition(Notification.Position.TOP_START).show();
                } else if (progress.getStatus() == WorkTaskStatus.ERROR) {
                    // TODO message
                    entry.setValue(progress.getName() + " - Failed: " + progress.getDetails());
                    changed = true;
                } else {
                    // TODO message
                    String newStatus = progress.getName() + " - Generating: " + progress.getPercent() + "%";
                    if (!newStatus.equals(entry.getValue())) {
                        entry.setValue(newStatus);
                        changed = true;
                    }
                }
            }
        }

        if (changed) updateActiveTasksUI();
        if (refreshFiles) refreshDownloadsList();
        updateQueueSpan();
    }

    /**
     * Rebuilds the visual list of active tasks dynamically.
     */
    private void updateActiveTasksUI() {
        activeTasksBox.removeAll();
        for (String statusText : activeTasks.values()) {
            Span label = uiComponents.create(Span.class);
            label.setText(statusText);

            if (statusText.contains("Failed")) {
                label.getElement().getThemeList().addAll(Arrays.asList("badge", "error"));
            } else if (statusText.contains("Queued")) {
                label.getElement().getThemeList().addAll(Arrays.asList("badge", "warning"));
            } else {
                label.getElement().getThemeList().addAll(Arrays.asList("badge"));
            }
            activeTasksBox.add(label);
        }
    }

    private void updateQueueSpan(){
        long numOfQ = natsService.getNumbOfMsgQueued(sendSubjectExport);
        if (numOfQ == 0) {
            // TODO message
            currentlyInQueue.setText("Currently no tasks in queue.");
        } else {
            // TODO message
            currentlyInQueue.setText("Currently " + numOfQ + " tasks in queue.");
        }
    }

    private void refreshDownloadsList() {
        String projectKeyPrefix = project.getProjectName() + "_" + project.getId();
        List<String> exportedKeys = natsService.getPreviousExportOfSameProject(projectKeyPrefix);

        List<KeyValueEntity> entities = exportedKeys.stream().map(key -> {
                    KeyValueEntity kvEntity = dataManager.create(KeyValueEntity.class);
                    kvEntity.setValue(KV_ENTITY_OBJECT_STORE_KEY, key);

                    kvEntity.setValue("fileName", key + ".json");

                    return kvEntity;
                }).sorted((e1, e2) -> e2
                        .getValue("fileName")
                        .toString()
                        .compareTo(e1.getValue("fileName").toString()))
                .collect(Collectors.toList());

        prevExportsDc.setItems(entities);
    }

    @Supply(to = "prevExportsDataGrid.downloadButton", subject = "renderer")
    protected Renderer<KeyValueEntity> downloadButtonRenderer(){
        return new ComponentRenderer<>(kv -> {
            String key = kv.getValue(KV_ENTITY_OBJECT_STORE_KEY);
            JmixButton downloadButton = uiComponents.create(JmixButton.class);
            // TODO message
            downloadButton.setText("Download");
            downloadButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            downloadButton.setIcon(VaadinIcon.DOWNLOAD.create());

            downloadButton.addClickListener(e -> {
                byte[] fileData = natsService.getFileFromBucket(key);
                if (fileData != null) {
                    // Match the filename we defined above
                    downloader.download(fileData, key + ".json");
                } else {
                    // TODO message
                    notifications.create("This file was deleted or is no longer available.")
                            .withThemeVariant(NotificationVariant.LUMO_WARNING).show();
                    refreshDownloadsList();
                }
            });
            return downloadButton;
        });
    }
}
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
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import eu.occtet.boc.model.*;
import eu.occtet.bocfrontend.config.ConfigNatsProperties;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.service.WorkTaskProgressMonitor;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.facet.Timer;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the user interface for exporting a project's Software Bill of Materials (SBOM).
 * <p>
 * This view delegates the heavy lifting of SBOM generation to a microservice via NATS messaging.
 * Because the generation process is asynchronous, it utilizes a UI timer to continuously poll
 * the task status and update the progress state for the user. Once the task completes,
 * the resulting artifacts are retrieved from the object store and made available for download.
 *
 * @see NatsService
 * @see WorkTaskProgressMonitor
 */
@ViewController(id = "ExportProjectSbomHelperView")
@ViewDescriptor(path = "export-project-sbom-helper-view.xml")
@DialogMode(width = "90%", height = "90%")
public class ExportProjectSbomHelperView extends StandardView {

    private static final Logger log = LogManager.getLogger(ExportProjectSbomHelperView.class);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String KV_ENTITY_OBJECT_STORE_KEY = "objectStoreKey";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private final ConfigNatsProperties natsProperties;


    @Autowired protected UiComponents uiComponents;
    @Autowired protected DataManager dataManager;
    @Autowired private NatsService natsService;
    @Autowired private Downloader downloader;
    @Autowired private Messages messages;
    @Autowired private Notifications notifications;
    @Autowired private WorkTaskProgressMonitor workTaskProgressMonitor;
    @Autowired private Dialogs dialogs;

    @ViewComponent private KeyValueCollectionContainer prevExportsDc;
    @ViewComponent private JmixButton generateSbomButton;
    @ViewComponent private JmixComboBox<String> sbomFormatComboBox;
    @ViewComponent private Timer progressTimer;
    @ViewComponent private VerticalLayout activeTasksBox;
    @ViewComponent private Span infoSpan;
    @ViewComponent private Span currentlyInQueue;
    @ViewComponent private JmixCheckbox enrichCopyrightCheckbox;

    private Project project;
    private String projectTaskPrefix;
    private final static String SPDX_2_3= "SPDX 2.3";
    private final static String CYCLONEDX_1_6= "CycloneDX 1.6";
    private final Map<String, String> activeTasks = new ConcurrentHashMap<>();

    public ExportProjectSbomHelperView(ConfigNatsProperties natsProperties) {
        this.natsProperties = natsProperties;
    }

    /**
     * Injects the project context required for initiating the SBOM export.
     * Must be called by the parent view before this dialog is opened.
     *
     * @param project the currently selected project instance
     */
    public void setProject(Project project) {
        this.project = project;
    }

    @Subscribe
    protected void onInit(final InitEvent event) {
        log.debug("init");
        sbomFormatComboBox.setItems(SPDX_2_3, CYCLONEDX_1_6);
        generateSbomButton.addClickListener(e ->
                handleExport(project, sbomFormatComboBox.getValue(), enrichCopyrightCheckbox.getValue())
        );

        infoSpan.setText(String.format(messages.getMessage("eu.occtet.bocfrontend.view" +
                ".project/exportProjectSbomHelperView.deletion.fino"), natsProperties.objectStoreTtl()));
    }

    @Subscribe
    protected void beforeShow(final BeforeShowEvent event) {


        String spdxPrefix = "SPDX_Export_" + project.getProjectName() + "_";
        String cycloneDxPrefix = "CycloneDX_Export_" + project.getProjectName() + "_";

        Map<String, WorkTaskProgress> globalProgress = workTaskProgressMonitor.getAllProgressMap();
        for (Map.Entry<String, WorkTaskProgress> entry : globalProgress.entrySet()) {
            String taskId = entry.getKey();
            WorkTaskProgress progress = entry.getValue();

            if (progress.getName() != null) {
                boolean isSpdxTask = progress.getName().startsWith(spdxPrefix);
                boolean isCycloneDxTask = progress.getName().startsWith(cycloneDxPrefix);

                if ((isSpdxTask || isCycloneDxTask) && progress.getStatus() == WorkTaskStatus.IN_PROGRESS) {
                    String resumingMsg = String.format(messages.getMessage("eu.occtet.bocfrontend.view" +
                            ".project/exportProjectSbomHelperView.task.resuming"), progress.getPercent());
                    activeTasks.put(taskId, progress.getName() + resumingMsg);
                }
            }
        }

        if (!activeTasks.isEmpty()) {
            progressTimer.start();
            updateActiveTasksUI();
        }

        refreshDownloadsList();
        updateQueueSpan();
    }

    @Subscribe("sbomFormatComboBox")
    public void onSbomFormatComboBoxComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<?>, ?> event) {
        if(sbomFormatComboBox.getValue().equals(SPDX_2_3)) {
            projectTaskPrefix = "SPDX_Export_" + project.getProjectName() + "_";
        }else if(sbomFormatComboBox.getValue().equals(CYCLONEDX_1_6)){
            projectTaskPrefix = "CycloneDX_Export_" + project.getProjectName() + "_";
        }


    }



    /**
     * Constructs and dispatches a new export task payload to the configured NATS subject.
     *
     * @param project    the project target for the SBOM generation
     * @param sbomFormat the desired output format selected by the user
     */
    private void handleExport(Project project, String sbomFormat, boolean enrichment) {
        if (sbomFormat == null) {
            log.warn("Attempted to start export without an active format selection.");
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view" +
                            ".project/exportProjectSbomHelperView.generate.sbom.combobox.format.notSelected.message"))
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .show();
            return;
        }

        String timeStamp = LocalDateTime.now().format(TIME_FORMATTER);
        String dynamicTaskName = projectTaskPrefix + timeStamp;
        String formatSuffix = sbomFormat.replace(" ", "").toLowerCase();

        String expectedObjectStoreKey = String.format("%s_%s_%s_%s",
                project.getProjectName(), project.getId(), timeStamp, formatSuffix);

        String taskId = UUID.randomUUID().toString();
        long actualTimestamp = Instant.now().getEpochSecond();

        log.debug("expected objectKeyStore: {}", expectedObjectStoreKey);

        try {
            if(sbomFormat.equals(SPDX_2_3)) {
                SpdxExportWorkData spdxExportWorkData = new SpdxExportWorkData(
                        project.getDocumentID(),
                        project.getId(),
                        expectedObjectStoreKey,
                        enrichment
                );
                WorkTask workTask = new WorkTask(taskId, dynamicTaskName, "Export data to microservice to create new SPDX", actualTimestamp, spdxExportWorkData);
                byte[] messagePayload = MAPPER.writeValueAsBytes(workTask);
                natsService.sendWorkMessageToStream(natsProperties.send_subject_export(), messagePayload);
            }else if(sbomFormat.equals(CYCLONEDX_1_6)){
                CycloneDxExportWorkData cycloneDxExportWorkData= generateCycloneDxExportWorkData(expectedObjectStoreKey, enrichment);
                log.debug("show cyclone workdata: {}", cycloneDxExportWorkData.getObjectStoreKey());
                WorkTask workTask = new WorkTask(taskId, dynamicTaskName, "Export data to microservice to create new CycloneDX", actualTimestamp, cycloneDxExportWorkData);
                byte[] messagePayload = MAPPER.writeValueAsBytes(workTask);
                natsService.sendWorkMessageToStream(natsProperties.send_subject_cyclonedx_export(), messagePayload);
            }
            activeTasks.put(taskId, dynamicTaskName + messages.getMessage("eu.occtet.bocfrontend.view" +
                    ".project/exportProjectSbomHelperView.task.queued"));
            updateActiveTasksUI();
            updateQueueSpan();
            progressTimer.start();

            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view" +
                            ".project/exportProjectSbomHelperView.notification.taskAdded"))
                    .withThemeVariant(NotificationVariant.LUMO_PRIMARY).show();

        } catch (Exception e) {
            log.error("Failed to serialize or dispatch work task payload to NATS.", e);
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view" +
                            ".project/exportProjectSbomHelperView.notification.taskFailed"))
                    .withThemeVariant(NotificationVariant.LUMO_ERROR).show();
        }
    }

    private CycloneDxExportWorkData generateCycloneDxExportWorkData(String expectedObjectStoreKey, boolean enrichment){

        // get version and serialnumber via nats
        String projectKeyPrefix = project.getProjectName() + "_" + project.getId();
        List<String> previousExportKeys = natsService.getPreviousExportOfSameProject(projectKeyPrefix);

        String serialNumber;
        int nextVersion;

        if (previousExportKeys == null || previousExportKeys.isEmpty()) {
            serialNumber = "urn:uuid:" + UUID.randomUUID().toString();
            nextVersion = 1;
        } else {
            nextVersion = previousExportKeys.size() + 1;

            previousExportKeys.sort(Comparator.reverseOrder());
            String latestKey = previousExportKeys.getFirst();

            serialNumber = fetchSerialNumberFromExistingSbom(latestKey);
        }
        return new CycloneDxExportWorkData(serialNumber, project.getId(), nextVersion, expectedObjectStoreKey,enrichment );
    }

    /**
     * helpermethod: loading last bom from nats
     */
    private String fetchSerialNumberFromExistingSbom(String latestKey) {
        try {
            byte[] fileData = natsService.getFileFromBucket(latestKey);

            if (fileData == null) {
                log.warn("olf sbom not found for key: {}", latestKey);
                return "urn:uuid:" + UUID.randomUUID().toString();
            }

            JsonParser parser = new JsonParser();
            Bom previousBom = parser.parse(fileData);

            if (previousBom.getSerialNumber() != null) {
                return previousBom.getSerialNumber();
            }
        } catch (Exception e) {
            log.error("could not read SBOM, generate new one", e);
        }

        return "urn:uuid:" + UUID.randomUUID().toString();
    }

    @Subscribe("progressTimer")
    protected void onProgressTimerTimerAction(final Timer.TimerActionEvent event) {
        updateQueueSpan();

        if (activeTasks.isEmpty()) {
            progressTimer.stop();
            return;
        }

        boolean uiNeedsUpdate = false;
        boolean downloadsNeedRefresh = false;

        Iterator<Map.Entry<String, String>> iterator = activeTasks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String taskId = entry.getKey();

            WorkTaskProgress progress = workTaskProgressMonitor.getProgressForTask(taskId);

            if (progress != null) {
                if (progress.getStatus() == WorkTaskStatus.COMPLETED) {
                    iterator.remove();
                    uiNeedsUpdate = true;
                    downloadsNeedRefresh = true;

                    String completedMsg = String.format(messages.getMessage("eu.occtet.bocfrontend.view.project/exportProjectSbomHelperView.task.completed"), progress.getName());
                    notifications.create(completedMsg)
                            .withType(Notifications.Type.SUCCESS)
                            .withDuration(0).withCloseable(true).show();

                } else if (progress.getStatus() == WorkTaskStatus.ERROR) {
                    String failedMsg = String.format(messages.getMessage("eu.occtet.bocfrontend.view" +
                            ".project/exportProjectSbomHelperView.task.failed"), progress.getDetails());
                    entry.setValue(progress.getName() + failedMsg);
                    uiNeedsUpdate = true;

                } else {
                    String generatingMsg = String.format(messages.getMessage("eu.occtet.bocfrontend.view" +
                            ".project/exportProjectSbomHelperView.task.generating"), progress.getPercent());
                    String newStatus = progress.getName() + generatingMsg;
                    if (!newStatus.equals(entry.getValue())) {
                        entry.setValue(newStatus);
                        uiNeedsUpdate = true;
                    }
                }
            }
        }

        if (uiNeedsUpdate) updateActiveTasksUI();
        if (downloadsNeedRefresh) refreshDownloadsList();
        updateQueueSpan();
    }

    /**
     * Refreshes the visual elements representing active, queued, or failed tasks.
     * Uses theme variants to clearly distinguish status states.
     */
    private void updateActiveTasksUI() {
        activeTasksBox.removeAll();

        String failedIndicator = messages.getMessage("eu.occtet.bocfrontend.view" +
                ".project/exportProjectSbomHelperView.task.failed").split(":")[0].trim();
        String queuedIndicator = messages.getMessage("eu.occtet.bocfrontend.view" +
                ".project/exportProjectSbomHelperView.task.queued").trim();

        for (String statusText : activeTasks.values()) {
            Span label = uiComponents.create(Span.class);
            label.setText(statusText);

            if (statusText.contains(failedIndicator)) {
                label.getElement().getThemeList().addAll(Arrays.asList("badge", "error"));
            } else if (statusText.contains(queuedIndicator)) {
                label.getElement().getThemeList().addAll(Arrays.asList("badge", "warning"));
            } else {
                label.getElement().getThemeList().addAll(List.of("badge"));
            }
            activeTasksBox.add(label);
        }
    }

    private void updateQueueSpan() {

        long numOfQ = natsService.getNumbOfMsgQueued(natsProperties.send_subject_export())
                +natsService.getNumbOfMsgQueued(natsProperties.send_subject_cyclonedx_export());
        if (numOfQ == 0) {
            currentlyInQueue.setText(messages.getMessage("eu.occtet.bocfrontend.view" +
                    ".project/exportProjectSbomHelperView.queue.empty"));
        } else {
            currentlyInQueue.setText(String.format(messages.getMessage("eu.occtet.bocfrontend.view" +
                    ".project/exportProjectSbomHelperView.queue.active"), numOfQ));
        }
    }

    /**
     * Fetches previously generated SBOM exports from the backend storage
     * and binds them to the data grid for user retrieval.
     */
    private void refreshDownloadsList() {
        String projectKeyPrefix = project.getProjectName() + "_" + project.getId();
        List<String> exportedKeys = natsService.getPreviousExportOfSameProject(projectKeyPrefix);

        List<KeyValueEntity> entities = exportedKeys.stream()
                .map(key -> {
                    KeyValueEntity kvEntity = dataManager.create(KeyValueEntity.class);
                    kvEntity.setValue(KV_ENTITY_OBJECT_STORE_KEY, key);
                    kvEntity.setValue("fileName", key + ".json");
                    return kvEntity;
                })
                .sorted(Comparator.comparing(e -> e.getValue("fileName").toString(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        prevExportsDc.setItems(entities);
    }

    @Supply(to = "prevExportsDataGrid.downloadButton", subject = "renderer")
    protected Renderer<KeyValueEntity> downloadButtonRenderer() {
        return new ComponentRenderer<>(kv -> {
            String key = kv.getValue(KV_ENTITY_OBJECT_STORE_KEY);
            JmixButton downloadButton = uiComponents.create(JmixButton.class);

            downloadButton.setText(messages.getMessage("eu.occtet.bocfrontend.view" +
                    ".project/exportProjectSbomHelperView.button.download"));
            downloadButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            downloadButton.setIcon(VaadinIcon.DOWNLOAD.create());

            downloadButton.addClickListener(e -> {
                byte[] fileData = natsService.getFileFromBucket(key);
                if (fileData != null) {
                    downloader.download(fileData, key + ".json");
                } else {
                    notifications.create(messages.getMessage("eu.occtet.bocfrontend.view" +
                                    ".project/exportProjectSbomHelperView.notification.fileDeleted"))
                            .withThemeVariant(NotificationVariant.LUMO_WARNING).show();
                    refreshDownloadsList();
                }
            });
            return downloadButton;
        });
    }

    @Supply(to = "prevExportsDataGrid.deleteButton", subject = "renderer")
    protected Renderer<KeyValueEntity> deleteButtonRenderer() {
        return new ComponentRenderer<>(kv -> {
            String key = kv.getValue(KV_ENTITY_OBJECT_STORE_KEY);
            JmixButton deleteButton = uiComponents.create(JmixButton.class);

            deleteButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR,  ButtonVariant.LUMO_PRIMARY);
            deleteButton.setIcon(VaadinIcon.TRASH.create());

            deleteButton.addClickListener(e -> dialogs.createOptionDialog()
                    .withText(messages.getMessage("exportProjectSbomHelperView.notification.fileDeletedSuccess"))
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(actionEvent -> {
                                natsService.deleteFileFromBucket(key);

                                notifications.create(messages.getMessage("exportProjectSbomHelperView.notification.fileDeletedSuccessfull"))
                                        .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                        .show();

                                refreshDownloadsList();
                            }),
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .open());

            return deleteButton;
        });
    }
}
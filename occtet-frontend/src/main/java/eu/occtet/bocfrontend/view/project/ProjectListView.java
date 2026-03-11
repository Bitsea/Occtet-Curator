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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.*;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.services.ProjectDeletionTracker;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
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
    private DialogWindows dialogWindows;
    @Autowired
    private Messages messages;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private Notifications notifications;
    @Autowired
    private ProjectDeletionTracker deletionTracker;
    @Autowired
    private OrtIssueRepository ortIssueRepository;
    @Autowired
    private OrtViolationRepository ortViolationRepository;
    @Autowired
    private CuratorTaskRepository curatorTaskRepository;

    @Subscribe
    public void onInit(final InitEvent event) {
        DataGrid.Column<Project> exportColumn = projectsDataGrid.getColumnByKey("exportBtn");

        exportColumn.setRenderer(new ComponentRenderer<>(project -> {
            JmixButton exportButton = uiComponents.create(JmixButton.class);
            exportButton.setIcon(VaadinIcon.DOWNLOAD.create());
            exportButton.setText(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.exportBtn"));
            exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            exportButton.setTooltipText(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.exportTooltip"));
            exportButton.addClickListener(clickEvent -> {
                dialogWindows.view(this, ExportProjectSbomHelperView.class)
                        .withViewConfigurer(v -> {
                            v.setProject(project);
                        }).open();
            });
            return exportButton;
        }));
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        UI ui = UI.getCurrent();
        if (deletionTracker.isDeleting() && ComponentUtil.getData(ui, "deletion_progress_shown") == null) {
            showProgressNotification(ui);
        }
    }

    @Subscribe("projectsDataGrid")
    public void clickOnProjectsDataGrid(ItemDoubleClickEvent<Project> event){
        DialogWindow<ProjectDetailView> window =
                dialogWindows.detail(this, Project.class)
                        .withViewClass(ProjectDetailView.class)
                        .editEntity(event.getItem())
                        .build();
        window.setWidth("100%");
        window.setHeight("100%");
        window.open();
    }

    @Subscribe(id = "removeButton", subject = "clickListener")
    public void onProjectsRemoveClick(final ClickEvent<JmixButton> event) {
        Project selectedProject = projectsDataGrid.getSingleSelectedItem();

        if (selectedProject == null) {
            return;
        }

        String warningText = messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.deleteConfirmText");

        dialogs.createOptionDialog()
                .withHeader(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.deleteConfirmHeader"))
                .withText(warningText)
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withVariant(ActionVariant.PRIMARY)
                                .withHandler(actionEvent -> executeAsyncDeletion(selectedProject)),
                        new DialogAction(DialogAction.Type.NO)
                )
                .open();
    }

    private void executeAsyncDeletion(Project project) {
        log.debug("Initiating asynchronous deletion for project: {}", project.getProjectName());

        UI ui = UI.getCurrent();
        final SecurityContext securityContext = SecurityContextHolder.getContext();

        deletionTracker.start();
        Notification progressNotification = showProgressNotification(ui);

        CompletableFuture.runAsync(() -> {
            SecurityContextHolder.setContext(securityContext);

            try {
                // important we need to delete files first, inventories and lastly the project
                SaveContext filesCtx = new SaveContext();
                List<File> files = fileRepository.findByProject(project);
                for (File file : files) {
                    filesCtx.removing(file);
                }
                dataManager.save(filesCtx);

                SaveContext relatedDataCtx = new SaveContext();
                List<OrtIssue> ortIssues = ortIssueRepository.findByProject(project);
                for (OrtIssue ortIssue : ortIssues) {
                    relatedDataCtx.removing(ortIssue);
                }
                List<OrtViolation> ortViolations = ortViolationRepository.findByProject(project);
                for (OrtViolation ortViolation : ortViolations) {
                    relatedDataCtx.removing(ortViolation);
                }
                List<CuratorTask> curatorTasks = curatorTaskRepository.findByProject(project);
                for (CuratorTask curatorTask : curatorTasks) {
                    relatedDataCtx.removing(curatorTask);
                }
                dataManager.save(relatedDataCtx);

                SaveContext inventoryCtx = new SaveContext();
                removeInventories(project, inventoryCtx);
                dataManager.save(inventoryCtx);

                // reload project after files got deleted otherwhise it will be detached
                Project freshProject = dataManager.load(Project.class)
                        .id(project.getId())
                        .fetchPlan(fp -> fp.add("files"))
                        .one();

                SaveContext projectCtx = new SaveContext();
                projectCtx.removing(freshProject);
                dataManager.save(projectCtx);

                ui.access(() -> {
                    if (ui.isAttached()) {
                        progressNotification.close();
                        projectsDataGrid.getDataProvider().refreshAll();
                        notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.deleteMessage"))
                                .withPosition(Notification.Position.TOP_END).show();
                    }
                    log.info("Project {} deleted asynchronously", project.getProjectName());
                });

            } catch (Exception e) {
                log.error("Failed to execute deletion for project", e);
                ui.access(() -> {
                    if (ui.isAttached()) {
                        progressNotification.close();
                        notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.deleteMessage.error"))
                                .withType(Notifications.Type.ERROR).withPosition(Notification.Position.TOP_END).show();
                    }
                });
            } finally {
                deletionTracker.finish();
                SecurityContextHolder.clearContext();
            }
        });
    }

    private Notification showProgressNotification(UI ui) {
        Notification progressNotification = new Notification();
        progressNotification.setPosition(Notification.Position.TOP_END);
        progressNotification.setDuration(0);

        Div text = new Div();
        text.setText(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.delete.progress.notification.text"));

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);

        progressNotification.add(new VerticalLayout(text, progressBar));

        ComponentUtil.setData(ui, "deletion_progress_shown", true);

        progressNotification.addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                ComponentUtil.setData(ui, "deletion_progress_shown", null);
            }
        });

        progressNotification.open();

        return progressNotification;
    }

    private void removeInventories(Project project, SaveContext saveContext){
        List<InventoryItem> inventoryItemList = inventoryItemRepository.findByProject(project);

        for(InventoryItem item : inventoryItemList){
            List<InventoryItem> itemList= inventoryItemRepository.findBySoftwareComponent(item.getSoftwareComponent());

            if(Boolean.TRUE.equals(item.getCurated()) && itemList.size()<=1){
                item.setProject(null);
                saveContext.saving(item);
            }else{
                //softwareComponent is global and gets reused, no deleting here
                item.setSoftwareComponent(null);
                saveContext.removing(item);
            }
        }
    }
}
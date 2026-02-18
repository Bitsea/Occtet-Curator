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

package eu.occtet.bocfrontend.view.curatortask;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.occtet.bocfrontend.dao.CuratorTaskRepository;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.TaskStatus;
import eu.occtet.bocfrontend.importer.TaskManager;
import eu.occtet.bocfrontend.importer.TaskParent;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.session.SessionData;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.action.list.CreateAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.facet.Timer;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

@Route(value = "curatorTask", layout = MainView.class)
@ViewController("CuratorTask.list")
@ViewDescriptor(value = "curatortask-list-view.xml", path = "curatortask-list-view.xml")
@LookupComponent("curatorTaskDataGrid")
@DialogMode(width = "600", height = "800")
public class ImportTaskListView extends StandardListView<CuratorTask> {


    private static final Logger log = LogManager.getLogger(ImportTaskListView.class);


    @ViewComponent
    private DataGrid<CuratorTask> curatorTaskDataGrid;
    @ViewComponent
    private JmixButton showFeedback;
    @ViewComponent
    private JmixButton showConfig;
    @ViewComponent
    private HorizontalLayout availableImportBox;
    @ViewComponent
    private MessageBundle messageBundle;
    @ViewComponent("curatorTaskDataGrid.create")
    private CreateAction<CuratorTask> curatorTaskDataGridCreate;
    @ViewComponent
    private CollectionContainer<CuratorTask> curatorTaskDc;

    @Autowired
    private Dialogs dialogs;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Dialogs messageDialog;

    @Autowired
    private CuratorTaskService curatorTaskService;
    @Autowired
    private DataManager dataManager;

    @Autowired
    private TaskManager taskManager;
;
    @Autowired
    private CuratorTaskRepository curatorTaskRepository;

    @Autowired
    private SessionData sessionData;


    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {

        updateAvailableImportsBox();

        curatorTaskDc.setItems(curatorTaskRepository.findByStatus(TaskStatus.COMPLETED));

    }

    /**
     * loads preexisting imports to the grid
     */
    private void load(){
        List<CuratorTask> curatorTasks = curatorTaskRepository.findAll();
        curatorTaskDc.setItems(curatorTasks);
    }


    private void updateAvailableImportsBox() {
        List<TaskParent> importers = taskManager.getAvailableImports();
        availableImportBox.removeAll();
        importers.forEach(importer -> {
            if(!importer.getName().contains("Dumb") && !importer.getName().contains("Flexera_Report_Import"))
                availableImportBox.add(createImportIcon(importer + "Id", importer));
        });
    }




    private VerticalLayout createImportIcon(String buttonId, TaskParent importer) {
        VerticalLayout verticalLayout = uiComponents.create(VerticalLayout.class);
        verticalLayout.setPadding(false);
        verticalLayout.setWidth("AUTO");
        verticalLayout.addClassName(LumoUtility.AlignItems.CENTER);
        verticalLayout.addClassName(LumoUtility.JustifyContent.CENTER);

        H6 h6 = uiComponents.create(H6.class);
        h6.setText(importer.getName());

        JmixImage<Object> importImageButton = uiComponents.create(JmixImage.class);
        importImageButton.setClassName("image-border");
        importImageButton.setId(buttonId);
        importImageButton.setHeight("100px");
        importImageButton.setWidth("100px");
        importImageButton.getElement().setAttribute("src", "icons/" + importer.getName().replace(" ", "").toLowerCase() + ".png");
        verticalLayout.add(importImageButton, h6);

        importImageButton.setTitle(messageBundle.getMessage(importer.getName().replace(" ", "")));
        importImageButton.addClickListener(e -> {
            sessionData.setAttribute("selectedImporter", importer);
            curatorTaskDataGridCreate.execute();
        });

        return verticalLayout;
    }

    @Subscribe("refreshTimer")
    public void onRefreshTimerTimerAction(final Timer.TimerActionEvent event) {
        List<CuratorTask> tasks = curatorTaskRepository.findAll();
        curatorTaskDc.setItems(tasks);
    }

    @Subscribe("curatorTaskDataGrid")
    public void onCuratorInitializerDataGridItemClick(final ItemClickEvent<CuratorTask> event) {
        showFeedback.setEnabled(false);
        showConfig.setEnabled(false);
        if (event.getItem() != null) {
            showFeedback.setEnabled(event.getItem().getStatus().equals(TaskStatus.COMPLETED));
            showConfig.setEnabled(!event.getItem().getTaskConfiguration().isEmpty());
        }
    }

    @Subscribe(id = "showConfig", subject = "clickListener")
    public void onShowConfigClick1(final ClickEvent<JmixButton> event) {
        Set<CuratorTask> selectedItems = curatorTaskDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty())
            return;
        StringBuilder sb = new StringBuilder();
        List<Configuration> configList = selectedItems.iterator().next().getTaskConfiguration();
        sb.append("\\<div>");
        for (Configuration cf : configList) {
            sb.append("<b>" + cf.getName() + ":</b> " + cf.getValue() + "<p>");
        }
        sb.append("</div>");
        messageDialog.createMessageDialog()
                .withHeader("Configuration")
                .withContent(new Html(sb.toString()))
                .open();
    }

    @Subscribe(id = "stopTaskBtn", subject = "clickListener")
    public void onStopClick(final ClickEvent<JmixButton> event) {
        Set<CuratorTask> selectedItems = curatorTaskDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty())
            return;
        for (CuratorTask task : selectedItems) {
            task.setStatus(TaskStatus.CANCELLED);
            dataManager.save(task);
        }
        load();
    }

    @Subscribe(id = "showFeedback", subject = "clickListener")
    public void onShowFeedbackClick(final ClickEvent<JmixButton> event) {
        Set<CuratorTask> selectedItems = curatorTaskDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty()){
            return;
        }
        VerticalLayout fbLayout = new VerticalLayout();
        selectedItems.iterator().next().getFeedback().forEach(fb -> fbLayout.add(new Span(fb)));
        dialogs.createMessageDialog()
                .withCloseOnOutsideClick(true)
                .withCloseOnEsc(true)
                .withResizable(true)
                .withHeader("Feedback")
                .withContent(fbLayout)
                .open();
    }

    @Subscribe(id = "removeStoppedBtn", subject = "clickListener")
    public void onRemoveStoppedBtnClick1(final ClickEvent<JmixButton> event) {
        dialogs.createOptionDialog()
                .withHeader("Please confirm")
                .withText("Do you really want to remove the cancelled imports?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(e -> {
                                    for (CuratorTask task : curatorTaskService.getCancelledTasks()) {
                                            try {
                                                dataManager.remove(task);
                                            } catch (Exception ex) {
                                                log.error(ex.getMessage());
                                            }
                                    }
                                }),
                        new DialogAction(DialogAction.Type.NO)
                )
                .open();
    }
}
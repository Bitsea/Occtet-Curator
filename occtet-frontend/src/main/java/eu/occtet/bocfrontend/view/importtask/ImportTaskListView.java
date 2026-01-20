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

package eu.occtet.bocfrontend.view.importtask;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.occtet.bocfrontend.dao.ImportTaskRepository;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.ImportStatus;
import eu.occtet.bocfrontend.entity.ImportTask;
import eu.occtet.bocfrontend.importer.ImportManager;
import eu.occtet.bocfrontend.importer.Importer;
import eu.occtet.bocfrontend.service.ImportTaskService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
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

@Route(value = "importTask", layout = MainView.class)
@ViewController("ImportTask.list")
@ViewDescriptor(value = "importtask-list-view.xml", path = "importtask-list-view.xml")
@LookupComponent("importTaskDataGrid")
@DialogMode(width = "600", height = "800")
public class ImportTaskListView extends StandardListView<ImportTask> {


    private static final Logger log = LogManager.getLogger(ImportTaskListView.class);


    @ViewComponent
    private DataGrid<ImportTask> importTaskDataGrid;
    @ViewComponent
    private JmixButton showFeedback;
    @ViewComponent
    private JmixButton showConfig;
    @ViewComponent
    private HorizontalLayout availableImportBox;
    @ViewComponent
    private MessageBundle messageBundle;
    @ViewComponent("importTaskDataGrid.create")
    private CreateAction<ImportTask> importTaskDataGridCreate;
    @ViewComponent
    private CollectionContainer<ImportTask> importTaskDc;

    @Autowired
    private Dialogs dialogs;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Dialogs messageDialog;

    @Autowired
    private ImportTaskService importTaskService;
    @Autowired
    private DataManager dataManager;

    @Autowired
    private ImportManager importManager;
;
    @Autowired
    private ImportTaskRepository importTaskRepository;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {

        updateAvailableImportsBox();

        importTaskDc.setItems(importTaskRepository.findByStatus(ImportStatus.COMPLETED.getId()));

    }

    /**
     * loads preexisting imports to the grid
     */
    private void load(){
        List<ImportTask> importTasks = importTaskRepository.findAll();
        importTaskDc.setItems(importTasks);
    }


    private void updateAvailableImportsBox() {
        List<Importer> importers = importManager.getAvailableImports();
        availableImportBox.removeAll();
        importers.forEach(importer -> {
            if(!importer.getName().contains("Dumb") && !importer.getName().contains("Flexera_Report_Import"))
                availableImportBox.add(createImportIcon(importer + "Id", importer, false));
        });
    }




    private VerticalLayout createImportIcon(String buttonId, Importer importer, boolean isRunning) {
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
        importImageButton.getElement().setAttribute("src", "icons/" + importer.getName().replace(" ", "") + ".png");
        verticalLayout.add(importImageButton, h6);

        if (!isRunning) {
            importImageButton.setTitle(messageBundle.getMessage(importer.getName().replace(" ", "")));
            importImageButton.addClickListener(e -> {
                importManager.preselectNewImport(importer);
                importTaskDataGridCreate.execute();
            });
        } else {
            importImageButton.setTitle(messageBundle.getMessage("scanning"));
        }
        return verticalLayout;
    }

    @Subscribe("refreshTimer")
    public void onRefreshTimerTimerAction(final Timer.TimerActionEvent event) {
        List<ImportTask> importers = importTaskRepository.findByStatus(ImportStatus.COMPLETED.getId());
        importers.addAll(importTaskRepository.findByStatus(ImportStatus.STOPPED.getId()));
        importTaskDc.setItems(importers);
    }

    @Subscribe("importTaskDataGrid")
    public void onImportInitializerDataGridItemClick(final ItemClickEvent<ImportTask> event) {
        showFeedback.setEnabled(false);
        showConfig.setEnabled(false);
        if (event.getItem() != null) {
            showFeedback.setEnabled(event.getItem().getStatus().equals(ImportStatus.COMPLETED.name()));
            showConfig.setEnabled(!event.getItem().getImportConfiguration().isEmpty());
        }
    }

    @Subscribe(id = "showConfig", subject = "clickListener")
    public void onShowConfigClick1(final ClickEvent<JmixButton> event) {
        Set<ImportTask> selectedItems = importTaskDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty())
            return;
        StringBuilder sb = new StringBuilder();
        List<Configuration> configList = selectedItems.iterator().next().getImportConfiguration();
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

    @Subscribe(id = "stopImportBtn", subject = "clickListener")
    public void onStopClick(final ClickEvent<JmixButton> event) {
        Set<ImportTask> selectedItems = importTaskDataGrid.getSelectedItems();
        if (selectedItems == null || selectedItems.isEmpty())
            return;
        for (ImportTask importer : selectedItems) {
            importer.updateStatus(ImportStatus.STOPPED.getId());
            dataManager.save(importer);
        }
        load();
    }

    @Subscribe(id = "showFeedback", subject = "clickListener")
    public void onShowFeedbackClick(final ClickEvent<JmixButton> event) {
        Set<ImportTask> selectedItems = importTaskDataGrid.getSelectedItems();
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
                .withText("Do you really want to remove the stopped imports?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(e -> {
                                    for (ImportTask importer : importTaskService.getStoppedImporters()) {
                                            try {
                                                dataManager.remove(importer);
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
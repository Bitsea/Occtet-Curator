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


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.AppConfigurationRepository;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.TaskStatus;
import eu.occtet.bocfrontend.importer.TaskManager;
import eu.occtet.bocfrontend.importer.TaskParent;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfigKey;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfiguration;
import eu.occtet.bocfrontend.service.ConfigurationService;
import eu.occtet.bocfrontend.service.Utilities;
import eu.occtet.bocfrontend.view.configuration.ConfigurationDetailView;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.session.SessionData;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.data.grid.ContainerDataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Route(value = "curatorTask/:id", layout = MainView.class)
@ViewController("CuratorTask.detail")
@ViewDescriptor(value = "curatortask-detail-view.xml", path = "curatortask-detail-view.xml")
@EditedEntityContainer("curatorTaskDc")
public class ImportTaskDetailView extends StandardDetailView<CuratorTask> {

    private static final Logger log = LogManager.getLogger(ImportTaskDetailView.class);

    @ViewComponent
    private JmixImage<Object> iconPlaceholder;
    @ViewComponent
    private H3 curatorTaskField;
    @ViewComponent
    private TypedTextField<Object> importName;
    @ViewComponent
    private FormLayout.FormItem importNameField;
    @ViewComponent
    private DataGrid<Configuration> configurationsDataGrid;
    @ViewComponent
    private JmixButton editBtn;
    @ViewComponent
    private JmixButton removeBtn;
    @ViewComponent
    private JmixComboBox<Project> projectComboBox;
    @ViewComponent
    private CollectionContainer<Configuration> configurationsDc;

    @Autowired
    private AppConfigurationRepository appConfigurationRepository;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private Messages messages;

    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private Utilities utilities;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private SessionData sessionData;
    @Autowired
    private Notifications notifications;


    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        TaskParent importer= (TaskParent) sessionData.getAttribute("selectedImporter");
        if (importer!= null) {
            // Set information about the selected import
            curatorTaskField.setText(importer.getName().replaceAll("_"," "));
            importNameField.setVisible(false);
            getEditedEntity().setTaskType(importer.getName());

            // Set components to default values
            ArrayList<Project> listProject = new ArrayList<>();

            dataManager.load(Project.class).all().list().forEach(listProject::add);
            projectComboBox.setItems(listProject);
            projectComboBox.setItemLabelGenerator(Project::getProjectName);

            configurationsDataGrid.setItems(new ContainerDataGridItems<>(configurationsDc));

            log.info("View created for import: {}", importer.getName());

            // Set import icon
            iconPlaceholder.setHeight("80px");
            iconPlaceholder.getElement().setAttribute("src", "icons/" + importer.getName().replace(" ", "").toLowerCase() + ".png");

        }
    }

    @Subscribe("projectComboBox")
    public void onProjectValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        TaskParent importer= (TaskParent) sessionData.getAttribute("selectedImporter");
        log.debug("importer selected:{}", importer.getName());
        if (event.getValue() != null) {
            setConfigurations(importer);
            importNameField.setVisible(true);
            importName.setValue(event.getValue().getProjectName() + " " + LocalDate.now());
            getEditedEntity().setTaskName(importName.getValue());
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        log.debug("onBeforeSave action triggered");
        getEditedEntity().setTaskName(importName.getValue());
        CuratorTask curatorTask = getEditedEntity();
        TaskParent importer= (TaskParent) sessionData.getAttribute("selectedImporter");
        if (importer == null) {
            dialogs.createMessageDialog().withHeader("Error")
                    .withText(messages.getMessage(getClass(), "error_unknown_import") + ": " + importer.getName()).open();
            event.preventSave();
            return;
        }

        List<Configuration> configurations = new ArrayList<>(configurationsDc.getItems());

        // sanity check for required configs
        for (String key : importer.getRequiredConfigurationKeys()) {
            Optional<Configuration> configuration = configurations.stream().filter(c -> c.getName().equals(key)).findFirst();
            if (configuration.isEmpty() || StringUtils.isEmpty(configuration.get().getValue())) {
                dialogs.createMessageDialog().withHeader("Error")
                        .withText(messages.getMessage(getClass(), "required_config_missing") + ": " + key).open();
                event.preventSave();
                return;
            }
        }

        curatorTask.setProject(projectComboBox.getValue());
        curatorTask.setTaskConfiguration(configurations);
        curatorTask.setStatus(TaskStatus.IN_PROGRESS);

        // We do not need to handle saving manually which will be done by saveAction
        log.debug("Validation passed. Entities are prepared and saved");
        log.info("Process import task for import: {}", importer.getName());

        AppConfiguration globalBasePath =
                appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH).orElse(null);

        if (globalBasePath == null ||
                globalBasePath.getValue() == null ||
                globalBasePath.getValue().isBlank()) {

            notifications.create(
                            messages.getMessage(
                                    "eu.occtet.bocfrontend.view.project/Project.globalPathNotSet.WarningMsg"
                            )
                    )
                    .withPosition(Notification.Position.TOP_CENTER)
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .show();

            event.preventSave();
            return;
        }
        taskManager.startImport(importer, curatorTask);
    }



    @Subscribe("configurationsDataGrid")
    public void onConfigurationsDataGridItemClick(final ItemClickEvent<Configuration> event) {
        if (event.getItem() != null) {
            editBtn.setEnabled(true);
            TaskParent importer= (TaskParent) sessionData.getAttribute("selectedImporter");
            // Enable the remove button if the import does not require the configuration
            removeBtn.setEnabled(!importer.isConfigurationRequired(event.getItem().getName()));
        }
    }

    @Subscribe("configurationsDataGrid")
    public void onConfigurationsDataGridItemDoubleClick(final ItemDoubleClickEvent<Configuration> event) {
        openConfigurationDetailView(event.getItem());
    }

    @Subscribe(id = "editBtn", subject = "clickListener")
    public void onEditBtnClick(final ClickEvent<JmixButton> event) {
        Optional<Configuration> selectedConfig = configurationsDataGrid.getSelectedItems().stream().findFirst();
        selectedConfig.ifPresent(this::openConfigurationDetailView);
    }

    private void openConfigurationDetailView(Configuration configToEdit) {
        DialogWindow<ConfigurationDetailView> window =
                dialogWindows.view(this, ConfigurationDetailView.class).build();

        window.getView().setup(this.getEditedEntity());

        TaskParent importer= (TaskParent) sessionData.getAttribute("selectedImporter");
        log.info("Opening configuration detail view for: {}", configToEdit.getName());
        log.debug("Import task: {}", importer.getName());

        Configuration.Type typeOfConfiguration = importer.getTypeOfConfiguration(configToEdit.getName());

        window.addAfterCloseListener(afterCloseEvent -> {
            if (window.getView().getFinalResult() != null &&
                    window.getView().getFinalResult().toString().equals("Edit")) {
                // get the updated value from the child view and set it on the parent's entity
                Configuration updatedConfig = window.getView().getConfigPayload();
                // find item in parent container and update it
                configurationsDc.getItems().stream()
                        .filter(c -> c.equals(updatedConfig))
                        .findFirst()
                        .ifPresent(c -> {
                            if (typeOfConfiguration == Configuration.Type.FILE_UPLOAD) {
                                c.setUpload(updatedConfig.getUpload());
                            }
                            c.setValue(updatedConfig.getValue());
                        });
            }
        });

        window.getView().setEntityToEdit(configToEdit);
        window.open();
    }


    private void setConfigurations(TaskParent taskParent) {
        ArrayList<Configuration> configurations = new ArrayList<>();

        taskParent.getSupportedConfigurationKeys().forEach(k -> {
            String defaultConfigurationValue = taskParent.getDefaultConfigurationValue(k);
            configurations.add(configurationService.create(k, defaultConfigurationValue));
        });

        configurationsDc.setItems(configurations);
    }

    /**
     * Provides a renderer for the "name" column in the configurations data grid.
     * The renderer applies custom casing to the configuration name before displaying it.
     */
    @Supply(to = "configurationsDataGrid.name", subject = "renderer")
    private Renderer<Configuration> configurationDataGridNameRenderer(){
        return new TextRenderer<>(config -> utilities.handleCasing(config.getName()));
    }
}

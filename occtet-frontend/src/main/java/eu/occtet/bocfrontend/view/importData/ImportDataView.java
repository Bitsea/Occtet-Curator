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

package eu.occtet.bocfrontend.view.importData;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.engine.ScannerManager;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.scanner.Scanner;
import eu.occtet.bocfrontend.service.ConfigurationService;
import eu.occtet.bocfrontend.service.ScannerInitializerService;
import eu.occtet.bocfrontend.service.Utilities;
import eu.occtet.bocfrontend.view.configuration.ConfigurationDetailView;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.grid.ContainerDataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route(value = "import", layout = MainView.class)
@ViewController(id = "importData")
@ViewDescriptor(path = "import-data-view.xml")
public class ImportDataView extends StandardView{

    private static final Logger log = LogManager.getLogger(ImportDataView.class);

    @ViewComponent
    private DataGrid<Configuration> configurationsDataGrid;
    @ViewComponent
    private JmixButton editBtn;
    @ViewComponent
    private JmixButton removeBtn;
    @ViewComponent
    private JmixComboBox<Project> projectComboBox;
    @ViewComponent
    private JmixComboBox<String> scannerComboBox;
    @ViewComponent
    private CollectionContainer<Configuration> configurationsDc;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private Messages messages;
    @Autowired
    private ScannerManager scannerManager;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private Utilities utilities;
    @Autowired
    private ScannerInitializerService scannerInitializerService;

    Scanner scanner;
    String scannerName;
    private ScannerInitializer scannerInitializer;
    @Autowired
    private Notifications notifications;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {

        List<String> listScanner = scannerManager.getAvailableScanners();
        scannerComboBox.setItems(listScanner);

        ArrayList<Project> listProject = new ArrayList<>();
        dataManager.load(Project.class).all().list().forEach(listProject::add);
        projectComboBox.setItems(listProject);
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
    }

    @Subscribe("scannerComboBox")
    public void onScannerFieldValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>, String> event) {

        if (event != null) {
            if(projectComboBox.getValue() != null){
                scannerName = scannerComboBox.getValue();
                scanner = scannerManager.findScannerByName(scannerName);
                //InventoryItem will change to project
                scannerInitializer = scannerInitializerService.createScannerInitializer(null,scannerName);
                setConfigurations(scanner);
                configurationsDataGrid.setItems(new ContainerDataGridItems<>(configurationsDc));
            }
        }
    }

    @Subscribe("startImportButton")
    public void startImport(ClickEvent<Button> event) {

        if(scanner != null){
        List<Configuration> configurations = new ArrayList<>(configurationsDc.getItems());

            // sanity check for required configs
            for (String key : scanner.getRequiredConfigurationKeys()) {
                Optional<Configuration> configuration = configurations.stream().filter(c -> c.getName().equals(key)).findFirst();
                if (configuration.isEmpty() || StringUtils.isEmpty(configuration.get().getValue())) {
                    dialogs.createMessageDialog().withHeader("Error")
                            .withText(messages.getMessage(getClass(), "required_config_missing") + ": " + key).open();
                    return;
                }
            }

            scannerInitializer.setScannerConfiguration(configurations);
            scannerInitializer.updateStatus(ScannerInitializerStatus.IN_PROGRESS.getId());

            // We do not need to handle saving manually which will be done by saveAction
            log.debug("Validation passed. Entities are prepared and saved");
            log.info("Enqueuing scanner task for scanner: {}", scannerInitializer.getScanner());
            scannerManager.enqueueScannerInitializer(scannerInitializer);
        }
        importInformation("Something went wrong, please check your input",NotificationVariant.LUMO_ERROR);
    }


    @Subscribe("configurationsDataGrid")
    public void onConfigurationsDataGridItemClick(final ItemClickEvent<Configuration> event) {
        if (event.getItem() != null) {
            editBtn.setEnabled(true);
            // Enable the remove button if the scanner does not require the configuration
            removeBtn.setEnabled(!scanner.isConfigurationRequired(event.getItem().getName()));
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

        window.getView().setup(scannerInitializer);

        log.info("Opening configuration detail view for: {}", configToEdit.getName());
        log.debug("Scanner task: {}", scannerInitializer.getScanner());

        Configuration.Type typeOfConfiguration = scanner.getTypeOfConfiguration(configToEdit.getName());

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

    private void setConfigurations(Scanner scanner) {
        ArrayList<Configuration> configurations = new ArrayList<>();

        scanner.getSupportedConfigurationKeys().forEach(k -> {
            //InventoryItem will change to project
            String defaultConfigurationValue = scanner.getDefaultConfigurationValue(k, null);
            configurations.add(configurationService.create(k, defaultConfigurationValue));
        });

        configurationsDc.setItems(configurations);
    }

    private void importInformation(String message, NotificationVariant variant){
        notifications.create(message)
                .withPosition(Notification.Position.MIDDLE)
                .withDuration(3000)
                .withThemeVariant(variant)
                .show();
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

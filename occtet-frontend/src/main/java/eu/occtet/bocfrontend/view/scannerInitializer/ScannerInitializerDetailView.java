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

package eu.occtet.bocfrontend.view.scannerInitializer;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.scanner.Scanner;
import eu.occtet.bocfrontend.engine.ScannerManager;
import eu.occtet.bocfrontend.service.ConfigurationService;
import eu.occtet.bocfrontend.service.InventoryItemService;
import eu.occtet.bocfrontend.service.Utilities;
import eu.occtet.bocfrontend.view.configuration.ConfigurationDetailView;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.data.grid.ContainerDataGridItems;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;


@Route(value = "scannerInitializer/:id", layout = MainView.class)
@ViewController("ScannerInitializer.detail")
@ViewDescriptor("scannerInitializer-detail-view.xml")
@EditedEntityContainer("scannerInitializerDc")
public class ScannerInitializerDetailView extends StandardDetailView<ScannerInitializer> {

    private static final Logger log = LogManager.getLogger(ScannerInitializerDetailView.class);

    @ViewComponent
    private JmixImage<Object> iconPlaceholder;
    @ViewComponent
    private H3 scannerField;
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
    private InventoryItemService inventoryItemService;

    Scanner scanner;
    String scannerName;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        scannerName = scannerManager.getPreselectedScanner();

        if (scannerName!= null) {
            // Set information about the selected scanner
            scannerField.setText(scannerName.replaceAll("_"," "));
            getEditedEntity().setScanner(scannerName);

            // Set components to default values
            ArrayList<Project> listProject = new ArrayList<>();

            dataManager.load(Project.class).all().list().forEach(listProject::add);
            projectComboBox.setItems(listProject);
            projectComboBox.setItemLabelGenerator(Project::getProjectName);

            configurationsDataGrid.setItems(new ContainerDataGridItems<>(configurationsDc));

            scanner = scannerManager.findScannerByName(scannerName);
            log.info("View created for scanner: {}", scanner);

            // Set scanner icon
            iconPlaceholder.setHeight("80px");
            iconPlaceholder.getElement().setAttribute("src", "icons/" + scannerName.replace(" ", "") + ".png");

        }
    }

    @Subscribe("projectComboBox")
    public void onProjectValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (event.getValue() != null) {
            Project chosenProject = event.getValue();
            List<InventoryItem> inventoryItemList = new ArrayList<>(inventoryItemService.findInventoryItemsOfProject(chosenProject));

            inventoryItemList.removeIf(in -> in.getParent() != null );
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        log.debug("onBeforeSave action triggered");

        ScannerInitializer scannerInitializer = getEditedEntity();

        if (scanner == null) {
            dialogs.createMessageDialog().withHeader("Error")
                    .withText(messages.getMessage(getClass(), "error_unknown_scanner") + ": " + scannerInitializer.getScanner()).open();
            event.preventSave();
            return;
        }

        List<Configuration> configurations = new ArrayList<>(configurationsDc.getItems());

        // sanity check for required configs
        for (String key : scanner.getRequiredConfigurationKeys()) {
            Optional<Configuration> configuration = configurations.stream().filter(c -> c.getName().equals(key)).findFirst();
            if (configuration.isEmpty() || StringUtils.isEmpty(configuration.get().getValue())) {
                dialogs.createMessageDialog().withHeader("Error")
                        .withText(messages.getMessage(getClass(), "required_config_missing") + ": " + key).open();
                event.preventSave();
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

        window.getView().setup(this.getEditedEntity());

        log.info("Opening configuration detail view for: {}", configToEdit.getName());
        log.debug("Scanner task: {}", this.getEditedEntity().getScanner());

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
            String defaultConfigurationValue = scanner.getDefaultConfigurationValue(k);
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

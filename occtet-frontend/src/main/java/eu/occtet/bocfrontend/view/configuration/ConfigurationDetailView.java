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

package eu.occtet.bocfrontend.view.configuration;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.importer.ImportManager;
import eu.occtet.bocfrontend.service.ConfigurationService;
import eu.occtet.bocfrontend.validator.NumericValidator;
import eu.occtet.bocfrontend.validator.PathValidator;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


@Route(value = "configurations/:id", layout = MainView.class)
@ViewController("Configuration.detail")
@ViewDescriptor("configuration-detail-view.xml")
@EditedEntityContainer("configurationDc")
public class ConfigurationDetailView extends StandardDetailView<Configuration> {

    private static final Logger log = LogManager.getLogger(ConfigurationDetailView.class);

    @ViewComponent
    private InstanceContainer<Configuration> configurationDc;
    @ViewComponent
    private TypedTextField<String> nameField;
    @ViewComponent
    private TypedTextField<String> valueField;
    @ViewComponent
    private FileUploadField uploadField;
    @ViewComponent
    private JmixCheckbox booleanField;

    @Autowired
    protected DataManager dataManager;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private ImportManager importManager;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private PathValidator pathValidator;
    @Autowired
    private NumericValidator numericValidator;

    // Importer names for which the configuration requires
    private final String FLEXERA = "Flexera_Report_Import";
    private final String SPDX = "SPDX_Import";

    private Configuration configPayload;
    private CuratorTask curatorTask;
    private result finalResult;

    public enum result {Cancel, Edit}


    public void setup(CuratorTask sI) {
        this.curatorTask = sI;
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        Configuration entity = getEditedEntity();
        this.nameField.setValue(entity.getName());

        // Initialize booleanField if the type is BOOLEAN
        if (configurationService.getTypeOfConfiguration(entity.getName(), curatorTask) == Configuration.Type.BOOLEAN) {
            booleanField.setValue("true".equals(entity.getValue()));
            booleanField.setVisible(true);
            valueField.setVisible(false);
            uploadField.setVisible(false);
        }
    }

    @Subscribe
    public void onInit(final InitEvent event) {
    }

    @Subscribe("nameField")
    public void onNameFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>, String> event) {
        // Combobox value changed. Check for type of config and adjust form accordingly.
        String key = event.getValue();
        log.debug("check key {}", key);
        updateValueFieldVisibility(key);
    }

    private void updateValueFieldVisibility(String key) {
        Configuration.Type typeOfConfiguration = configurationService.getTypeOfConfiguration(key, curatorTask);
        switch (typeOfConfiguration) {
            case FILE_UPLOAD:
                setupForFileUpload();
                break;
            case BASE_PATH:
                setupForBasePath();
                break;
            case NUMERIC:
                setupForNumeric();
                break;
            case STRING:
                uploadField.setVisible(false);
                valueField.setVisible(true);
                booleanField.setVisible(false);
                break;
            case BOOLEAN:
                uploadField.setVisible(false);
                valueField.setVisible(false);
                booleanField.setVisible(true);
                break;
        }
    }

    /**
     * Handles the "Save and Close" button click action.
     * This method validates input fields, processes the configuration object using the service,
     * and appropriately handles success or failure outcomes. If an exception occurs during the
     * process, a message dialog is displayed to inform the user.
     *
     * @param event the click event triggered by the "Save and Close" button
     */
    @Subscribe(id = "saveAndCloseBtn", subject = "clickListener")
    public void onSaveAndCloseBtnClick(final ClickEvent<JmixButton> event) {
        log.debug("Save and close clicked for configuration {}", configurationDc.getItem().getId());
        try {
            // Validate the input first before proceeding
            valueField.executeValidators();

            // Processing
            configPayload = configurationDc.getItem();
            boolean res = configurationService.handleConfig(
                    configPayload,
                    nameField.getValue(),
                    uploadField.getValue(),
                    uploadField.getUploadedFileName(),
                    booleanField.getValue(),
                    curatorTask
            );

            if (res) {
                log.debug("Configuration {} successfully handled", configPayload.getName());
                finalResult = result.Edit;
                close(StandardOutcome.DISCARD);
            } else { // prevent saving if config could not be handled
                log.error("Configuration {} could not be handled", configPayload.getName());
                dialogs.createMessageDialog().withHeader("Error")
                        .withText("Configuration could not be handled. Please check the entered values.")
                        .withCloseOnEsc(false)
                        .withCloseOnOutsideClick(false)
                        .open();
                finalResult = result.Cancel;
            }
        }catch (ValidationException e) {
            log.warn("Validation failed", e);
            // User will get notified about the error automatically
        } catch (Exception e) {
            // catch other exceptions just in case
            log.error("An unexpected error occurred:", e);
            dialogs.createMessageDialog().withHeader("Error")
                    .withText("An unexpected error occurred. Please check the entered values.")
                    .withCloseOnEsc(false)
                    .withCloseOnOutsideClick(false)
                    .open();
        }
    }

    @Subscribe(id = "closeBtn", subject = "clickListener")
    public void onCloseButtonClick1(final ClickEvent<JmixButton> event) {
        finalResult = result.Cancel;
        close(StandardOutcome.DISCARD);
    }

    public Configuration getConfigPayload() {
        return configPayload;
    }

    public result getFinalResult() {
        return finalResult;
    }

    // Setup methods for different configuration types
    private void setupForFileUpload() {
        log.debug("Setup for file upload");
        uploadField.setVisible(true);
        valueField.setVisible(false);
        booleanField.setVisible(false);
        int maxFileSizeInBytes = 70 * 1024 * 1024; // 70MB
        uploadField.setMaxFileSize(maxFileSizeInBytes);

        // Here the setup might vary depending on the import
        if (curatorTask.getTaskName().equals(FLEXERA)) {
            uploadField.setAcceptedFileTypes(".xlsx");
            uploadField.setHelperText("Upload a Flexera report in Excel format (.xlsx, max 70 MB)");
        }
    }

    private void setupForBasePath() {
        log.debug("Setup for base path");
        uploadField.setVisible(false);
        valueField.setVisible(true);
        booleanField.setVisible(false);
        valueField.addValidator(pathValidator);
    }

    private void setupForNumeric(){
        uploadField.setVisible(false);
        valueField.setVisible(true);
        booleanField.setVisible(false);
        valueField.addValidator(numericValidator);
    }
}
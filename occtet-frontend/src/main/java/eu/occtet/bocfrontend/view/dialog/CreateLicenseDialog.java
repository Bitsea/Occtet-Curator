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

package eu.occtet.bocfrontend.view.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.LicenseService;
import io.jmix.core.Messages;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


@ViewController("createLicenseDialog")
@ViewDescriptor("create-license-dialog.xml")
@DialogMode(width = "70%", height = "70%")
public class CreateLicenseDialog extends AbstractCreateContentDialog<SoftwareComponent>{

    private static final Logger log = LogManager.getLogger(CreateLicenseDialog.class);

    private SoftwareComponent softwareComponent;
    private License createdLicense;

    @ViewComponent
    private TextField licenseNameField;

    @ViewComponent
    private TextField licenseTypeField;

    @ViewComponent
    private TextField detailsUrlField;

    @ViewComponent
    private TextField priorityField;

    @ViewComponent
    private Checkbox isSpdxField;

    @ViewComponent
    private Checkbox isCuratedField;

    @ViewComponent
    private Checkbox isModifiedField;

    @ViewComponent
    private TextArea licenseTextField;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private Notifications notifications;

    @Autowired
    private Messages messages;


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){
        isCuratedField.setValue(false);
        isModifiedField.setValue(false);
        isSpdxField.setValue(false);
    }

    @Override
    public void setAvailableContent(SoftwareComponent content) {
        this.softwareComponent = content;
    }

    @Override
    @Subscribe("addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {
        String priority = priorityField.getValue();
        String licenseType = licenseTypeField.getValue();
        String licenseText = licenseTextField.getValue();
        String licenseName = licenseNameField.getValue();
        String detailsUrl = detailsUrlField.getValue();

        if (checkInput(priority, licenseType, licenseText, licenseName, detailsUrl)) {

            try {
                this.createdLicense = licenseService.createLicense(
                        Integer.valueOf(priority),
                        licenseType,
                        licenseText,
                        licenseName,
                        detailsUrl,
                        isModifiedField.getValue(),
                        isCuratedField.getValue(),
                        isSpdxField.getValue()
                );

                log.debug("Instantiated new license record: {}", createdLicense.getLicenseName());
                close(StandardOutcome.SAVE);

            } catch (IllegalArgumentException e) {
                notifications.create(messages.formatMessage(getClass(), "duplicate.error", licenseType))
                        .withDuration(4000)
                        .withPosition(Notification.Position.TOP_CENTER)
                        .withThemeVariant(NotificationVariant.LUMO_WARNING)
                        .show();
            }

        } else {
            notifications.create(messages.getMessage("unknown.error"))
                    .withDuration(3000)
                    .withPosition(Notification.Position.TOP_CENTER)
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .show();
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event){
        cancelButton(event);
    }

    private boolean checkInput(String priority, String licenseType, String licenseText, String licenseName, String detailsUrl){
        return priority != null && !priority.trim().isEmpty() &&
                licenseType != null && !licenseType.trim().isEmpty() &&
                licenseText != null && !licenseText.trim().isEmpty() &&
                licenseName != null && !licenseName.trim().isEmpty() &&
                detailsUrl != null && !detailsUrl.trim().isEmpty();
    }

    public License getCreatedLicense() {
        return createdLicense;
    }
}

/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.view.dialog;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.CopyrightService;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

@ViewController(id = "CreateCopyrightForSoftwareComponentDialog")
@ViewDescriptor(path = "create-copyright-for-software-component-dialog.xml")
@DialogMode(width = "70%", height = "70%")
public class CreateCopyrightForSoftwareComponentDialog extends AbstractCreateContentDialog<SoftwareComponent> {

    private static final Logger log = LogManager.getLogger(CreateCopyrightForSoftwareComponentDialog.class);

    private SoftwareComponent softwareComponent;
    private Copyright createdCopyright;

    @ViewComponent
    private TextField copyrightNameField;

    @ViewComponent
    private Checkbox isGarbageField;

    @ViewComponent
    private Checkbox isCuratedField;

    @Autowired
    private CopyrightService copyrightService;
     @Autowired
    private Notifications notifications;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){
        isGarbageField.setValue(false);
        isCuratedField.setValue(false);
    }

    /**
     * Loads the target software component into the dialog context, ensuring the existing
     * copyright collection is fetched for potential validation or reference.
     *
     * @param content the base software component entity passed from the parent view.
     */
    @Override
    public void setAvailableContent(SoftwareComponent content) {
        this.softwareComponent = content;
    }

    /**
     * Validates user input and instantiates a new general copyright entity via the service layer.
     * Passes an empty set for file locations to explicitly designate this as a component-level property.
     *
     * @param event the click event originating from the confirmation button.
     */
    @Override
    @Subscribe("addCopyrightButton")
    public void addContentButton(ClickEvent<Button> event) {
        String copyrightName = copyrightNameField.getValue();

        if (checkInput(copyrightName)) {
            this.createdCopyright = copyrightService.createAndSaveCopyright(
                    copyrightName,
                    Collections.emptySet(),
                    isCuratedField.getValue(),
                    isGarbageField.getValue()
            );

            log.debug("Created component-level copyright: {}", createdCopyright.getCopyrightText());
            close(StandardOutcome.SAVE);
        } else {
            notifications.create("Something went wrong, please check your input")
                    .withDuration(3000)
                    .withPosition(Notification.Position.BOTTOM_CENTER.TOP_CENTER)
                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
                    .show();
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelCopyright(ClickEvent<Button> event){
        cancelButton(event);
    }

    private boolean checkInput(String name){
        return name != null && !name.isEmpty();
    }

    /**
     * Exposes the successfully instantiated copyright so the parent view can manage
     * the entity relationship within its own data context.
     *
     * @return the newly created copyright instance, or null if creation failed.
     */
    public Copyright getCreatedCopyright() {
        return createdCopyright;
    }
}
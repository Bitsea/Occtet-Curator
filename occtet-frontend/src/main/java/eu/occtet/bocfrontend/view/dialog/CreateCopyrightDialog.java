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
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.CopyrightService;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.multiselectcombobox.JmixMultiSelectComboBox;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@ViewController("createCopyrightDialog")
@ViewDescriptor("create-copyright-dialog.xml")
@DialogMode(width = "70%", height = "70%")
public class CreateCopyrightDialog extends AbstractCreateContentDialog<InventoryItem>{

    private static final Logger log = LogManager.getLogger(CreateCopyrightDialog.class);

    private InventoryItem inventoryItem;

    private SoftwareComponent softwareComponent;

    @ViewComponent
    private TextField copyrightNameField;

    @ViewComponent
    private JmixMultiSelectComboBox<File> filePathComboBox;

    @ViewComponent
    private Checkbox isGarbageField;

    @ViewComponent
    private Checkbox isCuratedField;

    @Autowired
    private CopyrightService copyrightService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private FileRepository fileRepository;

    private Copyright createdCopyright;


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){
        isGarbageField.setValue(false);
        isCuratedField.setValue(false);

        filePathComboBox.setItems(fileRepository.findByProject(inventoryItem.getProject()));
        filePathComboBox.setItemLabelGenerator(File::getProjectPath);
    }

    @Override
    public void setAvailableContent(InventoryItem content) {
        this.inventoryItem = content;
    }

    @Override
    @Subscribe("addCopyrightButton")
    public void addContentButton(ClickEvent<Button> event) {

        String copyrightName = copyrightNameField.getValue();
        Set<File> location = filePathComboBox.getValue();

        if (checkInput(copyrightName, location)) {
            this.createdCopyright = copyrightService.createAndSaveCopyright(
                    copyrightName,
                    Set.copyOf(location),
                    isCuratedField.getValue(),
                    isGarbageField.getValue()
            );

            log.debug("Created copyright {}", createdCopyright.getCopyrightText());

            close(StandardOutcome.SAVE);
        } else {
            notifications.create("Something went wrong, please check your input")
                    .withDuration(3000)
                    .withPosition(Notification.Position.TOP_CENTER)
                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
                    .show();
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelCopyright(ClickEvent<Button> event){cancelButton(event);}

    private boolean checkInput(String name, Set<File> files){

        return !name.isEmpty() && files != null;
    }

    public Copyright getCreatedCopyright() {
        return createdCopyright;
    }
}

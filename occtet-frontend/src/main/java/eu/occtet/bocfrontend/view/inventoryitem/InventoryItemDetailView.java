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

package eu.occtet.bocfrontend.view.inventoryitem;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.InventoryItemService;
import eu.occtet.bocfrontend.service.SoftwareComponentService;
import eu.occtet.bocfrontend.view.dialog.AddCopyrightDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


@Route(value = "inventory-items/:id", layout = MainView.class)
@ViewController(id = "InventoryItem.detail")
@ViewDescriptor(path = "inventory-item-detail-view.xml")
@EditedEntityContainer("inventoryItemDc")
public class InventoryItemDetailView extends StandardDetailView<InventoryItem> {

    private static final Logger log = LogManager.getLogger(InventoryItemDetailView.class);

    @Autowired
    private ViewNavigators viewNavigator;
    @Autowired
    private DialogWindows dialogWindow;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @Autowired
    private InventoryItemService inventoryItemService;

    @ViewComponent
    private CollectionContainer<Copyright> copyrightsDc;
    @ViewComponent
    private JmixComboBox<Project> projectField;
    @ViewComponent
    private JmixComboBox<SoftwareComponent> softwareComponentField;

    private InventoryItem inventoryItem;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        inventoryItem = getEditedEntity();
        log.debug("Inventory Detail View opened for inventory Item: {} ", inventoryItem.getInventoryName());
        projectField.setItems(projectRepository.findAll());
        projectField.setValue(inventoryItem.getProject());
        projectField.setItemLabelGenerator(Project::getProjectName);
        softwareComponentField.setItems(softwareComponentRepository.findAll());
        softwareComponentField.setValue(inventoryItem.getSoftwareComponent());
        softwareComponentField.setItemLabelGenerator(sc -> sc.getName()+" "+sc.getVersion());
        copyrightsDc.setItems(inventoryItem.getCopyrights());
    }

    @Subscribe("projectField")
    public void onProjectFieldChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        getEditedEntity().setProject(event.getValue());
    }
    @Subscribe("softwareComponentField")
    public void onSoftwareComponentFieldChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<SoftwareComponent>,
            SoftwareComponent> event) {
        getEditedEntity().setSoftwareComponent(event.getValue());
    }

    @Subscribe(id = "parentButton")
    public void showParentDetails(ClickEvent<Button> event) {
        if (inventoryItem != null && inventoryItem.getParent() != null) {
            viewNavigator.detailView(this, InventoryItem.class)
                    .editEntity(inventoryItem.getParent())
                    .navigate();
        }
    }

    @Subscribe(id = "softwareComponentButton")
    public void showSoftwareComponentDetails(ClickEvent<Button> event) {
        SoftwareComponent sc = inventoryItem.getSoftwareComponent();
        if (sc != null) {
            viewNavigator.detailView(this, SoftwareComponent.class)
                    .editEntity(sc)
                    .navigate();
        }
    }

    @Subscribe(id = "addCopyrightButton")
    public void addCopyright(ClickEvent<Button> event) {
        InventoryItem item = getEditedEntity();
        DialogWindow<AddCopyrightDialog> window = dialogWindow.view(this, AddCopyrightDialog.class).build();
        window.getView().setAvailableContent(inventoryItem);
        window.open();
        afterAddContentAction(window, item);
    }

    private void afterAddContentAction(DialogWindow<?> window, InventoryItem item) {
        window.addAfterCloseListener(close -> {
            if (close.closedWith(StandardOutcome.CLOSE)) {
                copyrightsDc.setItems(item.getCopyrights());
            }
        });
    }

    @Subscribe("saveAndCloseButton")
    public void checkInventoryItem(ClickEvent<Button> event){
        inventoryItemService.controlInventoryItem(inventoryItem);
    }
}
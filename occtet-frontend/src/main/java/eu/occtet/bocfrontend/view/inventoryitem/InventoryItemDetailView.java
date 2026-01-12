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

package eu.occtet.bocfrontend.view.inventoryitem;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.dao.SuggestionRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.service.InventoryItemService;
import eu.occtet.bocfrontend.view.audit.fragment.AutocompleteField;
import eu.occtet.bocfrontend.view.dialog.AddCopyrightDialog;
import eu.occtet.bocfrontend.view.dialog.AddLicenseDialog;
import eu.occtet.bocfrontend.view.dialog.CreateLicenseDialog;
import eu.occtet.bocfrontend.view.license.LicenseDetailView;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.softwareComponent.SoftwareComponentDetailView;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Route(value = "inventory-items/:id", layout = MainView.class)
@ViewController(id = "InventoryItem.detail")
@ViewDescriptor(path = "inventory-item-detail-view.xml")
@EditedEntityContainer("inventoryItemDc")
public class InventoryItemDetailView extends StandardDetailView<InventoryItem> {

    private static final Logger log = LogManager.getLogger(InventoryItemDetailView.class);

    @Autowired
    private DialogWindows dialogWindows;
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
    @ViewComponent
    private JmixComboBox<InventoryItem> parentField;
    @ViewComponent
    private CollectionContainer<License> licenseDc;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private Messages messages;
    @ViewComponent
    private VerticalLayout auditNotesText;
    @ViewComponent
    private VerticalLayout inventoryNameField;
    @Autowired
    private SuggestionRepository suggestionRepository;

    private InventoryItem inventoryItem;
    private List<String> suggestions;
    private AutocompleteField autocompleteAuditNotes;
    private AutocompleteField autocompleteInventoryName;

    @Subscribe
    public void onInit(InitEvent event) {
        projectField.setItems(projectRepository.findAll());
        projectField.setItemLabelGenerator(Project::getProjectName);
        parentField.setItems(inventoryItemRepository.findAll());
        parentField.setItemLabelGenerator(InventoryItem::getInventoryName);
        softwareComponentField.setItems(softwareComponentRepository.findAll());
        softwareComponentField.setItemLabelGenerator(sc -> sc.getName() + " " + sc.getVersion());
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        inventoryItem = getEditedEntity();

        log.debug("Inventory Detail View opened for inventory Item: {} ", inventoryItem.getInventoryName());
        projectField.setValue(inventoryItem.getProject());
        if(inventoryItem.getParent() != null) {
            parentField.setValue(inventoryItem.getParent());
        }

        if(inventoryItem.getSoftwareComponent() != null) {
            softwareComponentField.setValue(inventoryItem.getSoftwareComponent());
            updateLicenses(inventoryItem.getSoftwareComponent(), licenseDc);
            if (inventoryItem.getSoftwareComponent().getCopyrights() != null)
                copyrightsDc.setItems(inventoryItem.getSoftwareComponent().getCopyrights());
        }

        loadSuggestions("auditNotes");
        autocompleteAuditNotes = new AutocompleteField( messages.getMessage(getClass(), "auditNotes"));
        autocompleteAuditNotes.setOptions(suggestions);
        autocompleteAuditNotes.initializeField();
        if(inventoryItem.getExternalNotes()!=null)
            autocompleteAuditNotes.setValue(inventoryItem.getExternalNotes());
        auditNotesText.add(autocompleteAuditNotes);

        loadSuggestions("inventoryNames");
        autocompleteInventoryName= new AutocompleteField(messages.getMessage(getClass(), "inventoryName"));
        autocompleteInventoryName.setOptions(suggestions);
        autocompleteInventoryName.initializeField();
        autocompleteInventoryName.setValue(inventoryItem.getInventoryName());
        inventoryNameField.add(autocompleteInventoryName);

    }

    /**
     * loads the suggestions strings from db
     * for autocomplete fields
     */
    private void loadSuggestions(String context){

        suggestions= suggestionRepository.findByContext(context).stream().map(Suggestion::getSentence)
                .filter(Objects::nonNull).collect(Collectors.toList());

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
        log.debug("parent {}", inventoryItem.getParent());
        if (parentField.getValue() != null) {
            DialogWindow<InventoryItemDetailView> dialog = dialogWindows.detail(this, InventoryItem.class)
                    .withViewClass(InventoryItemDetailView.class)
                    .editEntity(parentField.getValue()).build();
            dialog.setHeight("90%");
            dialog.setWidth("90%");
            dialog.open();
        }
    }

    @Subscribe(id = "softwareComponentButton")
    public void showSoftwareComponentDetails(ClickEvent<Button> event) {
        SoftwareComponent sc = softwareComponentField.getValue();
        if (sc != null) {
            DialogWindow<SoftwareComponentDetailView> dialog = dialogWindows.detail(this, SoftwareComponent.class)
                    .withViewClass(SoftwareComponentDetailView.class)
                    .editEntity(sc).build();
            dialog.setHeight("90%");
            dialog.setWidth("90%");
            dialog.open();
        }
    }

    @Subscribe(id = "addCopyrightButton")
    public void addCopyright(ClickEvent<Button> event) {
        InventoryItem item = getEditedEntity();
        DialogWindow<AddCopyrightDialog> window = dialogWindows.view(this, AddCopyrightDialog.class).build();
        window.getView().setAvailableContent(inventoryItem);
        window.open();
        afterAddContentAction(window, item);
    }

    private void afterAddContentAction(DialogWindow<?> window, InventoryItem item) {
        window.addAfterCloseListener(close -> {
            if (close.closedWith(StandardOutcome.CLOSE)) {
                copyrightsDc.setItems(item.getSoftwareComponent().getCopyrights());
            }
        });
    }

    /**
     * Handles the action of adding a license. When triggered, this method opens a dialog
     * allowing the user to add a license to the software component. After the dialog
     * is closed, the licenses associated with the software component are updated.
     *
     * @param event the event object triggered by the click action on the dropdown button item
     */
    @Subscribe(id = "editLicense.addLicense")
    public void addLicense(DropdownButtonItem.ClickEvent event) {
        if (softwareComponentField.getValue() != null) {
            DialogWindow<AddLicenseDialog> window = dialogWindows.view(this, AddLicenseDialog.class).build();
            window.getView().setAvailableContent(softwareComponentField.getValue());
            window.open();
            window.addAfterCloseListener(close ->
                    updateLicenses(softwareComponentField.getValue(), licenseDc));
        }
    }

    @Subscribe("saveAndCloseButton")
    public void checkInventoryItem(ClickEvent<Button> event){
        inventoryItemService.controlInventoryItem(inventoryItem);
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        inventoryItem.setExternalNotes(autocompleteAuditNotes.getValue());
        inventoryItem.setInventoryName(autocompleteInventoryName.getValue());
    }

    /**
     * Handles the creation and addition of a new license via a dialog window. This method
     * opens a dialog to input license details for the associated software component. After
     * the dialog is closed, it updates the licenses linked to the software component.
     *
     * @param event the click event triggered by the user interacting with the dropdown button item
     */
    @Subscribe(id = "editLicense.createLicense")
    public void createAndAddLicense(DropdownButtonItem.ClickEvent event) {
        if (softwareComponentField.getValue() != null) {
            DialogWindow<CreateLicenseDialog> window = dialogWindows.view(this, CreateLicenseDialog.class).build();
            window.getView().setAvailableContent(softwareComponentField.getValue());
            window.open();
            window.addAfterCloseListener(close ->
                    updateLicenses(softwareComponentField.getValue(), licenseDc));
        }

    }

    private void updateLicenses(SoftwareComponent softwareComponent, CollectionContainer<License> container) {
        if (softwareComponent != null) {
            container.setItems(softwareComponent.getLicenses());
        } else {
            container.setItems(new ArrayList<>());
        }
    }

    /**
     * Handles the double-click event on an item in the licenses data grid.
     * Opens a dialog window to display and edit details of the selected license.
     *
     * @param event the event triggered when an item in the licenses data grid is double-clicked.
     *              Contains the license item that was clicked.
     */
    @Subscribe("licensesDataGrid")
    public void onLicensesDataGridItemDoubleClick(final ItemDoubleClickEvent<License> event) {
        DialogWindow<LicenseDetailView> window = dialogWindows.view(this, LicenseDetailView.class).build();
        window.getView().setEntityToEdit(event.getItem());
        window.open();
    }


}
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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.dao.SuggestionRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.Suggestion;
import eu.occtet.bocfrontend.service.InventoryItemService;
import eu.occtet.bocfrontend.view.audit.fragment.AutocompleteField;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.softwareComponent.SoftwareComponentDetailView;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

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
    private JmixComboBox<Project> projectField;
    @ViewComponent
    private JmixComboBox<SoftwareComponent> softwareComponentField;
    @ViewComponent
    private JmixComboBox<InventoryItem> parentField;
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
        projectField.setItemLabelGenerator(p -> p.getProjectName() + " - " + p.getVersion());
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
            parentField.setReadOnly(true);
        }
        if(inventoryItem.getSoftwareComponent() != null){
            softwareComponentField.setValue(inventoryItem.getSoftwareComponent());
            softwareComponentField.setReadOnly(true);
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

    @Subscribe("saveAndCloseButton")
    public void checkInventoryItem(ClickEvent<Button> event){
        inventoryItemService.controlInventoryItem(inventoryItem);
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        inventoryItem.setExternalNotes(autocompleteAuditNotes.getValue());
        inventoryItem.setInventoryName(autocompleteInventoryName.getValue());
    }
}
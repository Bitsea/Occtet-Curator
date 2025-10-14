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
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.InventoryItemService;
import eu.occtet.bocfrontend.service.SoftwareComponentService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@Route(value = "inventory-items", layout = MainView.class)
@ViewController(id = "InventoryItem.list")
@ViewDescriptor(path = "inventory-item-list-view.xml")
@LookupComponent("inventoryItemsDataGrid")
@DialogMode(width = "64em")
public class InventoryItemListView extends StandardListView<InventoryItem> {

    private final List<String> valuesOfBoolean = List.of("True","False");
    private List<InventoryItem> inventoryItemsList;

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private JmixComboBox<SoftwareComponent> softwareComponentComboBox;

    @ViewComponent
    private JmixComboBox<String> cveComboBox;

    @ViewComponent
    private CollectionContainer<InventoryItem> inventoryItemsDc;

    @ViewComponent
    private CollectionLoader<InventoryItem> inventoryItemsDl;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private Accordion projectAccordion;

    @ViewComponent
    private Accordion attributesAccordion;

    @ViewComponent
    private JmixComboBox<String> curatedComboBox;

    @ViewComponent
    private JmixComboBox<String> vulnerabilityComboBox;

    @ViewComponent
    private JmixComboBox<License> licenseTypeComboBox;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private InventoryItemService inventoryItemService;
    
    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private LicenseRepository licenseRepository;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);

        List<SoftwareComponent> softwareComponents = softwareComponentRepository.findAll();
        softwareComponentComboBox.setItems(softwareComponents);
        softwareComponentComboBox.setItemLabelGenerator(sc -> sc.getName()+" "+sc.getVersion());

        curatedComboBox.setItems(valuesOfBoolean);
        vulnerabilityComboBox.setItems(valuesOfBoolean);

        cveComboBox.setItems(softwareComponentService.getAllCVEFoundInSoftwareComponents(softwareComponents));

        licenseTypeComboBox.setItems(licenseRepository.findAll());
        licenseTypeComboBox.setItemLabelGenerator(License::getLicenseType);

        projectAccordion.close();
        attributesAccordion.close();

        inventoryItemsList = inventoryItemRepository.findAll();
    }

    @Subscribe("projectComboBox")
    public void onProjectComboBoxValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>,
            Project> event) {
        applyAllFilter();
    }

    @Subscribe("softwareComponentComboBox")
    public void onsoftwareComponentComboBoxValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<SoftwareComponent>,
            SoftwareComponent> event) {
        applyAllFilter();
    }

    @Subscribe("curatedComboBox")
    public void onCuratedComboBoxValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,
            String> event){
        applyAllFilter();
    }

    @Subscribe("cveComboBox")
    public void onCveComboBoxValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>
            ,String> event){
        applyAllFilter();
    }

    @Subscribe("vulnerabilityComboBox")
    public void onVulnerabilityComboBoxValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>
            ,String> event){
        applyAllFilter();
    }

    @Subscribe("licenseTypeComboBox")
    public void setSoftwareComponentComboBoxValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<License>
            ,License> event){
        applyAllFilter();
    }

    @Subscribe("searchButton")
    public void searchLicense(ClickEvent<Button> event){

        String searchWord = searchField.getValue();
        List<InventoryItem> items = inventoryItemsDc.getItems();
        List<InventoryItem> filteredItems = new ArrayList<>();

        if(!searchWord.isEmpty() && event != null) {
            filteredItems =
                    items.stream().filter(item -> item.getInventoryName().toUpperCase().contains(searchWord.toUpperCase())).toList();
            inventoryItemsDc.setItems(filteredItems);
        }else if(searchWord.isEmpty() && event != null){
            inventoryItemsDl.load();
        }
    }

    private void applyAllFilter(){
        Project project = projectComboBox.getValue();
        SoftwareComponent softwareComponent = softwareComponentComboBox.getValue();
        Boolean curated = curatedComboBox.getValue() != null ? Boolean.parseBoolean(curatedComboBox.getValue()) : null;
        Boolean vulnerability = vulnerabilityComboBox.getValue() != null ? Boolean.parseBoolean(vulnerabilityComboBox.getValue()) : null;
        String cve = cveComboBox.getValue();
        License license = licenseTypeComboBox.getValue();

        List<InventoryItem> filteredList = new ArrayList<>(inventoryItemsList);

        if (project != null){
            filteredList.retainAll(inventoryItemService.findInventoryItemsOfProject(project));
        }
        if (softwareComponent != null){
            filteredList.retainAll(inventoryItemService.findInventoryItemsOfSoftwareComponent(softwareComponent));
        }
        if (curated != null){
            filteredList.retainAll(inventoryItemService.findInventoryItemsByCurated(curated));
        }
        if (vulnerability != null){
            filteredList.retainAll(inventoryItemService.findInventoryItemsByIsVulnerable(vulnerability));
        }
//        if (cve != null){ TODO check ticket 978
//            filteredList.retainAll(inventoryItemService.findInventoryItemBySoftwareComponentCve(cve));
//        }
        if (license != null){
            filteredList.retainAll(inventoryItemService.findInventoryItemsByLicense(license));
        }

        inventoryItemsDc.setItems(filteredList);
    }

    @Supply(to = "inventoryItemsDataGrid.licenses", subject = "renderer")
    private Renderer<InventoryItem> licensesRenderer() {
        return new TextRenderer<>(inventoryItem -> {
            SoftwareComponent softwareComponent = inventoryItem.getSoftwareComponent();

            if (softwareComponent == null || softwareComponent.getLicenses() == null) {
                return "";
            }

            return softwareComponent.getLicenses().stream()
                    .map(License::getLicenseType)
                    .collect(Collectors.joining(", "));
        });
    }
}
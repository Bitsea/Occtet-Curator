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

package eu.occtet.bocfrontend.view.license;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.LicenseService;
import eu.occtet.bocfrontend.service.SPDXLicenseService;
import eu.occtet.bocfrontend.service.SoftwareComponentService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@Route(value = "licenses", layout = MainView.class)
@ViewController(id = "License.list")
@ViewDescriptor(path = "license-list-view.xml")
@LookupComponent("licensesDataGrid")
@DialogMode(width = "64em")
public class LicenseListView extends StandardListView<License> {

    private static final Logger log = LogManager.getLogger(LicenseListView.class);

    private SoftwareComponent softwareComponent;
    private List<License> licensesList;
    private final List<String> valuesOfBoolean = List.of("True","False");

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private JmixComboBox<SoftwareComponent> softwareComponentComboBox;

    @ViewComponent
    private CollectionContainer<License> licensesDc;

    @ViewComponent
    private CollectionLoader<License> licensesDl;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private Accordion projectAccordion;

    @ViewComponent
    private Accordion attributesAccordion;

    @ViewComponent
    private JmixComboBox<String> curatedComboBox;

    @ViewComponent
    private JmixComboBox<Integer> priorityComboBox;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SoftwareComponentService softwareComponentService;

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private SPDXLicenseService spdxLicenseService;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event){

        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);

        softwareComponentComboBox.setItems(softwareComponentRepository.findAll());
        softwareComponentComboBox.setItemLabelGenerator(sc -> sc.getName() + " " + sc.getVersion());

        List<Integer> valuesPriority = new ArrayList<>();
        for(License l : licensesDc.getItems()){
            if(!valuesPriority.contains(l.getPriority())){
                valuesPriority.add(l.getPriority());
            }
        }
        priorityComboBox.setItems(valuesPriority);
        curatedComboBox.setItems(valuesOfBoolean);

        projectAccordion.close();
        attributesAccordion.close();

        licensesList = licenseRepository.findAll();
    }

    @Subscribe("projectComboBox")
    public void showComponentsFromProject(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>,
            Project> event){
        applyAllFilters();
    }

    @Subscribe("softwareComponentComboBox")
    public void showLicensesFromSoftwareComponent(final AbstractField.ComponentValueChangeEvent<JmixComboBox<SoftwareComponent>,
            SoftwareComponent> event){
        applyAllFilters();
    }

    @Subscribe("curatedComboBox")
    public void findLicensesByCurated(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,String> event){
        applyAllFilters();
    }

    @Subscribe("priorityComboBox")
    public void findLicensesByPriority(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,String> event){
        applyAllFilters();
    }

    @Subscribe("searchButton")
    public void searchLicense(ClickEvent<Button> event){

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){

           List<License> allLicenses = licensesDc.getItems();
           List<License> searchLicenses = new ArrayList<>();

           for(License license : allLicenses){
               if(license.getLicenseType() != null){
                   if(license.getLicenseType().toUpperCase().contains(searchWord.toUpperCase()))
                       searchLicenses.add(license);
               }
               if(license.getLicenseName() != null){
                   if(license.getLicenseName().equals(searchWord))
                       searchLicenses.add(license);
               }
           }
           if(!searchLicenses.isEmpty()){
               licensesDc.setItems(searchLicenses);
           }
        }else if(searchWord.isEmpty() && event != null){
            licensesDc.setItems(softwareComponent.getLicenses());
        }
    }

    @Subscribe("fetchSPDXButton")
    public void fetchSPDX_Licenses(ClickEvent<Button> event){
        spdxLicenseService.readDefaultLicenseInfos();
        licensesDl.load();
    }

    private void applyAllFilters(){
        Project project = projectComboBox.getValue();
        SoftwareComponent softwareComponent = softwareComponentComboBox.getValue();
        Boolean curated = curatedComboBox.getValue() != null ? Boolean.parseBoolean(curatedComboBox.getValue()) : null;
        Integer prio = priorityComboBox.getValue() != null ? Integer.parseInt(priorityComboBox.getValue().toString()) : null;


        List<License> filteredLicenses = new ArrayList<>(licensesList);
        if (project != null){
            filteredLicenses.retainAll(licenseService.findLicensesByProject(project));
        }
        if (softwareComponent != null){
            filteredLicenses.retainAll(softwareComponent.getLicenses());
        }
        if (curated != null){
            filteredLicenses.retainAll(licenseService.findLicenseByCurated(curated));
        }
        if (prio != null){
            filteredLicenses.retainAll(licenseService.findLicenseByPriority(prio));
        }

        licensesDc.setItems(filteredLicenses);
    }
}
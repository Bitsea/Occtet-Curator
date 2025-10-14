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

package eu.occtet.bocfrontend.view.copyright;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.service.CopyrightService;
import eu.occtet.bocfrontend.service.SoftwareComponentService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


@Route(value = "copyrights", layout = MainView.class)
@ViewController(id = "Copyright.list")
@ViewDescriptor(path = "copyright-list-view.xml")
@LookupComponent("copyrightsDataGrid")
@DialogMode(width = "64em")
public class CopyrightListView extends StandardListView<Copyright> {

    private static final Logger log = LogManager.getLogger(CopyrightListView.class);

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private JmixComboBox<SoftwareComponent> softwareComponentComboBox;

    @ViewComponent
    private JmixComboBox<String> curatedComboBox;

    @ViewComponent
    private JmixComboBox<String> garbageComboBox;

    @ViewComponent
    private CollectionContainer<Copyright> copyrightsDc;

    @ViewComponent
    private CollectionLoader<Copyright> copyrightsDl;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private Accordion projectAccordion;

    @ViewComponent
    private Accordion attributesAccordion;

    @ViewComponent
    private FileUploadField uploadFile;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CopyrightService copyrightService;

    @Autowired
    private SoftwareComponentService softwareComponentService;

    @Autowired
    private Downloader downloader;

    @Autowired
    private Notifications notifications;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private CopyrightRepository copyrightRepository;

    private List<Copyright> copyrightsList;
    private SoftwareComponent softwareComponent;

    private final List<String> valuesOfBoolean = List.of("True","False");

    private static final String CURATED = "curated";
    private static final String GARGABE = "garbage";

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){

        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);

        softwareComponentComboBox.setItems(softwareComponentRepository.findAll());
        softwareComponentComboBox.setItemLabelGenerator(softwareComponent ->
                softwareComponent.getName()+" "+softwareComponent.getVersion());

        curatedComboBox.setItems(valuesOfBoolean);
        garbageComboBox.setItems(valuesOfBoolean);

        projectAccordion.close();
        attributesAccordion.close();

        copyrightsList = copyrightRepository.findAll();
    }

    @Subscribe("projectComboBox")
    public void showComponentsFromProject(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        applyAllFilters();
    }

    @Subscribe("softwareComponentComboBox")
    public void showCopyrightsFromSoftwareComponentComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<SoftwareComponent>,
            SoftwareComponent> event){
        applyAllFilters();
    }

    @Subscribe("curatedComboBox")
    public void findCopyrightsByCurated(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,String> event){
        applyAllFilters();
    }

    @Subscribe("garbageComboBox")
    public void findCopyrightsByGarbage(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>,String> event){
        applyAllFilters();
    }

    @Subscribe("searchButton")
    public void searchCopyright(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if (!searchWord.isEmpty() && event != null) {

            List<Copyright> allCopyrights = copyrightsDc.getItems();
            List<Copyright> searchCopyrights = new ArrayList<>();

            for(Copyright copyright : allCopyrights){

                if(copyright.getCopyrightText() != null){
                    if(copyright.getCopyrightText().toUpperCase().contains(searchWord.toUpperCase()))
                        searchCopyrights.add(copyright);
                }
            }
            if(!searchCopyrights.isEmpty()){
                copyrightsDc.setItems(searchCopyrights);
            }
        }else if(searchWord.isEmpty() && event != null){
            copyrightsDl.load();
        }
    }

    @Subscribe("downloadButton")
    public void downloadGarbageCopyrights(ClickEvent<Button> event){

        List<Copyright> copyrightList = copyrightsDc.getItems();
        List<Copyright> garbageList;

        if(event != null && !copyrightList.isEmpty()){
            garbageList = copyrightList.stream().filter(Copyright::getGarbage).toList();
            if(!garbageList.isEmpty()){
                copyrightService.createYML(garbageList);
                downloader.download(copyrightService.getYmlFileRef());
            }else{
                getInfoMessage("No garbage copyrights");
            }
        }
    }

    @Subscribe("uploadFile")
    public void uploadCopyrights(final FileUploadSucceededEvent<FileUploadField> event){

        File file = copyrightService.createFileUploadCopyrights(event);
        if(file != null){
            List<String> garbageCopys = copyrightService.readYML(file);
            if(!garbageCopys.isEmpty()){
                copyrightService.setGarbageCopyrightsInJSON(garbageCopys);
                getInfoMessage("File uploaded successfully");
            }else{
                getInfoMessage("Upload failed!");
            }
        }
    }

    private void applyAllFilters(){
        Project project = projectComboBox.getValue();
        SoftwareComponent softwareComponent = softwareComponentComboBox.getValue();
        Boolean curated = curatedComboBox.getValue() != null ? Boolean.parseBoolean(curatedComboBox.getValue()) : null;
        Boolean garbage = garbageComboBox.getValue() != null ? Boolean.parseBoolean(garbageComboBox.getValue()) : null;

        List<Copyright> filteredCopyrights = new ArrayList<>(copyrightsList);

        if (project != null){
            filteredCopyrights.retainAll(copyrightService.findCopyrightsByProject(project));
        }
        if (softwareComponent != null){
            filteredCopyrights.retainAll(copyrightService.findCopyrightsBySoftwareComponent(softwareComponent));
        }
        if (curated != null){
            filteredCopyrights.retainAll(copyrightService.findCopyrightsByCurated(curated));
        }
        if (garbage != null){
            filteredCopyrights.retainAll(copyrightService.findCopyrightsByGarbage(garbage));
        }

        copyrightsDc.setItems(filteredCopyrights);
    }

    private void getInfoMessage(String message){

        notifications.create(message)
                .withPosition(Notification.Position.TOP_CENTER)
                .withDuration(3000)
                .show();
    }
}
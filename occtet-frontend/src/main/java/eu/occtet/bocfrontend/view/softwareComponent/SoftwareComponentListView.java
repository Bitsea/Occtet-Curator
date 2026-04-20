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

package eu.occtet.bocfrontend.view.softwareComponent;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


@Route(value = "software-components", layout = MainView.class)
@ViewController(id = "SoftwareComponent.list")
@ViewDescriptor(path = "software-component-list-view.xml")
@LookupComponent("softwareComponentsDataGrid")
@DialogMode(width = "64em")
public class SoftwareComponentListView extends StandardListView<SoftwareComponent> {

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private CollectionLoader<SoftwareComponent> softwareComponentsDl;

    @ViewComponent
    private HorizontalLayout filterBox;

    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @Autowired
    Messages messages;


    @Subscribe
    public void onInit(InitEvent event){
        Project showAllProject = new Project();
        showAllProject.setProjectName(messages.getMessage("Showall"));
        showAllProject.setVersion("");
        showAllProject.setId(new Random().nextLong());

        List<Project> allProjects = new java.util.ArrayList<>();
        allProjects.add(showAllProject);
        allProjects.addAll(projectRepository.findAll());

        projectComboBox.setItems(allProjects);
        projectComboBox.setItemLabelGenerator(project -> {
            if (messages.getMessage("Showall").equals(project.getProjectName())) {
                return project.getProjectName();
            }
            return project.getProjectName() + " - " + project.getVersion();
        });
    }

    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        if(event != null){
            Project selectedProject = event.getValue();
            if (selectedProject == null || messages.getMessage("Showall").equals(selectedProject.getProjectName())) {
                List<SoftwareComponent> softwareComponents = softwareComponentRepository.findAll();
                loadSoftwareComponent(softwareComponents);
                filterBox.setVisible(!softwareComponents.isEmpty());
            } else {
                List<SoftwareComponent> softwareComponents = softwareComponentRepository.findByProject(event.getValue());
                loadSoftwareComponent(softwareComponents);
                filterBox.setVisible(!softwareComponents.isEmpty());
            }
        }
    }

    @Supply(to = "softwareComponentsDataGrid.licenses", subject = "renderer")
    private Renderer<SoftwareComponent> licensesRenderer() {
        return new TextRenderer<>(component -> {

            if (component.getLicenses() == null) {
                return "";
            }
            return component.getLicenses().stream()
                    .map(usage -> usage.getTemplate().getLicenseType())
                    .collect(Collectors.joining(", "));
        });
    }

    @Subscribe("softwareComponentsDataGrid")
    public void clickOnSoftwareComponentDatagrid(ItemDoubleClickEvent<SoftwareComponent> event){
        DialogWindow<SoftwareComponentDetailView> window =
                dialogWindows.detail(this, SoftwareComponent.class)
                        .withViewClass(SoftwareComponentDetailView.class)
                        .editEntity(event.getItem())
                        .build();
        window.setWidth("100%");
        window.setHeight("100%");
        window.open();
    }

    private void loadSoftwareComponent(List<SoftwareComponent> softwareComponents){
        softwareComponentsDl.setParameter("softwareComponents",softwareComponents);
        softwareComponentsDl.load();
    }
}
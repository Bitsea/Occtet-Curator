/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https:www.apache.orglicensesLICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *   License-Filename: LICENSE
 */
package eu.occtet.bocfrontend.view.ortViolation;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.OrtViolation;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Messages;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;

@Route(value = "ortViolation", layout = MainView.class)
@ViewController(id = "ortViolation.list")
@ViewDescriptor(path = "ort-violation-list-view.xml")
@LookupComponent("ortViolationDataGrid")
@DialogMode(width = "64em")
public class OrtViolationListView extends StandardListView<OrtViolation> {

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private HorizontalLayout filterBox;

    @ViewComponent
    private CollectionLoader<OrtViolation> ortViolationDl;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private Messages messages;

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
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (event != null) {
            Project selectedProject = event.getValue();
            if (selectedProject != null && !messages.getMessage("Showall").equals(selectedProject.getProjectName())) {
                ortViolationDl.setParameter("project", selectedProject);
            } else {
                ortViolationDl.removeParameter("project");
            }

            ortViolationDl.load();
            filterBox.setVisible(true);
        }
    }
}

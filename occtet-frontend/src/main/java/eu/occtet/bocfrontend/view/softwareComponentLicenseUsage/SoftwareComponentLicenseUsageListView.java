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

package eu.occtet.bocfrontend.view.softwareComponentLicenseUsage;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentLicenseUsageRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.SoftwareComponentLicenseUsage;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Messages;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Random;

@Route(value = "usage-licenses", layout = MainView.class)
@ViewController(id = "SoftwareComponentLicenseUsage.list")
@ViewDescriptor(path = "usage-license-list-view.xml")
@LookupComponent("licensesDataGrid")
@DialogMode(width = "80%", height = "80%")
public class SoftwareComponentLicenseUsageListView extends StandardListView<SoftwareComponentLicenseUsage>{

    @Autowired
    private SoftwareComponentLicenseUsageRepository licenseRepository;
    @ViewComponent
    private HorizontalLayout filterBox;
    @Autowired
    private Messages messages;
    @ViewComponent
    private JmixComboBox<Project> projectComboBox;
    @ViewComponent
    private CollectionLoader<SoftwareComponentLicenseUsage> licensesDl;
    @Autowired
    private ProjectRepository projectRepository;

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

            if (selectedProject != null && messages.getMessage("Showall").equals(selectedProject.getProjectName())) {
                selectedProject = null;
            }

            licensesDl.setParameter("project", selectedProject);
            licensesDl.load();

            filterBox.setVisible(!licensesDl.getContainer().getItems().isEmpty());
        }
    }


    @Supply(to = "licensesDataGrid.usageText", subject = "renderer")
    private Renderer<SoftwareComponentLicenseUsage> effectiveTextColumnRenderer() {
        //only first part of text is shown due nicer view
        return new TextRenderer<>(usage -> {
            String text = usage.getEffectiveText();
            if (text == null) {
                return "";
            }
            return StringUtils.abbreviate(text, 100);
        });
    }

    @Supply(to = "licensesDataGrid.customName", subject = "renderer")
    private Renderer<SoftwareComponentLicenseUsage> effectiveCustomNameColumnRenderer() {
        return new TextRenderer<>(usage -> {
            String name = usage.getEffectiveName();
            return name != null ? name : "";
        });
    }

}

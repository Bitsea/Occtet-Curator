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

package eu.occtet.bocfrontend.view.copyright;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.*;


@Route(value = "copyrights", layout = MainView.class)
@ViewController(id = "Copyright.list")
@ViewDescriptor(path = "copyright-list-view.xml")
@LookupComponent("copyrightsDataGrid")
@DialogMode(width = "64em")
public class CopyrightListView extends StandardListView<Copyright> {

    private static final Logger log = LogManager.getLogger(CopyrightListView.class);

    @ViewComponent
    private CollectionContainer<Copyright> copyrightsDc;

    @ViewComponent
    private CollectionLoader<Copyright> copyrightsDl;


    @Autowired
    protected UiComponents uiComponents;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private ProjectRepository projectRepository;

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private HorizontalLayout filterBox;

    @ViewComponent
    private DataGrid<Copyright> copyrightsDataGrid;

    @ViewComponent
    private JmixButton saveButton;

    @ViewComponent
    private JmixButton exitButton;

    @ViewComponent
    private JmixButton markButton;

    private Project project;


    @Subscribe
    public void onInit(InitEvent event){
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
        project = null;
    }


    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        if(event != null){
            project = event.getValue();
            updateDatagridForProject(project);
            filterBox.setVisible(true);
            markButton.setVisible(true);
        }
    }

    @Subscribe("markButton")
    public void clickOnMarkButton(ClickEvent<Button> event) {
        copyrightsDataGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        setButtonVisible(true);
    }

    @Subscribe("saveButton")
    public void clickOnSaveButton(ClickEvent<Button> event) {
        Set<Copyright> selectedCopyrights = copyrightsDataGrid.getSelectedItems();
        selectedCopyrights.forEach(copyright -> {
            if(copyright.isGarbage()){
                copyright.setGarbage(false);
            }else if(!copyright.isGarbage()){
                copyright.setGarbage(true);
            }
            dataManager.save(copyright);
        });
        updateDatagridForProject(project);
        copyrightsDataGrid.setSelectionMode(Grid.SelectionMode.NONE);
        setButtonVisible(false);
    }

    @Subscribe("exitButton")
    public void clickOnExitButton(ClickEvent<Button> event) {
        copyrightsDataGrid.setSelectionMode(Grid.SelectionMode.NONE);
        setButtonVisible(false);
    }

    @Supply(to = "copyrightsDataGrid.garbage", subject = "renderer")
    protected Renderer<Copyright> garbageComponentRenderer() {
        return new ComponentRenderer<>(this::createCheckbox);
    }

    private JmixCheckbox createCheckbox(Copyright copyright){
        JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
        checkbox.setReadOnly(true);
        checkbox.setValue(copyright.isGarbage());
        return checkbox;
    }

    private void updateDatagridForProject(Project project){
        log.debug("Loading copyrights for project: " + project.getProjectName());
        copyrightsDl.setParameter("project",project);
        copyrightsDl.load();
    }

    private void setButtonVisible(boolean isVisible){
        saveButton.setVisible(isVisible);
        exitButton.setVisible(isVisible);
    }

}
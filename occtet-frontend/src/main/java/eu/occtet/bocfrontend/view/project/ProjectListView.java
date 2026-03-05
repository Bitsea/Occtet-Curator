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

package eu.occtet.bocfrontend.view.project;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


@Route(value = "projects", layout = MainView.class)
@ViewController(id = "Project.list")
@ViewDescriptor(path = "project-list-view.xml")
@LookupComponent("projectsDataGrid")
@DialogMode(width = "64em")
public class ProjectListView extends StandardListView<Project> {

    private static final Logger log = LogManager.getLogger(ProjectListView.class);

    @ViewComponent
    private DataGrid<Project> projectsDataGrid;

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private Messages messages;
    @Autowired
    private Dialogs dialogs;

    @Subscribe
    public void onInit(final InitEvent event) {
        DataGrid.Column<Project> exportColumn = projectsDataGrid.getColumnByKey("exportBtn");

        exportColumn.setRenderer(new ComponentRenderer<>(project -> {

            JmixButton exportButton = uiComponents.create(JmixButton.class);
            exportButton.setIcon(VaadinIcon.DOWNLOAD.create());
            exportButton.setText(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.exportBtn"));
            exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            exportButton.setTooltipText(messages.getMessage("eu.occtet.bocfrontend.view.project/projectListView.exportTooltip"));
            exportButton.addClickListener(clickEvent -> {
                dialogWindows.view(this, ExportProjectSbomHelperView.class)
                        .withViewConfigurer(v -> {
                            v.setProject(project);
                        }).open();
            });

            return exportButton;
        }));
    }

    @Subscribe("projectsDataGrid")
    public void clickOnProjectsDataGrid(ItemDoubleClickEvent<Project> event){
        DialogWindow<ProjectDetailView> window =
                dialogWindows.detail(this, Project.class)
                        .withViewClass(ProjectDetailView.class)
                        .editEntity(event.getItem())
                        .build();
        window.setWidth("100%");
        window.setHeight("100%");
        window.open();
    }
}

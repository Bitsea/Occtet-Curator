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

package eu.occtet.bocfrontend.view.vexData;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.dao.VexDataRepository;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Route(value = "vex-data", layout = MainView.class)
@ViewController(id = "VexData.list")
@ViewDescriptor(path = "vex-data-list-view.xml")
@LookupComponent("vexDataDataGrid")
@DialogMode(width = "64em")
public class VexDataListView extends StandardListView<VexData> {

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private HorizontalLayout filterBox;

    @ViewComponent
    private CollectionLoader<VexData> vexDataDl;

    @Autowired
    private VexDataRepository vexDataRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private DialogWindows dialogWindows;

    @Subscribe
    public void onInit(InitEvent event){
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
    }

    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        if(event != null){
            List<SoftwareComponent> softwareComponents = softwareComponentRepository.findByProject(event.getValue());
            List<VexData> vexDataList = vexDataRepository.findBySoftwareComponents(softwareComponents);
            vexDataDl.setParameter("vexDataList",vexDataList);
            vexDataDl.load();
            filterBox.setVisible(true);
        }
    }

    @Subscribe("vexDataDataGrid")
    public void clickOnVexDataDataGrid(ItemDoubleClickEvent<VexData> event) {
        DialogWindow<VexDataDetailView> window =
                dialogWindows.detail(this, VexData.class)
                        .withViewClass(VexDataDetailView.class)
                        .editEntity(event.getItem())
                        .build();
        window.setWidth("100%");
        window.setHeight("100%");
        window.open();
    }
}

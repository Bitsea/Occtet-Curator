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

package eu.occtet.bocfrontend.view.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@ViewController("addSoftwareComponentDialog")
@ViewDescriptor("add-software-component-dialog.xml")
@DialogMode(width = "70%", height = "70%")
public class AddSoftwareComponentDialog extends AbstractAddContentDialog<List<SoftwareComponent>>{


    private static final Logger log = LogManager.getLogger(AddSoftwareComponentDialog.class);

    private List<SoftwareComponent> softwareComponents;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @ViewComponent
    private CollectionContainer<SoftwareComponent> softwareComponentDc;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private DataGrid<SoftwareComponent> softwareComponentDataGrid;


    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        softwareComponentRepository.findAvailableComponents(softwareComponents).stream()
                .forEach(s-> log.debug("available SC : {}", s.getName()));
        softwareComponentDc.setItems(softwareComponentRepository.findAvailableComponents(softwareComponents));
    }



    @Override
    @Subscribe("softwareComponentDc")
    public void setAvailableContent(List<SoftwareComponent> softwareComponents){
        log.debug("set content");
        this.softwareComponents = Objects.requireNonNullElseGet(softwareComponents, ArrayList::new);
        List<SoftwareComponent> available = softwareComponentRepository.findAvailableComponents(softwareComponents);
        available.forEach(s -> log.debug("set content: available SC : {}", s.getName()));
        softwareComponentDc.setItems(available);
    }

    @Override
    @Subscribe(id = "addComponentButton")
    public void addContentButton(ClickEvent<Button> event) {
        List<SoftwareComponent> selectedComponents = getSelectedComponents();

        if (!selectedComponents.isEmpty()) {
            close(StandardOutcome.SAVE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {
        String searchWord = searchField.getValue();

        if (!searchWord.isEmpty() && event != null) {
            List<SoftwareComponent> listFindings = softwareComponentRepository.findAvailableComponents(softwareComponents)
                    .stream().filter(s -> s.getName().toLowerCase().contains(searchWord.toLowerCase())
                            || s.getPurl().toLowerCase().contains(searchWord.toLowerCase()))
                    .toList();
            softwareComponentDc.setItems(listFindings);
        } else {
            softwareComponentDc.setItems(softwareComponentRepository.findAvailableComponents(softwareComponents));
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event){
        cancelButton(event);
    }

    public List<SoftwareComponent> getSelectedComponents() {
        return new ArrayList<>(softwareComponentDataGrid.getSelectedItems());
    }
}

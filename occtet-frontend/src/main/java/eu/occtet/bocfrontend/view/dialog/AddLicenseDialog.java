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

package eu.occtet.bocfrontend.view.dialog;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@ViewController("addLicenseDialog")
@ViewDescriptor("add-license-dialog.xml")
@DialogMode(width = "1000px", height = "650px")
public class AddLicenseDialog extends AbstractAddContentDialog<SoftwareComponent> {

    private static final Logger log = LogManager.getLogger(AddLicenseDialog.class);

    private SoftwareComponent softwareComponent;

    private License license;

    @Autowired
    private LicenseRepository licenseRepository;

    @ViewComponent
    private CollectionContainer<License> licenseDc;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private DataGrid<License> licensesDataGrid;

    @Autowired
    private DataManager dataManager;

    @Override
    @Subscribe("licenseDc")
    public void setAvailableContent(SoftwareComponent softwareComponent){
        this.softwareComponent = dataManager.load(SoftwareComponent.class)
                .id(softwareComponent.getId()).fetchPlan(f -> f.add("licenses")).one();
        log.debug("setAvailableContent called with SoftwareComponent: {}", softwareComponent);
        licenseDc.setItems(licenseRepository.findAvailableLicenses(this.softwareComponent.getLicenses()));
    }

    @Subscribe("licensesDataGrid")
    public void selectAvailableContent(final ItemClickEvent<License> event){license = event.getItem();}

    @Override
    @Subscribe(id = "addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<License> licenses = new ArrayList<>(licensesDataGrid.getSelectedItems());
        if(!licenses.isEmpty() && softwareComponent != null){
            softwareComponent.getLicenses().addAll(licenses);
            dataManager.save(this.softwareComponent);
            close(StandardOutcome.SAVE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){
            List<License> listFindings= licenseRepository.findAll().stream().filter(l-> l.getLicenseName().toLowerCase().contains(searchWord.toLowerCase())
                    || l.getLicenseType().toLowerCase().contains(searchWord.toLowerCase())).toList();
            licenseDc.setItems(listFindings);
        }else{
            licenseDc.setItems(licenseRepository.findAvailableLicenses(softwareComponent.getLicenses()));
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event){cancelButton(event);}
}

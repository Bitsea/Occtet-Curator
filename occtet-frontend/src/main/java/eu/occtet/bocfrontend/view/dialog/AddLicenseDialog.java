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
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import eu.occtet.bocfrontend.dao.LicenseRepository;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponentLicenseUsage;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@ViewController("addLicenseDialog")
@ViewDescriptor("add-license-dialog.xml")
@DialogMode(width = "70%", height = "70%")
public class AddLicenseDialog extends AbstractAddContentDialog<SoftwareComponent> {

    private static final Logger log = LogManager.getLogger(AddLicenseDialog.class);

    private SoftwareComponent softwareComponent;


    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private CollectionContainer<License> licenseDc;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private DataGrid<License> licensesDataGrid;

    @Override
    @Subscribe("licenseDc")
    public void setAvailableContent(SoftwareComponent softwareComponent) {
        if (softwareComponent.getUsageLicenses() == null ) {
            this.softwareComponent = dataManager.load(SoftwareComponent.class)
                    .id(softwareComponent.getId())
                    .fetchPlan(f -> f.add("licenseUsages", f1 -> f1.add("template")))
                    .one();
        } else {
            this.softwareComponent = softwareComponent;
        }
        log.debug("setAvailableContent called with SoftwareComponent: {}", softwareComponent);
        loadAvailableLicenses();
    }

    private void loadAvailableLicenses() {
        List<License> alreadyLinkedTemplates = this.softwareComponent.getUsageLicenses().stream()
                .map(SoftwareComponentLicenseUsage::getTemplate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (alreadyLinkedTemplates.isEmpty()) {
            licenseDc.setItems(dataManager.load(License.class).all().list());
        } else {
            licenseDc.setItems(licenseRepository.findAvailableLicenses(alreadyLinkedTemplates));
        }
    }

    @Override
    @Subscribe(id = "addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {

        if (licensesDataGrid.getSelectedItems().isEmpty()) {
            return;
        }

        close(StandardOutcome.SAVE);
        }


    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {
        String searchWord = searchField.getValue();
        if (searchWord != null && !searchWord.isEmpty()) {
            List<License> listFindings = licenseRepository.findAll().stream()
                    .filter(l -> l.getLicenseName().toLowerCase().contains(searchWord.toLowerCase())
                            || l.getLicenseType().toLowerCase().contains(searchWord.toLowerCase()))
                    .collect(Collectors.toList());

            List<License> usedTemplates = this.softwareComponent.getUsageLicenses().stream()
                    .map(SoftwareComponentLicenseUsage::getTemplate)
                    .collect(Collectors.toList());

            listFindings.removeIf(usedTemplates::contains);

            licenseDc.setItems(listFindings);
        } else {
            licenseDc.setItems(licenseRepository.findAvailableLicenses(softwareComponent.getUsageLicenses().stream().map(SoftwareComponentLicenseUsage::getTemplate).collect(Collectors.toList())));
        }
    }


    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event) {
        cancelButton(event);
    }

    public List<License> getSelectedLicenses() {
        return new ArrayList<>(licensesDataGrid.getSelectedItems());
    }
}

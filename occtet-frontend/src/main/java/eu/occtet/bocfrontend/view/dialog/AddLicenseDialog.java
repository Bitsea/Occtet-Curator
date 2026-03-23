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
import eu.occtet.bocfrontend.dao.TemplateLicenseRepository;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.TemplateLicense;
import eu.occtet.bocfrontend.entity.UsageLicense;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@ViewController("addLicenseDialog")
@ViewDescriptor("add-license-dialog.xml")
@DialogMode(width = "1000px", height = "650px")
public class AddLicenseDialog extends AbstractAddContentDialog<SoftwareComponent> {

    private static final Logger log = LogManager.getLogger(AddLicenseDialog.class);

    private SoftwareComponent softwareComponent;

    private TemplateLicense selectedTemplate;

    @Autowired
    private TemplateLicenseRepository templateLicenseRepository;

    @ViewComponent
    private CollectionContainer<TemplateLicense> licenseDc;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private DataGrid<TemplateLicense> licensesDataGrid;

    @Autowired
    private DataManager dataManager;

    @Override
    @Subscribe("licenseDc")
    public void setAvailableContent(SoftwareComponent softwareComponent) {
        this.softwareComponent = dataManager.load(SoftwareComponent.class)
                .id(softwareComponent.getId())
                .fetchPlan(f -> f.add("licenses", f1 -> f1.add("template")))
                .one();

        log.debug("setAvailableContent called with SoftwareComponent: {}", softwareComponent);
        loadAvailableLicenses();
    }

    private void loadAvailableLicenses() {
        List<TemplateLicense> usedTemplates = this.softwareComponent.getLicenses().stream()
                .map(UsageLicense::getTemplate)
                .collect(Collectors.toList());

        if (usedTemplates.isEmpty()) {
            licenseDc.setItems(templateLicenseRepository.findAll());
        } else {
            licenseDc.setItems(templateLicenseRepository.findAvailableLicenses(usedTemplates));
        }
    }

    @Subscribe("licensesDataGrid")
    public void selectAvailableContent(final ItemClickEvent<TemplateLicense> event) {
        selectedTemplate = event.getItem();
    }

    @Override
    @Subscribe(id = "addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<TemplateLicense> selectedTemplates = new ArrayList<>(licensesDataGrid.getSelectedItems());

        if (!selectedTemplates.isEmpty() && softwareComponent != null) {
            SaveContext saveContext = new SaveContext();

            for (TemplateLicense template : selectedTemplates) {
                UsageLicense newUsage = dataManager.create(UsageLicense.class);
                newUsage.setTemplate(template);
                newUsage.setSoftwareComponent(softwareComponent);
                newUsage.setModified(false);
                newUsage.setCurated(false);
                newUsage.setUsageText(template.getTemplateText());

                softwareComponent.getLicenses().add(newUsage);
                saveContext.saving(newUsage);
            }

            saveContext.saving(softwareComponent);
            dataManager.save(saveContext);

            close(StandardOutcome.SAVE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if (searchWord != null && !searchWord.isEmpty()) {
            List<TemplateLicense> listFindings = templateLicenseRepository.findAll().stream()
                    .filter(l -> l.getLicenseName().toLowerCase().contains(searchWord.toLowerCase())
                            || l.getLicenseType().toLowerCase().contains(searchWord.toLowerCase()))
                    .collect(Collectors.toList());

            List<TemplateLicense> usedTemplates = this.softwareComponent.getLicenses().stream()
                    .map(UsageLicense::getTemplate)
                    .collect(Collectors.toList());

            listFindings.removeIf(usedTemplates::contains);

            licenseDc.setItems(listFindings);
        } else {
            loadAvailableLicenses();
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event) {
        cancelButton(event);
    }
}

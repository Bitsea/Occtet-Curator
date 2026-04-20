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
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.dao.TemplateLicenseRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.TemplateLicense;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@ViewController("addLicenseToCopyrightDialog")
@ViewDescriptor("add-license-to-copyright-dialog.xml")
@DialogMode(width = "70%", height = "70%")
public class AddLicenseToCopyrightDialog extends AbstractAddContentDialog<Copyright>{

    private static final Logger log = LogManager.getLogger(AddLicenseToCopyrightDialog.class);


    private TemplateLicense templateLicense;

    private Copyright copyright;

    @Autowired

    private LicenseRepository licenseRepository;
    private TemplateLicenseRepository templateLicenseRepository;

    @ViewComponent
    private CollectionContainer<TemplateLicense> licenseDc;

    @ViewComponent
    private TextField searchField;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @ViewComponent
    private DataGrid<TemplateLicense> licensesDataGrid;

    @Subscribe("licensesDataGrid")
    public void selectAvailableContent(final ItemClickEvent<TemplateLicense> event) {
        templateLicense = event.getItem();
    }

    @Subscribe("licenseDc")
    @Override
    public void setAvailableContent(Copyright copyright) {
        this.copyright = copyright;
        licenseDc.setItems(templateLicenseRepository.findAll());
    }

    @Override
    @Subscribe(id = "addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<TemplateLicense> selectedLicenses = new ArrayList<>(licensesDataGrid.getSelectedItems());
        log.debug("adding licenses {}", selectedLicenses.size());

        // Note: Fixed bitwise '&' to logical '&&' and checked for empty list
        if (event != null && !selectedLicenses.isEmpty()) {
            for (TemplateLicense license : selectedLicenses) {
                if (!this.copyright.getLicenses().contains(license)) {
                    this.copyright.getLicenses().add(license);
                }
            }
            copyrightRepository.save(this.copyright);
            close(StandardOutcome.CLOSE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if (searchWord != null && !searchWord.isEmpty() && event != null) {
            List<TemplateLicense> listFindings = templateLicenseRepository.findAll().stream()
                    .filter(l -> l.getLicenseName().toLowerCase().contains(searchWord.toLowerCase())
                            || l.getLicenseType().toLowerCase().contains(searchWord.toLowerCase()))
                    .toList();
            licenseDc.setItems(listFindings);
        } else {
            licenseDc.setItems(templateLicenseRepository.findAll());
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event) {
        cancelButton(event);
    }

}

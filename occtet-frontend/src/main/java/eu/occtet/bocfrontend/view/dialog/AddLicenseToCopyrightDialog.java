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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import eu.occtet.bocfrontend.dao.TemplateLicenseRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.SoftwareComponentLicenseUsage;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@ViewController("addLicenseToCopyrightDialog")
@ViewDescriptor("add-license-to-copyright-dialog.xml")
@DialogMode(width = "70%", height = "70%")
public class AddLicenseToCopyrightDialog extends StandardView{

    private static final Logger log = LogManager.getLogger(AddLicenseToCopyrightDialog.class);


    private SoftwareComponentLicenseUsage license;

    private Set<Copyright> copyrights;
    private SoftwareComponent softwareComponent;

    @Autowired

    private TemplateLicenseRepository templateLicenseRepository;

    @ViewComponent
    private CollectionContainer<SoftwareComponentLicenseUsage> licenseDc;

    @ViewComponent
    private TextField searchField;


    @ViewComponent
    private DataGrid<SoftwareComponentLicenseUsage> licensesDataGrid;



    @Subscribe("licenseDc")
    public void setAvailableContent(Set<Copyright> selectedCopyrights, SoftwareComponent softwareComponent) {
        this.copyrights = selectedCopyrights;
        this.softwareComponent= softwareComponent;
        licenseDc.setItems(softwareComponent.getUsageLicenses());
    }


    @Subscribe(id = "addLicenseButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<SoftwareComponentLicenseUsage> selectedLicenses = new ArrayList<>(licensesDataGrid.getSelectedItems());
        log.debug("adding licenses {}", selectedLicenses.size());

        // Note: Fixed bitwise '&' to logical '&&' and checked for empty list
        if (event != null && !selectedLicenses.isEmpty()) {
            for (SoftwareComponentLicenseUsage license : selectedLicenses) {
                for(Copyright copyright: copyrights) {
                    if (!copyright.getLicenses().contains(license)) {
                        copyright.getLicenses().add(license);
                    }
                }
            }
            close(StandardOutcome.CLOSE);
        }
    }


    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if (searchWord != null && !searchWord.isEmpty() && event != null) {
            List<SoftwareComponentLicenseUsage> listFindings = softwareComponent.getUsageLicenses().stream()
                    .filter(l -> l.getEffectiveName().toLowerCase().contains(searchWord.toLowerCase())
                            || l.getTemplate().getLicenseType().toLowerCase().contains(searchWord.toLowerCase()))
                    .toList();
            licenseDc.setItems(listFindings);
        } else {
            licenseDc.setItems(softwareComponent.getUsageLicenses());
        }
    }


    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event) {
        close(StandardOutcome.DISCARD);;
    }


    @Supply(to = "licensesDataGrid.customName", subject = "renderer")
    private Renderer<SoftwareComponentLicenseUsage> effectiveCustomNameColumnRenderer() {
        return new TextRenderer<>(SoftwareComponentLicenseUsage::getEffectiveName);
    }

}

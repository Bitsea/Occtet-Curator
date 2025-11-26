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

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@ViewController("overviewInformationDialog")
@ViewDescriptor("overview-information-dialog.xml")
@DialogMode(width = "1000px", height = "700px")
public class OverviewLicenseInfoDialog extends StandardView {

    @ViewComponent
    private CollectionContainer<InventoryItem> inventoryItemsDc;

    @ViewComponent
    private H2 title;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    public void setLicenseType(License license){
        List<SoftwareComponent> allComponents = softwareComponentRepository.findAll();
        List<SoftwareComponent> licenseComponents = new ArrayList<>();
        Set<InventoryItem> items = new HashSet<>();

        allComponents.forEach(softwareComponent -> {
            List<License> licenses = softwareComponent.getLicenses();
            if(licenses != null){
                if(licenses.contains(license)){
                    licenseComponents.add(softwareComponent);
                }
            }
        });
        licenseComponents.forEach(softwareComponent -> {
            items.addAll(inventoryItemRepository.findBySoftwareComponent(softwareComponent));
        });
        title.setText("License: "+license.getLicenseName());
        inventoryItemsDc.setItems(items);
    }

    @Supply(to = "inventoryItemsDataGrid.showInventoryBtn", subject = "renderer")
    private Renderer<Project> auditVulnerabilityDataGridShowBtnRenderer() {
        return new ComponentRenderer<>(this::showInventoryItemButton);
    }

    private JmixButton showInventoryItemButton(){
        JmixButton itemButton = uiComponents.create(JmixButton.class);
        itemButton .setIcon(VaadinIcon.CHEVRON_CIRCLE_RIGHT.create());
        itemButton .addThemeVariants(ButtonVariant.LUMO_SMALL);
        return itemButton;
    }

}

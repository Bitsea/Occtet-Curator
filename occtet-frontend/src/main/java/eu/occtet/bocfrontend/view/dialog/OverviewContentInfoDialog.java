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
import com.vaadin.flow.component.html.H3;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.view.audit.fragment.InventoryItemTabFragment;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ViewController("overviewContentInfoDialog")
@ViewDescriptor("overview-content-info-dialog.xml")
@DialogMode(width = "1000px", height = "700px")
public class OverviewContentInfoDialog extends StandardView {

    private static final Logger log = LogManager.getLogger(OverviewContentInfoDialog.class);

    @ViewComponent
    private CollectionLoader<InventoryItem> inventoryItemsDl;

    @ViewComponent
    private DataGrid<InventoryItem> inventoryItemsDataGrid;

    @ViewComponent
    private H3 title;

    @ViewComponent
    private InventoryItemTabFragment inventoryItemTabFragment;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    private InventoryItem inventoryItem;


    public void setInformationContent(Object content, Project project){
        List<InventoryItem> items = new ArrayList<>();
        if(content instanceof License license){
            items = inventoryItemRepository.findByLicenseAndProject(license,project);
            title.setText(license.getLicenseName());
        }else if(content instanceof Vulnerability vulnerability){
            items = inventoryItemRepository.findByVulnerabilityAndProject(vulnerability,project);
            title.setText(vulnerability.getVulnerabilityId());
        }
        updateDatagridForProject(items);
    }

    @Subscribe("inventoryItemsDataGrid")
    public void clickOnInventoryDatagrid(final ItemClickEvent<InventoryItem> event){
        if(event.getClickCount() == 2){
            setInventoryItem(event.getItem());
            close(StandardOutcome.CLOSE);
        }
    }

    @Subscribe("cancelButton")
    public void closeDialog(ClickEvent<Button> event){close(StandardOutcome.CLOSE);}

    private void setInventoryItem(InventoryItem item){inventoryItem = item;}
    public InventoryItem getInventoryItem(){return inventoryItem;}

    private void updateDatagridForProject(List<InventoryItem> items){
        inventoryItemsDl.setParameter("items",items);
        inventoryItemsDl.load();
    }

}

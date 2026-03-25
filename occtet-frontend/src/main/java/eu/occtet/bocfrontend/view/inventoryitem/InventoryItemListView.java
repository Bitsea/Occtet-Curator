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

package eu.occtet.bocfrontend.view.inventoryitem;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


@Route(value = "inventory-items", layout = MainView.class)
@ViewController(id = "InventoryItem.list")
@ViewDescriptor(path = "inventory-item-list-view.xml")
@LookupComponent("inventoryItemsDataGrid")
@DialogMode(width = "64em")
public class InventoryItemListView extends StandardListView<InventoryItem> {

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;

    @ViewComponent
    private HorizontalLayout filterBox;

    @ViewComponent
    private CollectionLoader<InventoryItem> inventoryItemsDl;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Subscribe
    public void onInit(InitEvent event){
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(project -> project.getProjectName()+" - "+project.getVersion());
    }

    @Subscribe(id = "projectComboBox")
    public void clickOnProjectComboBox(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event){
        if(event != null){
            List<InventoryItem> itemList = inventoryItemRepository.findByProject(event.getValue());
            loadInventoryItems(itemList);
            filterBox.setVisible(!itemList.isEmpty());
        }
    }

    @Subscribe("inventoryItemsDataGrid")
    public void clickOnInventoryItemDatagrid(ItemDoubleClickEvent<InventoryItem> event){
        DialogWindow<InventoryItemDetailView> window =
                dialogWindows.detail(this, InventoryItem.class)
                        .withViewClass(InventoryItemDetailView.class)
                        .editEntity(event.getItem())
                        .build();
        window.setWidth("100%");
        window.setHeight("100%");
        window.open();
    }

    @Subscribe("showAllButton")
    public void clickOnShowAllButton(ClickEvent<Button> event){
        List<InventoryItem> items = inventoryItemRepository.findAll();
        loadInventoryItems(items);
        filterBox.setVisible(!items.isEmpty());
    }

    private void loadInventoryItems(List<InventoryItem> inventoryItems){
        inventoryItemsDl.setParameter("inventoryItems",inventoryItems);
        inventoryItemsDl.load();
    }
}
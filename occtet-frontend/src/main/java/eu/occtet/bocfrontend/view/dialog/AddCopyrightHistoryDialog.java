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
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ViewController("addCopyrightHistoryDialog")
@ViewDescriptor("add-copyright-history-dialog.xml")
@DialogMode(width = "900px", height = "650px")
public class AddCopyrightHistoryDialog extends AbstractAddContentDialog<InventoryItem> {

    @ViewComponent
    private CollectionContainer<Copyright> copyrightDcHistory;
    @ViewComponent
    private DataGrid<Copyright> copyrightHistoryDataGrid;
    @ViewComponent
    private TextField searchField;

    private InventoryItem latestInventoryItem;

    private InventoryItem historyItem;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @ViewComponent
    private DataContext dataContext;

    @Override
    @Subscribe
    public void setAvailableContent(InventoryItem content) {
        this.historyItem = content;
        copyrightDcHistory.setItems(content.getCopyrights());
    }

    public void setLatestInventoryItem(InventoryItem inventoryItem){
        this.latestInventoryItem = dataContext.merge(inventoryItem);
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent event) {

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){
            List<Copyright> copyrightsFromItem = historyItem.getCopyrights();
            List<Copyright> searchedCopyrights = new ArrayList<>();
            for(Copyright copyright : copyrightsFromItem){
                if (copyright.getCopyrightText().equals(searchWord)){
                    searchedCopyrights.add(copyright);
                }
            }
            copyrightDcHistory.setItems(searchedCopyrights);
        }else{
            copyrightDcHistory.setItems(historyItem.getCopyrights());
        }
    }

    @Override
    @Subscribe("MoveCopyrightHistoryButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<Copyright> copyrights = copyrightHistoryDataGrid.getSelectedItems().stream().toList();
        if(copyrights != null && event != null){
            historyItem.getCopyrights().removeAll(copyrights);
            dataManager.save(historyItem);
            latestInventoryItem.getCopyrights().addAll(copyrights);
            dataManager.save(latestInventoryItem);
        }
        close(StandardOutcome.CLOSE);
    }

    @Subscribe(id="cancelButton")
    public void cancelCopyright(ClickEvent<Button> event){cancelButton(event);}

}

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
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@ViewController("addCopyrightDialog")
@ViewDescriptor("add-copyright-dialog.xml")
@DialogMode(width = "1000px", height = "650px")
public class AddCopyrightDialog extends AbstractAddContentDialog<SoftwareComponent> {

    private static final Logger log = LogManager.getLogger(AddCopyrightDialog.class);

    private SoftwareComponent softwareComponent;

    @ViewComponent
    private CollectionContainer<Copyright> copyrightDc;

    @ViewComponent
    private DataGrid<Copyright> copyrightDataGrid;

    @ViewComponent
    private TextField searchField;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Autowired
    private DataManager dataManager;

    @Override
    @Subscribe("copyrightDc")
    public void setAvailableContent(SoftwareComponent softwareComponent) {
        this.softwareComponent = dataManager.load(SoftwareComponent.class).id(softwareComponent.getId()).one();
        log.debug("setAvailableContent");
        copyrightDc.setItems(copyrightRepository.findAll());
    }

    @Override
    @Subscribe(id = "addCopyrightButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<Copyright> copyrights = new ArrayList<>(copyrightDataGrid.getSelectedItems());
        if(!copyrights.isEmpty()){
            this.softwareComponent.getCopyrights().addAll(copyrights);
            dataManager.save(this.softwareComponent);
            close(StandardOutcome.CLOSE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){
            List<Copyright> copyrightsFromItem = this.softwareComponent.getCopyrights();
            List<Copyright> searchedCopyrights = new ArrayList<>();
            for(Copyright copyright : copyrightsFromItem){
                if (copyright.getCopyrightText().contains(searchWord)){
                    searchedCopyrights.add(copyright);
                }
            }
            copyrightDc.setItems(searchedCopyrights);
        }else{
            copyrightDc.setItems(copyrightRepository.findAll());
        }
    }

    @Subscribe(id="cancelButton")
    public void cancelLicense(ClickEvent<Button> event){cancelButton(event);}
}
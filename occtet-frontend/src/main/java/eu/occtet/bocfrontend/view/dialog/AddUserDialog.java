/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https:www.apache.orglicensesLICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *   License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.view.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.MemberRepository;
import eu.occtet.bocfrontend.entity.Organization;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.User;
import io.jmix.core.DataManager;
import io.jmix.core.security.UserRepository;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@ViewController("addUserDialog")
@ViewDescriptor("add-user-dialog.xml")
@DialogMode(width = "1000px", height = "650px")
public class AddUserDialog extends AbstractAddContentDialog<User>{
    private static final Logger log = LogManager.getLogger(AddUserDialog.class);

    private Organization organization;

    private User user;

    @Autowired
    private MemberRepository memberRepository;

    @ViewComponent
    private CollectionContainer<User> userDc;

    @ViewComponent
    private TextField searchField;

    @ViewComponent
    private DataGrid<User> userDataGrid;

    @Autowired
    private DataManager dataManager;

    @Subscribe("userDc")
    @Override
    public void setAvailableContent(Organization organization){
        this.organization = dataManager.load(Organization.class)
                .id(organization.getId()).fetchPlan(f -> f.add("users")).one();
        log.debug("setAvailableContent called with Organization: {}", organization);
        userDc.setItems(memberRepository.findAll());
    }

    @Subscribe("userDataGrid")
    public void selectAvailableContent(final ItemClickEvent<User> event){
        user = event.getItem();}

    @Override
    @Subscribe(id = "addButton")
    public void addContentButton(ClickEvent<Button> event) {

        List<Project> projects = new ArrayList<>(projectDataGrid.getSelectedItems());
        if(!projects.isEmpty() && organization != null){
            organization.getProjects().addAll(projects);
            dataManager.save(this.organization);
            close(StandardOutcome.SAVE);
        }
    }

    @Override
    @Subscribe(id = "searchButton")
    public void searchContentButton(ClickEvent<Button> event) {

        String searchWord = searchField.getValue();
        if(!searchWord.isEmpty() && event != null){
            List<Project> listFindings= userRepository.findAll().stream().filter(p-> p.getProjectName().toLowerCase().contains(searchWord.toLowerCase())
                    || p.getProjectContact().toLowerCase().contains(searchWord.toLowerCase())).toList();
            projectDc.setItems(listFindings);
        }else{
            projectDc.setItems(userRepository.findAvailableProjects( organization.getProjects()));
        }
    }

    @Subscribe(id = "cancelButton")
    public void cancelLicense(ClickEvent<Button> event){cancelButton(event);}
}

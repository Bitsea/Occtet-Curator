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

package eu.occtet.bocfrontend.view.organization;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Organization;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.User;
import eu.occtet.bocfrontend.view.dialog.AddProjectDialog;
import eu.occtet.bocfrontend.view.dialog.AddUserDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;



@Route(value = "organization/:id", layout = MainView.class)
@ViewController(id = "Organization.detail")
@ViewDescriptor(path = "organization-detail-view.xml")
@EditedEntityContainer("organizationDc")
public class OrganizationDetailView extends StandardDetailView<Organization> {

    private static final Logger log = LogManager.getLogger(OrganizationDetailView.class);


    @Autowired
    private DialogWindows dialogWindow;

    @ViewComponent
    private CollectionContainer<Project> projectDc;


    @ViewComponent
    private CollectionContainer<User> userDc;

    @Subscribe(id = "addProjectButton", subject = "clickListener")
    public void onAddProjectButtonClick(final ClickEvent<JmixButton> event) {

        DialogWindow<AddProjectDialog> window = dialogWindow.view(this, AddProjectDialog.class).build();
        window.getView().setAvailableContent(getEditedEntity());
        window.open();
        window.addAfterCloseListener(close ->
                updateProjectGrid());

    }

    public void updateProjectGrid(){
        log.debug("update projects {}", getEditedEntity().getProjects().size());
        projectDc.setItems(getEditedEntity().getProjects());
    }

    @Subscribe(id = "addMemberButton", subject = "clickListener")
    public void onAddMemberButtonClick(final ClickEvent<JmixButton> event) {
        DialogWindow<AddUserDialog> window = dialogWindow.view(this, AddUserDialog.class).build();
        window.getView().setAvailableContent(getEditedEntity());
        window.open();
        window.addAfterCloseListener(close ->
                updateMemberGrid());

    }

    public void updateMemberGrid(){
        log.debug("update users {}", getEditedEntity().getProjects().size());
        userDc.setItems(getEditedEntity().getUsers());
    }




}

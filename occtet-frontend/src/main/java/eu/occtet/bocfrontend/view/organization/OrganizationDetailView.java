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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.UserRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.Organization;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.User;
import eu.occtet.bocfrontend.view.dialog.AddProjectDialog;
import eu.occtet.bocfrontend.view.dialog.AddUserDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Route(value = "organization/:id", layout = MainView.class)
@ViewController(id = "Organization.detail")
@ViewDescriptor(path = "organization-detail-view.xml")
@EditedEntityContainer("organizationDc")
public class OrganizationDetailView extends StandardDetailView<Organization> {

    private static final Logger log = LogManager.getLogger(OrganizationDetailView.class);


    @Autowired
    private DialogWindows dialogWindow;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @ViewComponent
    private DataGrid<User> userDataGrid;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Messages messages;
    @Autowired
    private Dialogs dialogs;

    @ViewComponent
    private JmixButton removeMemberButton;
    @ViewComponent
    private CollectionContainer<Project> projectDc;
    @ViewComponent
    private CollectionContainer<User> userDc;
    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private DataGrid<Project> projectDataGrid;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        userDc.setItems(userRepository.findByOrganization(this.getEditedEntity()));
        projectDc.setItems(projectRepository.findByOrganization(this.getEditedEntity()));
    }



    @Subscribe(id = "addProjectButton", subject = "clickListener")
    public void onAddProjectButtonClick(final ClickEvent<JmixButton> event) {

        DialogWindow<AddProjectDialog> window = dialogWindow.view(this, AddProjectDialog.class).build();
        window.getView().setAvailableContent(getEditedEntity());
        window.open();
        window.addAfterCloseListener(close -> {
            if (close.closedWith(StandardOutcome.SAVE)) {
                List<Project> selectedProjects = window.getView().getSelection();

                if (selectedProjects != null && !selectedProjects.isEmpty()) {
                    for (Project project : selectedProjects) {
                        if (project.getOrganization() == null) {
                            Project mergedProject = dataContext.merge(project);
                            mergedProject.setOrganization(getEditedEntity());
                            projectDc.getMutableItems().add(mergedProject);
                        } else {
                            log.error("Project already has an organization: {}", project);
                            notifications.create(messages.formatMessage(
                                    "project.error.alreadyAssigned",
                                    project.getProjectName()
                            ));
                        }
                    }
                }
            }
        });
    }

    @Subscribe(id = "addMemberButton", subject = "clickListener")
    public void onAddMemberButtonClick(final ClickEvent<JmixButton> event) {
        DialogWindow<AddUserDialog> window = dialogWindow.view(this, AddUserDialog.class).build();
        window.getView().setAvailableContent(getEditedEntity());
        window.open();
        window.addAfterCloseListener(close -> {
            if (close.closedWith(StandardOutcome.SAVE)) {
                List<User> selectedUsers = window.getView().getSelection();

                if (selectedUsers != null && !selectedUsers.isEmpty()) {
                    for (User user : selectedUsers) {
                        if (user.getOrganization() == null) {
                            User mergedUser = dataContext.merge(user);
                            mergedUser.setOrganization(getEditedEntity());
                            userDc.getMutableItems().add(mergedUser);
                        } else {
                            log.error("User already has an organization: {}", user);
                            notifications.create(messages.formatMessage(
                                    "user.error.alreadyAssigned",
                                    user.getUsername()
                            ));
                        }
                    }
                }
            }
        });
    }

    @Subscribe(id = "removeMemberButton", subject = "clickListener")
    public void onRemoveUserButtonClick(final ClickEvent<JmixButton> event) {
        if (userDataGrid.getSelectionMode() != DataGrid.SelectionMode.MULTI) {
            removeMemberButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            userDataGrid.setSelectionMode(DataGrid.SelectionMode.MULTI);
        } else {
            Set<User> selectedUsers = userDataGrid.getSelectedItems();
            if (!selectedUsers.isEmpty()) {
                String userNames = selectedUsers.stream()
                        .map(User::getUsername)
                        .collect(Collectors.joining(", "));

                dialogs.createOptionDialog()
                        .withHeader(messages.getMessage(getClass(), "removeUsers.header"))
                        .withText(messages.formatMessage(getClass(), "removeUsers.message", userNames))
                        .withActions(
                                new DialogAction(DialogAction.Type.OK)
                                        .withText(messages.getMessage("actions.Confirm"))
                                        .withVariant(ActionVariant.DANGER)
                                        .withHandler(actionEvent -> {
                                            for (User user : selectedUsers) {
                                                User mergedUser = dataContext.merge(user);
                                                mergedUser.setOrganization(null);

                                                userDc.getMutableItems().remove(mergedUser);
                                            }

                                            userDataGrid.getSelectionModel().deselectAll();
                                            userDataGrid.setSelectionMode(DataGrid.SelectionMode.NONE);
                                        }),

                                new DialogAction(DialogAction.Type.CANCEL)
                                        .withVariant(ActionVariant.DEFAULT)
                                        .withHandler(actionEvent -> {
                                            userDataGrid.getSelectionModel().deselectAll();
                                            userDataGrid.setSelectionMode(DataGrid.SelectionMode.NONE);
                                        })
                        )
                        .open();
            } else {
                removeMemberButton.removeThemeVariants(ButtonVariant.LUMO_ERROR);
                userDataGrid.setSelectionMode(DataGrid.SelectionMode.NONE);
            }
        }
    }
}

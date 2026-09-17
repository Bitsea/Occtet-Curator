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

package eu.occtet.bocfrontend.view.user;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.OrganizationRepository;
import eu.occtet.bocfrontend.entity.Organization;
import eu.occtet.bocfrontend.entity.User;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.view.*;
import io.jmix.security.model.ResourceRole;
import io.jmix.security.role.ResourceRoleRepository;
import io.jmix.security.role.assignment.RoleAssignment;
import io.jmix.security.role.assignment.RoleAssignmentRepository;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@Route(value = "users/:id", layout = MainView.class)
@ViewController(id = "User.detail")
@ViewDescriptor(path = "user-detail-view.xml")
@EditedEntityContainer("userDc")
public class UserDetailView extends StandardDetailView<User> {

    @ViewComponent
    private TypedTextField<String> usernameField;

    @Autowired
    private ResourceRoleRepository resourceRoleRepository;
    @Autowired
    private RoleAssignmentRepository roleAssignmentRepository;
    @ViewComponent
    private PasswordField passwordField;
    @ViewComponent
    private PasswordField confirmPasswordField;
    @ViewComponent
    private ComboBox<String> timeZoneField;
    @ViewComponent
    private MessageBundle messageBundle;
    @Autowired
    private Notifications notifications;
    @ViewComponent
    private JmixComboBox<ResourceRole> roleField;
    @Autowired
    private EntityStates entityStates;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @ViewComponent
    private JmixComboBox<Organization> organization;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private Environment environment;


    private boolean isOidcActive() {
        return environment.acceptsProfiles(Profiles.of("oidc"));
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        List<ResourceRole> roles = new ArrayList<>(resourceRoleRepository.getAllRoles());
        roleField.setItems(roles);
        roleField.setItemLabelGenerator(ResourceRole::getName);

        organization.setItems(organizationRepository.findAll());
        timeZoneField.setItems(List.of(TimeZone.getAvailableIDs()));
    }

    @Subscribe
    public void onInitEntity(final InitEntityEvent<User> event) {
        usernameField.setReadOnly(false);
        passwordField.setVisible(true);
        confirmPasswordField.setVisible(true);
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        if (isOidcActive()) {
            // deactivate in oidc modus, keycloak is master here
            roleField.setReadOnly(true);
            roleField.setHelperText("Rollen werden extern über Keycloak verwaltet.");
        }
        if (!entityStates.isNew(getEditedEntity())) {
            Collection<RoleAssignment> assignments = roleAssignmentRepository
                    .getAssignmentsByUsername(getEditedEntity().getUsername());

            assignments.stream()
                    .filter(a -> "resource".equals(a.getRoleType()))
                    .findFirst()
                    .ifPresent(assignment -> {
                        ResourceRole role = resourceRoleRepository.findRoleByCode(assignment.getRoleCode());
                        roleField.setValue(role);
                    });
        }
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            usernameField.focus();
        }
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        if (entityStates.isNew(getEditedEntity())
                && !Objects.equals(passwordField.getValue(), confirmPasswordField.getValue())) {
            event.getErrors().add(messageBundle.getMessage("passwordsDoNotMatch"));
        }
    }

    @Subscribe
    public void onBeforeSave(final BeforeSaveEvent event) {
        if (entityStates.isNew(getEditedEntity())) {
            getEditedEntity().setPassword(passwordEncoder.encode(passwordField.getValue()));

            notifications.create(messageBundle.getMessage("noAssignedRolesNotification"))
                    .withType(Notifications.Type.WARNING)
                    .withPosition(Notification.Position.TOP_END)
                    .show();
        }

        //give user role
        if (!isOidcActive()) {
        ResourceRole selectedRole = roleField.getValue();
        if (selectedRole != null) {
            String username = getEditedEntity().getUsername();

            // remove existing role
            List<RoleAssignmentEntity> existingAssignments = dataManager.load(RoleAssignmentEntity.class)
                    .query("select e from sec_RoleAssignmentEntity e where e.username = :username and e.roleType = :roleType")
                    .parameter("username", username)
                    .parameter("roleType", "resource")
                    .list();

            if (!existingAssignments.isEmpty()) {
                dataManager.remove(existingAssignments);
            }

            // save new role
            RoleAssignment newAssignment = new RoleAssignment(
                    username,
                    selectedRole.getCode(),
                    "resource"
            );
            dataManager.save(newAssignment);
        }
        }
    }
}
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

package eu.occtet.bocfrontend.view.main;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.User;
import eu.occtet.bocfrontend.service.UserService;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.app.main.StandardMainView;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
public class MainView extends StandardMainView {

    @Value("${spring.security.oauth2.authorizationserver.endpoint.oidc.logout-uri}")
    private String logoutUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.redirect-uri}")
    private String redirectUri;

    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Messages messages;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private UserService userService;

    @Subscribe
    private void onReady(final ReadyEvent event) {
        User sessionUser = (User) currentAuthentication.getUser();
        if (sessionUser.getOrganization() == null) {
            User freshDbUser = dataManager.load(User.class)
                    .id(sessionUser.getId())
                    .one();
            if (freshDbUser.getOrganization() != null) {
                sessionUser.setOrganization(freshDbUser.getOrganization());
                notifications.create(messages.getMessage("mainView.organizationAssigned"))
                        .withType(Notifications.Type.SUCCESS)
                        .withPosition(Notification.Position.TOP_CENTER)
                        .withDuration(3000)
                        .show();
                UI.getCurrent().getPage().reload();
            }
        } else if (sessionUser.getOrganization() != null && !userService.isAdmin()) {
            User freshDbUser = dataManager.load(User.class)
                    .id(sessionUser.getId())
                    .one();
            if (freshDbUser.getOrganization() == null) {
                notifications.create(messages.formatMessage("mainView.organizationRemoved",
                                sessionUser.getOrganization().getOrganizationName()))
                        .withType(Notifications.Type.SUCCESS)
                        .withPosition(Notification.Position.TOP_CENTER)
                        .withDuration(3000)
                        .show();
                sessionUser.setOrganization(null);
                UI.getCurrent().getPage().reload();
            }
        }
    }

    /**
     * logout action has to be connected to the keycloak logout
     * here the address has to be adjusted according do environment
     * @param event
     */
    @Subscribe("logoutAction")
    public void onLogoutAction(ActionPerformedEvent event) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OidcUser user = (OidcUser) authentication.getPrincipal();
        String idToken = user.getIdToken().getTokenValue();
        String logoutUrl = logoutUri
                + "?id_token_hint=" + idToken
                + "&post_logout_redirect_uri="+redirectUri;

        UI.getCurrent().getPage().setLocation(logoutUrl);

    }
}

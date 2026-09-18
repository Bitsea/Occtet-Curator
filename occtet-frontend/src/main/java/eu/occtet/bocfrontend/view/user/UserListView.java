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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.User;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Route(value = "users", layout = MainView.class)
@ViewController(id = "User.list")
@ViewDescriptor(path = "user-list-view.xml")
@LookupComponent("usersDataGrid")
@DialogMode(width = "64em")
public class UserListView extends StandardListView<User> {

    @Autowired
    private Environment environment;
    @ViewComponent
    private DataGrid<User> usersDataGrid;


    @Subscribe
    public void onInit(final InitEvent event) {
        if (environment.acceptsProfiles(Profiles.of("oidc"))) {
            Action createAction = usersDataGrid.getAction("createAction");
            if (createAction != null) {
                createAction.setVisible(false);
            }
        }
    }
}
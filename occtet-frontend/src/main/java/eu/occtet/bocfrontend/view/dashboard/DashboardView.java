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

package eu.occtet.bocfrontend.view.dashboard;

import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

@Route(value = "dashboard-view", layout = MainView.class)
@ViewController(id = "DashboardView")
@ViewDescriptor(path = "dashboard-view.xml")
public class DashboardView extends StandardView {

    @ViewComponent
    private CollectionLoader<Project> projectsDl;

    @ViewComponent
    private CollectionLoader<Vulnerability> vulnerabilitiesDl;

    @ViewComponent
    private DataGrid<Vulnerability> vulnerabilitiesGrid;

    @ViewComponent
    private EntityComboBox<Project> projectSelector;

    @Subscribe
    public void onInit(InitEvent event) {
        // 1. Load the projects for the dropdown
        projectsDl.load();

        // 2. Configure dropdown label (Show Project Name)
        projectSelector.setItemLabelGenerator(Project::getProjectName);

        // 3. Configure Grid Tooltips (Gracefully handles null values)
        vulnerabilitiesGrid.getColumnByKey("riskScore")
                .setTooltipGenerator(v -> v.getRiskScore() != null ?
                        "Risk Score: " + v.getRiskScore() : "No Score");

        vulnerabilitiesGrid.getColumnByKey("weightedSeverity")
                .setTooltipGenerator(v -> v.getWeightedSeverity() != null ?
                        "Weighted Severity: " + v.getWeightedSeverity() : "No Severity");
    }
}
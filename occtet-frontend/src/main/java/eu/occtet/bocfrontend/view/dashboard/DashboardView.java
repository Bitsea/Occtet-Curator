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

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vulnerability.VulnerabilityDetailView;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;


@Route(value = "dashboard-view", layout = MainView.class)
@ViewController(id = "DashboardView")
@ViewDescriptor(path = "dashboard-view.xml")
public class DashboardView extends StandardView {

    @ViewComponent
    private CollectionLoader<Project> projectsDl;

    @ViewComponent
    private CollectionLoader<Vulnerability> vulnerabilitiesDl;

    @ViewComponent
    private CollectionContainer<Vulnerability> vulnerabilitiesDc;

    @ViewComponent
    private DataGrid<Vulnerability> vulnerabilitiesGrid;

    @ViewComponent
    private EntityComboBox<Project> projectSelector;

    @ViewComponent
    protected Chart chartVulnerabilites;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private DataManager dataManager;



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

    @Subscribe("vulnerabilitiesGrid")
    public void clickOnVulnerabilitiesDataGrid(ItemDoubleClickEvent<Vulnerability> event){
        if(event.getClickCount() == 2){openDetailView(event.getItem());}
    }

    @Subscribe("projectSelector")
    public void chooseProject(AbstractField.ComponentValueChangeEvent event){
        if(event != null){
            Project project = (Project) event.getValue();
            setValuesForPieChart(project);
        }
    }

    private void openDetailView(Vulnerability vulnerability){

        DialogWindow<VulnerabilityDetailView> dialog = dialogWindows.detail(this, Vulnerability.class)
                .withViewClass(VulnerabilityDetailView.class)
                .editEntity(vulnerability)
                .build();
        dialog.setWidth("100%");
        dialog.open();
    }

    private void setValuesForPieChart(Project project) {

        Long noneRisk = dataManager.loadValues(getSpecificLoadContext(project,0,0,true))
                .getFirst().getValue("sumRiskScore");
        Long lowRisk = dataManager.loadValues(getSpecificLoadContext(project,0.1,3.9,false))
                .getFirst().getValue("sumRiskScore");
        Long mediumRisk = dataManager.loadValues(getSpecificLoadContext(project,4.0,6.9,false))
                .getFirst().getValue("sumRiskScore");
        Long highRisk = dataManager.loadValues(getSpecificLoadContext(project,7.0,8.9,false))
                .getFirst().getValue("sumRiskScore");
        Long criticalRisk = dataManager.loadValues(getSpecificLoadContext(project,9.0,10.0,false))
                .getFirst().getValue("sumRiskScore");

        ListChartItems<MapDataItem> chartItems = new ListChartItems<>(
                new MapDataItem(Map.of("description","None risk","value",noneRisk)),
                new MapDataItem(Map.of("description","Low","value",lowRisk)),
                new MapDataItem(Map.of("description","Medium","value",mediumRisk)),
                new MapDataItem(Map.of("description","High","value",highRisk)),
                new MapDataItem(Map.of("description","Critical","value",criticalRisk))
        );

        chartVulnerabilites.withDataSet(
                new DataSet().withSource(
                        new DataSet.Source<MapDataItem>()
                                .withDataProvider(chartItems)
                                .withCategoryField("description")
                                .withValueField("value")

                )
        );
    }

    private ValueLoadContext getSpecificLoadContext(Project project, double minRisk, double maxRisk, boolean isNone){

        ValueLoadContext context;

        if(isNone){
            context = new ValueLoadContext()
                    .setQuery(new ValueLoadContext.Query("""
                select count(distinct v) as sumRiskScore
                from InventoryItem i
                join i.softwareComponent s
                join s.vulnerabilities v
                join i.project p
                where p.id = :project_id and v.riskScore = 0.0
                """).setParameter("project_id",project.getId()))
                    .addProperty("sumRiskScore");

            return context;
        }else{
            context = new ValueLoadContext()
                    .setQuery(new ValueLoadContext.Query("""
                select count(distinct v) as sumRiskScore
                from InventoryItem i
                join i.softwareComponent s
                join s.vulnerabilities v
                join i.project p
                where p.id = :project_id and (v.riskScore >= :minRisk and v.riskScore <= :maxRisk)
                """).setParameter("project_id",project.getId())
                            .setParameter("minRisk",minRisk)
                            .setParameter("maxRisk", maxRisk))
                    .addProperty("sumRiskScore");

            return context;
        }
    }
}
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.boc.model.WorkTaskProgress;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.service.WorkTaskProgressMonitor;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.util.CuratorTaskUI;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vulnerability.VulnerabilityDetailView;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.MapDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.shared.Color;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.ValueLoadContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.listbox.JmixListBox;
import io.jmix.flowui.facet.Timer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Route(value = "dashboard-view", layout = MainView.class)
@ViewController(id = "DashboardView")
@ViewDescriptor(path = "dashboard-view.xml")
public class DashboardView extends StandardView {

    private static final int CURRENT_TASKS_LAST_UPDATE_THRESHOLD_MINUTES = 30;
    
    @ViewComponent
    private CollectionLoader<Project> projectsDl;

    @ViewComponent
    private DataGrid<Vulnerability> vulnerabilitiesGrid;

    @ViewComponent
    private EntityComboBox<Project> projectSelector;

    @ViewComponent
    protected Chart chartVulnerabilites;

    @ViewComponent
    protected Chart chartSoftwareComponent;

    @ViewComponent
    private JmixListBox<WorkTaskProgress> runningTasksList;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private DataManager dataManager;

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private Messages messages;

    private final static String sumRiskScore = "sumRiskScore";
    private final static String sumRiskValue = "value";
    private final static String sumRiskLevel = "Level";
    private final static String lowRisk = "Low";
    private final static String mediumRisk = "Medium";
    private final static String highRisk = "High";
    private final static String criticalRisk = "Critical";

    private final static int ZERO = 0;
    private final static double v_0_1 = 0.1;
    private final static double v_3_9 = 3.9;
    private final static double v_4_0 = 4.0;
    private final static double v_6_9 = 6.9;
    private final static double v_7_0 = 7.0;
    private final static double v_8_9 = 8.9;
    private final static double v_9_0 = 9.0;
    private final static double v_10 = 10.0;


    private static final Logger log = LogManager.getLogger(DashboardView.class);
    @Autowired
    private UiComponents uiComponents;

    @Autowired
    WorkTaskProgressMonitor workTaskProgressMonitor;

    @Subscribe
    public void onInit(InitEvent event) {
        projectsDl.load();

        projectSelector.setItemLabelGenerator(Project::getProjectName);

        vulnerabilitiesGrid.getColumnByKey("riskScore")
                .setTooltipGenerator(v -> v.getRiskScore() != null ?
                        messages.getMessage("eu.occtet.bocfrontend.view.dashboard/dashboardView.tooltip.riskScore") + ": " + v.getRiskScore() :
                        messages.getMessage("eu.occtet.bocfrontend.view.dashboard/dashboardView.tooltip.NoScore"));

        vulnerabilitiesGrid.getColumnByKey("weightedSeverity")
                .setTooltipGenerator(v -> v.getWeightedSeverity() != null ?
                        messages.getMessage("eu.occtet.bocfrontend.view.dashboard/dashboardView.tooltip.severity") + ": " + v.getWeightedSeverity() :
                        messages.getMessage("eu.occtet.bocfrontend.view.dashboard/dashboardView.tooltip.NoSeverity"));
    }

    @Subscribe("vulnerabilitiesGrid")
    public void clickOnVulnerabilitiesDataGrid(ItemDoubleClickEvent<Vulnerability> event) {
        if (event.getClickCount() == 2) {
            openDetailView(event.getItem());
        }
    }

    @Subscribe("projectSelector")
    public void chooseProject(AbstractField.ComponentValueChangeEvent event) {
        if (event != null) {
            Project project = (Project) event.getValue();
            setValuesForPieCharts(project);
        }
    }

    @Subscribe("refreshTimer")
    public void onRefreshTimerTimerAction(final Timer.TimerActionEvent event) {
        List<WorkTaskProgress> currentTasks = workTaskProgressMonitor.getAllProgress();
        log.debug("found {} tasks to display",currentTasks.size());
        runningTasksList.setItems(currentTasks);
    }

    private void openDetailView(Vulnerability vulnerability) {

        DialogWindow<VulnerabilityDetailView> dialog = dialogWindows.detail(this, Vulnerability.class)
                .withViewClass(VulnerabilityDetailView.class)
                .editEntity(vulnerability)
                .build();
        dialog.setWidth("100%");
        dialog.setHeight("100%");
        dialog.open();
    }

    private void setValuesForPieCharts(Project project) {

        //Values for vulnerability pie chart
        Long vulnerLowRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_0_1, v_3_9, DashboardQueryRisk.vulnerabilityRisk)))
                .getFirst().getValue(sumRiskScore);
        Long vulnerMediumRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_4_0, v_6_9, DashboardQueryRisk.vulnerabilityRisk)))
                .getFirst().getValue(sumRiskScore);
        Long vulnerHighRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_7_0, v_8_9, DashboardQueryRisk.vulnerabilityRisk)))
                .getFirst().getValue(sumRiskScore);
        Long vulnerCriticalRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_9_0, v_10, DashboardQueryRisk.vulnerabilityRisk)))
                .getFirst().getValue(sumRiskScore);


        //Values for softwareComponent pie chart
        Long softwareNoRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, ZERO, ZERO, DashboardQueryRisk.noRisk)))
                .getFirst().getValue(sumRiskScore);
        Long softwareLowRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_0_1, v_3_9, DashboardQueryRisk.softwareRisk)))
                .getFirst().getValue(sumRiskScore);
        Long softwareMediumRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_4_0, v_6_9, DashboardQueryRisk.softwareRisk)))
                .getFirst().getValue(sumRiskScore);
        Long softwareHighRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_7_0, v_8_9, DashboardQueryRisk.softwareRisk)))
                .getFirst().getValue(sumRiskScore);
        Long softwareCriticalRisk =
                dataManager.loadValues(Objects.requireNonNull(getSpecificLoadContext(project, v_9_0, v_10, DashboardQueryRisk.softwareRisk)))
                .getFirst().getValue(sumRiskScore);


        List<MapDataItem> vulnerabilityItems = new ArrayList<>();
        List<MapDataItem> softwareComponentItems = new ArrayList<>();
        List<Color> dynamicColors = new ArrayList<>();

        if (softwareNoRisk != null && softwareNoRisk > ZERO){
            softwareComponentItems.add(new MapDataItem(Map.of(sumRiskLevel, messages.getMessage("eu.occtet.bocfrontend.view.dashboard/dashboardView.tooltip.NoRisk"), sumRiskValue, softwareNoRisk)));
            dynamicColors.add(Color.DARKGREEN);
        }

        if ((vulnerLowRisk!= null && vulnerLowRisk > ZERO) && (softwareLowRisk != null && softwareLowRisk > ZERO)) {
            vulnerabilityItems.add(new MapDataItem(Map.of(sumRiskLevel, lowRisk, sumRiskValue, vulnerLowRisk)));
            softwareComponentItems.add(new MapDataItem(Map.of(sumRiskLevel, lowRisk, sumRiskValue, softwareLowRisk)));
            dynamicColors.add(Color.DARKORANGE);
        }

        if (vulnerMediumRisk != null && vulnerMediumRisk > ZERO && (softwareMediumRisk != null && softwareMediumRisk > ZERO)) {
            vulnerabilityItems.add(new MapDataItem(Map.of(sumRiskLevel, mediumRisk, sumRiskValue, vulnerMediumRisk)));
            softwareComponentItems.add(new MapDataItem(Map.of(sumRiskLevel, mediumRisk, sumRiskValue, softwareMediumRisk)));
            dynamicColors.add(Color.ORANGERED);
        }

        if (vulnerHighRisk != null && vulnerHighRisk > ZERO && (softwareHighRisk != null && softwareHighRisk > ZERO)) {
            vulnerabilityItems.add(new MapDataItem(Map.of(sumRiskLevel, highRisk, sumRiskValue, vulnerHighRisk)));
            softwareComponentItems.add(new MapDataItem(Map.of(sumRiskLevel, highRisk, sumRiskValue, softwareHighRisk)));
            dynamicColors.add(Color.FIREBRICK);
        }

        if (vulnerCriticalRisk != null && vulnerCriticalRisk > ZERO && (softwareCriticalRisk != null && softwareCriticalRisk > ZERO)) {
            vulnerabilityItems.add(new MapDataItem(Map.of(sumRiskLevel, criticalRisk, sumRiskValue, vulnerCriticalRisk)));
            softwareComponentItems.add(new MapDataItem(Map.of(sumRiskLevel, criticalRisk, sumRiskValue, softwareCriticalRisk)));
            dynamicColors.add(Color.DARKRED);
        }

        ListChartItems<MapDataItem> vulnerabilityChartItems = new ListChartItems<>(vulnerabilityItems);
        ListChartItems<MapDataItem> softwareChartItems = new ListChartItems<>(softwareComponentItems);

        if (!dynamicColors.isEmpty()) {
            if(dynamicColors.size() == 1 && dynamicColors.contains(Color.DARKGREEN)){
                chartSoftwareComponent.setColorPalette(dynamicColors.toArray(new Color[ZERO]));
            }
            chartSoftwareComponent.setColorPalette(dynamicColors.toArray(new Color[ZERO]));
            List<Color> vDynamicColors = dynamicColors.stream().filter(c -> !c.equals(Color.DARKGREEN)).toList();
            chartVulnerabilites.setColorPalette(vDynamicColors.toArray(new Color[ZERO]));
        }
        setChartDataSet(chartVulnerabilites,vulnerabilityChartItems);
        setChartDataSet(chartSoftwareComponent,softwareChartItems);

    }

    private void setChartDataSet(Chart chart, ListChartItems<MapDataItem> listChartItems){

        chart.withDataSet(
                new DataSet().withSource(
                        new DataSet.Source<MapDataItem>()
                                .withDataProvider(listChartItems)
                                .withCategoryField(sumRiskLevel)
                                .withValueField(sumRiskValue)
                )
        );
    }

    private ValueLoadContext getSpecificLoadContext(Project project, double minRisk, double maxRisk, DashboardQueryRisk query) {

        switch(query){
            case noRisk -> {
                return new ValueLoadContext()
                        .setQuery(new ValueLoadContext.Query("""
                        select count(s) as sumRiskScore
                        from InventoryItem i
                        join i.softwareComponent s
                        join i.project p
                        where p.id = :project_id and s.vulnerabilities is empty
                        """).setParameter("project_id", project.getId()))
                        .addProperty("sumRiskScore");
            }
            case vulnerabilityRisk -> {
                return new ValueLoadContext()
                        .setQuery(new ValueLoadContext.Query("""
                        select count(distinct v) as sumRiskScore
                        from InventoryItem i
                        join i.softwareComponent s
                        join s.vulnerabilities v
                        join i.project p
                        where p.id = :project_id and (v.riskScore >= :minRisk and v.riskScore <= :maxRisk)
                        """).setParameter("project_id", project.getId())
                                .setParameter("minRisk", minRisk)
                                .setParameter("maxRisk", maxRisk))
                        .addProperty("sumRiskScore");
            }
            case softwareRisk -> {
                return new ValueLoadContext()
                        .setQuery(new ValueLoadContext.Query("""
                        select count(s) as sumRiskScore
                        from InventoryItem i
                        join i.softwareComponent s
                        join s.vulnerabilities v
                        join i.project p
                        where p.id = :project_id and (v.riskScore >= :minRisk and v.riskScore <= :maxRisk)
                        """).setParameter("project_id", project.getId())
                                .setParameter("minRisk", minRisk)
                                .setParameter("maxRisk", maxRisk))
                        .addProperty("sumRiskScore");
            }
            default -> {
                return null;
            }
        }
    }

    @Supply(to = "runningTasksList", subject = "renderer")
    private ComponentRenderer runningTasksListRenderer() {
        return new ComponentRenderer<HorizontalLayout,WorkTaskProgress>(task -> {
            HorizontalLayout row = uiComponents.create(HorizontalLayout.class);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            Icon icon = CuratorTaskUI.iconForTaskStatus(task.getStatus());
            icon.setSize("10px");
            row.add(icon );
            row.add(new Span(task.getName() + " ("+ task.getDetails() + ", "+task.getPercent() + "%)"));
            return row;
        }); 
    }
    
    
}
/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.bocfrontend.view.audit;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.dataview.GridDataView;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.*;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.model.FileTreeNode;
import eu.occtet.bocfrontend.service.AuditViewStateService;
import eu.occtet.bocfrontend.service.FileContentService;
import eu.occtet.bocfrontend.service.FileTreeCacheService;
import eu.occtet.bocfrontend.view.audit.helper.FileTreeGridFactory;
import eu.occtet.bocfrontend.view.audit.helper.TabManager;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.*;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Route(value = "audit-view/:projectId?", layout = MainView.class)
@ViewController(id = "AuditView")
@ViewDescriptor(path = "audit-view.xml")
public class AuditView extends StandardView implements BeforeEnterObserver {

    private static final Logger log = LogManager.getLogger(AuditView.class);

    @Autowired private ProjectRepository projectRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private Dialogs dialogs;
    @Autowired private Notifications notifications;
    @Autowired private FileContentService fileContentService;
    @Autowired private DataManager dataManager;
    @Autowired private FileTreeCacheService fileTreeCacheService;
    @Autowired private AuditViewStateService viewStateService;
    @Autowired private FileTreeGridFactory fileTreeGridFactory;
    @Autowired private UiComponents uiComponents;
    @Autowired private Fragments fragments;

    @ViewComponent private DataContext dataContext;
    @ViewComponent private JmixComboBox<Project> projectComboBox;
    @ViewComponent private CollectionContainer<InventoryItem> inventoryItemDc;
    @ViewComponent private JmixTabSheet inventoryItemTabSheet;
    @ViewComponent private Tab inventoryItemSection;
    @ViewComponent private JmixTabSheet filesTabSheet;
    @ViewComponent private Tab filesSection;
    @ViewComponent private JmixTabSheet mainTabSheet;
    @ViewComponent private TreeDataGrid<InventoryItem> inventoryItemDataGrid;
    @ViewComponent private VerticalLayout fileTreeGridLayout;
    @ViewComponent private CollectionLoader<InventoryItem> inventoryItemDl;

    private TabManager tabManager;
    private TreeDataGrid<FileTreeNode> fileTreeGrid;
    private Map<UUID, Long> fileCounts = new HashMap<>();
    private boolean suppressNavigation = false;

    @Subscribe
    protected void onInit(InitEvent event) {
        initializeProjectComboBox();
        initializeInventoryDataGrid();
        initializeTabManager();
        addTabSelectionListeners();
    }

    private void addTabSelectionListeners() {
        inventoryItemTabSheet.addSelectedChangeListener(e -> handleTabSelectionChange());
        filesTabSheet.addSelectedChangeListener(e -> handleTabSelectionChange());
    }

    private void handleTabSelectionChange() {
        Serializable activeIdentifier = tabManager.getActiveTabIdentifier();
        onTabChange(activeIdentifier);
    }

    private void initializeProjectComboBox() {
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
    }

    private void initializeInventoryDataGrid() {
        inventoryItemDataGrid.setTooltipGenerator(InventoryItem::getInventoryName);
    }

    private void initializeTabManager() {
        this.tabManager = new TabManager.Builder(fragments, dialogs, fileContentService, notifications)
                .withDataContext(dataContext)
                .withInventoryItemTabSection(inventoryItemSection)
                .withInventoryItemTabSheet(inventoryItemTabSheet)
                .withFilesTabSection(filesSection)
                .withFilesTabSheet(filesTabSheet)
                .withMainTabSheet(mainTabSheet)
                .withTabChangeCallback(this::onTabChange)
                .withHostView(this)
                .build();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RouteParameters params = event.getRouteParameters();

        // Extract URL parameters
        Optional<String> projectIdParam = params.get("projectId");

        if (projectIdParam.isPresent()) {
            handleUrlNavigation(projectIdParam.get());
        } else {
            handleSessionRestoration();
        }
    }

    private void handleUrlNavigation(String projectIdStr) {
        suppressNavigation = true;
        try {
            UUID projectId = UUID.fromString(projectIdStr);
            projectRepository.findById(projectId).ifPresentOrElse(
                    project -> {
                        projectComboBox.setValue(project);
                        refreshAllDataForProject(project);

                        // Restore session tabs first
                        viewStateService.get()
                                .filter(state -> state.projectId().equals(projectId))
                                .ifPresent(state -> {
                                    restoreSessionTabs(state);

                                    log.debug("Restoring state from session: {}", state.activeTabIdentifier());
                                    if (state.activeTabIdentifier() != null) {
                                        UI.getCurrent().access(() -> {
                                            tabManager.selectTab(state.activeTabIdentifier());
                                        });
                                    }
                                });
                    },
                    () -> {
                        handleSessionRestoration();
                    }
            );
        } catch (IllegalArgumentException e) {
            log.warn("Invalid project ID in URL: {}", projectIdStr);
            handleSessionRestoration();
        } finally {
            suppressNavigation = false;
        }
    }

    private void handleSessionRestoration() {
        viewStateService.get().ifPresent(state -> {
            projectRepository.findById(state.projectId()).ifPresent(project -> {
                suppressNavigation = true;
                try {
                    projectComboBox.setValue(project);
                    refreshAllDataForProject(project);
                    restoreSessionTabs(state);

                    // Select the previously active tab
                    log.debug("Restoring state from session: {}", state.activeTabIdentifier());
                    if (state.activeTabIdentifier() != null) {
                        UI.getCurrent().access(() -> {
                            tabManager.selectTab(state.activeTabIdentifier());
                        });
                    }
                } finally {
                    suppressNavigation = false;
                }
            });
        });
    }

    private void restoreSessionTabs(AuditViewStateService.AuditViewState state) {
        // Restore inventory item tabs without auto-selecting them
        state.openInventoryTabsIds().stream()
                .flatMap(id -> inventoryItemRepository.findById(id).stream())
                .forEach(item -> tabManager.openInventoryItemTab(item,false));

        // Restore file tabs without auto-selecting them
        state.openFileTabsPaths().forEach(nodePath -> {
            fileTreeCacheService.findNodeByPath(projectComboBox.getValue(), nodePath)
                    .ifPresent(node -> tabManager.openFileTab(node, false));
        });

//        // Select the previously active tab with a slight delay
//        tabManager.selectTab(state.activeTabIdentifier());
    }

    private Optional<FileTreeNode> findNodeByPath(String fullPath) {
        if (fileTreeGrid == null) return Optional.empty();

        DataProvider<FileTreeNode, ?> dataProvider = fileTreeGrid.getDataProvider();
        if (dataProvider instanceof GridDataView) {
            return ((GridDataView<FileTreeNode>) dataProvider).getItems()
                    .filter(node -> fullPath.equals(node.getFullPath()))
                    .findFirst();
        }
        return Optional.empty();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        saveStateToSession();
    }

    private void saveStateToSession() {
        Project selectedProject = projectComboBox.getValue();
        if (selectedProject == null) {
            viewStateService.clear();
            return;
        }

        var state = new AuditViewStateService.AuditViewState(
                selectedProject.getId(),
                tabManager.getOpenInventoryItemIds(),
                tabManager.getOpenFilePaths(),
                tabManager.getActiveTabIdentifier()
        );
        log.debug("Saving state to session: {}", state);
        viewStateService.save(state);
    }

    private void onTabChange(Serializable activeIdentifier) {
        if (suppressNavigation) {
            log.trace("Navigation suppressed — skipping URL update for {}", activeIdentifier);
            return;
        }
        log.debug("Tab changed, updating URL for {}", activeIdentifier);
        saveStateToSession();
    }

    private void updateUrl(Serializable activeIdentifier) {
        if (suppressNavigation) {
            log.trace("Suppressing navigation (updateUrl) to avoid recursion");
            return;
        }
        Project selectedProject = projectComboBox.getValue();
        if (selectedProject == null) return;

        try {
            UI.getCurrent().navigate(AuditView.class, new RouteParameters(new RouteParam("projectId",
                    selectedProject.getId().toString())));

        } finally {
            suppressNavigation = false;
        }
    }

    private void refreshAllDataForProject(Project project) {
        if (project == null) {
            clearView();
            return;
        }
        refreshInventoryItemDc(project);
        rebuildFileTree(project);
    }

    public void refreshInventoryItemDc(Project project) {
        inventoryItemDl.setParameter("project", project);
        inventoryItemDl.load();
        loadFileCounts(project);
    }

    private void loadFileCounts(Project project) {
        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query(
                        "select cl.inventoryItem.id as itemId, count(cl) as fileCount from CodeLocation cl " +
                                "where cl.inventoryItem.project = :project group by cl.inventoryItem.id")
                        .setParameter("project", project))
                .addProperty("itemId")
                .addProperty("fileCount");

        List<KeyValueEntity> counts = dataManager.loadValues(context);
        this.fileCounts = counts.stream()
                .collect(Collectors.toMap(
                        kv -> kv.getValue("itemId"),
                        kv -> (Long) kv.getValue("fileCount")
                ));
    }

    @Supply(to = "inventoryItemDataGrid.fileNumCol", subject = "renderer")
    Renderer<InventoryItem> filesCountRenderer() {
        return new TextRenderer<>(item -> fileCounts.getOrDefault(item.getId(), 0L).toString());
    }

    private void rebuildFileTree(Project project) {
        List<FileTreeNode> rootNodes = fileTreeCacheService.getFileTree(project);
        this.fileTreeGrid = fileTreeGridFactory.create(
                rootNodes,
                fileTreeNode -> tabManager.openFileTab(fileTreeNode, true),
                inventoryItem -> tabManager.openInventoryItemTab(inventoryItem, true)
        );

        fileTreeGridLayout.removeAll();
        fileTreeGridLayout.add(addExpandCollapseAllButtons(fileTreeGrid));
        fileTreeGridLayout.add(this.fileTreeGrid);
    }

    @Subscribe("projectComboBox")
    public void onProjectComboBoxValueChange(
            final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (suppressNavigation) {
            return;
        }

        if (event.getValue() == event.getOldValue()) return;

        if (event.isFromClient() && tabManager.hasOpenTabs()) {
            dialogs.createOptionDialog()
                    .withHeader("Change Project")
                    .withText("Do you want to close all tabs from the previous project?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(e -> {
                                tabManager.closeAllTabs();
                                switchProject(event.getValue());
                            }),
                            new DialogAction(DialogAction.Type.NO)
                                    .withText("Keep Tabs")
                                    .withHandler(e -> switchProject(event.getValue()))
                    ).open();
        } else {
            switchProject(event.getValue());
        }
    }

    private void switchProject(Project project) {
        refreshAllDataForProject(project);
        saveStateToSession();
        updateUrl(null); // Update URL to show only project
    }

    @Subscribe("inventoryItemDataGrid")
    public void onInventoryItemDataGridClick(final ItemClickEvent<InventoryItem> event) {
        if (event.getClickCount() == 2) {
            tabManager.openInventoryItemTab(event.getItem(), true);
        } else {
            toggleExpansion(inventoryItemDataGrid, event.getItem());
        }
    }

    private <T> void toggleExpansion(TreeDataGrid<T> grid, T item) {
        if (grid.isExpanded(item)) {
            grid.collapse(item);
        } else {
            grid.expand(item);
        }
    }

    private void clearView() {
        inventoryItemDc.setItems(Collections.emptyList());
        fileTreeGridLayout.removeAll();
        tabManager.closeAllTabs();
    }

    public <T> HorizontalLayout addExpandCollapseAllButtons(TreeDataGrid<T> grid) {
        JmixButton expandAll = uiComponents.create(JmixButton.class);
        expandAll.setText("Expand all");
        expandAll.addClickListener(event -> expandChildrenOfRoots(grid));

        JmixButton collapseAll = uiComponents.create(JmixButton.class);
        collapseAll.setText("Collapse all");
        collapseAll.addClickListener(event -> collapseChildrenOfRoots(grid));

        HorizontalLayout toolbar = uiComponents.create(HorizontalLayout.class);
        toolbar.setWidthFull();
        toolbar.setSpacing(true);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        toolbar.add(expandAll, collapseAll);
        return toolbar;
    }

    @Subscribe("expandBtn")
    protected void onExpandBtnClick(final ClickEvent<JmixButton> event) {
        expandChildrenOfRoots(inventoryItemDataGrid);
    }

    @Subscribe("collapseBtn")
    protected void onCollapseBtnClick(final ClickEvent<JmixButton> event) {
        collapseChildrenOfRoots(inventoryItemDataGrid);
    }

    private <T> void expandChildrenOfRoots(TreeDataGrid<T> grid) {
        if (grid == null) return;
        iterateGridItems(grid, grid::expand);
    }

    private <T> void collapseChildrenOfRoots(TreeDataGrid<T> grid) {
        if (grid == null) return;
        iterateGridItems(grid, grid::collapse);
    }

    private <T> void iterateGridItems(TreeDataGrid<T> grid, Consumer<T> action) {
        DataProvider<T, ?> dp = grid.getDataProvider();
        if (dp instanceof GridLazyDataView) {
            ((GridLazyDataView<T>) dp).getItems().forEach(action);
        } else if (dp instanceof GridDataView) {
            ((GridDataView<T>) dp).getItems().forEach(action);
        }
    }
}
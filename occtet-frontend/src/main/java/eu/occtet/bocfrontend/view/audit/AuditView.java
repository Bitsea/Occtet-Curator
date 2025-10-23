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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
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
import eu.occtet.bocfrontend.view.audit.helper.*;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.*;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
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
import java.util.stream.Collectors;

/**
 * AuditView serves as a user interface controller for managing and displaying audit-related data,
 * including projects, inventory items, files, and tabs.
 * <ul>
 * <li>Manages project and inventory contexts and maintains the UI state.</li>
 * <li>Supports user interactions such as switching tabs, selecting projects, and navigating between views.</li>
 * <li>Implements state persistence by saving and restoring the session state.</li>
 * <li>Handles file counts, inventory data, and other project-related data management functionalities.</li>
 * </ul>
 */
@Route(value = "audit-view/:projectId?", layout = MainView.class)
@ViewController(id = "AuditView")
@ViewDescriptor(path = "audit-view.xml")
public class AuditView extends StandardView{

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
    @Autowired private ComponentFactory toolBoxFactory;
    @Autowired private TreeGridHelper treeGridHelper;
    @Autowired private RendererFactory rendererFactory;


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
    @ViewComponent HorizontalLayout toolbarBox;

    private TabManager tabManager;
    private Map<UUID, Long> fileCounts = new HashMap<>();
    private boolean suppressNavigation = false;

    /**
     * Handles actions to be performed before the view is entered. This method ensures
     * proper initialization of the project context based on route parameters or session state.
     *
     * @param event the event triggered before navigating to this view
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
        log.debug("BeforeEnterEvent called");
        Optional<String> projectIdParam = event.getRouteParameters().get("projectId");
        initializeProjectContext(projectIdParam);
    }

    /**
     * Initializes the AuditView. Executes setup for the project combo box,
     * inventory data grid, tab manager, and tab selection listeners.
     *
     * @param event the initialization event invoked during view initialization
     */
    @Subscribe
    protected void onInit(InitEvent event) {
        initializeProjectComboBox();
        initializeInventoryDataGrid();
        initializeTabManager();
        addTabSelectionListeners();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        saveStateToSession();
    }

    /**
     * Initializes the project context for the AuditView. This method determines the appropriate
     * project to load based on the provided parameter or the saved session state. If no project ID
     * is available, it clears the view.
     *
     * @param projectIdParam an optional parameter containing the project ID from the URL or other sources
     */
    private void initializeProjectContext(Optional<String> projectIdParam) {
        if (projectIdParam.isPresent()) {
            loadProjectFromUrl(projectIdParam.get());
            return;
        }

        viewStateService.get()
                .map(AuditViewStateService.AuditViewState::projectId)
                .ifPresentOrElse(
                        projectId -> {
                            log.debug("No projectId in URL, redirecting to session project {}", projectId);
                            UI.getCurrent().getPage().getHistory()
                                    .replaceState(null, "audit-view/" + projectId);
                            loadProjectFromUrl(projectId.toString());
                        },
                        () -> {
                            log.debug("No project found in session; clearing view");
                            clearView();
                        });
    }

    private void loadProjectFromUrl(String projectIdStr) {
        suppressNavigation = true;
        try {
            UUID projectId = UUID.fromString(projectIdStr);
            projectRepository.findById(projectId).ifPresent(project -> {
                log.debug("Loading project {} from URL", project.getProjectName());
                projectComboBox.setValue(project);
                refreshAllDataForProject(project);
                restoreTabsAndState();
            });
        } catch (Exception e) {
            log.warn("Invalid projectId in URL: {}", projectIdStr, e);
        } finally {
            suppressNavigation = false;
        }
    }

    private void restoreTabsAndState() {
        viewStateService.get().ifPresent(state -> {
            restoreSessionTabs(state);

            log.debug("Restoring session state: {}", state);
            if (state.activeTabIdentifier() != null) {
                UI ui = UI.getCurrent();
                ui.access(() -> {
                    tabManager.selectTab(state.activeTabIdentifier());
                    if (state.activeTabIdentifier() instanceof InventoryItem) {
                        mainTabSheet.setSelectedTab(inventoryItemSection);
                    } else if (state.activeTabIdentifier() instanceof FileTreeNode) {
                        mainTabSheet.setSelectedTab(filesSection);
                    }
                });
            }
        });
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
        HorizontalLayout inventoryToolbar = toolBoxFactory.createToolBox(inventoryItemDataGrid, true, false);
        toolbarBox.removeAll();
        toolbarBox.add(inventoryToolbar);
        toolBoxFactory.createInfoButtonHeaderForInventoryGrid(inventoryItemDataGrid, "status");

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

    private void restoreSessionTabs(AuditViewStateService.AuditViewState state) {
        // Restore inventory item tabs without auto-selecting them
        state.openInventoryTabsIds().stream()
                .flatMap(id -> inventoryItemRepository.findById(id).stream())
                .forEach(item -> tabManager.openInventoryItemTab(item,false));

        // Restore file tabs without auto-selecting them
        state.openFileTabsPaths().forEach(nodePath -> fileTreeCacheService.findNodeByPath(projectComboBox.getValue(), nodePath)
                .ifPresent(node -> tabManager.openFileTab(node, false)));
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

    private void updateUrl() {
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
                        kv -> kv.getValue("fileCount")
                ));
    }

    @Supply(to = "inventoryItemDataGrid.fileNumCol", subject = "renderer")
    Renderer<InventoryItem> filesCountRenderer() {
        return rendererFactory.filesCountRenderer(projectComboBox.getValue());
    }

    @Supply(to = "inventoryItemDataGrid.status", subject = "renderer")
    Renderer<InventoryItem> statusRenderer() {
        return rendererFactory.statusRenderer();
    }

    private void rebuildFileTree(Project project) {
        List<FileTreeNode> rootNodes = fileTreeCacheService.getFileTree(project);
        TreeDataGrid<FileTreeNode> fileTreeGrid = fileTreeGridFactory.create(
                rootNodes,
                node -> tabManager.openFileTab(node, true),
                item -> tabManager.openInventoryItemTab(item, true)
        );

        fileTreeGridLayout.removeAll();
        HorizontalLayout toolbar = toolBoxFactory.createToolBox(fileTreeGrid, false, true);
        fileTreeGridLayout.add(toolbar, fileTreeGrid);
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
        updateUrl();
    }

    @Subscribe("inventoryItemDataGrid")
    public void onInventoryItemDataGridClick(final ItemClickEvent<InventoryItem> event) {
        if (event.getClickCount() == 2) {
            tabManager.openInventoryItemTab(event.getItem(), true);
        } else {
            treeGridHelper.toggleExpansion(inventoryItemDataGrid, event.getItem());
        }
    }



    private void clearView() {
        inventoryItemDc.setItems(Collections.emptyList());
        fileTreeGridLayout.removeAll();
        tabManager.closeAllTabs();
    }
}
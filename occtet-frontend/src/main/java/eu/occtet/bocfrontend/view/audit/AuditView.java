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

package eu.occtet.bocfrontend.view.audit;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.*;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.engine.TabManager;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.factory.ComponentFactory;
import eu.occtet.bocfrontend.factory.FileTreeGridFactory;
import eu.occtet.bocfrontend.factory.RendererFactory;
import eu.occtet.bocfrontend.model.FileTreeNode;
import eu.occtet.bocfrontend.service.*;
import eu.occtet.bocfrontend.view.audit.fragment.OverviewProjectTabFragment;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.*;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.model.*;
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
    @Autowired private Fragments fragments;
    @Autowired private Dialogs dialogs;
    @Autowired private Notifications notifications;
    @Autowired private DataManager dataManager;
    @Autowired private FileContentService fileContentService;
    @Autowired private FileTreeCacheService fileTreeCacheService;
    @Autowired private AuditViewStateService viewStateService;
    @Autowired private TreeGridHelper treeGridHelper;
    @Autowired private FileTreeGridFactory fileTreeGridFactory;
    @Autowired private ComponentFactory componentFactory;
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
    @ViewComponent OverviewProjectTabFragment overviewProjectTabFragment;


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
        overviewProjectTabFragment.setHostView(this);
        overviewProjectTabFragment.setDefaultAccordionValues();
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

    /**
     * Loads a project based on its ID string passed via URL. This method attempts to parse the project ID,
     * fetches the project from the repository, and updates the UI state accordingly. It handles invalid
     * IDs gracefully by logging the error and ensuring navigation suppression state is properly managed.
     *
     * @param projectIdStr the string representation of the project ID to be loaded from the URL
     */
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

    /**
     * Restores the active tabs and session state for the AuditView.
     *
     * This method retrieves the previously saved state of the AuditView using
     * the viewStateService. It restores the session tabs, including
     * inventory item and file tree tabs, and attempts to reselect the last active
     * tab based on the identifier stored in the state. If an active tab is found,
     * it updates the UI components appropriately.
     */
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
        HorizontalLayout inventoryToolbox = componentFactory.createToolBox(
                inventoryItemDataGrid, InventoryItem.class);
        // Find the vulnerability filter checkbox and add a value change listener to it.
        findCheckBoxById(inventoryToolbox, componentFactory.getVulnerabilityFilterId())
                .ifPresentOrElse(checkbox -> {
                    checkbox.addValueChangeListener(event ->
                                    onVulnerabilityFilterToggled(Boolean.TRUE.equals(event.getValue())));
                }, () -> log.warn("Unable to find vulnerability filter checkbox in inventory toolbox")
                );
        toolbarBox.removeAll();
        toolbarBox.add(inventoryToolbox);
        componentFactory.createInfoButtonHeaderForInventoryGrid(inventoryItemDataGrid, "status");

        inventoryItemDataGrid.setTooltipGenerator(InventoryItem::getInventoryName);
    }

    private Optional<Checkbox> findCheckBoxById(Component container, String id){
        return container.getChildren()
                .filter(child -> id.equals(child.getId().orElse(null)))
                .filter(child -> child instanceof Checkbox)
                .map(child -> (Checkbox) child)
                .findFirst();
    }

    /**
     * Toggles the vulnerability filter for inventory items. Updates the data loader parameter to
     * filter items based on their vulnerability status and reloads the data.
     *
     * @param vulnerableOnly a boolean indicating whether to display only vulnerable inventory items (true)
     *                        or all items (false)
     */
    private void onVulnerabilityFilterToggled(boolean vulnerableOnly) {
        Project project = projectComboBox.getValue();
        if (project == null) return;

        inventoryItemDl.setParameter("vulnerableOnly", vulnerableOnly);
        inventoryItemDl.load();
        loadFileCounts(project);
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


    /**
     * Restores the session tabs for the AuditView, including inventory item tabs and file tabs,
     * without automatically selecting them.
     *
     * @param state containing the previously saved open inventory and file tabs state
     */
    private void restoreSessionTabs(AuditViewStateService.AuditViewState state) {
        // Restore inventory item tabs without auto-selecting them
        state.openInventoryTabsIds().stream()
                .flatMap(id -> inventoryItemRepository.findById(id).stream())
                .forEach(item -> tabManager.openInventoryItemTab(item,false));

        // Restore file tabs without auto-selecting them
        state.openFileTabsPaths().forEach(nodePath -> fileTreeCacheService.findNodeByPath(projectComboBox.getValue(), nodePath)
                .ifPresent(node -> tabManager.openFileTab(node, false)));
    }

    /**
     * Saves the current state of the AuditView to the user's session.
     *
     * This method captures the active state of the application, including the selected project,
     * open inventory item tabs, open file tabs, and the currently active tab identifier.
     * If no project is selected, the session state is cleared.
     */
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


    /**
     * Handles changes to the active tab in the AuditView.
     *
     * This method updates the application's state or URL when the active tab changes.
     *
     * @param activeIdentifier the identifier of the newly active tab
     */
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
            UI.getCurrent().access(() -> {
                UI.getCurrent().navigate(AuditView.class, new RouteParameters(
                        new RouteParam("projectId", selectedProject.getId().toString())
                ));
            });

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
        inventoryItemDl.setParameter("vulnerableOnly", false);
        inventoryItemDl.load();
        loadFileCounts(project);
    }


    /**
     * Loads the count of files associated with inventory items for the specified project.
     * This method queries the database for each inventory item in the project,
     * retrieves the count of associated files, and updates the internal file counts map.
     * It also refreshes the data in the inventory item data grid.
     *
     * @param project the project whose inventory item file counts are to be loaded
     */
    private void loadFileCounts(Project project) {
        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query("""
                            select cl.inventoryItem.id as itemId, count(cl) as fileCount
                            from CodeLocation cl
                            where cl.inventoryItem.project = :project
                            group by cl.inventoryItem.id
                        """)
                        .setParameter("project", project))
                .addProperty("itemId")
                .addProperty("fileCount");

        List<KeyValueEntity> counts = dataManager.loadValues(context);
        this.fileCounts = counts.stream()
                .collect(Collectors.toMap(
                        kv -> kv.getValue("itemId"),
                        kv -> kv.getValue("fileCount")
                ));

        inventoryItemDataGrid.getDataProvider().refreshAll();
    }

    @Supply(to = "inventoryItemDataGrid.fileNumCol", subject = "renderer")
    Renderer<InventoryItem> filesCountRenderer() {
        return rendererFactory.filesCountRenderer(() -> fileCounts != null ? fileCounts : Collections.emptyMap());
    }

    @Supply(to = "inventoryItemDataGrid.status", subject = "renderer")
    Renderer<InventoryItem> statusRenderer() {
        return rendererFactory.statusRenderer();
    }

    private void rebuildFileTree(Project project) {
        List<FileTreeNode> rootNodes = fileTreeCacheService.getFileTree(project);
        FileTreeGridFactory.TreeGridWithFilter treeWithFilter  =
                fileTreeGridFactory.createTreeGridWithFilter(rootNodes,
                        node -> tabManager.openFileTab(node, true),
                        item -> tabManager.openInventoryItemTab(item, true)
                );
        fileTreeGridLayout.removeAll();

        HorizontalLayout toolBox = componentFactory.createToolBox(treeWithFilter.grid(), FileTreeNode.class);
        toolBox.addComponentAsFirst(treeWithFilter.filterField());
        fileTreeGridLayout.add(toolBox);
        fileTreeGridLayout.add(treeWithFilter.grid());
    }

    /**
     * Handles the value change event for the projectComboBox component.
     * This method updates the current project context, manages active tabs,
     * and prompts the user to confirm actions if necessary.
     *
     * @param event contains old and new selected project,
     */
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
        overviewProjectTabFragment.setProjectOverview(event.getValue());
    }

    private void switchProject(Project project) {
        refreshAllDataForProject(project);
        saveStateToSession();
        updateUrl();
    }

    @Subscribe("inventoryItemDataGrid")
    public void onInventoryItemDataGridClick(final ItemClickEvent<InventoryItem> event) {
        if (event.getClickCount() == 2) {
            inventoryItemSection.setVisible(true);
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

    public void handleInventoryItemFromOverview(InventoryItem item){
        if(!inventoryItemSection.isVisible()){
            inventoryItemSection.setVisible(true);
        }
        tabManager.openInventoryItemTab(item,true);
    }
}
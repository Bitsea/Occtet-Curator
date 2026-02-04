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


import com.vaadin.flow.component.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.*;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.factory.UiComponentFactory;
import eu.occtet.bocfrontend.factory.RendererFactory;
import eu.occtet.bocfrontend.model.FileReviewedFilterMode;
import eu.occtet.bocfrontend.service.*;
import eu.occtet.bocfrontend.view.audit.fragment.OverviewProjectTabFragment;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.*;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.*;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AuditView serves as the central user interface controller for the audit workflow,
 * orchestrating the management and display of projects, inventory items, and file hierarchies.
 *
 * <p>Key functionalities include:</p>
 * <ul>
 * <li><b>Context Management:</b> Manages project and inventory contexts and maintains the UI state.</li>
 * <li><b>Advanced File Search:</b> Implements hierarchy-aware search logic that visualizes search results by expanding paths to matches while keeping the structure navigable.</li>
 * <li><b>State Persistence:</b> Implements comprehensive session state saving and restoring, covering:
 * <ul>
 * <li>Selected Project and active navigation.</li>
 * <li>Open Inventory and File tabs.</li>
 * <li><b>Expansion State:</b> Tracks and restores expanded nodes within the file tree to preserve the user's view across refreshes.</li>
 * </ul>
 * </li>
 * <li><b>Tree & Tab Synchronization:</b> Ensures that file paths to open tabs are automatically expanded and visible in the grid upon session restoration.</li>
 * </ul>
 */
@Route(value = "audit-view/:projectId?", layout = MainView.class)
@ViewController(id = "AuditView")
@ViewDescriptor(path = "audit-view.xml")
@CssImport(themeFor = "vaadin-grid", value = "./themes/BocFrontend/BocFrontend.css")
public class AuditView extends StandardView{

    private static final Logger log = LogManager.getLogger(AuditView.class);

    @Autowired private ProjectRepository projectRepository;
    @Autowired private FileRepository fileRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;

    @Autowired private Fragments fragments;
    @Autowired private Dialogs dialogs;
    @Autowired private Notifications notifications;
    @Autowired private DataManager dataManager;

    @Autowired private FileContentService fileContentService;
    @Autowired private AuditViewStateService viewStateService;

    @Autowired private TreeGridHelper treeGridHelper;

    @Autowired private UiComponentFactory componentFactory;
    @Autowired private RendererFactory rendererFactory;

    @ViewComponent private DataContext dataContext;
    @ViewComponent private CollectionContainer<InventoryItem> inventoryItemDc;
    @ViewComponent private CollectionLoader<InventoryItem> inventoryItemDl;
    @ViewComponent private CollectionContainer<File> fileDc;

    @ViewComponent private JmixComboBox<Project> projectComboBox;
    @ViewComponent private JmixTabSheet inventoryItemTabSheet;
    @ViewComponent private Tab inventoryItemSection;
    @ViewComponent private JmixTabSheet filesTabSheet;
    @ViewComponent private Tab filesSection;
    @ViewComponent private JmixTabSheet mainTabSheet;
    @ViewComponent private TreeDataGrid<InventoryItem> inventoryItemDataGrid;
    @ViewComponent private TreeDataGrid<File> fileTreeGrid;
    @ViewComponent private HorizontalLayout toolbarBox;
    @ViewComponent private VerticalLayout fileTreeGridLayout;
    @ViewComponent private OverviewProjectTabFragment overviewProjectTabFragment;

    private TabManager tabManager;
    private Map<Long, Long> fileCounts = new HashMap<>();
    private boolean suppressNavigation = false;
    private final Set<Long> expandedItemIds = new HashSet<>();

    private FileTreeSearchHelper fileTreeSearchHelper;
    @Autowired
    private TransactionTemplate transactionTemplate; // important for fileTreeSearchHelper

    @Autowired
    private Messages messages;
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
        initializeTabManager();
        initializeProjectComboBox();
        initializeInventoryDataGrid();
        initializeFileTreeGrid();
        addTabSelectionListeners();
        overviewProjectTabFragment.setHostView(this);
        overviewProjectTabFragment.setDefaultAccordionValues();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        saveStateToSession();
    }

    /**
     * Initializes the file tree grid, including UI components, event listeners, and context menu setup.
     * Configures the grid's behavior for filtering, searching, expanding, collapsing, and handling item selection.
     *
     * This method performs the following operations:
     * - Creates a file tree toolbox layout and adds it to the grid layout.
     * - Configures filter logic to handle file review filtering based on user selections.
     * - Implements search functionalities, including field interaction, navigation between search results,
     *   and counting matching items.
     * - Sets up context menu options and double-click behavior for grid items.
     * - Tracks the expansion and collapse state of items in the grid to maintain UI consistency.
     */
    private void initializeFileTreeGrid() {
        FlexLayout toolboxWrapper = componentFactory.createFileTreeToolbox(fileTreeGrid);
        fileTreeGridLayout.addComponentAsFirst(toolboxWrapper);

        JmixComboBox<FileReviewedFilterMode> filterBox = (JmixComboBox<FileReviewedFilterMode>)
                findComponentById(toolboxWrapper, UiComponentFactory.REVIEWED_FILTER_ID);
        TextField searchField = (TextField) findComponentById(toolboxWrapper, UiComponentFactory.SEARCH_FIELD_ID);
        JmixButton nextBtn = (JmixButton) findComponentById(toolboxWrapper, UiComponentFactory.FIND_NEXT_ID);
        JmixButton prevBtn = (JmixButton) findComponentById(toolboxWrapper, UiComponentFactory.FIND_PREVIOUS_ID);
        NativeLabel countLabel = (NativeLabel) findComponentById(toolboxWrapper, UiComponentFactory.COUNT_LABEL_ID);
        JmixButton searchBtn = (JmixButton) findComponentById(toolboxWrapper, UiComponentFactory.SEARCH_BUTTON);

        // Filter Logic
        if (filterBox != null) {
            filterBox.addValueChangeListener(e ->
                    treeGridHelper.reviewedFilterChangeValueListenerAction(e, projectComboBox.getValue(), fileTreeGrid)
            );
        }

        // Search Logic
        if (searchField != null && countLabel != null && filterBox != null) {
            this.fileTreeSearchHelper = new FileTreeSearchHelper(
                    fileRepository,
                    fileTreeGrid,
                    countLabel,
                    expandedItemIds,
                    () -> filterBox.getValue().asBoolean(),
                    transactionTemplate
            );

            searchField.addKeyDownListener(Key.ENTER, e -> {
                if (e.getModifiers().contains(KeyModifier.SHIFT)) {
                    fileTreeSearchHelper.previous(projectComboBox.getValue());
                } else {
                    fileTreeSearchHelper.onEnterKeyPressed(searchField.getValue(), projectComboBox.getValue());
                }
            });
            searchBtn.addClickListener(e -> fileTreeSearchHelper.performSearch(searchField.getValue(),
                    projectComboBox.getValue()));
        }

        if (nextBtn != null && fileTreeSearchHelper != null) {
            nextBtn.addClickListener(e -> fileTreeSearchHelper.next(projectComboBox.getValue()));
        }
        if (prevBtn != null && fileTreeSearchHelper != null) {
            prevBtn.addClickListener(e -> fileTreeSearchHelper.previous(projectComboBox.getValue()));
        }

        // Grid Context Menu & Listeners
        treeGridHelper.setupFileGridContextMenu(fileTreeGrid, tabManager);

        fileTreeGrid.addItemClickListener(event -> {
            File clickedFile = event.getItem();
            if (event.getClickCount() == 2 && Boolean.FALSE.equals(clickedFile.getIsDirectory())) {
                tabManager.openFileTab(clickedFile, true);
            } else {
                treeGridHelper.toggleExpansion(fileTreeGrid, clickedFile);
            }
        });

        // Track expansion state
        fileTreeGrid.addExpandListener(event -> {
            event.getItems().forEach(item -> expandedItemIds.add(item.getId()));
            saveStateToSession();
        });

        fileTreeGrid.addCollapseListener(event -> {
            event.getItems().forEach(file -> expandedItemIds.remove(file.getId()));
            saveStateToSession();
        });
    }

    /**
     * Recursive helper to find a component by ID within a component tree.
     * Necessary because in case UiComponentFactory nests components in sub-layouts
     */
    private Component findComponentById(Component root, String id) {
        if (id.equals(root.getId().orElse(null))) {
            return root;
        }
        return root.getChildren()
                .map(child -> findComponentById(child, id))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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
            Long projectId = Long.parseLong(projectIdStr);
            projectRepository.findById(projectId).ifPresent(project -> {
                log.debug("Loading project {} from URL", project.getProjectName());

                if (this.fileTreeSearchHelper != null) {
                    this.fileTreeSearchHelper.reset();
                }

                projectComboBox.setValue(project);
                refreshAllDataForProject(project);
                restoreTabsAndState();
                overviewProjectTabFragment.setProjectOverview(project);
            });
        } catch (Exception e) {
            log.warn("Invalid projectId in URL: {}", projectIdStr, e);
        } finally {
            suppressNavigation = false;
        }
    }

    /**
     * Restores the active tabs and session state for the AuditView.
     * <p>
     * This method retrieves the previously saved state of the AuditView using
     * the viewStateService. It restores the session tabs, including
     * inventory item and file tree tabs, and attempts to reselect the last active
     * tab based on the identifier stored in the state. If an active tab is found,
     * it updates the UI components appropriately.
     */
    private void restoreTabsAndState() {
        viewStateService.get().ifPresent(state -> {
            restoreSessionTabs(state);

            Set<Long> idsToExpand = new HashSet<>(state.expandedNodeIds());
            this.expandedItemIds.addAll(idsToExpand);

            log.debug("Restoring session state: {} expanded nodes to restore", idsToExpand.size());

            if (!idsToExpand.isEmpty()) {
                List<File> filesToExpand = fileRepository.findAllById(idsToExpand);

                if (!filesToExpand.isEmpty()) {
                    fileTreeGrid.expand(filesToExpand);
                } else {
                    log.warn("Session had expanded IDs, but DB returned 0 files. IDs: {}", idsToExpand);
                }
            }

            if (state.activeTabIdentifier() != null) {
                UI ui = UI.getCurrent();
                ui.access(() -> tabManager.selectTab(state.activeTabIdentifier()));
            }
        });
    }

    private void addTabSelectionListeners() {
        inventoryItemTabSheet.addSelectedChangeListener(e -> handleTabSelectionChange());
        filesTabSheet.addSelectedChangeListener(e -> handleTabSelectionChange());
        mainTabSheet.addSelectedChangeListener(e -> handleTabSelectionChange());
    }

    private void handleTabSelectionChange() {
        Serializable activeIdentifier = tabManager.getActiveTabIdentifier();
        onTabChange(activeIdentifier);
    }

    private void initializeProjectComboBox() {
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
    }

    /**
     * Initializes the inventory data grid for displaying and managing inventory items.
     */
    private void initializeInventoryDataGrid() {
        HorizontalLayout inventoryToolbox = componentFactory.createToolBox(
                inventoryItemDataGrid, InventoryItem.class,
                () -> treeGridHelper.expandChildrenOfRoots(inventoryItemDataGrid),
                () -> treeGridHelper.collapseChildrenOfRoots(inventoryItemDataGrid));

        findCheckBoxById(inventoryToolbox, componentFactory.getVulnerabilityFilterId())
                .ifPresentOrElse(checkbox -> {
                    checkbox.addValueChangeListener(event ->
                                    onVulnerabilityFilterToggled(Boolean.TRUE.equals(event.getValue())));
                }, () -> log.warn("Unable to find vulnerability filter checkbox in inventory toolbox")
                );

        // TODO is not listening due the default jmix listener
        inventoryItemDataGrid.addItemClickListener(event -> {
            if (event.getClickCount() == 2) {
                tabManager.openInventoryItemTab(event.getItem(), true);
            }
        });
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
        // need to get list first and then ensure that the files section is visable
        List<Long> fileIds = state.openFileTabsIds();
        List<Long> inventoryIds = state.openInventoryTabsIds();
        if (!fileIds.isEmpty()) {
            filesSection.setVisible(true);
        }
        if(!inventoryIds.isEmpty()){
            inventoryItemSection.setVisible(true);
        }
        fileIds.stream()
                .flatMap(id -> fileRepository.findById(id).stream())
                .forEach(file -> tabManager.openFileTab(file, false));
    }

    /**
     * Saves the current state of the AuditView to the user's session.
     * <p>
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
                tabManager.getOpenFileIds(),
                tabManager.getActiveTabIdentifier(),
                expandedItemIds
        );
        log.debug("Saving state to session: {}", state);
        viewStateService.save(state);
    }


    /**
     * Handles changes to the active tab in the AuditView.
     * <p>
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
                        new RouteParam("projectId", selectedProject.getId())
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

        fileTreeGrid.setDataProvider(new FileHierarchyProvider(fileRepository, project));
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
                            from File cl
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
                    .withHeader(messages.getMessage("eu.occtet.bocfrontend.view.audit/projectComboBox"))
                    .withText(messages.getMessage("eu.occtet.bocfrontend.view.audit/projectComboBoxText"))
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(e -> {
                                tabManager.closeAllTabs();
                                switchProject(event.getValue());
                            }),
                            new DialogAction(DialogAction.Type.NO)
                                    .withText(messages.getMessage("eu.occtet.bocfrontend.view.audit/projectComboBoxDialog"))
                                    .withHandler(e -> switchProject(event.getValue()))
                    ).open();
        } else {
            switchProject(event.getValue());
        }
        overviewProjectTabFragment.setProjectOverview(event.getValue());
    }

    private void switchProject(Project project) {
        if (this.fileTreeSearchHelper != null) {
            this.fileTreeSearchHelper.reset();
        }
        refreshAllDataForProject(project);
        saveStateToSession();
        updateUrl();
    }


    private void clearView() {
        inventoryItemDc.setItems(Collections.emptyList());
        fileDc.setItems(Collections.emptyList());
        tabManager.closeAllTabs();
    }

    public void handleInventoryItemFromOverview(InventoryItem item){
        if(!inventoryItemSection.isVisible()){
            inventoryItemSection.setVisible(true);
        }
        tabManager.openInventoryItemTab(item,true);
    }

    @Install(to = "inventoryItemDataGrid.create", subject = "initializer")
    private void inventoryItemDataGridCreateActionInitializer(final InventoryItem inventoryItem) {
        inventoryItem.setProject(projectComboBox.getValue());
    }

    public TabManager getTabManager() {
        return tabManager;
    }

}
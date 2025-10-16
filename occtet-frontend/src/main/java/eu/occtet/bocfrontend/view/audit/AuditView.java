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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.model.FileResult;
import eu.occtet.bocfrontend.model.FileTreeNode;
import eu.occtet.bocfrontend.service.FileContentService;
import eu.occtet.bocfrontend.service.FileTreeCacheService;
import eu.occtet.bocfrontend.view.codeviewerfragment.CodeViewerFragment;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.*;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.UiComponentUtils;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Route(value = "audit-view", layout = MainView.class)
@ViewController(id = "AuditView")
@ViewDescriptor(path = "audit-view.xml")
public class AuditView extends StandardView {

    private static final Logger log = LogManager.getLogger(AuditView.class);

    private static final String SESSION_KEY = "AUDITVIEW_KEY";

    private final Map<InventoryItem, Tab> openInventoryTabs = new HashMap<>();
    private final Map<FileTreeNode, Tab> openFileTabs = new HashMap<>();
    private Map<UUID, Long> fileCounts = new HashMap<>();

    @ViewComponent
    private JmixComboBox<Project> projectComboBox;
    @ViewComponent
    private CollectionContainer<InventoryItem> inventoryItemDc;
    @ViewComponent
    private JmixTabSheet inventoryItemTabSheet;
    @ViewComponent
    private Tab inventoryItemSection;
    @ViewComponent
    private JmixTabSheet filesTabSheet;
    @ViewComponent
    private Tab filesSection;
    @ViewComponent
    private JmixTabSheet mainTabSheet;
    @ViewComponent
    private TreeDataGrid<InventoryItem> inventoryItemDataGrid;
    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private VerticalLayout fileTreeGridLayout;
    @ViewComponent
    private CollectionLoader<InventoryItem> inventoryItemDl;

    @Autowired
    private Fragments fragments;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Notifications notifications;
    @Autowired
    private FileContentService fileContentService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private FileTreeCacheService fileTreeCacheService;


    @Subscribe
    protected void onInit(InitEvent event) {
        projectComboBox.setItems(projectRepository.findAll());
        projectComboBox.setItemLabelGenerator(Project::getProjectName);
        inventoryItemDataGrid.setTooltipGenerator(InventoryItem::getInventoryName);
    }

    @Subscribe
    public void onBefore(final BeforeShowEvent event) {
        restoreStateFromSession();
    }

    @Subscribe
    public void onBeforeClose(BeforeCloseEvent event) {
        saveStateToSession();
    }

    private TreeDataGrid<FileTreeNode> createAndPrepareFileTreeGrid(List<FileTreeNode> rootNodes) {
        TreeDataGrid<FileTreeNode> fileTreeGrid = uiComponents.create(TreeDataGrid.class);
        fileTreeGrid.setThemeName("no-row-borders compact row-stripes");
        fileTreeGrid.setWidthFull();
        fileTreeGrid.setHeightFull();

        fileTreeGrid.addHierarchyColumn(FileTreeNode::getName).setHeader("File");
        TreeData<FileTreeNode> treeData = new TreeData<>();
        for (FileTreeNode root : rootNodes) {
            treeData.addItem(null, root);
            addChildrenRecursively(treeData, root);
        }

        fileTreeGrid.setTooltipGenerator(FileTreeNode::getName);

        fileTreeGrid.addItemClickListener(event -> {
            FileTreeNode clickedNode = event.getItem();
            if (event.getClickCount() == 2 && !clickedNode.isDirectory()) {
                openFileTabAction(clickedNode);
            } else {
                if (fileTreeGrid.isExpanded(clickedNode)) {
                    fileTreeGrid.collapse(clickedNode);
                } else {
                    fileTreeGrid.expand(clickedNode);
                }
            }
        });

        GridContextMenu<FileTreeNode> contextMenu = fileTreeGrid.getContextMenu();
        contextMenu.addItem("Open", event -> event.getItem().ifPresent(this::openFileTabAction));
        contextMenu.addItem("Copy Name", event -> event.getItem().ifPresent(node -> copyToClipboard(node.getName())));
        contextMenu.addItem("Copy Absolute Path", event -> event.getItem().ifPresent(node -> copyToClipboard(node.getFullPath())));
        contextMenu.addItem("Open Inventory", event -> {
            event.getItem().ifPresent(node -> {
                CodeLocation codeLocation = node.getCodeLocation();
                if (codeLocation != null && codeLocation.getInventoryItem() != null) {
                    openInventoryItemOpenTabAction(codeLocation.getInventoryItem());
                }
            });
        });

        fileTreeGrid.addComponentColumn(node -> {
            if (node.isDirectory()) return null;
            Icon circleIcon = uiComponents.create(Icon.class);
            circleIcon.setIcon(VaadinIcon.CIRCLE);
            circleIcon.setSize("12px");
            String status;
            CodeLocation codeLocation = node.getCodeLocation();
            if (codeLocation == null || codeLocation.getInventoryItem() == null) {
                circleIcon.getStyle().set("color", "var(--lumo-error-color)");
                status = "Not in inventory";
            } else if (Boolean.TRUE.equals(codeLocation.getInventoryItem().getCurated())) {
                circleIcon.getStyle().set("color", "var(--lumo-success-color)");
                status = "Curated";
            } else {
                circleIcon.getStyle().set("color", "var(--lumo-primary-color)");
                status = "Included but not curated";
            }
            circleIcon.setTooltipText(status);
            return circleIcon;
        }).setHeader("").setFlexGrow(0).setWidth("20px");

        fileTreeGrid.setDataProvider(new TreeDataProvider<>(treeData));
        return fileTreeGrid;
    }

    /**
     * Handles the value change event for the projectComboBox component.
     * This method updates the inventory items associated with the selected project
     * and prompts the user to decide whether to close or keep any open tabs related to the previous project.
     */
    @Subscribe("projectComboBox")
    public void onProjectFieldComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<Project>, Project> event) {
        if (event.getValue() != event.getOldValue()) {
            refreshAllDataForProject(event.getValue());
            saveStateToSession();
            if (event.isFromClient()) {
                promptToCloseOldTabs();
            }
        }
    }

    /**
     * Handles the click event on an inventory item in the data grid.
     * Expands or collapses the clicked inventory item row based on its current state.
     */
    @Subscribe("inventoryItemDataGrid")
    public void onInventoryItemDataGridClick(final ItemClickEvent<InventoryItem> event) {
        if (event.getClickCount() == 2) {
            openInventoryItemOpenTabAction(event.getItem());
        } else {
            if (inventoryItemDataGrid.isExpanded(event.getItem())) {
                inventoryItemDataGrid.collapse(event.getItem());
            } else {
                inventoryItemDataGrid.expand(event.getItem());
            }
        }
    }

    public void refreshInventoryItemDc(Project project) {
        inventoryItemDl.setParameter("project", project);
        inventoryItemDl.load();

        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query(
                        "select cl.inventoryItem.id as itemId, count(cl) as fileCount from CodeLocation cl " +
                                "where cl.inventoryItem.project = :project group by cl.inventoryItem.id")
                        .setParameter("project", project))
                .addProperty("itemId")
                .addProperty("fileCount");

        List<KeyValueEntity> counts = dataManager.loadValues(context);
        this.fileCounts = counts.stream()
                .collect(Collectors.toMap(kv -> kv.getValue("itemId"), kv -> (Long) kv.getValue("fileCount")));
    }

    @Supply(to = "inventoryItemDataGrid.fileNumCol", subject = "renderer")
    Renderer<InventoryItem> filesCountRenderer() {
        return new TextRenderer<>(item -> fileCounts.getOrDefault(item.getId(), 0L).toString());
    }


    private void refreshAllDataForProject(Project project) {
        if (project == null) {
            clearView();
            return;
        }

        refreshInventoryItemDc(project);

        List<FileTreeNode> rootNodes = fileTreeCacheService.getFileTree(project);
        TreeDataGrid<FileTreeNode> fileTreeGrid = createAndPrepareFileTreeGrid(rootNodes);
        fileTreeGridLayout.removeAll();
        fileTreeGridLayout.add(fileTreeGrid);
    }

    private void clearView() {
        inventoryItemDc.setItems(Collections.emptyList());
        fileTreeGridLayout.removeAll();
    }

    private void promptToCloseOldTabs() {
        if (!openInventoryTabs.isEmpty() || !openFileTabs.isEmpty()) {
            dialogs.createOptionDialog()
                    .withHeader("Change Project")
                    .withText("Do you want to close all tabs from the previous project?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES).withHandler(e -> closeAllTabs()),
                            new DialogAction(DialogAction.Type.NO).withText("Keep Tabs")
                    ).open();
        }
    }

    private void copyToClipboard(String text) {
        UiComponentUtils.copyToClipboard(text)
                .then(success -> notifications.create("Text copied!").withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS).show(),
                        error -> notifications.create("Copy failed!").withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR).show());
    }

    private <T> void openTabAction(Map<T, Tab> openTabsMap, JmixTabSheet tabSheet, T key, String tabTitle,
                                   Supplier<Component> contentSupplier, Consumer<Tab> onClose) {
        if (openTabsMap.containsKey(key)) {
            tabSheet.setSelectedTab(openTabsMap.get(key));
            return;
        }

        Component fragment = contentSupplier.get();
        Tab tab = tabSheet.add(tabTitle, fragment);

        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(click -> {
            if (onClose != null)
                onClose.accept(tab);
        });

        tab.addComponentAtIndex(1, closeButton);
        openTabsMap.put(key, tab);
        tabSheet.setSelectedTab(tab);

        saveStateToSession();
    }

    private void addChildrenRecursively(TreeData<FileTreeNode> treeData, FileTreeNode parent) {
        parent.getChildren().forEach(child -> {
            treeData.addItem(parent, child);
            addChildrenRecursively(treeData, child);
        });
    }

    /**
     * Opens an inventory item in a new tab within the inventory item tab sheet.
     * If the tab for the selected inventory item is already open, it focuses on the existing tab.
     * Otherwise, it creates a new tab for the selected inventory item, adds a close button to it,
     * and saves the current state to the session.
     */
    private void openInventoryItemOpenTabAction(InventoryItem selected) {
        if (selected == null) return;
        mainTabSheet.setSelectedTab(inventoryItemSection);
        openTabAction(openInventoryTabs, inventoryItemTabSheet, selected, selected.getInventoryName(),
                () -> {
                    InventoryItemTabFragment fragment = fragments.create(this, InventoryItemTabFragment.class);
                    fragment.setHostView(this);
                    fragment.setInventoryItem(selected);
                    return fragment;
                }, this::handleClosingInventoryItemTab);
    }

    private void openFileTabAction(FileTreeNode file) {
        if (file == null) return;
        mainTabSheet.setSelectedTab(filesSection);
        openTabAction(openFileTabs, filesTabSheet, file, file.getName(),
                () -> {
                    CodeViewerFragment fragment = fragments.create(this, CodeViewerFragment.class);
                    FileResult res = fileContentService.getFileContent(file.getFullPath());
                    if (res instanceof FileResult.Success(String content, String fileName)) {
                        fragment.setCodeEditorContent(content, fileName);
                    } else if (res instanceof FileResult.Failure(String errorMessage)) {
                        log.warn("Could not view file: {}", errorMessage);
                        notifications.create(errorMessage).withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR).show();
                    }
                    return fragment;
                }, tab -> closeFileTab(file));
    }

    /**
     * Handles the closing of an inventory item tab in the inventory item tab sheet.
     * If there are unsaved changes in the associated inventory item, the user is prompted
     * to either discard the changes or keep the tab open. Otherwise, the tab is closed
     * and the session state is updated.
     */
    private void handleClosingInventoryItemTab(Tab tab) {
        openInventoryTabs.entrySet().stream()
                .filter(entry -> entry.getValue().equals(tab)).findFirst()
                .ifPresent(entry -> {
                    InventoryItem selected = entry.getKey();
                    InventoryItemTabFragment fragment = (InventoryItemTabFragment) inventoryItemTabSheet.getComponent(tab);
                    if (fragment != null && dataContext.isModified(fragment.getInventoryItem())) {
                        dialogs.createOptionDialog()
                                .withHeader("Unsaved changes")
                                .withText("Do you want to discard the changes?")
                                .withActions(
                                        new DialogAction(DialogAction.Type.YES).withHandler(e -> closeInventoryItemTab(selected)),
                                        new DialogAction(DialogAction.Type.NO)
                                ).open();
                    } else {
                        closeInventoryItemTab(selected);
                    }
                });
    }

    private void closeInventoryItemTab(InventoryItem item) {
        Tab tab = openInventoryTabs.remove(item);
        if (tab != null) {
            inventoryItemTabSheet.remove(tab);
        }
        saveStateToSession();
    }

    private void closeFileTab(FileTreeNode file) {
        Tab tab = openFileTabs.remove(file);
        if (tab != null) {
            filesTabSheet.remove(tab);
        }
        saveStateToSession();
    }

    private void closeAllTabs() {
        new HashMap<>(openInventoryTabs).keySet().forEach(this::closeInventoryItemTab);
        new HashMap<>(openFileTabs).keySet().forEach(this::closeFileTab);
    }

    /**
     * Saves the current state of the inventory
     */
    private void saveStateToSession() {
        Project selectedProject = projectComboBox.getValue();
        AuditViewState state = new AuditViewState(
                selectedProject != null ? selectedProject.getId() : null,
                openInventoryTabs.keySet().stream().map(InventoryItem::getId).toList(),
                new ArrayList<>(openFileTabs.keySet())
        );
        VaadinSession.getCurrent().setAttribute(SESSION_KEY, state);
    }

    /**
     * Restores the state of the inventory from the session.
     */
    private void restoreStateFromSession() {
        Object stateObject = VaadinSession.getCurrent().getAttribute(SESSION_KEY);
        if (!(stateObject instanceof AuditViewState state)) {
            return;
        }

        log.debug("Restoring state from session: {}", state);

        if (state.projectId() != null) {
            projectRepository.findById(state.projectId()).ifPresent(project -> {
                projectComboBox.setValue(project);

                refreshGridsForProject(project);

                state.openInventoryTabsIds().forEach(id ->
                        inventoryItemRepository.findById(id).ifPresent(this::openInventoryItemOpenTabAction));
                state.openFileTabsPaths().forEach(this::openFileTabAction);
            });
        }
    }

    private void refreshGridsForProject(Project project) {
        if (project == null) return;

        refreshInventoryItemDc(project);

        List<FileTreeNode> rootNodes = fileTreeCacheService.getFileTree(project);
        TreeDataGrid<FileTreeNode> fileTreeGrid = createAndPrepareFileTreeGrid(rootNodes);
        fileTreeGridLayout.removeAll();
        fileTreeGridLayout.add(fileTreeGrid);
    }

    // DTO for storing session state
    public record AuditViewState(
            UUID projectId,
            List<UUID> openInventoryTabsIds,
            List<FileTreeNode> openFileTabsPaths
    ) implements Serializable {}
}
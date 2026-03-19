/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.bocfrontend.service;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.view.audit.TabManager;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.factory.AuditViewUiComponentFactory;
import eu.occtet.bocfrontend.model.FileReviewedFilterMode;
import eu.occtet.bocfrontend.view.audit.FileHierarchyProvider;
import io.jmix.core.Messages;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.component.grid.JmixTreeGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

/**
 * A helper class to facilitate operations and interactions with tree grid components, focusing on tasks
 * such as expanding/collapsing nodes, applying filters, configuring context menus, and managing nodes'
 * expansion states.
 */
@Service
public class TreeGridHelper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Notifications notifications;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private AuditViewUiComponentFactory uiComponentFactory;
    @Autowired
    private Messages messages;

    public <T> void expandChildrenOfRoots(JmixTreeGrid<T> grid) {
        handleCollapseAndExpand(grid, false);
    }

    public <T> void collapseChildrenOfRoots(JmixTreeGrid<T> grid) {
        handleCollapseAndExpand(grid, true);
    }

    private <T> void handleCollapseAndExpand(JmixTreeGrid<T> grid, boolean collapse) {
        if (grid == null) return;
        DataProvider<T, ?> dataProvider = grid.getDataProvider();
        if (dataProvider instanceof HierarchicalDataProvider<T,?> hierarchicalDataProvider) {
            Stream<T> rootStream = hierarchicalDataProvider.fetch(new HierarchicalQuery<>(null,null));
            rootStream.forEach(root -> {
                if (collapse)
                    grid.collapseRecursively(List.of(root), Integer.MAX_VALUE);
                else
                    grid.expandRecursively(List.of(root), Integer.MAX_VALUE);
            });
        }
    }

    public <T> void toggleExpansion(TreeDataGrid<T> grid, T item) {
        if (grid.isExpanded(item)) {
            grid.collapse(item);
        } else {
            grid.expand(item);
        }
    }

    public void copyToClipboard(String text) {
        UiComponentUtils.copyToClipboard(text)
                .then(success -> notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.audit/notification.copySuccess")
                                + " " + text)
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                .show(),
                        error -> notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.audit/notification.copyFailed"))
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR)
                                .show());
    }

    /**
     * Sets up a context menu for a file grid. The context menu dynamically provides options
     * based on the selected file, such as opening the file, copying its details, or accessing related inventory items.
     *
     * @param grid       the file grid component to which the context menu is attached
     * @param tabManager the tab manager responsible for managing and opening tabs for files and related items
     */
    public void setupFileGridContextMenu(TreeDataGrid<File> grid, TabManager tabManager) {
        GridContextMenu<File> contextMenu = grid.getContextMenu();

        contextMenu.setDynamicContentHandler(file -> {
            if (file == null) return false;
            contextMenu.removeAll();

            if (Boolean.FALSE.equals(file.getIsDirectory())) {
                contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.FILE_TEXT_O,
                                messages.getMessage("eu.occtet.bocfrontend.view.audit/context.openFile")),
                        event -> tabManager.openFileTab(file, true));

                Set<InventoryItem> items = file.getInventoryItems();

                GridMenuItem<File> inventoryMenuItem = contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.CUBE,
                        messages.getMessage("eu.occtet.bocfrontend.view.audit/context.openInventory")));

                if (items != null && !items.isEmpty()) {
                    GridSubMenu<File> subMenu = inventoryMenuItem.getSubMenu();

                    for (InventoryItem item : items) {
                        subMenu.addItem(item.getInventoryName(),
                                e -> {
                                    inventoryItemRepository.findById(item.getId())
                                            .ifPresent(reloadedItem -> {
                                                log.debug("Opening inventory: {}", reloadedItem.getInventoryName());
                                                tabManager.openInventoryItemTab(reloadedItem, true);
                                            });
                                });
                    }
                } else {
                    inventoryMenuItem.setEnabled(false);
                }

                contextMenu.add(new Hr());
            }

            if (Boolean.TRUE.equals(file.getIsDirectory())) {
                contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.EXPAND_FULL,
                        messages.getMessage("eu.occtet.bocfrontend.view.audit/context.expandRecursive")), event -> {
                    grid.expandRecursively(List.of(file), Integer.MAX_VALUE);
                });

                contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.COMPRESS,
                        messages.getMessage("eu.occtet.bocfrontend.view.audit/context.collapseRecursive")), event -> {
                    grid.collapseRecursively(List.of(file), Integer.MAX_VALUE);
                });
                contextMenu.add(new Hr());
            }

            contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.COPY,
                            messages.getMessage("eu.occtet.bocfrontend.view.audit/context.copyName")),
                    event -> copyToClipboard(file.getFileName()));

            contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.CLIPBOARD_TEXT,
                            messages.getMessage("eu.occtet.bocfrontend.view.audit/context.projectPath")),
                    event -> copyToClipboard(file.getProjectPath()));

            contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.CLIPBOARD_TEXT,
                            messages.getMessage("eu.occtet.bocfrontend.view.audit/context.artifactPath")),
                    event -> copyToClipboard(file.getArtifactPath()));

            boolean isReviewed = Boolean.TRUE.equals(file.getReviewed());
            if (isReviewed) {
                contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.THIN_SQUARE,
                                messages.getMessage("eu.occtet.bocfrontend.view.audit/context.markUnreviewed")),
                        event -> updateFileReviewStatus(file, false, grid));
            } else {
                contextMenu.addItem(uiComponentFactory.createContextMenuItem(VaadinIcon.CHECK_SQUARE,
                                messages.getMessage("eu.occtet.bocfrontend.view.audit/context.markReviewed")),
                        event -> updateFileReviewStatus(file, true, grid));
            }
            return true;
        });
    }

    /**
     * Handles the change in value of the reviewed filter for the file view.
     * Updates the data sorting behavior based on the selected filter mode
     * and modifies the visual representation (e.g., dimming rows) to reflect
     * the active filter selection.
     *
     * @param event the value change event containing the source component and the new selected value
     * @param currentProject the current project being displayed, used to ensure the context is valid
     * @param fileTreeGrid the tree data grid displaying the files, which will have its data provider and
     *                     visual representation updated based on the filter mode
     */
    public void reviewedFilterChangeValueListenerAction(
            AbstractField.ComponentValueChangeEvent<ComboBox<FileReviewedFilterMode>, FileReviewedFilterMode> event,
            Project currentProject,
            TreeDataGrid<File> fileTreeGrid) {

        if (currentProject == null) return;

        FileReviewedFilterMode mode = event.getValue();
        DataProvider<File, ?> dataProvider = fileTreeGrid.getDataProvider();

        if (dataProvider instanceof FileHierarchyProvider provider) {
            provider.setReviewedFilter(mode);

            if (mode == FileReviewedFilterMode.SHOW_ALL) {
                fileTreeGrid.setClassNameGenerator(file -> null);
            } else {
                Boolean targetStatus = mode.asBoolean();
                fileTreeGrid.setClassNameGenerator(file -> {
                    boolean matches = Objects.equals(file.getReviewed(), targetStatus);
                    return matches ? null : "dim-row";
                });
            }
        }
    }

    private void updateFileReviewStatus(File file, boolean status, TreeDataGrid<File> grid) {
        file.setReviewed(status);
        fileRepository.save(file);

        grid.getDataProvider().refreshItem(file);
    }
}

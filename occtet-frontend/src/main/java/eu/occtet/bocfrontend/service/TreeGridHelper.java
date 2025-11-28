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

import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.engine.TabManager;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.view.audit.FileHierarchyProvider;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.component.grid.JmixTreeGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A helper class to facilitate operations and interactions with tree grid components, focusing on tasks
 * such as expanding/collapsing nodes, applying filters, configuring context menus, and managing nodes'
 * expansion states.
 */
@Service
public class TreeGridHelper {

    @Autowired
    private Notifications notifications;
    @Autowired
    private FileRepository fileRepository;

    public <T> void expandChildrenOfRoots(JmixTreeGrid<T> grid) {
        if (grid == null) return;
        DataProvider<T, ?> dataProvider = grid.getDataProvider();
        if (dataProvider instanceof HierarchicalDataProvider<T,?> hierarchicalDataProvider) {
            Stream<T> rootStream = hierarchicalDataProvider.fetch(new HierarchicalQuery<>(null,null));
            rootStream.forEach(root -> grid.expandRecursively(List.of(root), Integer.MAX_VALUE));
        }
    }

    public <T> void collapseChildrenOfRoots(JmixTreeGrid<T> grid) {
        if (grid == null)
            return;

        DataProvider<T, ?> dataProvider = grid.getDataProvider();
        if (dataProvider instanceof TreeDataProvider) {
            grid.collapseRecursively(grid.getTreeData().getRootItems(), Integer.MAX_VALUE);
        } else {
            try (Stream<T> stream = dataProvider.fetch(new Query<>())) {
                stream.forEach(item -> {
                    grid.collapseRecursively(List.of(item), Integer.MAX_VALUE);
                });
            }
        }
    }

    public <T> void toggleExpansion(TreeDataGrid<T> grid, T item) {
        if (grid.isExpanded(item)) {
            grid.collapse(item);
        } else {
            grid.expand(item);
        }
    }

    /**
     * Applies a filter to a JmixTreeGrid, updates the displayed data, and expands the matching nodes along
     * with their ancestors and descendants.
     *
     * @param <T>           the type of the items in the tree grid
     * @param grid          the JmixTreeGrid instance to which the filter and expansion should be applied
     * @param provider      the TreeDataProvider used to manage the data of the tree grid
     * @param filterText    the text used as a filter to search and identify matching nodes
     * @param nameExtractor a function that extracts the name or text representation of the nodes for filtering
     */
    public <T> void applyFilterAndExpand(JmixTreeGrid<T> grid,
                                         TreeDataProvider<T> provider,
                                         String filterText,
                                         Function<T, String> nameExtractor) {
        String text = filterText.toLowerCase().trim();
        TreeData<T> treeData = provider.getTreeData();

        if (text.isEmpty()) {
            provider.clearFilters();
            grid.setClassNameGenerator(item -> null); // Clear highlights
            return;
        }

        List<T> allNodes = treeData.getRootItems().stream()
                .flatMap(root -> flattenTree(provider, root).stream()).toList();
        Predicate<T> directMatchPrediacate = node -> nameExtractor.apply(node).toLowerCase().contains(text);
        List<T> directMatches = allNodes.stream().filter(directMatchPrediacate).toList();
        Set<T> nodesToShow = new HashSet<>();
        for (T match : directMatches) {
            nodesToShow.add(match);
            collectAncestors(nodesToShow, treeData, match);
            collectDescendants(nodesToShow, provider, match);
        }

        provider.setFilter(nodesToShow::contains);

        // Expand all parents of the target
        collapseChildrenOfRoots(grid);
        for (T root : treeData.getRootItems()){
            expandAncestorsOfMatches(root, grid, provider, directMatchPrediacate);
        }
    }

    private <T> List<T> flattenTree(TreeDataProvider<T> provider, T node) {
        List<T> nodes = new ArrayList<>();
        nodes.add(node);
        provider.getTreeData().getChildren(node).forEach(child -> nodes.addAll(flattenTree(provider, child)));
        return nodes;
    }

    private <T> void collectAncestors(Set<T> set, TreeData<T> treeData, T node) {
        T parent = treeData.getParent(node);
        while (parent != null) {
            if (!set.add(parent)) {
                break;
            }
            parent = treeData.getParent(parent);
        }
    }

    private <T> void collectDescendants(Set<T> set, TreeDataProvider<T> provider, T node) {
        provider.getTreeData().getChildren(node).forEach(child -> {
            if (set.add(child)) {
                collectDescendants(set, provider, child);
            }
        });
    }

    private <T> boolean expandAncestorsOfMatches(T node, JmixTreeGrid<T> grid, TreeDataProvider<T> provider, Predicate<T> filter) {
        boolean matches = filter.test(node);
        List<T> children = provider.getTreeData().getChildren(node);

        boolean descendantMatches = false;
        for (T child : children) {
            if (expandAncestorsOfMatches(child, grid, provider, filter)) {
                descendantMatches = true;
            }
        }

        if (descendantMatches) {
            grid.expand(node); // expand ancestors only
        }

        return matches || descendantMatches;
    }

    public void copyToClipboard(String text) {
        UiComponentUtils.copyToClipboard(text)
                .then(success -> notifications.create("Text copied!")
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                .show(),
                        error -> notifications.create("Copy failed!")
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
                contextMenu.addItem("Open", event -> tabManager.openFileTab(file, true));
            }
            contextMenu.addItem("Copy name", event -> copyToClipboard(file.getFileName()));
            contextMenu.addItem("Copy Absolute Path", event -> copyToClipboard(file.getAbsolutePath()));
            contextMenu.addItem("Open Inventory", event -> {
                notifications.show("Under development...");
                // TODO determine where to set the associated codeLocation of a file (in backend or frontend?)
                if (file.getCodeLocation() != null && file.getCodeLocation().getInventoryItem() != null) {
                    tabManager.openInventoryItemTab(file.getCodeLocation().getInventoryItem(), true);
                }
            });
            return true;
        });
    }

    /**
     * Expands the paths in the file tree grid based on the provided file hierarchy data.
     * This method determines nodes that need expansion and invokes the grid to expand them.
     * Note: The provider could contain a search/filter-term which will effect the expansion of nodes etc..
     *
     * @param provider the file hierarchy provider containing path IDs and exact match IDs for calculation
     * @param fileTreeGrid the tree data grid displaying the file hierarchy to be updated
     */
    public void expandPathOnly(FileHierarchyProvider provider, TreeDataGrid<File> fileTreeGrid) {
        Set<UUID> allIdsToShow = provider.getPathIds();
        Set<UUID> exactMatches = provider.getExactMatchIds();

        Set<UUID> idsToExpand = new HashSet<>(allIdsToShow);
        idsToExpand.removeAll(exactMatches);

        if (!idsToExpand.isEmpty()) {
            List<File> nodesToExpand = fileRepository.findByIdIn(idsToExpand);
            fileTreeGrid.expand(nodesToExpand);
        }
    }

    /**
     * Restores the expansion state in a TreeDataGrid by expanding the nodes corresponding to the provided set of IDs.
     *
     * @param idsToExpand a set of UUIDs representing the IDs of nodes to expand; if null or empty, no action is performed
     * @param fileTreeGrid the TreeDataGrid containing file nodes that will be expanded based on the provided IDs
     */
    public void restoreExpansionState(Set<UUID> idsToExpand, TreeDataGrid<File> fileTreeGrid) {
        if (idsToExpand == null || idsToExpand.isEmpty()) return;
        List<File> nodesToExpand = fileRepository.findByIdIn(idsToExpand);
        if (!nodesToExpand.isEmpty()){
            fileTreeGrid.expand(nodesToExpand);
        }
    }
}

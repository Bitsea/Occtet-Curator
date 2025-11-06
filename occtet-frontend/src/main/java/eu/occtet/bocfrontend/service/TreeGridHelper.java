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

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
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
 * A helper class for managing the expansion and collapsing of nodes in a tree grid.
 * Provides methods to perform operations like expanding all child nodes of root items,
 * collapsing all child nodes of root items, and toggling the expansion state of a specific item.
 */
@Service
public class TreeGridHelper {

    @Autowired
    private Notifications notifications;

    public <T> void expandChildrenOfRoots(JmixTreeGrid<T> grid) {
        if (grid == null)
            return;

        DataProvider<T, ?> dataProvider = grid.getDataProvider();
        if (dataProvider instanceof TreeDataProvider) {
            grid.expandRecursively(grid.getTreeData().getRootItems(), Integer.MAX_VALUE);
        } else {
            try (Stream<T> stream = dataProvider.fetch(new Query<>())) {
                stream.forEach(item -> {
                    grid.expandRecursively(List.of(item), Integer.MAX_VALUE);
                });
            }
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
}

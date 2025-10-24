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

package eu.occtet.bocfrontend.view.audit.helper;

import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.component.grid.JmixTreeGrid;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

/**
 * A helper class for managing the expansion and collapsing of nodes in a tree grid.
 * Provides methods to perform operations like expanding all child nodes of root items,
 * collapsing all child nodes of root items, and toggling the expansion state of a specific item.
 */
@Component
public class TreeGridHelper {


    public <T> void expandChildrenOfRoots(JmixTreeGrid<T> grid) {
        if (grid == null) return;

        DataProvider<T, ?> dataProvider = grid.getDataProvider();
        if (dataProvider instanceof TreeDataProvider) {
            grid.expandRecursively(grid.getTreeData().getRootItems(), Integer.MAX_VALUE);
        } else {
            try (Stream<T> stream = dataProvider.fetch(new Query<>())){
                stream.forEach(item -> {
                    grid.expandRecursively(List.of(item), Integer.MAX_VALUE);
                });
            }
        }
    }

    public <T> void collapseChildrenOfRoots(JmixTreeGrid<T> grid) {
        if (grid == null) return;

        DataProvider<T, ?> dataProvider = grid.getDataProvider();
        if (dataProvider instanceof TreeDataProvider) {
            grid.collapseRecursively(grid.getTreeData().getRootItems(), Integer.MAX_VALUE);
        } else {
            try (Stream<T> stream = dataProvider.fetch(new Query<>())){
                stream.forEach(item -> {
                    grid.collapseRecursively(List.of(item), Integer.MAX_VALUE);
                });
            }
        }    }

    public <T> void toggleExpansion(TreeDataGrid<T> grid, T item) {
        if (grid.isExpanded(item)) {
            grid.collapse(item);
        } else {
            grid.expand(item);
        }
    }
}

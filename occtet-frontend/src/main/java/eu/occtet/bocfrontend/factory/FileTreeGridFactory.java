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

package eu.occtet.bocfrontend.factory;

import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.model.FileTreeNode;
import eu.occtet.bocfrontend.service.TreeGridHelper;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGridColumn;
import io.jmix.flowui.component.grid.TreeDataGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * A factory component for creating and configuring the file tree grid used in the audit view
 */
@Component
public class FileTreeGridFactory {

    @Autowired private UiComponents uiComponents;
    @Autowired private TreeGridHelper treeGridHelper;

    /**
     * Creates a TreeGridWithFilter component that includes a hierarchical tree grid populated
     * with the provided root nodes and a search filter field for filtering the displayed nodes.
     *
     * @param rootNodes The list of root nodes representing the starting points of the tree hierarchy.
     *                  These nodes include information on their children and other related metadata.
     * @param itemClickListener A consumer that is triggered when an item in the tree grid is clicked.
     *                          It receives the clicked FileTreeNode as input for processing.
     * @param openInventoryHandler A consumer that is invoked when an inventory item is opened from the context menu.
     *                             It receives an InventoryItem object as input for handling.
     * @return A TreeGridWithFilter object that includes both the configured tree grid and a search filter field.
     */
    public TreeGridWithFilter createTreeGridWithFilter(
            List<FileTreeNode> rootNodes,
            Consumer<FileTreeNode> itemClickListener,
            Consumer<InventoryItem> openInventoryHandler) {

        TreeDataGrid<FileTreeNode> grid = uiComponents.create(TreeDataGrid.class);

        TreeData<FileTreeNode> treeData = new TreeData<>();
        for (FileTreeNode root : rootNodes) {
            treeData.addItem(null, root);
            addChildrenRecursively(treeData, root);
        }

        TreeDataProvider<FileTreeNode> provider = new TreeDataProvider<>(treeData);
        grid.setDataProvider(provider);

        configureGridAppearance(grid);
        configureColumns(grid);
        configureListeners(grid, itemClickListener);
        configureContextMenu(grid, openInventoryHandler);

        HorizontalLayout layout = uiComponents.create(HorizontalLayout.class);
        layout.setSpacing(false);

        TextField filterField = uiComponents.create(TextField.class);
        filterField.setPlaceholder("Search files...");
        filterField.setClearButtonVisible(true);

        layout.add(filterField);

        filterField.addValueChangeListener(e -> {
            String searchText = e.getValue();
            treeGridHelper.applyFilterAndExpand(grid, provider, searchText, FileTreeNode::getName);
        });

        return new TreeGridWithFilter(grid, layout);
    }


    private void configureGridAppearance(TreeDataGrid<FileTreeNode> grid){
        grid.setThemeName("no-row-borders compact row-stripes");
        grid.setWidthFull();
        grid.setHeightFull();
    }

    private void configureColumns(TreeDataGrid<FileTreeNode> grid){
        DataGridColumn<FileTreeNode> column = grid.addHierarchyColumn(FileTreeNode::getName);
        column.setHeader("File");
        column.setTooltipGenerator(FileTreeNode::getName);
    }

    private void configureListeners(TreeDataGrid<FileTreeNode> grid,
                                    Consumer<FileTreeNode> itemClickListener){
        grid.addItemClickListener(event -> {
            FileTreeNode clickedNode = event.getItem();
            if (event.getClickCount() == 2 && !clickedNode.isDirectory()) {
                itemClickListener.accept(clickedNode);
            } else {
                if (grid.isExpanded(clickedNode)) {
                    grid.collapse(clickedNode);
                } else {
                    grid.expand(clickedNode);
                }
            }
        });
    }

    private void configureContextMenu(TreeDataGrid<FileTreeNode> grid,
                                      Consumer<InventoryItem> openInventoryHandler){
        GridContextMenu<FileTreeNode> contextMenu = grid.getContextMenu();

        contextMenu.addItem("Open", event ->
                event.getItem().ifPresent(node -> {
                    if (!node.isDirectory() && node.getCodeLocation() != null) {
                        openInventoryHandler.accept(node.getCodeLocation().getInventoryItem());
                    }
                })
        );

        contextMenu.addItem("Copy Name", event ->
                event.getItem().ifPresent(node -> treeGridHelper.copyToClipboard(node.getName()))
        );

        contextMenu.addItem("Copy Absolute Path", event ->
                event.getItem().ifPresent(node -> treeGridHelper.copyToClipboard(node.getFullPath()))
        );

        contextMenu.addItem("Open Inventory", event ->
                event.getItem().ifPresent(node -> {
                    if (node.getCodeLocation() != null && node.getCodeLocation().getInventoryItem() != null) {
                        openInventoryHandler.accept(node.getCodeLocation().getInventoryItem());
                    }
                })
        );
    }

    private void addChildrenRecursively(TreeData<FileTreeNode> treeData, FileTreeNode parent) {
        for (FileTreeNode child : parent.getChildren()) {
            treeData.addItem(parent, child);
            addChildrenRecursively(treeData, child);
        }
    }

    /**
     * Helper class to return both grid and filter field
     */
        public record TreeGridWithFilter(TreeDataGrid<FileTreeNode> grid, HorizontalLayout filterField) {
    }
}
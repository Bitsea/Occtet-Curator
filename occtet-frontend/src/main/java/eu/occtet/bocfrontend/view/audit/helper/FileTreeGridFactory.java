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

import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.model.FileTreeNode;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.UiComponentUtils;
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

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Notifications notifications;

    /**
     * creates a new, fully configured file tree grid for displaying the file nodes.
     */
    public TreeDataGrid<FileTreeNode> create(List<FileTreeNode> nodes, Consumer<FileTreeNode> itemClickListener,
                                             Consumer<InventoryItem> openInventoryHandler) {
        TreeDataGrid<FileTreeNode> grid = uiComponents.<TreeDataGrid>create(TreeDataGrid.class);
        configureGridAppearance(grid);
        configureDataProvider(grid, nodes);
        configureListeners(grid, itemClickListener);
        configureColumns(grid);
        configureContextMenu(grid, openInventoryHandler, itemClickListener);
        return grid;
    }

    private void configureGridAppearance(TreeDataGrid<FileTreeNode> grid){
        grid.setThemeName("no-row-borders compact row-stripes");
        grid.setWidthFull();
        grid.setHeightFull();
    }

    private void configureDataProvider(TreeDataGrid<FileTreeNode> grid, List<FileTreeNode> rootNodes){
        TreeData<FileTreeNode> treeData = new TreeData<>();
        for (FileTreeNode node : rootNodes) {
            treeData.addItem(null, node);
            addChildrenRecursively(treeData, node);
        }
        grid.setDataProvider(new TreeDataProvider<>(treeData));
    }

    private void addChildrenRecursively(TreeData<FileTreeNode> treeData, FileTreeNode parent) {
        parent.getChildren().forEach(child -> {
            treeData.addItem(parent, child);
            addChildrenRecursively(treeData, child);
        });
    }

    private void configureListeners(TreeDataGrid<FileTreeNode> grid, Consumer<FileTreeNode> itemClickListener){
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

    private void configureColumns(TreeDataGrid<FileTreeNode> grid){
        grid.addHierarchyColumn(FileTreeNode::getName).setHeader("File")
                .setTooltipGenerator(FileTreeNode::getName);

        // TODO move to inventoryItem grid
//        DataGridColumn<FileTreeNode> statusColumn = fileTreeGrid.addComponentColumn(node -> {
//            if (node.isDirectory()) return null;
//            Icon circleIcon = uiComponents.create(Icon.class);
//            circleIcon.setIcon(VaadinIcon.CIRCLE);
//            circleIcon.setSize("12px");
//            String status;
//            CodeLocation codeLocation = node.getCodeLocation();
//            if (codeLocation == null || codeLocation.getInventoryItem() == null) {
//                circleIcon.getStyle().set("color", "var(--lumo-error-color)");
//                status = "Not in inventory";
//            } else if (Boolean.TRUE.equals(codeLocation.getInventoryItem().getCurated())) {
//                circleIcon.getStyle().set("color", "var(--lumo-success-color)");
//                status = "Curated";
//            } else {
//                circleIcon.getStyle().set("color", "var(--lumo-primary-color)");
//                status = "Included but not curated";
//            }
//            circleIcon.setTooltipText(status);
//            return circleIcon;
//        });
//        statusColumn.setHeader("").setFlexGrow(0).setWidth("20px");
    }

    private void configureContextMenu(TreeDataGrid<FileTreeNode> grid, Consumer<InventoryItem> openInventoryHandler,
                                      Consumer<FileTreeNode> openFileHandler){
        GridContextMenu<FileTreeNode> contextMenu = grid.getContextMenu();
        contextMenu.addItem("Open", event -> event.getItem().ifPresent(openFileHandler));
        contextMenu.addItem("Copy Name", event -> event.getItem().ifPresent(node -> copyToClipboard(node.getName())));
        contextMenu.addItem("Copy Absolute Path", event -> event.getItem().ifPresent(node -> copyToClipboard(node.getFullPath())));
        contextMenu.addItem("Open Inventory", event -> event.getItem()
                .map(FileTreeNode::getCodeLocation)
                .map(CodeLocation::getInventoryItem)
                .ifPresent(openInventoryHandler));
    }
    private void copyToClipboard(String text) {
        UiComponentUtils.copyToClipboard(text)
                .then(success -> notifications.create("Text copied!").withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS).show(),
                        error -> notifications.create("Copy failed!").withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR).show());
    }

}

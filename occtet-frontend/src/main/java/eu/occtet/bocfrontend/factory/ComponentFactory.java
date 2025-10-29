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

import com.github.javaparser.quality.Nullable;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.model.FileTreeNode;
import eu.occtet.bocfrontend.service.TreeGridHelper;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;


/**
 * Factory class for creating UI components for the Audit View.
 * This class generates reusable UI components aimed to simplify interactions within grids
 * and enhance user experience.
 */
@Component
public class ComponentFactory {

    @Autowired
    private TreeGridHelper treeGridHelper;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Fragments fragments;
    @Autowired
    private DialogWindows dialogWindow;
    @ViewComponent
    private Notifications notifications;

    /**
     * Creates an information button with a tooltip and attaches it to the header of the specified column in the inventory grid.
     * The button also displays a popover with additional information when clicked.
     *
     * @param inventoryItemDataGrid The TreeDataGrid containing the inventory items.
     * @param colKey The key of the column where the information button will be added.
     */
    public void createInfoButtonHeaderForInventoryGrid(TreeDataGrid<InventoryItem> inventoryItemDataGrid, String colKey) {
        Grid.Column<InventoryItem> statusColumn = inventoryItemDataGrid.getColumnByKey(colKey);

        if (statusColumn == null) {
            return;
        }

        JmixButton infoButton = uiComponents.create(JmixButton.class);
        infoButton.setTooltipText("Show information");
        infoButton.setIcon(VaadinIcon.INFO_CIRCLE_O.create());
        infoButton.setThemeName("small icon tertiary");
        infoButton.setWidth("24px");
        infoButton.setHeight("24px");

        Popover popover = createInfoPopover(infoButton);

        statusColumn.setHeader(infoButton);
    }

    private Popover createInfoPopover(JmixButton target) {
        Popover popover = new Popover();
        VerticalLayout legend = new VerticalLayout(
                createLegendItem(VaadinIcon.CIRCLE, "curated-icon", "Curated"),
                createLegendItem(VaadinIcon.CIRCLE, "not-curated-icon", "Not Curated")
        );

        legend.setPadding(true);
        popover.addThemeVariants(PopoverVariant.ARROW, PopoverVariant.LUMO_NO_PADDING);
        popover.setPosition(PopoverPosition.TOP);
        popover.setModal(true);
        popover.setTarget(target);
        popover.add(legend);

        popover.setCloseOnOutsideClick(true);

        return popover;
    }

    private HorizontalLayout createLegendItem(VaadinIcon iconType, String className, String labelText) {
        Icon icon = iconType.create();
        icon.setSize("14px");
        icon.setClassName(className);

        NativeLabel label = new NativeLabel(labelText);
        label.getStyle().set("font-size", "14px");

        HorizontalLayout layout = new HorizontalLayout(icon, label);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        return layout;
    }

    /**
     * Creates a toolbox for a given and entity class.
     * The created toolbox may include specific components or functionalities
     * based on the type of the entity class provided.
     *
     * @param <T> The type of the items in the TreeDataGrid.
     * @param grid The TreeDataGrid instance for which the toolbox is created.
     * @param entityClass The class type of the entity corresponding to the grid's items.
     * @param vulnerabilityToggleHandler An optional Consumer that handles toggling
     *                                     vulnerabilities in case of {InventoryItem.
     * @return A HorizontalLayout representing the created toolbox, or null
     *         if the entity class is not supported.
     */
    public <T> HorizontalLayout createToolBox(TreeDataGrid<T> grid, Class<T> entityClass,
                                              @Nullable Consumer<Boolean> vulnerabilityToggleHandler) {
        if (entityClass.equals(InventoryItem.class)) {
            return createToolBoxForInventoryItemGrid(grid, entityClass, vulnerabilityToggleHandler);
        } else if (entityClass.equals(FileTreeNode.class)) {
            return createToolBoxForFileTreeNodeGrid(grid, entityClass);
        }
        return null;
    }

    private <T> HorizontalLayout createToolBoxForInventoryItemGrid(TreeDataGrid<T> grid, Class<T> entityClass, @Nullable Consumer<Boolean> vulnerabilityToggleHandler) {
        HorizontalLayout toolbox = uiComponents.create(HorizontalLayout.class);

        toolbox.setSpacing(true);
        toolbox.setPadding(true);
        toolbox.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

        Checkbox vulnerabilityFilter = uiComponents.create(Checkbox.class);
        vulnerabilityFilter.setLabel("vulnerable");
        vulnerabilityFilter.setValue(false);
        vulnerabilityFilter.addValueChangeListener(event -> {
            if (vulnerabilityToggleHandler != null) {
                vulnerabilityToggleHandler.accept(Boolean.TRUE.equals(event.getValue()));
            }
        });

        toolbox.setClassName("toolbox-audit-view");
        toolbox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        toolbox.setAlignItems(FlexComponent.Alignment.START);
        toolbox.setWidthFull();
        toolbox.add(vulnerabilityFilter, createExpandAndCollapseToolBar(grid));

        return toolbox;
    }

    private <T> HorizontalLayout createToolBoxForFileTreeNodeGrid(TreeDataGrid<T> grid, Class<T> entityClass) {
        HorizontalLayout toolbox = uiComponents.create(HorizontalLayout.class);

        toolbox.setSpacing(true);
        toolbox.setPadding(true);
        toolbox.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

        JmixButton searchButton = uiComponents.create(JmixButton.class);
        searchButton.setTooltipText("Search");
        searchButton.setIcon(VaadinIcon.SEARCH.create());
        searchButton.setThemeName("small icon primary");
        searchButton.addClickListener(event -> {}); // TODO

        toolbox.setClassName("toolbox-audit-view");
        toolbox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        toolbox.setAlignItems(FlexComponent.Alignment.START);
        toolbox.setWidthFull();
        toolbox.add(searchButton, createExpandAndCollapseToolBar(grid));

        return toolbox;
    }

    /**
     * Creates a horizontal toolbar containing "Expand all" and "Collapse all" buttons for a TreeDataGrid.
     * The "Expand all" button expands all children of root items in the provided grid,
     * while the "Collapse all" button collapses all children of root items.
     *
     * @param <T> The type of the items in the TreeDataGrid.
     * @param grid The TreeDataGrid instance for which the expand and collapse functionality is being created.
     * @return A HorizontalLayout containing the "Expand all" and "Collapse all" buttons.
     */
    public <T> HorizontalLayout createExpandAndCollapseToolBar(TreeDataGrid<T> grid) {
        Icon expandIcon = VaadinIcon.EXPAND.create();
        Icon compressIcon = VaadinIcon.COMPRESS.create();

        JmixButton expandAll = uiComponents.create(JmixButton.class);
        expandAll.setTooltipText("Expand all");
        expandAll.setIcon(expandIcon);
        expandAll.setThemeName("small icon primary");
        expandAll.setMaxHeight("20px");
        expandAll.setMaxWidth("28px");
        expandAll.addClickListener(event -> treeGridHelper.expandChildrenOfRoots(grid));

        JmixButton collapseAll = uiComponents.create(JmixButton.class);
        collapseAll.setTooltipText("Collapse all");
        collapseAll.setIcon(compressIcon);
        collapseAll.setThemeName("small icon primary");
        collapseAll.setMaxHeight("20px");
        collapseAll.setMaxWidth("28px");
        collapseAll.addClickListener(event -> treeGridHelper.collapseChildrenOfRoots(grid));

        HorizontalLayout mergeLayout = uiComponents.create(HorizontalLayout.class);
        mergeLayout.setMargin(false);
        mergeLayout.setPadding(false);
        mergeLayout.setWidthFull();
        mergeLayout.getStyle().set("spacing", "2px");
        mergeLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        mergeLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        mergeLayout.add(expandAll, collapseAll);

        return mergeLayout;
    }
}


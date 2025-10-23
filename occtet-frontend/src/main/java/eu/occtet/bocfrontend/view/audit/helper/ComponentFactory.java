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
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Vulnerability;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.grid.JmixTreeGrid;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


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
    @ViewComponent
    private Notifications notifications;

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

    public <T> HorizontalLayout createToolBox(TreeDataGrid<T> grid, boolean vulnerabilityFilterIsVisible,
                                              boolean searchButtonIsVisible) {
        HorizontalLayout toolbox = uiComponents.create(HorizontalLayout.class);

        toolbox.setSpacing(true);
        toolbox.setPadding(true);
        toolbox.setAlignItems(FlexComponent.Alignment.CENTER);

        JmixButton searchButton = uiComponents.create(JmixButton.class);
        searchButton.setTooltipText("Search");
        searchButton.setIcon(VaadinIcon.SEARCH.create());
        searchButton.setThemeName("small icon primary");
        searchButton.setVisible(searchButtonIsVisible);
        if (searchButtonIsVisible) {

        }

        Checkbox vulnerabilityFilter = uiComponents.create(Checkbox.class);
        vulnerabilityFilter.setLabel("vulnerable");
        vulnerabilityFilter.setValue(false);
        vulnerabilityFilter.setVisible(vulnerabilityFilterIsVisible);
        if (vulnerabilityFilterIsVisible) {
            vulnerabilityFilter.addValueChangeListener(event -> {
                vulnerabilityFilterValueChangeListener(grid);
            });
        }

        toolbox.addClassName("toolbox");
        toolbox.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        toolbox.getStyle().set("border-radius", "6px");
        toolbox.getStyle().set("padding", "var(--lumo-space-s)");
        toolbox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        toolbox.setAlignItems(FlexComponent.Alignment.START);
        toolbox.setWidthFull();
        toolbox.add(searchButton, vulnerabilityFilter, createExpandAndCollapseToolBar(grid));

        return toolbox;
    }

    public <T> HorizontalLayout createExpandAndCollapseToolBar(TreeDataGrid<T> grid) {
        JmixButton expandAll = uiComponents.create(JmixButton.class);
        expandAll.setTooltipText("Expand all");
        expandAll.setIcon(VaadinIcon.EXPAND.create());
        expandAll.setThemeName("small icon primary");
        expandAll.setWidth("20px");
        expandAll.setHeight("20px");
        expandAll.addClickListener(event -> treeGridHelper.expandChildrenOfRoots(grid));

        JmixButton collapseAll = uiComponents.create(JmixButton.class);
        collapseAll.setTooltipText("Collapse all");
        collapseAll.setIcon(VaadinIcon.COMPRESS.create());
        collapseAll.setThemeName("small icon primary");
        collapseAll.setWidth("20px");
        collapseAll.setHeight("20px");
        collapseAll.addClickListener(event -> treeGridHelper.collapseChildrenOfRoots(grid));

        HorizontalLayout toolbar = uiComponents.create(HorizontalLayout.class);
        toolbar.setWidthFull();
        toolbar.setMargin(false);
        toolbar.setPadding(false);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbar.add(expandAll, collapseAll);

        return toolbar;
    }

    private <T> void vulnerabilityFilterValueChangeListener(TreeDataGrid<T> grid){
       // TODO
    }

//    private boolean isVulnerable(InventoryItem item) {
//    }

}


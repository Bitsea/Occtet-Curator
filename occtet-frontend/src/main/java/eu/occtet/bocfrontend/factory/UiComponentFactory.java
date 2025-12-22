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

package eu.occtet.bocfrontend.factory;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.model.FileReviewedFilterMode;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Factory class responsible for creating various UI components such as toolboxes, headers,
 * and other elements for utilization in user interface construction and customization.
 */
@Component
public class UiComponentFactory {

    @Autowired
    private UiComponents uiComponents;

    public static final String SEARCH_FIELD_ID = "search-field";
    public static final String REVIEWED_FILTER_ID = "reviewed-filter";
    public static final String FIND_NEXT_ID = "find-next";
    public static final String FIND_PREVIOUS_ID = "find-previous";
    public static final String FILTER_BUTTON_ID = "filter-button";
    public static final String SEARCH_LAYOUT_ID = "search-layout";
    public static final String COUNT_LABEL_ID = "count-label";

    private final String vulnerabilityFilterId = "vulnerability-filter";

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
     * @return A HorizontalLayout representing the created toolbox, or null
     *         if the entity class is not supported.
     */
    public <T> HorizontalLayout createToolBox(TreeDataGrid<T> grid, Class<T> entityClass, Runnable onExpand, Runnable onCollapse) {
        if (entityClass.equals(InventoryItem.class)) {
            return createToolBoxForInventoryItemGrid(grid, onExpand, onCollapse);
        } else if (entityClass.equals(File.class)) {
            return createFileTreeToolbox((TreeDataGrid<File>) grid);
        }
        return null;
    }

    private <T> HorizontalLayout createToolBoxForInventoryItemGrid(TreeDataGrid<T> grid,Runnable onExpand, Runnable onCollapse) {
        HorizontalLayout toolbox = uiComponents.create(HorizontalLayout.class);

        toolbox.setSpacing(true);
        toolbox.setPadding(true);
        toolbox.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);

        Checkbox vulnerabilityFilter = uiComponents.create(Checkbox.class);
        vulnerabilityFilter.setId(vulnerabilityFilterId); // important
        vulnerabilityFilter.setLabel("vulnerable");
        vulnerabilityFilter.setValue(false);

        toolbox.setClassName("toolbox-audit-view");
        toolbox.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        toolbox.setAlignItems(FlexComponent.Alignment.START);
        toolbox.setWidthFull();
        toolbox.add(vulnerabilityFilter, createExpandAndCollapseToolBar(grid, onExpand, onCollapse));

        return toolbox;
    }

    public HorizontalLayout createFileTreeToolbox(TreeDataGrid<File> grid) {
        HorizontalLayout layout = uiComponents.create(HorizontalLayout.class);
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setWidthFull();
        layout.setClassName("toolbox-audit-view");
        layout.setAlignItems(FlexComponent.Alignment.END);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        // Search layout
        HorizontalLayout searchHl = uiComponents.create(HorizontalLayout.class);
        searchHl.setId(SEARCH_LAYOUT_ID);
        // Search Field
        TextField searchField = uiComponents.create(TextField.class);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setId(SEARCH_FIELD_ID);
        searchField.setPlaceholder("Search files...");
        searchField.setClearButtonVisible(true);
        searchField.setWidth("300px");
        JmixButton findNext = uiComponents.create(JmixButton.class);
        findNext.setId(FIND_NEXT_ID);
        findNext.setIcon(VaadinIcon.ARROW_RIGHT.create());
        findNext.setThemeName("icon small tertiary-inline");
        JmixButton findPrevious = uiComponents.create(JmixButton.class);
        findPrevious.setId(FIND_PREVIOUS_ID);
        findPrevious.setIcon(VaadinIcon.ARROW_LEFT.create());
        findPrevious.setThemeName("icon small tertiary-inline");
        NativeLabel countLabel = new NativeLabel("0:0");
        countLabel.setId(COUNT_LABEL_ID);

        searchHl.add(searchField, findNext, findPrevious, countLabel);

        // Reviewed Filter ComboBox
        JmixComboBox<FileReviewedFilterMode> reviewedFilter = uiComponents.create(JmixComboBox.class);
        reviewedFilter.setId(REVIEWED_FILTER_ID);
        reviewedFilter.setLabel("Status");

        reviewedFilter.setItems(FileReviewedFilterMode.values());

        reviewedFilter.setItemLabelGenerator(item -> switch (item) {
            case SHOW_ALL -> "Show All";
            case REVIEWED_ONLY -> "Reviewed";
            case NOT_REVIEWED_ONLY -> "Not Reviewed";
        });

        reviewedFilter.setValue(FileReviewedFilterMode.SHOW_ALL);
        reviewedFilter.setWidth("150px");

        layout.add(searchHl, reviewedFilter);

        return layout;
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
    public <T> HorizontalLayout createExpandAndCollapseToolBar(
            TreeDataGrid<T> grid,
            Runnable onExpand,
            Runnable onCollapse
    ) {
        Icon expandIcon = VaadinIcon.EXPAND.create();
        Icon compressIcon = VaadinIcon.COMPRESS.create();

        JmixButton expandAll = uiComponents.create(JmixButton.class);
        expandAll.setTooltipText("Expand all");
        expandAll.setIcon(expandIcon);
        expandAll.setThemeName("icon");

        JmixButton collapseAll = uiComponents.create(JmixButton.class);
        collapseAll.setTooltipText("Collapse all");
        collapseAll.setIcon(compressIcon);
        collapseAll.setThemeName("icon");

        expandAll.addClickListener(event -> onExpand.run());
        collapseAll.addClickListener(event -> onCollapse.run());

        HorizontalLayout mergeLayout = uiComponents.create(HorizontalLayout.class);
        mergeLayout.setMargin(false);
        mergeLayout.setPadding(false);
        mergeLayout.setSpacing(false);
        mergeLayout.setWidthFull();
        mergeLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        mergeLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        mergeLayout.add(expandAll, collapseAll);

        return mergeLayout;
    }

    public String getVulnerabilityFilterId() {
        return vulnerabilityFilterId;
    }

    /**
     * Helper to create a consistent, aligned menu item with an icon.
     */
    public com.vaadin.flow.component.Component createContextMenuItem(VaadinIcon icon, String text) {
        Icon i = icon.create();
        i.setSize("16px");
        i.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span s = new Span(text);
        s.getStyle().set("font-size", "var(--lumo-font-size-m)");

        HorizontalLayout layout = new HorizontalLayout(i, s);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setMargin(false);
        return layout;
    }
}


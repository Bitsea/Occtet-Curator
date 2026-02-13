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

package eu.occtet.bocfrontend.component;


import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import io.jmix.core.querycondition.Condition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.filter.FilterComponent;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.DataLoader;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * CustomParameterFilter is a filter component designed to integrate with a DataLoader
 * and apply map-based Boolean filters. Each filter is represented by a checkbox, and
 * the parameters are linked to a DataLoader query for dynamic filtering.
 * <p>
 * This component facilitates user interaction by enabling or disabling specific filters
 * and automatically synchronizing their states with the associated DataLoader query
 * parameters. It supports custom callbacks for external handling of filter state changes.
 * </p>
 * The class is an extension of the {@link Composite} framework under Jmix
 * and implements the {@link FilterComponent} interface.
 */
public class CustomParameterFilter extends Composite<JmixFormLayout> implements FilterComponent {

    private final HorizontalLayout rootLayout;
    private final HorizontalLayout labelsContainer;

    private final Map<String, JmixCheckbox> checkboxMap = new HashMap<>();

    private final Map<String, String> paramMap = new HashMap<>();

    private DataLoader dataLoader;

    private Consumer<CustomParameterFilter> onFilterApplied;

    // Flag to prevent multiple database loads when clicking "Clear All"
    private boolean isMassUpdate = false;

    /**
     * Creates a new filter component based on a configuration map.
     * <p>
     * <b>Map Structure:</b>
     * <ul>
     * <li><b>Key:</b> The Label displayed to the user (e.g., "Vulnerable Only").</li>
     * <li><b>Value:</b> The exact parameter name used in the JPQL query (e.g., "vulnerableOnly").</li>
     * </ul>
     * </p>
     * <p>
     * <b>Required JPQL Structure:</b><br>
     * For every parameter defined in the map, your DataLoader query must include a boolean toggle logic:
     * <pre>
     * AND (
     * :paramName = false
     * OR e.someAttribute = true
     * )
     * </pre>
     * <i>Note: All boolean parameters are initialized to FALSE by default.</i>
     * </p>
     *
     * @param uiComponents Jmix UI components factory.
     * @param filterConfig A map defining the label-to-parameter mappings. Must not be null or empty.
     * @throws IllegalArgumentException if the filterConfig is null or empty.
     */
    public CustomParameterFilter(UiComponents uiComponents,
                                 Map<String, String> filterConfig) {
        if (filterConfig == null || filterConfig.isEmpty()) {
            throw new IllegalArgumentException(
                    "CustomParameterFilter configuration cannot be null or empty. " +
                            "Please provide at least one Label -> Parameter mapping."
            );
        }

        rootLayout = uiComponents.create(HorizontalLayout.class);
        rootLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        rootLayout.setSpacing(false);
        rootLayout.setPadding(false);
        rootLayout.setMargin(false);
        rootLayout.setWidthFull();

        labelsContainer = uiComponents.create(HorizontalLayout.class);
        labelsContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        labelsContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        labelsContainer.setClassName("filter-labels-container");
        labelsContainer.setPadding(false);
        labelsContainer.setMargin(false);
        labelsContainer.setVisible(false); // Start hidden

        JmixButton filterButton = uiComponents.create(JmixButton.class);
        filterButton.setIcon(VaadinIcon.FILTER.create());
        filterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_ICON);

        Popover popover = new Popover();
        popover.setTarget(filterButton);

        VerticalLayout popoverContent = uiComponents.create(VerticalLayout.class);
        popoverContent.setPadding(false);
        popoverContent.setSpacing("4px");

        // Clear all Button
        JmixButton clearAllBtn = uiComponents.create(JmixButton.class);
        clearAllBtn.setText("Clear all");
        clearAllBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        clearAllBtn.setClassName("clear-all-filters-button");
        clearAllBtn.addClickListener(e -> reset());
        popoverContent.add(clearAllBtn);

        // Add checkboxes
        filterConfig.forEach((label, paramName) -> {
            JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
            checkbox.setLabel(label);
            checkbox.setValue(false);
            checkbox.addValueChangeListener(e -> {
                if (!isMassUpdate) {
                    updateLabels();
                    apply();
                }
            });

            checkboxMap.put(label, checkbox);
            paramMap.put(label, paramName);
            popoverContent.add(checkbox);
        });

        popover.add(popoverContent);
        rootLayout.add(filterButton, labelsContainer);
    }

    /**
     * Updates the label container by removing all existing labels and adding new ones
     * based on the current state of the checkboxes in the checkbox map.
     * @see <a href="https://vaadin.com/docs/latest/components/badge">vaadin-badge</a>
     * for theming option
     */
    private void updateLabels(){
        labelsContainer.removeAll();

        checkboxMap.forEach((label, checkbox) -> {
            if (Boolean.TRUE.equals(checkbox.getValue())) {
                Span badge = new Span();
                badge.getElement().getThemeList().add("badge small");
                badge.setClassName("filter-badge");
                Span text = new Span(label);
                Icon closeIcon = VaadinIcon.CLOSE_SMALL.create();
                closeIcon.setSize("12px");
                closeIcon.addClickListener(e -> {
                    checkbox.setValue(false);
                });
                badge.add(text, closeIcon);
                labelsContainer.add(badge);
            }
        });
        boolean hasLabels = labelsContainer.getComponentCount() > 0;
        labelsContainer.setVisible(hasLabels);
    }

    /**
     * Sets a consumer that will be executed when a filter action is applied.
     *
     * @param onFilterApplied a {@link Consumer} that accepts a {@link CustomParameterFilter}
     * which represents the updated state of the filter. This consumer is triggered whenever
     * a filter is applied. Cannot be null.
     */
    public void setOnFilterApplied(Consumer<CustomParameterFilter> onFilterApplied) {
        this.onFilterApplied = onFilterApplied;
    }

    /**
     * Helper method to sync Checkbox state -> DataLoader Parameters
     * without triggering a database load.
     */
    private void pushParametersToLoader() {
        paramMap.forEach((label, paramName) -> {
            JmixCheckbox cb = checkboxMap.get(label);
            boolean value = cb != null && Boolean.TRUE.equals(cb.getValue());
            dataLoader.setParameter(paramName, value);
        });
    }

    /**
     * Applies the current filter parameters to the associated DataLoader and optionally triggers
     * a callback if a consumer has been provided.
     * This method synchronizes the checkbox states with the DataLoader parameters
     * by invoking {@link #pushParametersToLoader()}. After updating the parameters, it initiates
     * a data load by calling the DataLoader's {@code load()} method.
     * If a filter-applied callback is set via {@link #setOnFilterApplied(Consumer)},
     * the callback is triggered, passing the current instance of {@code CustomParameterFilter} as a parameter.
     * The method has no effect if the DataLoader is not set.
     */
    @Override
    public void apply() {
        if (dataLoader == null) return;

        pushParametersToLoader();
        dataLoader.load();

        if (onFilterApplied != null) onFilterApplied.accept(this);
    }

    /**
     * Initializes the content layout for the component. Adds the root layout to the
     * form layout obtained from the superclass.
     *
     * @return the initialized {@link JmixFormLayout} containing the root layout.
     */
    @Override
    public JmixFormLayout initContent(){
        JmixFormLayout content = super.initContent();
        content.add(rootLayout);
        return content;
    }

    @Override
    public DataLoader getDataLoader() {
        return dataLoader;
    }

    /**
     * Sets the {@link DataLoader} instance for this filter and initializes default parameter values.
     * <p>
     * When a {@link DataLoader} is provided, the filter configuration map is iterated over, and each
     * parameter is initialized with a default value of {@code false} within the {@link DataLoader}.
     * </p>
     * @param dataLoader the {@link DataLoader} instance to be associated with this filter. It may be null,
     *                   in which case no default parameter initialization occurs.
     */
    @Override
    public void setDataLoader(@Nonnull DataLoader dataLoader) {
        this.dataLoader = dataLoader;

        paramMap.forEach((label, paramName) -> {
            this.dataLoader.setParameter(paramName, false);
        });
    }

    /**
     * Resets the state of all checkboxes in the filter to their default unchecked state.
     * This method iterates through all values in the checkbox map and sets their
     * value to false, effectively clearing any user-applied filter selections.
     */
    public void reset() {
        if (dataLoader == null) return;
        isMassUpdate = true;
        try {
            checkboxMap.values().forEach(cb -> cb.setValue(false));
            updateLabels();
            pushParametersToLoader();
        } finally {
            isMassUpdate = false;
        }
        dataLoader.load();
        if (onFilterApplied != null) onFilterApplied.accept(this);
    }

    @Override
    public boolean isAutoApply() {
        return true;
    }

    @Override
    public void setAutoApply(boolean autoApply) {
        // nothing must be true by default
    }

    @Override
    public boolean isConditionModificationDelegated() {
        return false;
    }

    @Override
    public void setConditionModificationDelegated(boolean conditionModificationDelegated) {

    }

    @Override public void setEnabled(boolean enabled) {
        rootLayout.setEnabled(enabled);
    }

    @Override
    public Condition getQueryCondition() {
        return LogicalCondition.and();
    }
}

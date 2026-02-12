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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import io.jmix.core.querycondition.Condition;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.filter.FilterComponent;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.DataLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CustomParameterFilter extends Composite<JmixFormLayout> implements FilterComponent {

    private final HorizontalLayout rootLayout;
    private final HorizontalLayout labelsContainer;

    private final Map<String, JmixCheckbox> checkboxMap = new HashMap<>();
    // NOTE: the key.value must match the exact name of the parameter in the query used outside
    private final Map<String, String> paramMap = new HashMap<>();

    private DataLoader dataLoader;

    private Consumer<CustomParameterFilter> onFilterApplied;

    public CustomParameterFilter(UiComponents uiComponents,
                                 Map<String, String> filterConfig) {
        rootLayout = uiComponents.create(HorizontalLayout.class);
        rootLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        rootLayout.setSpacing(false);
        rootLayout.setPadding(false);
        rootLayout.setMargin(false);

        labelsContainer = uiComponents.create(HorizontalLayout.class);
        labelsContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        labelsContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        labelsContainer.setMaxWidth("40%");
        labelsContainer.setSpacing(false);
        labelsContainer.setPadding(false);
        labelsContainer.setMargin(false);
        JmixButton filterButton = uiComponents.create(JmixButton.class);
        filterButton.setIcon(VaadinIcon.FILTER.create());
        filterButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_ICON);

        Popover popover = new Popover();
        popover.setTarget(filterButton);

        VerticalLayout popoverContent = uiComponents.create(VerticalLayout.class);
        popoverContent.setPadding(true);
        popoverContent.setSpacing(true);

        filterConfig.forEach((label, paramName) -> {
            JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
            checkbox.setLabel(label);
            checkbox.setValue(false);
            checkbox.addValueChangeListener(e -> {
                updateLabels();
                apply();
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
                Span badge = new Span(label);
                badge.getElement().getThemeList().add("badge small");
                labelsContainer.add(badge);
            }
        });
    }

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

    @Override
    public void apply() {
        if (dataLoader == null) return;

        pushParametersToLoader();
        dataLoader.load();

        if (onFilterApplied != null) onFilterApplied.accept(this);
    }

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

    @Override
    public void setDataLoader(DataLoader dataLoader) {
        this.dataLoader = dataLoader;

        if (this.dataLoader != null) {
            pushParametersToLoader();
        }
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
        return null;
    }
}

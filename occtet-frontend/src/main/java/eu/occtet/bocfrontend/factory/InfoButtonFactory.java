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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverPosition;
import com.vaadin.flow.component.popover.PopoverVariant;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.kit.component.button.JmixButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@org.springframework.stereotype.Component
public class InfoButtonFactory {
    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Messages messages;

    public JmixButton createInfoButtonFromComponent(Component component, String width, String height){
        String defaultWidth = "24px";
        String defaultHeight = "24px";
        if(width == null || width.isBlank()) width = defaultWidth;
        if(height == null || height.isBlank()) height = defaultHeight;

        log.debug("Creating info button for component: {}", component.getClass().getSimpleName());
        JmixButton infoButton = uiComponents.create(JmixButton.class);
        infoButton.setTooltipText(messages.getMessage("eu.occtet.bocfrontend.factory/InfoButtonFactory.info.button.tooltip"));
        infoButton.setIcon(VaadinIcon.INFO_CIRCLE_O.create());
        infoButton.setThemeName("small icon tertiary");
        infoButton.setWidth(width);
        infoButton.setHeight(height);

        createInfoPopover(infoButton, component);

        return infoButton;
    }

    private Popover createInfoPopover(JmixButton target, Component component) {
        Popover popover = new Popover();
        VerticalLayout content = uiComponents.create(VerticalLayout.class);

        content.setSpacing(true);
        content.add(component);

        popover.addThemeVariants(PopoverVariant.ARROW, PopoverVariant.LUMO_NO_PADDING);
        popover.setPosition(PopoverPosition.TOP);
        popover.setModal(true);
        popover.setTarget(target);
        popover.add(content);

        popover.setCloseOnOutsideClick(true);

        return popover;
    }
}

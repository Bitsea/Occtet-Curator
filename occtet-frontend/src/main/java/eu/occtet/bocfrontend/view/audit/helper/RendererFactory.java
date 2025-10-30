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

package eu.occtet.bocfrontend.view.audit.helper;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import eu.occtet.bocfrontend.entity.InventoryItem;
import io.jmix.flowui.UiComponents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.Boolean.*;

/**
 * The RendererFactory class is responsible for providing renderers
 * for visual representation of objects in a user interface.
 * This class utilizes the Jmix framework and provides methods for custom renderers,
 * such as status and file count, based on the associated data.
 */
@Component
public class RendererFactory {

    @Autowired
    private UiComponents uiComponents;

    /**
     * Creates a renderer that visually represents the "curated" status of an InventoryItem.
     */
    public Renderer<InventoryItem> statusRenderer(){
        return new ComponentRenderer<>(item -> {
            Icon circleIcon  = uiComponents.create(Icon.class);
            circleIcon.setIcon(VaadinIcon.CIRCLE);
            circleIcon.setSize("12px");
            String status = "";
            if (item == null) return circleIcon;
            if (FALSE.equals(item.getCurated())) {
                circleIcon.setClassName("not-curated-icon");
                status = "Not curated";
            } else if (TRUE.equals(item.getCurated()) || item.getCurated() == null) {
                circleIcon.setClassName("curated-icon");
                status = "Curated";
            } // more...
            circleIcon.setTooltipText(status);
            return circleIcon;
        });
    }

    /**
     * Creates a renderer that displays the file count for a given InventoryItem.
     * The renderer uses the provided supplier to retrieve a map of file counts,
     * where the keys are the IDs of InventoryItem instances, and the values are their respective file counts.
     *
     * @param fileCountsSupplier a Supplier providing a Map that contains the file count data.
     *                           The map's keys are UUID values corresponding to InventoryItem IDs,
     *                           and the values are Long representing the file counts.
     * @return a Renderer for rendering the file count of an InventoryItem.
     */
    public Renderer<InventoryItem> filesCountRenderer(Supplier<Map<UUID, Long>> fileCountsSupplier) {
        return new TextRenderer<>(item -> fileCountsSupplier.get().getOrDefault(item.getId(), 0L).toString());

    }
}

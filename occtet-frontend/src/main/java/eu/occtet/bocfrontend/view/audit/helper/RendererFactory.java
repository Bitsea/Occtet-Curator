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

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.flowui.UiComponents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Autowired
    private DataManager dataManager;

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

    public Renderer<InventoryItem> filesCountRenderer(Project project ){
        return new TextRenderer<>(item -> loadFileCounts(project).getOrDefault(item.getId(), 0L).toString());
    }

    private Map<UUID, Long> loadFileCounts(Project project) {
        ValueLoadContext context = new ValueLoadContext()
                .setQuery(new ValueLoadContext.Query(
                        "select cl.inventoryItem.id as itemId, count(cl) as fileCount from CodeLocation cl " +
                                "where cl.inventoryItem.project = :project group by cl.inventoryItem.id")
                        .setParameter("project", project))
                .addProperty("itemId")
                .addProperty("fileCount");

        List<KeyValueEntity> counts = dataManager.loadValues(context);
        return counts.stream()
                .collect(Collectors.toMap(
                        kv -> kv.getValue("itemId"),
                        kv -> kv.getValue("fileCount")
                ));
    }


}

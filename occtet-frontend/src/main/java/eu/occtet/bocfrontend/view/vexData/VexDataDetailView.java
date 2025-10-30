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

package eu.occtet.bocfrontend.view.vexData;

import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@Route(value = "vexData/:id", layout = MainView.class)
@ViewController(id = "VexData.detail")
@ViewDescriptor(path = "vex-data-detail-view.xml")
@EditedEntityContainer("vexDataDc")
public class VexDataDetailView extends StandardDetailView<VexData> {

    @Autowired
    private UiComponents uiComponents;
    @ViewComponent
    private VirtualList<Vulnerability> virtualList;

    private SoftwareComponent softwareComponent;
    private Set<Vulnerability> selectedVulnerabilities;

    public void setSelectedVulnerabilitiesAndComponent(Set<Vulnerability> selectedVulnerabilities, SoftwareComponent softwareComponent) {
        this.selectedVulnerabilities = selectedVulnerabilities;
        this.softwareComponent = softwareComponent;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        virtualList.setRenderer(vulnerabilityRenderer());
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (selectedVulnerabilities != null) {
            virtualList.setItems(selectedVulnerabilities);
        }
    }

    private Renderer<Vulnerability> vulnerabilityRenderer() {
        // TODO set values
        return new ComponentRenderer<>(vulnerability -> {
            HorizontalLayout mainLayout = uiComponents.create(HorizontalLayout.class);
            mainLayout.setPadding(true);
            mainLayout.setSpacing(true);
            mainLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            mainLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            mainLayout.addClassName("vulnerability-virtual-list-card");

            TextField vulnerabilityName = uiComponents.create(TextField.class);
            vulnerabilityName.setLabel("ID");
            vulnerabilityName.setValue(vulnerability.getVulnerabilityId());

            VerticalLayout sourceLayout = uiComponents.create(VerticalLayout.class);
            NativeLabel sourceLabel = uiComponents.create(NativeLabel.class);
            sourceLabel.setText("Source:");
            TextField sourceName = uiComponents.create(TextField.class);
            sourceLayout.add(sourceLabel, sourceName);

            VerticalLayout analysisLayout = uiComponents.create(VerticalLayout.class);
            analysisLayout.setSpacing(false);
            NativeLabel analysisLabel = uiComponents.create(NativeLabel.class);
            analysisLabel.setText("Analysis:");
            TextField analysisState = uiComponents.create(TextField.class);
            analysisState.setLabel("State");
            TextField analysisDetail = uiComponents.create(TextField.class);
            analysisDetail.setLabel("Detail");
            analysisLayout.add(analysisLabel, analysisState, analysisDetail);

            mainLayout.add(vulnerabilityName, sourceLayout, analysisLayout);
            return mainLayout;
        });
    }
}

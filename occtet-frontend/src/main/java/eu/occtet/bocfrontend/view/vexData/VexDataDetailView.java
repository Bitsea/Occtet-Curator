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
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.factory.VexDataFactory;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vexData.fragment.VexMetaDataFragment;
import eu.occtet.bocfrontend.view.vexData.fragment.VexVulnerabilityAnalysisFragment;
import eu.occtet.bocfrontend.view.vexData.fragment.VexVulnerabilityFragment;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.details.JmixDetails;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Route(value = "vexData/:id", layout = MainView.class)
@ViewController(id = "VexData.detail")
@ViewDescriptor(path = "vex-data-detail-view.xml")
@EditedEntityContainer("vexDataDc")
public class VexDataDetailView extends StandardDetailView<VexData> {

    private static final Logger log = LogManager.getLogger(VexDataDetailView.class);

    @Autowired
    private Fragments fragments;

    @ViewComponent
    private JmixDetails metaDataDetailsBox;

    @ViewComponent
    private TextField bomFormat;

    @ViewComponent
    private TextField specVersion;

    @ViewComponent
    private TextField serialNumber;
    @ViewComponent
    private IntegerField version;

    @Autowired
    private VexDataFactory vexDataFactory;
    @ViewComponent
    private VirtualList vulnerabilityBox;

    @Autowired
    private UiComponents uiComponents;

    private SoftwareComponent softwareComponent;
    private List<Vulnerability> selectedVulnerabilities;

    public void setSelectedVulnerabilitiesAndComponent(Set<Vulnerability> selectedVulnerabilities, SoftwareComponent softwareComponent) {
        this.selectedVulnerabilities = new ArrayList<>(selectedVulnerabilities);
        this.softwareComponent = softwareComponent;
    }

    @Supply(to = "vulnerabilityBox", subject = "renderer")
    protected Renderer<Vulnerability> dayRenderer() {
        return new ComponentRenderer<>(this::createVulnerabilityComponent);
    }

    private VerticalLayout createVulnerabilityComponent(Vulnerability v) {
        VexData vexData = getEditedEntity();
        VerticalLayout layout = uiComponents.create(VerticalLayout.class);
        log.debug("add vulnerability : {}", v.getVulnerabilityId());
        VexVulnerabilityFragment vFragment = fragments.create(this, VexVulnerabilityFragment.class);
        vFragment.setVexData(vexData);
        VexVulnerabilityAnalysisFragment afragment = fragments.create(this, VexVulnerabilityAnalysisFragment.class);
        afragment.setVexData(vexData);
        //afragment.setComboBoxes();
        afragment.setHostView(this);
        vFragment.setVulnerabilityAndAnalysisBox(v, afragment);
        vFragment.setHostView(this);

        layout.add(vFragment);

        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeFull();
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        return layout;
    }

    @Subscribe(id = "saveButton", subject = "clickListener")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {



    }




    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        VexData vexData= getEditedEntity();
        vulnerabilityBox.setItems(selectedVulnerabilities);
        vexDataFactory.addVexData(vexData,softwareComponent, selectedVulnerabilities);
        VexMetaDataFragment fragment = fragments.create(this, VexMetaDataFragment.class);
        fragment.setVexData(vexData);
        fragment.setHostView(this);
        metaDataDetailsBox.setWidthFull();
        metaDataDetailsBox.add(fragment);

    }


}

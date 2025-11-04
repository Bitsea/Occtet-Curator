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

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.factory.VexDataFactory;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vexData.fragment.VexMetaDataFragment;
import eu.occtet.bocfrontend.view.vexData.fragment.VexVulnerabilityFragment;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.details.JmixDetails;
import io.jmix.flowui.component.virtuallist.JmixVirtualList;
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
    private JmixVirtualList<Vulnerability> virtualList;
    @Autowired
    private UiComponents uiComponents;

    @Supply(to = "virtualList", subject = "renderer")
    protected Renderer<Vulnerability> dayRenderer() {
        return new ComponentRenderer<>(this::createVulnerabilityComponent);
    }

    private VerticalLayout createVulnerabilityComponent() {
        log.debug("create vulnerability list");
        VerticalLayout layout = uiComponents.create(VerticalLayout.class);
        for(Vulnerability v: selectedVulnerabilities) {
            VexVulnerabilityFragment fragment = fragments.create(this, VexVulnerabilityFragment.class);
            fragment.setVulnerability(v);
            layout.add(fragment);
        }
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        return layout;
    }

    private SoftwareComponent softwareComponent;
    private List<Vulnerability> selectedVulnerabilities;

    public void setSelectedVulnerabilitiesAndComponent(Set<Vulnerability> selectedVulnerabilities, SoftwareComponent softwareComponent) {
        this.selectedVulnerabilities = new ArrayList<>(selectedVulnerabilities);
        this.softwareComponent = softwareComponent;
    }


    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        log.debug("before show Selected vulnerabilities for creation {} , softwareComponent {}", selectedVulnerabilities.size(), softwareComponent.getName());

        VexData vexData= getEditedEntity();
        vexDataFactory.addVexData(vexData,softwareComponent, selectedVulnerabilities);
        virtualList.setItems(selectedVulnerabilities);
        log.debug("virtual list items size {}", virtualList.getDataProvider().size(new com.vaadin.flow.data.provider.Query<>()));
        bomFormat.setValue(vexData.getBomFormat());
        specVersion.setValue(vexData.getSpecVersion());
        serialNumber.setValue(vexData.getSerialNumber());
        version.setValue(vexData.getVersion());
        log.debug("set some vex data values, version {}", vexData.getVersion());
        VexMetaDataFragment fragment = fragments.create(this, VexMetaDataFragment.class);
        log.debug("fragment created");
        fragment.setVexData(vexData);
        metaDataDetailsBox.add(fragment);

    }


}

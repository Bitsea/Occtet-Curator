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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.factory.VexDataFactory;
import eu.occtet.bocfrontend.model.vexModels.VexVulnerability;
import eu.occtet.bocfrontend.model.vexModels.Vulnerabilites;
import eu.occtet.bocfrontend.service.VexDataService;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vexData.fragment.VexMetaDataFragment;
import eu.occtet.bocfrontend.view.vexData.fragment.VexVulnerabilityAnalysisFragment;
import eu.occtet.bocfrontend.view.vexData.fragment.VexVulnerabilityFragment;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.details.JmixDetails;
import io.jmix.flowui.download.Downloader;
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


    @Autowired
    private VexDataFactory vexDataFactory;
    @ViewComponent
    private VirtualList vulnerabilityBox;
    @Autowired
    private Downloader downloader;
    @Autowired
    private VexDataService vexDataService;

    @Autowired
    private UiComponents uiComponents;

    private List<VexVulnerabilityFragment> vexVulnerabilityFragment;
    private List<VexVulnerability> vulnerabilities;
    private SoftwareComponent softwareComponent;
    private List<Vulnerability> selectedVulnerabilities;

    public void setSelectedVulnerabilitiesAndComponent(Set<Vulnerability> selectedVulnerabilities, SoftwareComponent softwareComponent) {
        this.selectedVulnerabilities = new ArrayList<>(selectedVulnerabilities);
        this.softwareComponent = softwareComponent;
    }

    @Supply(to = "vulnerabilityBox", subject = "renderer")
    protected Renderer<Vulnerability> dayRenderer() {
        log.debug("calling renderer");
        vexVulnerabilityFragment= new ArrayList<>();
        return new ComponentRenderer<>(this::createVulnerabilityComponent);
    }

    private VerticalLayout createVulnerabilityComponent(Vulnerability v) {
        log.debug("create uiComponent for vul");
        VexData vexData = getEditedEntity();

        VerticalLayout layout = uiComponents.create(VerticalLayout.class);
        VexVulnerabilityFragment vFragment = fragments.create(this, VexVulnerabilityFragment.class);
        vFragment.setVexData(vexData);
        VexVulnerabilityAnalysisFragment afragment = fragments.create(this, VexVulnerabilityAnalysisFragment.class);
        afragment.setVexData(vexData);
        afragment.setHostView(this);
        vFragment.setVulnerabilityAndAnalysisBox(v, afragment);
        vFragment.setHostView(this);

        layout.add(vFragment);
        vexVulnerabilityFragment.add(vFragment);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeFull();
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        return layout;
    }

    private void handleVulnerabilities(VexVulnerability vexVulnerability) {
        log.debug("Handling vulnerability: " + vexVulnerability.id());
        long vulCount= vulnerabilities.stream().filter(v -> v.id().equals(vexVulnerability.id())).count();
        if(vulCount> 0){
           List<VexVulnerability> list= vulnerabilities.stream().filter(v -> v.id().equals(vexVulnerability.id())).toList();
            vulnerabilities.removeAll(list);
        }else {
            vulnerabilities.add(vexVulnerability);
        }

    }

    @Subscribe(id = "saveButton", subject = "clickListener")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        vulnerabilities= new ArrayList<>();
        vexVulnerabilityFragment.forEach(vf -> {
            handleVulnerabilities(vf.getVexVulnerability());
        });
        vexDataFactory.addVulnerabilityDataAsJson(getEditedEntity(), vulnerabilities);

        downloader.download(vexDataService.createJsonFile(getEditedEntity()));
    }




    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        VexData vexData= getEditedEntity();
        if (selectedVulnerabilities == null) {
            selectedVulnerabilities= vexData.getVulnerability();
        }
        if(softwareComponent == null){
            softwareComponent= vexData.getSoftwareComponent();
        }
        vulnerabilityBox.setItems(selectedVulnerabilities);
        vexDataFactory.addVexData(vexData,softwareComponent, selectedVulnerabilities);
        VexMetaDataFragment fragment = fragments.create(this, VexMetaDataFragment.class);
        fragment.setVexData(vexData);
        fragment.setHostView(this);
        metaDataDetailsBox.setWidthFull();
        metaDataDetailsBox.add(fragment);

    }


}

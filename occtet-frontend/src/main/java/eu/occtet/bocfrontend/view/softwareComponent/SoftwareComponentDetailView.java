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

package eu.occtet.bocfrontend.view.softwareComponent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.boc.model.VulnerabilityServiceWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.view.dialog.AddLicenseDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vulnerability.VulnerabilityDetailView;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Route(value = "software-components/:id", layout = MainView.class)
@ViewController(id = "SoftwareComponent.detail")
@ViewDescriptor(path = "software-component-detail-view.xml")
@EditedEntityContainer("softwareComponentDc")
public class SoftwareComponentDetailView extends StandardDetailView<SoftwareComponent> {

    private static final Logger log = LogManager.getLogger(SoftwareComponentDetailView.class);

    @ViewComponent
    private CollectionContainer<License> licenseDc;

    @Autowired
    private DialogWindows dialogWindow;

    @Autowired
    private Notifications notifications;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private NatsService natsService;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        SoftwareComponent softwareComponent = getEditedEntity();
        softwareComponent.setCurated(false); // set default value to false
        licenseDc.setItems(softwareComponent.getLicenses());
    }

    @Subscribe(id = "addLicenseButton")
    public void addLicenses(ClickEvent<Button> event){

        SoftwareComponent softwareComponent = getEditedEntity();
        if(softwareComponent != null){
            DialogWindow<AddLicenseDialog> window = dialogWindow.view(this,AddLicenseDialog.class).build();
            window.getView().setAvailableContent(softwareComponent);
            window.open();
            afterAddContentAction(window,softwareComponent);
        }
    }

    private void afterAddContentAction(DialogWindow<?> window, SoftwareComponent softwareComponent){
        window.addAfterCloseListener(close -> {
            if(close.closedWith(StandardOutcome.CLOSE)){
                licenseDc.setItems(softwareComponent.getLicenses());
            }
        });
    }

    @Supply(to = "vulnerabilityDataContainer.actions", subject = "renderer")
    private Renderer<Vulnerability> actionsButtonRenderer() {
        return new ComponentRenderer<>(vulnerability -> {
            JmixButton infoButton = uiComponents.create(JmixButton.class);
            infoButton.setIcon(VaadinIcon.INFO_CIRCLE.create());
            infoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            infoButton.setTooltipText("View Details");

            infoButton.addClickListener(e -> {
                dialogWindow.view(this, VulnerabilityDetailView.class)
                        .withViewConfigurer(v -> v.setEntityToEdit(vulnerability)).open();
            });

            return infoButton;
        });
    }

    @Subscribe("updateData")
    public void updateDataButtonAction(ClickEvent<JmixButton> event) {
        VulnerabilityServiceWorkData vulnerabilityServiceWorkData =
                new VulnerabilityServiceWorkData(getEditedEntity().getId());
        WorkTask workTask = new WorkTask(
                "vulnerability_task",
                "sending software component to vulnerability microservice",
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond(),
                vulnerabilityServiceWorkData);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String message = objectMapper.writeValueAsString(workTask);
            log.info("Sending software id to vulnerability microservice with message: {}", message);
            natsService.sendWorkMessageToStream("work.vulnerability", message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e){
            log.error(e);
            notifications.show("Error sending data to vulnerability microservice: " + e.getMessage());
        }
    }
}
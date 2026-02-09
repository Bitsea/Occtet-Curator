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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.boc.model.VulnerabilityServiceWorkData;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.view.dialog.AddLicenseDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vulnerability.VulnerabilityDetailView;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

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

    @Value("${nats.send-subject-vulnerabilities}")
    private String sendSubjectVulnerabilities;

    @Autowired
    private Messages messages;

    @Autowired
    private CuratorTaskService curatorTaskService;

    @Autowired
    private CuratorTaskFactory curatorTaskFactory;

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
            infoButton.setTooltipText(messages.getMessage("eu.occtet.bocfrontend.view.softwareComponent/softwareComponent.tooltip.detailButton"));

            infoButton.addClickListener(e -> {
                dialogWindow.view(this, VulnerabilityDetailView.class)
                        .withViewConfigurer(v -> v.setEntityToEdit(vulnerability)).open();
            });

            return infoButton;
        });
    }

    @Subscribe("updateData")
    public void updateDataButtonAction(ClickEvent<JmixButton> event) {
        // FIXME where to take the project and other params from?

        CuratorTask task = curatorTaskFactory.create(null, null, null);

        VulnerabilityServiceWorkData vulnerabilityServiceWorkData =
                new VulnerabilityServiceWorkData(getEditedEntity().getId());

        boolean res = curatorTaskService.saveAndRunTask(task,vulnerabilityServiceWorkData,"sending software component to vulnerability microservice",sendSubjectVulnerabilities );

        // TODO show message if failed?

    }
}
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

package eu.occtet.bocfrontend.view.services;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.boc.model.MicroserviceDescriptor;
import eu.occtet.boc.model.StatusDescriptor;
import eu.occtet.boc.model.UsageType;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.view.dialog.servicesDialog.SpdxServicesDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Resources;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.listbox.JmixListBox;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static eu.occtet.boc.model.WorkerStatus.IDLE;
import static eu.occtet.boc.model.WorkerStatus.WORKING;

@Route(value = "services-view", layout = MainView.class)
@ViewController(id = "ServicesView")
@ViewDescriptor(path = "services-view.xml")
public class ServicesView extends StandardView {

    @ViewComponent
    protected JmixListBox<StatusDescriptor> statusList;

    @Autowired
    protected UiComponents uiComponents;
    @Autowired
    protected Resources resources;

    @Autowired
    private NatsService natsService;
    @ViewComponent
    private Span streamStatus;

    private List<MicroserviceDescriptor> availableServices = new ArrayList<>();
    private List<StatusDescriptor> statusDescriptors = new ArrayList<>();

    private static final Logger log = LogManager.getLogger(ServicesView.class);

    private UI ui;

    @Autowired
    private Dialogs dialogs;

    @Autowired
    private DialogWindows dialogWindow;


    @Subscribe
    protected void onInit(InitEvent event) {
        ui = UI.getCurrent();
        // connect listeners to NATS service
        natsService.addMicroserviceDescriptorListener(this::onMicroserviceDescriptorReceived);
        natsService.addStatusDescriptorListener(this::onStatusDescriptorReceived);

        updateAvailableServices();

        updateServiceStatus();

        updateNatsStreamStatus();

    }

    private void updateNatsStreamStatus() {
        ui.access(() ->{
            streamStatus.setText(natsService.getStreamStatusAsString());
        });
    }


    @Scheduled(fixedRate = 10000)
    public void execute() {
        updateServiceStatus();
    }

    private void updateServiceStatus() {
        statusDescriptors.clear();
        natsService.sendSimpleMessage("system","status");
    }

    @Subscribe(id = "detectServices", subject = "clickListener")
    public void onDetectServicesClick(final ClickEvent<JmixButton> event) {
        updateAvailableServices();
    }

    @Subscribe(id = "stopServices", subject = "clickListener")
    public void onStopServicesClick(final ClickEvent<JmixButton> event) {
        dialogs.createOptionDialog()
                .withHeader("Please confirm")
                .withText("Do you really want to stop all services?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withHandler(e -> {
                                    natsService.sendSimpleMessage("system","exit");
                                    statusDescriptors.clear();
                                }),
                        new DialogAction(DialogAction.Type.NO)
                )
                .open();
    }

    private void updateAvailableServices() {
        availableServices = new ArrayList<>();
        natsService.sendHello();
    }

    @Supply(to = "statusList", subject = "renderer")
    protected ComponentRenderer<HorizontalLayout, StatusDescriptor> statusListBoxRenderer() {
        return new ComponentRenderer<>(statusDescriptor -> {
            HorizontalLayout row = uiComponents.create(HorizontalLayout.class);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            Icon icon = ComponentUtils.parseIcon("vaadin:circle");
            icon.setColor(statusDescriptor.getStatus()==IDLE ? "green" : statusDescriptor.getStatus()==WORKING ? "orange" : "red");
            icon.setSize("12px");
            row.add(icon );

            VerticalLayout column = uiComponents.create(VerticalLayout.class);
            column.add(new H4(statusDescriptor.getName() +": " + statusDescriptor.getStatus() ));
            Optional<MicroserviceDescriptor> optionalMicroserviceDescriptor = availableServices.stream().filter(md -> md.getName().equals(statusDescriptor.getName())).findFirst();
            if(optionalMicroserviceDescriptor.isPresent()) {
                MicroserviceDescriptor microserviceDescriptor = optionalMicroserviceDescriptor.get();
                Span descriptionAndVersion = new Span(microserviceDescriptor.getDescription()
                        + ", version " + microserviceDescriptor.getVersion());
                column.add(descriptionAndVersion);
            }
            column.setPadding(false);
            column.setSpacing(false);
            row.add(column);
            return row;
        });
    }


    private void onStatusDescriptorReceived(StatusDescriptor statusDescriptor) {
        log.debug("received a status descriptor from {}", statusDescriptor.getName());
        // avoid duplicates
        if (statusDescriptors.stream().noneMatch(s -> s.getName().equals(statusDescriptor.getName()))) {
            statusDescriptors.add(statusDescriptor);
            ui.access(() -> {
                statusList.setItems(statusDescriptors);
            });
        }
    }

    private void onMicroserviceDescriptorReceived(MicroserviceDescriptor microserviceDescriptor) {
        log.debug("received a microservice descriptor from {}", microserviceDescriptor.getName());
        // avoid duplicates
        if (availableServices.stream().noneMatch(s -> s.getName().equals(microserviceDescriptor.getName()))) {
            availableServices.add(microserviceDescriptor);
        }
    }

    private void openDialogService(String name){

        switch(name){
            case "occtet-nats-spdx-service" -> dialogWindow.view(this,SpdxServicesDialog.class).open();

        }
    }
}
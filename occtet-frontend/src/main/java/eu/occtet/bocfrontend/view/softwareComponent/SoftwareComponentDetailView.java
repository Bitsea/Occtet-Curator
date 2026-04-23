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
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.boc.model.VulnerabilityServiceWorkData;
import eu.occtet.bocfrontend.dao.ProjectRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.factory.CuratorTaskFactory;
import eu.occtet.bocfrontend.service.CuratorTaskService;
import eu.occtet.bocfrontend.view.dialog.AddCopyrightDialog;
import eu.occtet.bocfrontend.view.dialog.AddLicenseDialog;
import eu.occtet.bocfrontend.view.dialog.CreateCopyrightForSoftwareComponentDialog;
import eu.occtet.bocfrontend.view.dialog.CreateLicenseDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.vulnerability.VulnerabilityDetailView;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.vaadin.flow.component.notification.Notification.Position.TOP_CENTER;


@Route(value = "software-components/:id", layout = MainView.class)
@ViewController(id = "SoftwareComponent.detail")
@ViewDescriptor(path = "software-component-detail-view.xml")
@EditedEntityContainer("softwareComponentDc")
public class SoftwareComponentDetailView extends StandardDetailView<SoftwareComponent> {

    private static final Logger log = LogManager.getLogger(SoftwareComponentDetailView.class);

    @ViewComponent
    private CollectionContainer<License> licenseDc;
    @ViewComponent
    private CollectionContainer<Copyright> copyrightDc;
    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private DataGrid<Copyright> copyrightDataGrid;
    @ViewComponent
    private DataGrid<License> licensesDataGrid;
    @ViewComponent
    private JmixButton removeCopyrightButton;
    @ViewComponent
    private JmixButton removeLicenseButton;

    @Autowired
    private DialogWindows dialogWindow;
    @Autowired
    private Notifications notifications;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Messages messages;
    @Autowired
    private CuratorTaskService curatorTaskService;
    @Autowired
    private CuratorTaskFactory curatorTaskFactory;
    @Autowired
    private ProjectRepository projectRepository;

    @Value("${nats.send-subject-vulnerabilities}")
    private String sendSubjectVulnerabilities;

    private static final String UPDATE_VULNERS_CURATOR_TASK_TYPE = "updating_vulnerabilities";
    private static final String UPDATE_VULNERS_CURATOR_TASK_NAME = "updating_vulnerabilities_for_";

    private boolean deleteCopyrightMode = false;
    private boolean deleteLicenseMode = false;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        SoftwareComponent softwareComponent = getEditedEntity();
        licenseDc.setItems(softwareComponent.getLicenses());
        copyrightDc.setItems(softwareComponent.getCopyrights());
    }

    /**
     * Opens a dialog to select and append existing copyrights to the software component.
     * This ensures additions are staged in the data context and only persisted when the main view is saved.
     *
     * @param event the click event triggered by selecting the add copyright dropdown item.
     */
    @Subscribe(id = "editCopyright.addCopyright")
    public void addCopyrights(DropdownButtonItem.ClickEvent event) {
        SoftwareComponent softwareComponent = getEditedEntity();

        if (softwareComponent != null) {
            DialogWindow<AddCopyrightDialog> window = dialogWindow.view(this, AddCopyrightDialog.class).build();
            window.getView().setAvailableContent(softwareComponent);
            window.open();

            window.addAfterCloseListener(close -> {
                if (close.closedWith(StandardOutcome.SAVE)) {
                    List<Copyright> selectedCopyrights = window.getView().getSelectedCopyrights();

                    if (selectedCopyrights != null && !selectedCopyrights.isEmpty()) {
                        for (Copyright copyright : selectedCopyrights) {
                            Copyright trackedCopyright = dataContext.merge(copyright);

                            if (softwareComponent.getCopyrights() == null) {
                                softwareComponent.setCopyrights(new ArrayList<>());
                            }

                            if (!softwareComponent.getCopyrights().contains(trackedCopyright)) {
                                softwareComponent.getCopyrights().add(trackedCopyright);
                                copyrightDc.getMutableItems().add(trackedCopyright);
                            }
                        }
                        infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.CopyrightAdd"));
                    }
                }
            });
        }
    }

    /**
     * Initializes a new component-level copyright entry via a specialized dialog.
     * The resulting entity relationship is staged strictly in memory to defer database
     * persistence until the user saves the primary view.
     *
     * @param event the click event triggered by selecting the create copyright dropdown item.
     */
    @Subscribe(id = "editCopyright.createCopyright")
    public void createAndAddCopyright(DropdownButtonItem.ClickEvent event) {
        SoftwareComponent softwareComponent = getEditedEntity();
        DialogWindow<CreateCopyrightForSoftwareComponentDialog> window = dialogWindow.view(this,
                CreateCopyrightForSoftwareComponentDialog.class).build();

        window.getView().setAvailableContent(softwareComponent);
        window.open();

        window.addAfterCloseListener(close -> {
            if (close.closedWith(StandardOutcome.SAVE)) {
                Copyright newCopyright = window.getView().getCreatedCopyright();

                if (newCopyright != null) {
                    Copyright trackedCopyright = dataContext.merge(newCopyright);
                    softwareComponent.getCopyrights().add(trackedCopyright);
                    copyrightDc.getMutableItems().add(trackedCopyright);

                    infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.CopyrightCreate"));
                }
            }
        });
    }

    /**
     * Opens a dialog to select and append existing licenses to the software component.
     * This ensures modifications are tracked safely in the data context until the user confirms the overall changes.
     * @param event the click event triggered by selecting the add license dropdown item.
     */
    @Subscribe(id = "editLicense.addLicense")
    public void addLicenses(DropdownButtonItem.ClickEvent event) {
        SoftwareComponent softwareComponent = getEditedEntity();
        DialogWindow<AddLicenseDialog> window = dialogWindow.view(this, AddLicenseDialog.class).build();
        window.getView().setAvailableContent(softwareComponent);
        window.open();

        window.addAfterCloseListener(close -> {
            if (close.closedWith(StandardOutcome.SAVE)) {
                List<License> selectedLicenses = window.getView().getSelectedLicenses();

                if (selectedLicenses != null && !selectedLicenses.isEmpty()) {
                    for (License license : selectedLicenses) {
                        License trackedLicense = dataContext.merge(license);

                        if (!softwareComponent.getLicenses().contains(trackedLicense)) {
                            softwareComponent.getLicenses().add(trackedLicense);
                            licenseDc.getMutableItems().add(trackedLicense);
                        }
                    }
                    infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.LicenseAdd"));
                }
            }
        });
    }

    /**
     * Initializes a new license entry and links it to the current software component.
     * The new relationship is managed strictly in memory.
     *
     * @param event the click event triggered by selecting the create license dropdown item
     */
    @Subscribe(id = "editLicense.createLicense")
    public void createAndAddLicense(DropdownButtonItem.ClickEvent event) {
        SoftwareComponent softwareComponent = getEditedEntity();
        DialogWindow<CreateLicenseDialog> window = dialogWindow.view(this, CreateLicenseDialog.class).build();
        window.getView().setAvailableContent(softwareComponent);
        window.open();

        window.addAfterCloseListener(close -> {
            if (close.closedWith(StandardOutcome.SAVE)) {
                License newLicense = window.getView().getCreatedLicense();

                if (newLicense != null) {
                    License trackedLicense = dataContext.merge(newLicense);
                    if (softwareComponent.getLicenses() == null) {
                        softwareComponent.setLicenses(new ArrayList<>());
                    }
                    softwareComponent.getLicenses().add(trackedLicense);
                    licenseDc.getMutableItems().add(trackedLicense);
                    infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.LicenseCreate"));
                }
            }
        });
    }

    /**
     * Toggles the UI deletion mode for copyrights to allow for batch removal.
     * Switches the grid selection mode to multiple and displays the deletion confirmation button.
     *
     * @param event the click event triggered by selecting the remove copyright dropdown item
     */
    @Subscribe(id = "editCopyright.removeCopyright")
    public void toggleRemoveCopyrightMode(DropdownButtonItem.ClickEvent event) {
        deleteCopyrightMode = !deleteCopyrightMode;

        copyrightDataGrid.setSelectionMode(
                deleteCopyrightMode ? DataGrid.SelectionMode.MULTI : DataGrid.SelectionMode.SINGLE
        );
        removeCopyrightButton.setVisible(deleteCopyrightMode);
    }

    /**
     * Severs the link between the selected copyrights and the current software component.
     * Updates the in-memory data context and UI container to reflect the removal without committing to the database.
     *
     * @param event the click event triggered by the remove copyright action button
     */
    @Subscribe(id = "removeCopyrightButton")
    public void removeCopyrights(ClickEvent<JmixButton> event) {
        Set<Copyright> selectedCopyrights = copyrightDataGrid.getSelectedItems();
        SoftwareComponent softwareComponent = getEditedEntity();

        if (!selectedCopyrights.isEmpty() && softwareComponent != null) {
            for (Copyright copyright : selectedCopyrights) {
                softwareComponent.getCopyrights().remove(copyright);
                copyrightDc.getMutableItems().remove(copyright);
            }
            infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.CopyrightRemove"));
        }

        deleteCopyrightMode = false;
        copyrightDataGrid.setSelectionMode(DataGrid.SelectionMode.SINGLE);
        copyrightDataGrid.deselectAll();
        removeCopyrightButton.setVisible(false);
    }

    /**
     * Toggles the UI deletion mode for licenses to allow batch removal.
     * Switches the grid selection mode to multiple and updates visibility for the confirm removal button.
     *
     * @param event the click event triggered by selecting the remove license dropdown item
     */
    @Subscribe(id = "editLicense.removeLicense")
    public void toggleRemoveLicenseMode(DropdownButtonItem.ClickEvent event) {
        deleteLicenseMode = !deleteLicenseMode;

        licensesDataGrid.setSelectionMode(
                deleteLicenseMode ? DataGrid.SelectionMode.MULTI : DataGrid.SelectionMode.SINGLE
        );
        removeLicenseButton.setVisible(deleteLicenseMode);
    }

    /**
     * Severs the link between the selected licenses and the current software component.
     * Applies the changes strictly to the in-memory data context and updates the UI container.
     *
     * @param event the click event triggered by the remove license action button
     */
    @Subscribe(id = "removeLicenseButton")
    public void removeLicenses(ClickEvent<JmixButton> event) {
        Set<License> selectedLicenses = licensesDataGrid.getSelectedItems();
        SoftwareComponent softwareComponent = getEditedEntity();

        if (!selectedLicenses.isEmpty() && softwareComponent != null) {
            for (License license : selectedLicenses) {
                softwareComponent.getLicenses().remove(license);
                licenseDc.getMutableItems().remove(license);
            }
            infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.LicenseRemove"));
        }

        deleteLicenseMode = false;
        licensesDataGrid.setSelectionMode(DataGrid.SelectionMode.SINGLE);
        licensesDataGrid.deselectAll();
        removeLicenseButton.setVisible(false);
    }

    @Supply(to = "vulnerabilityLinksDataGrid.actions", subject = "renderer")
    private Renderer<ComponentVulnerabilityLink> actionsButtonRenderer() {
        return new ComponentRenderer<>(link -> {
            JmixButton infoButton = uiComponents.create(JmixButton.class);
            infoButton.setIcon(VaadinIcon.INFO_CIRCLE.create());
            infoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            infoButton.setTooltipText(messages.getMessage("eu.occtet.bocfrontend.view.softwareComponent/softwareComponent.tooltip.detailButton"));

            infoButton.addClickListener(e -> {
                DialogWindow<VulnerabilityDetailView> windows = dialogWindow.view(this, VulnerabilityDetailView.class)
                        .withViewConfigurer(v -> v.setEntityToEdit(link.getVulnerability())).open();
                windows.setSizeFull();
            });

            return infoButton;
        });
    }

    @Subscribe("updateData")
    public void updateDataButtonAction(ClickEvent<JmixButton> event) {
        String importName = UPDATE_VULNERS_CURATOR_TASK_NAME + getEditedEntity().getName();
        Project project = projectRepository.findAll().getFirst();

        CuratorTask task = curatorTaskFactory.create(project, importName, UPDATE_VULNERS_CURATOR_TASK_TYPE);

        VulnerabilityServiceWorkData vulnerabilityServiceWorkData =
                new VulnerabilityServiceWorkData(getEditedEntity().getId());

        boolean res = curatorTaskService.saveAndRunTask(task,vulnerabilityServiceWorkData,"sending software component to vulnerability microservice",sendSubjectVulnerabilities );
        notifications.create(messages.getMessage("eu.occtet.bocfrontend.view.softwareComponent/notification.updateData"))
                .withThemeVariant(NotificationVariant.LUMO_SUCCESS).show();

        if(!res) {
            log.debug("Failed to run task for updating vulnerabilites of software component {}",
                    getEditedEntity().getName());
        }
    }

    private void infoMessage(String message) {
        notifications.create(message)
                .withPosition(TOP_CENTER)
                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                .withDuration(3000)
                .show();
    }
}
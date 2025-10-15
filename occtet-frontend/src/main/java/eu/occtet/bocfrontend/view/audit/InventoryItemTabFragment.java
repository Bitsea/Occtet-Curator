/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.bocfrontend.view.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import eu.occtet.boc.model.VulnerabilityServiceWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.view.audit.filestabfragment.FilesTabFragment;
import eu.occtet.bocfrontend.view.copyright.CopyrightDetailView;
import eu.occtet.bocfrontend.view.dialog.*;
import eu.occtet.bocfrontend.view.inventoryitem.InventoryItemDetailView;
import eu.occtet.bocfrontend.view.license.LicenseDetailView;
import eu.occtet.bocfrontend.view.softwareComponent.SoftwareComponentDetailView;
import eu.occtet.bocfrontend.view.vulnerability.VulnerabilityDetailView;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.richtexteditor.RichTextEditor;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import jakarta.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@FragmentDescriptor("InventoryItemTabFragment.xml")
public class InventoryItemTabFragment extends Fragment<JmixTabSheet> {

    private static final Logger log = LogManager.getLogger(InventoryItemTabFragment.class);
    @Autowired
    protected UiComponents uiComponents;

    private View<?> hostView;
    private InventoryItem inventoryItem;
    private SoftwareComponent softwareComponent;
    private boolean deleteMode = false;

    @ViewComponent
    private CollectionContainer<License> licenseDc;
    @ViewComponent
    private CollectionContainer<Copyright> copyrightDc;
    @ViewComponent
    private InstanceContainer<InventoryItem> inventoryItemDc;
    @ViewComponent
    private InstanceContainer<SoftwareComponent> softwareComponentDc;
    @ViewComponent
    private JmixButton parentButton;
    @ViewComponent
    private JmixButton softwareComponentButton;
    @ViewComponent
    private DropdownButton editLicense;
    @ViewComponent
    private JmixButton removeLicenseButton;
    @ViewComponent
    private JmixButton removeCopyrightButton;
    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private DataGrid<License> licensesDataGrid;
    @ViewComponent
    private DataGrid<Copyright> copyrightsDataGrid;
    @ViewComponent
    private JmixButton auditHistoryButton;
    @ViewComponent
    private CollectionContainer<Copyright> copyrightDcHistory;
    @ViewComponent
    private TextField softwareComponentHistoryField;
    @ViewComponent
    private TextField parentHistoryID;
    @ViewComponent
    private JmixButton parentHistoryButton;
    @ViewComponent
    private RichTextEditor auditNotesText;
    @ViewComponent
    private DataGrid<InventoryItem> inventoryDataGridHistory;

    @ViewComponent
    private CollectionContainer<InventoryItem> inventoryItemDcHistory;

    // Tab fragments
    @ViewComponent
    private FilesTabFragment filesTabFragment;

    @Autowired
    private DialogWindows dialogWindow;
    @Autowired
    private Notifications notifications;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @Autowired
    private NatsService natsService;


    public void setInventoryItem(@Nonnull InventoryItem inventoryItem) {
        // track instances !important for saving
        this.inventoryItem = dataContext.merge(inventoryItem);
        this.softwareComponent = inventoryItem.getSoftwareComponent();
        if (this.softwareComponent != null) {
            this.softwareComponent = dataContext.merge(this.softwareComponent);
            softwareComponentDc.setItem(this.softwareComponent);
        }

        inventoryItemDc.setItem(this.inventoryItem);
        updateCopyrights(this.inventoryItem, copyrightDc);
        updateLicenses(this.softwareComponent, licenseDc);

        //History of inventory
        setHistoryOfInventory(inventoryItem);

        filesTabFragment.setInventoryItemId(this.inventoryItem);

        // Handle disabling buttons to prevent errors
        parentButton.setEnabled(this.inventoryItem.getParent() != null);
        softwareComponentButton.setEnabled(softwareComponent != null);
        editLicense.setEnabled(softwareComponent != null);
    }

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;

        filesTabFragment.setHostView(hostView);
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    @Subscribe("saveAction")
    public void onSaveAction(ActionPerformedEvent event) {
        if (dataContext.hasChanges()) {
            log.debug("Inventory Item {} has changes.", inventoryItem.getInventoryName());
            dataContext.save();
            notifications.create("Changes saved.").withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                    .show();
            if (hostView instanceof AuditView) {
                ((AuditView) hostView).refreshInventoryItemDc(inventoryItem.getProject());
            }
        } else {
            log.debug("Inventory Item {} has no changes.", inventoryItem.getInventoryName());
            notifications.create("No changes to save.").withPosition(Notification.Position.BOTTOM_END).show();
        }
    }

    @Subscribe(id = "editLicense.addLicense")
    public void addLicense(DropdownButtonItem.ClickEvent event) {
        if (softwareComponent != null) {
            DialogWindow<AddLicenseDialog> window = dialogWindow.view(hostView, AddLicenseDialog.class).build();
            window.getView().setAvailableContent(softwareComponent);
            window.open();
            window.addAfterCloseListener(close ->
                    updateLicenses(softwareComponent, licenseDc));
        }
    }

    @Subscribe(id = "editLicense.removeLicense")
    public void removeLicenses(DropdownButtonItem.ClickEvent event) {
        deleteMode = !deleteMode;

        licensesDataGrid.setSelectionMode(
                deleteMode ? DataGrid.SelectionMode.MULTI : DataGrid.SelectionMode.SINGLE
        );
        removeLicenseButton.setVisible(deleteMode);
    }

    @Subscribe(id = "removeLicenseButton")
    public void removeLicenses(ClickEvent<JmixButton> event) {
        Set<License> selectedLicenses = licensesDataGrid.getSelectedItems();

        if (!selectedLicenses.isEmpty()) {
            softwareComponent.getLicenses().removeAll(selectedLicenses);
            softwareComponentRepository.save(softwareComponent);
            updateLicenses(softwareComponent, licenseDc);

            notifications.create("Licenses removed.")
                    .withPosition(Notification.Position.BOTTOM_END)
                    .show();
        }

        deleteMode = false;
        licensesDataGrid.setSelectionMode(DataGrid.SelectionMode.SINGLE);
        licensesDataGrid.deselectAll();
        removeLicenseButton.setVisible(false);
    }

    @Subscribe("editLicense.createLicense")
    public void createAndAddLicense(DropdownButtonItem.ClickEvent event) {
        if (softwareComponent != null) {
            DialogWindow<CreateLicenseDialog> window = dialogWindow.view(hostView, CreateLicenseDialog.class).build();
            window.getView().setAvailableContent(softwareComponent);
            window.open();
            window.addAfterCloseListener(close ->
                    updateLicenses(softwareComponent, licenseDc));
        }

    }

    @Subscribe("licensesDataGrid")
    public void onLicensesDataGridItemDoubleClick(final ItemDoubleClickEvent<License> event) {
        DialogWindow<LicenseDetailView> window = dialogWindow.view(hostView, LicenseDetailView.class).build();
        window.getView().setEntityToEdit(event.getItem());
        window.open();
    }

    @Subscribe("copyrightsDataGrid")
    public void onCopyrightDataGridItemDoubleClick(final ItemDoubleClickEvent<Copyright> event) {
        DialogWindow<CopyrightDetailView> window = dialogWindow.view(hostView, CopyrightDetailView.class).build();
        window.getView().setEntityToEdit(event.getItem());
        window.setSizeFull();
        window.open();
    }


    @Subscribe(id = "editCopyright.addCopyright")
    public void addCopyright(DropdownButtonItem.ClickEvent event) {

        DialogWindow<AddCopyrightDialog> window = dialogWindow.view(hostView, AddCopyrightDialog.class).build();
        window.getView().setAvailableContent(inventoryItem);
        window.open();
        window.addAfterCloseListener(close -> updateCopyrights(inventoryItem, copyrightDc));
    }

    @Subscribe(id = "editCopyright.removeCopyright")
    public void removeCopyrights(DropdownButtonItem.ClickEvent event) {
        deleteMode = !deleteMode;

        copyrightsDataGrid.setSelectionMode(
                deleteMode ? DataGrid.SelectionMode.MULTI : DataGrid.SelectionMode.SINGLE
        );
        removeCopyrightButton.setVisible(deleteMode);
    }

    @Subscribe("editCopyright.createCopyright")
    public void createAndAddCopyright(DropdownButtonItem.ClickEvent event) {
        if (inventoryItem != null) {
            DialogWindow<CreateCopyrightDialog> window = dialogWindow.view(hostView, CreateCopyrightDialog.class).build();
            window.getView().setAvailableContent(inventoryItem);
            window.open();
            window.addAfterCloseListener(close ->
                    updateCopyrights(inventoryItem, copyrightDc));
        }

    }

    @Subscribe(id = "removeCopyrightButton")
    public void removeCopyrights(ClickEvent<JmixButton> event) {
        Set<Copyright> selectedCopyrights = copyrightsDataGrid.getSelectedItems();

        if (!selectedCopyrights.isEmpty()) {
            inventoryItem.getCopyrights().removeAll(selectedCopyrights);
            inventoryItemRepository.save(inventoryItem);   // or your proper repo
            updateCopyrights(inventoryItem, copyrightDc);

            notifications.create("Copyrights removed.")
                    .withPosition(Notification.Position.BOTTOM_END)
                    .show();
        }

        deleteMode = false;
        copyrightsDataGrid.setSelectionMode(DataGrid.SelectionMode.SINGLE);
        copyrightsDataGrid.deselectAll();
        removeCopyrightButton.setVisible(false);
    }

    @Subscribe(id = "parentButton")
    public void showParentDetails(ClickEvent<Button> event) {

        InventoryItem parent = inventoryItem.getParent();
        if (parent != null) {
            dialogWindow.view(hostView, InventoryItemDetailView.class)
                    .withViewConfigurer(i -> i.setEntityToEdit(parent)).open();
        }
    }

    @Subscribe(id = "softwareComponentButton")
    public void showSoftwareComponentDetails(ClickEvent<Button> event) {
        dialogWindow.view(hostView, SoftwareComponentDetailView.class)
                .withViewConfigurer(scView -> scView.setEntityToEdit(softwareComponent)).open();
    }

    private void updateCopyrights(InventoryItem inventoryItem, CollectionContainer<Copyright> container) {
        container.setItems(inventoryItem.getCopyrights());
    }

    private void updateLicenses(SoftwareComponent softwareComponent, CollectionContainer<License> container) {
        if (softwareComponent != null) {
            container.setItems(softwareComponent.getLicenses());
        } else {
            container.setItems(new ArrayList<>());
        }
    }

    @Subscribe(id = "inventoryDataGridHistory")
    public void showHistoryfromInventoryItem(final ItemClickEvent<InventoryItem> event) {

        InventoryItem item = event.getItem();
        if (item != null) {
            if (item.getExternalNotes() != null && !item.getExternalNotes().isEmpty()) {
                auditHistoryButton.setEnabled(true);
            } else {
                auditHistoryButton.setEnabled(false);
            }
            if (item.getCopyrights() != null) {
                copyrightDcHistory.setItems(item.getCopyrights());
            }
            if (item.getSoftwareComponent() != null) {
                softwareComponentHistoryField.setValue(item.getSoftwareComponent().getName());
            }
            if (item.getParent() != null) {
                parentHistoryID.setValue(item.getParent().getInventoryName());
                parentHistoryButton.setEnabled(true);
            }
        }
    }

    @Subscribe(id = "auditHistoryButton")
    public void addAuditHistoryToInventory(final ClickEvent<Button> event) {
        InventoryItem historyItem = inventoryDataGridHistory.getSingleSelectedItem();
        if (historyItem != null) {
            String notes = this.inventoryItem.getExternalNotes();
            if (notes == null) {
                this.inventoryItem.setExternalNotes(getTimeStampSeperator() + "<br>" + historyItem.getExternalNotes());
            } else {
                this.inventoryItem.setExternalNotes(notes + "<br>" + getTimeStampSeperator() + "<br>" + historyItem.getExternalNotes());
            }
            inventoryItemRepository.save(this.inventoryItem);
            auditNotesText.setValue(this.inventoryItem.getExternalNotes());
        }
    }

    @Subscribe(id = "copyrightHistoryButton")
    public void addHistoryCopyrights(final ClickEvent<Button> event) {

        InventoryItem historyItem = inventoryDataGridHistory.getSingleSelectedItem();
        if (historyItem != null) {
            DialogWindow<AddCopyrightHistoryDialog> window = dialogWindow.view(hostView, AddCopyrightHistoryDialog.class).build();
            window.getView().setLatestInventoryItem(inventoryItem);
            window.getView().setAvailableContent(historyItem);
            window.addAfterOpenListener(e ->
                    log.debug("Copyrights before: {}", inventoryItem.getCopyrights().size()));
            window.addAfterCloseListener(e -> {
                log.debug("Copyrights after: {}", inventoryItem.getCopyrights().size());
                updateCopyrights(inventoryItem, copyrightDc);
                updateCopyrights(historyItem, copyrightDcHistory);
            });
            window.open();
        }
    }

    @Subscribe("updateData")
    public void updateDataButtonAction(ClickEvent<JmixButton> event) {
        VulnerabilityServiceWorkData vulnerabilityServiceWorkData =
                new VulnerabilityServiceWorkData(inventoryItem.getSoftwareComponent().getId());
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
        } catch (Exception e) {
            log.error(e);
            notifications.show("Error sending data to vulnerability microservice: " + e.getMessage());
        }
    }

    @Supply(to = "inventoryDataGridHistory.createdAt", subject = "renderer")
    private Renderer<InventoryItem> inventoryItemDataGridDateRenderer() {
        return new TextRenderer<>(item -> item.getCreatedAt().toLocalDate().toString());
    }

    private void setHistoryOfInventory(InventoryItem inventoryItem) {

        List<InventoryItem> historyItems;
        SoftwareComponent softwareComponent = inventoryItem.getSoftwareComponent();

        if (softwareComponent != null) {
            historyItems = inventoryItemRepository.findBySoftwareComponent(softwareComponent);
            historyItems.remove(inventoryItem);
        } else {
            historyItems = new ArrayList<>();
        }
        inventoryItemDcHistory.setItems(historyItems);
    }

    private String getTimeStampSeperator() {
        return "------------- " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + "------------";
    }

    @Supply(to = "vulnerabilityDataContainer.actions", subject = "renderer")
    private Renderer<Vulnerability> actionsButtonRenderer() {
        return new ComponentRenderer<>(vulnerability -> {
            JmixButton infoButton = uiComponents.create(JmixButton.class);
            infoButton.setIcon(VaadinIcon.INFO_CIRCLE.create());
            infoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            infoButton.setTooltipText("View Details");

            infoButton.addClickListener(e -> {
                dialogWindow.view(hostView, VulnerabilityDetailView.class)
                        .withViewConfigurer(v -> v.setEntityToEdit(vulnerability)).open();
            });

            return infoButton;
        });
    }
}
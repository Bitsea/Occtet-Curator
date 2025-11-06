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

package eu.occtet.bocfrontend.fragment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import eu.occtet.bocfrontend.dao.InventoryItemRepository;
import eu.occtet.bocfrontend.dao.SoftwareComponentRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.view.audit.AuditView;
import eu.occtet.bocfrontend.view.copyright.CopyrightDetailView;
import eu.occtet.bocfrontend.view.dialog.*;
import eu.occtet.bocfrontend.view.inventoryitem.InventoryItemDetailView;
import eu.occtet.bocfrontend.view.license.LicenseDetailView;
import eu.occtet.bocfrontend.view.softwareComponent.SoftwareComponentDetailView;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    @ViewComponent
    private Vulnerabilitytabfragment vulnerabilitytabfragment;

    @Autowired
    private DialogWindows dialogWindow;
    @Autowired
    private Notifications notifications;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;


    /**
     * Sets the inventory item for the current fragment and initializes necessary
     * components, updates data contexts, and manages UI state based on the supplied
     * inventory item.
     *
     * @param inventoryItem the object to be set. It must not be null.
     */
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
        vulnerabilitytabfragment.setSoftwareComponent(this.softwareComponent);

        // Handle disabling buttons to prevent errors
        parentButton.setEnabled(this.inventoryItem.getParent() != null);
        softwareComponentButton.setEnabled(softwareComponent != null);
        editLicense.setEnabled(softwareComponent != null);
    }

    /**
     * Sets the host view for the current fragment and updates references
     * in associated tab fragments.
     *
     * @param hostView the view to be set as the host view. It must not be null.
     */
    public void setHostView(View<?> hostView) {
        this.hostView = hostView;

        vulnerabilitytabfragment.setHostView(hostView);
        filesTabFragment.setHostView(hostView);
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    /**
     * Handles the save action triggered by the user.
     * This method checks if there are unsaved changes in the data context.
     * If changes are present, it saves them and updates relevant views.
     * If no changes are detected, it notifies the user.
     *
     * @param event the event object captured when the saveAction is triggered
     */
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

    /**
     * Handles the action of adding a license. When triggered, this method opens a dialog
     * allowing the user to add a license to the software component. After the dialog
     * is closed, the licenses associated with the software component are updated.
     *
     * @param event the event object triggered by the click action on the dropdown button item
     */
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

    /**
     * Toggles the deletion mode for licenses and updates the UI components accordingly.
     * When triggered, this method changes the selection mode of the licenses data grid
     * to either single or multiple based on the current state. It also toggles the
     * visibility of the "Remove License" button.
     *
     **/
    @Subscribe(id = "editLicense.removeLicense")
    public void removeLicenses(DropdownButtonItem.ClickEvent event) {
        deleteMode = !deleteMode;

        licensesDataGrid.setSelectionMode(
                deleteMode ? DataGrid.SelectionMode.MULTI : DataGrid.SelectionMode.SINGLE
        );
        removeLicenseButton.setVisible(deleteMode);
    }

    /**
     * Handles the removal of selected licenses from the software component associated with the current view.
     * Updates the software component repository, notifies the user upon successful removal, and adjusts the
     * UI state including the data grid selection mode and button visibility.
     *
     * @param event the click event triggered by the user interacting with the "Remove License" button
     */
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

    /**
     * Handles the creation and addition of a new license via a dialog window. This method
     * opens a dialog to input license details for the associated software component. After
     * the dialog is closed, it updates the licenses linked to the software component.
     *
     * @param event the click event triggered by the user interacting with the dropdown button item
     */
    @Subscribe(id = "editLicense.createLicense")
    public void createAndAddLicense(DropdownButtonItem.ClickEvent event) {
        if (softwareComponent != null) {
            DialogWindow<CreateLicenseDialog> window = dialogWindow.view(hostView, CreateLicenseDialog.class).build();
            window.getView().setAvailableContent(softwareComponent);
            window.open();
            window.addAfterCloseListener(close ->
                    updateLicenses(softwareComponent, licenseDc));
        }

    }

    /**
     * Handles the double-click event on an item in the licenses data grid.
     * Opens a dialog window to display and edit details of the selected license.
     *
     * @param event the event triggered when an item in the licenses data grid is double-clicked.
     *              Contains the license item that was clicked.
     */
    @Subscribe("licensesDataGrid")
    public void onLicensesDataGridItemDoubleClick(final ItemDoubleClickEvent<License> event) {
        DialogWindow<LicenseDetailView> window = dialogWindow.view(hostView, LicenseDetailView.class).build();
        window.getView().setEntityToEdit(event.getItem());
        window.open();
    }

    /**
     * Handles the double-click event on an item in the copyrights data grid.
     * Opens a dialog window to display and edit details of the selected copyright.
     *
     * @param event the event triggered when an item in the copyrights data grid is double-clicked.
     *              Contains the copyright item that was clicked.
     */
    @Subscribe("copyrightsDataGrid")
    public void onCopyrightDataGridItemDoubleClick(final ItemDoubleClickEvent<Copyright> event) {
        DialogWindow<CopyrightDetailView> window = dialogWindow.view(hostView, CopyrightDetailView.class).build();
        window.getView().setEntityToEdit(event.getItem());
        window.setSizeFull();
        window.open();
    }

    /**
     * Handles the addition of a copyright to an inventory item.
     * When this method is triggered, it opens a dialog window to
     * allow the user to add copyright information. After the dialog
     * is closed, it updates the associated copyright entries
     * for the inventory item.
     *
     * @param event the click event triggered by user interaction
     *              with the dropdown button item that initiates
     *              the process of adding a copyright
     */
    @Subscribe(id = "editCopyright.addCopyright")
    public void addCopyright(DropdownButtonItem.ClickEvent event) {

        DialogWindow<AddCopyrightDialog> window = dialogWindow.view(hostView, AddCopyrightDialog.class).build();
        window.getView().setAvailableContent(inventoryItem);
        window.open();
        window.addAfterCloseListener(close -> updateCopyrights(inventoryItem, copyrightDc));
    }

    /**
     * Toggles the deletion mode for copyrights and updates the UI components accordingly.
     * This method switches the selection mode of the copyrights data grid to either
     * single or multiple depending on the current state. It also manages the visibility
     * of the "Remove Copyright" button.
     *
     * @param event the click event triggered by the user interacting with the dropdown button item
     */
    @Subscribe(id = "editCopyright.removeCopyright")
    public void removeCopyrights(DropdownButtonItem.ClickEvent event) {
        deleteMode = !deleteMode;

        copyrightsDataGrid.setSelectionMode(
                deleteMode ? DataGrid.SelectionMode.MULTI : DataGrid.SelectionMode.SINGLE
        );
        removeCopyrightButton.setVisible(deleteMode);
    }

    /**
     * Handles the creation and addition of a new copyright entry via a dialog window.
     * When triggered, this method opens a dialog for the user to input copyright details
     * associated with the current inventory item. After the dialog is closed, the method
     * updates the list of copyrights linked to the inventory item.
     *
     * @param event the click event triggered by the user interacting with the dropdown button item
     */
    @Subscribe(id = "editCopyright.createCopyright")
    public void createAndAddCopyright(DropdownButtonItem.ClickEvent event) {
        if (inventoryItem != null) {
            DialogWindow<CreateCopyrightDialog> window = dialogWindow.view(hostView, CreateCopyrightDialog.class).build();
            window.getView().setAvailableContent(inventoryItem);
            window.open();
            window.addAfterCloseListener(close ->
                    updateCopyrights(inventoryItem, copyrightDc));
        }

    }

    /**
     * Handles the removal of selected copyrights from the inventory item associated with the current view.
     * If any copyrights are selected, they are removed from the inventory item, and the changes
     * are persisted using the inventory item repository. The method also updates the UI components,
     * notifies the user upon successful removal, and resets the data grid selection mode and button visibility.
     *
     * @param event the ClickEvent triggered by the user interacting with the "Remove Copyright" button.
     *              Contains details about the button click action.
     */
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

    /**
     * Handles the click event on the "Parent" button to display details of
     * the parent inventory item. If the current inventory item has a parent,
     * this method opens a dialog window to view its details.
     *
     * @param event the event triggered by the user clicking the "Parent" button.
     *              Contains information about the click action.
     */
    @Subscribe(id = "parentButton")
    public void showParentDetails(ClickEvent<Button> event) {

        InventoryItem parent = inventoryItem.getParent();
        if (parent != null) {
            dialogWindow.view(hostView, InventoryItemDetailView.class)
                    .withViewConfigurer(i -> i.setEntityToEdit(parent)).open().setSizeFull();
        }
    }

    /**
     * Displays the details of a software component in a separate dialog window.
     * Opens a view allowing the user to view and edit properties of the selected software component.
     *
     * @param event the ClickEvent triggered by the user interacting with the associated button.
     *              Contains details about the button click action.
     */
    @Subscribe(id = "softwareComponentButton")
    public void showSoftwareComponentDetails(ClickEvent<Button> event) {
        dialogWindow.view(hostView, SoftwareComponentDetailView.class)
                .withViewConfigurer(scView -> scView.setEntityToEdit(softwareComponent))
                .open().setSizeFull();
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

}
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

package eu.occtet.bocfrontend.view.audit.fragment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.dao.*;
import eu.occtet.bocfrontend.entity.*;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.view.audit.AuditView;
import eu.occtet.bocfrontend.view.copyright.CopyrightDetailView;
import eu.occtet.bocfrontend.view.dialog.*;
import eu.occtet.bocfrontend.view.inventoryitem.InventoryItemDetailView;
import eu.occtet.bocfrontend.view.license.LicenseDetailView;
import eu.occtet.bocfrontend.view.softwareComponent.SoftwareComponentDetailView;
import io.jmix.core.*;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.*;
import io.jmix.flowui.view.*;
import io.nats.client.JetStreamApiException;
import jakarta.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static io.jmix.flowui.fragment.FragmentUtils.findComponent;

@FragmentDescriptor("InventoryItemTabFragment.xml")
public class InventoryItemTabFragment extends Fragment<JmixTabSheet> {

    private static final Logger log = LogManager.getLogger(InventoryItemTabFragment.class);
    @Autowired
    protected UiComponents uiComponents;

    private View<?> hostView;
    private InventoryItem inventoryItem;
    private SoftwareComponent softwareComponent;
    private boolean deleteLicenseMode = false;
    private boolean deleteCopyrightMode = false;

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
    private JmixButton auditReuseButton;
    @ViewComponent
    private TextField softwareComponentReuseField;
    @ViewComponent
    private TextField parentReuseID;
    @ViewComponent
    private JmixButton parentReuseButton;
    @Autowired
    private Messages messages;
    @ViewComponent
    private InstanceLoader<InventoryItem> inventoryItemDlReuse;
    @ViewComponent
    private JmixComboBox<InventoryItem> parentField;
    @ViewComponent
    private CollectionLoader<License> licensesDl;
    @ViewComponent
    private CollectionLoader<Copyright> copyrightDl;
    @ViewComponent
    private JmixComboBox<Linking> linkingComboBox;
    // Tab fragments
    @ViewComponent
    private FilesTabFragment filesTabFragment;
    @ViewComponent
    private Vulnerabilitytabfragment vulnerabilitytabfragment;
    @ViewComponent
    private TextField inventoryProjectReuseField;
    @ViewComponent
    private FilesTabFragment filesReuseTabFragment;
    @ViewComponent
    private Tab reuseTab;
    @ViewComponent
    private JmixButton downloadBtn;
    @ViewComponent
    private TextField downloadUrlTextField;
    @ViewComponent
    private JmixButton saveButton;


    @Autowired
    private DialogWindows dialogWindow;
    @Autowired
    private Notifications notifications;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private SoftwareComponentRepository softwareComponentRepository;
    @ViewComponent
    private VerticalLayout inventoryNameField;
    @Autowired
    private SuggestionRepository suggestionRepository;

    private List<String> suggestions;
    private AutocompleteField autocompleteAuditNotes;
    private AutocompleteField autocompleteInventoryName;

    @ViewComponent
    private VerticalLayout auditNotesText;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private NatsService natsService;
    @Autowired
    private EntityStates entityStates;

    public void activateAutocomplete() {
        log.info("on before show");
        loadSuggestions("auditNotes");
        autocompleteAuditNotes = new AutocompleteField( messages.getMessage(getClass(), "auditNotes"));
        autocompleteAuditNotes.setOptions(suggestions);
        autocompleteAuditNotes.initializeField();
        if(this.inventoryItem.getExternalNotes()!=null)
            autocompleteAuditNotes.setValue(this.inventoryItem.getExternalNotes());
        auditNotesText.add(autocompleteAuditNotes);

        loadSuggestions("inventoryNames");
        autocompleteInventoryName= new AutocompleteField(messages.getMessage(getClass(), "inventoryName"));
        autocompleteInventoryName.setOptions(suggestions);
        autocompleteInventoryName.initializeField();
        autocompleteInventoryName.setValue(inventoryItem.getInventoryName());
        autocompleteInventoryName.setClassName("autocompleteInventoryStyle");
        inventoryNameField.add(autocompleteInventoryName);

    }

    /**
     * loads the suggestions strings from db
     * for autocomplete fields
     */
    private void loadSuggestions(String context){
        suggestions= suggestionRepository.findByContext(context).stream().map(Suggestion::getSentence)
                .filter(Objects::nonNull).collect(Collectors.toList());

    }


    /**
     * Sets the inventory item for the current fragment and initializes necessary
     * components, updates data contexts, and manages UI state based on the supplied
     * inventory item.
     *
     * @param inventoryItem the object to be set. It must not be null.
     */
    public void setInventoryItem(@Nonnull InventoryItem inventoryItem) {
        SoftwareComponent sc = inventoryItem.getSoftwareComponent();

        if (sc != null && !entityStates.isNew(sc) &&
                (!entityStates.isLoaded(sc, "licenses") || !entityStates.isLoaded(sc, "copyrights"))) {

            SoftwareComponent fullyLoadedSc = dataManager.load(SoftwareComponent.class)
                    .id(sc.getId())
                    .fetchPlan(f -> f.addAll("licenses", "copyrights", "vulnerabilityLinks", "vulnerabilityLinks.vulnerability"))
                    .one();

            // Swap lazy proxy for the fully loaded one
            inventoryItem.setSoftwareComponent(fullyLoadedSc);
        }
        // track instances !important for saving
        this.inventoryItem = dataContext.merge(inventoryItem);
        this.softwareComponent = this.inventoryItem.getSoftwareComponent();

        if (this.softwareComponent != null) {
            softwareComponentDc.setItem(this.softwareComponent);
        }

        parentField.setItems(inventoryItemRepository.findAll());
        parentField.setItemLabelGenerator(InventoryItem::getInventoryName);
        if(inventoryItem.getParent() != null) {
            parentField.setValue(inventoryItem.getParent());
            log.debug("Inventory Item Parent: {}", inventoryItem.getParent().getInventoryName());
        }
        linkingComboBox.setItems(List.of(Linking.DYNAMIC,Linking.STATIC,Linking.NONE));
        linkingComboBox.setItemLabelGenerator(Linking::getId);
        linkingComboBox.setValue(Linking.fromId(this.inventoryItem.getLinking()));

        log.debug("Set Inventory Item: {} class: {}", this.inventoryItem.getInventoryName(), this.inventoryItem.getClass().getSimpleName());
        inventoryItemDc.setItem(this.inventoryItem);
        log.debug("1");
        updateCopyrightsFromInventory(this.inventoryItem);
        log.debug("2");
        updateLicensesFromInventoryItem(this.inventoryItem);
        log.debug("Updated copyrights and licenses for Inventory Item: {}", this.inventoryItem.getInventoryName());
        //Reuse of inventory
        visibleReuseItem(inventoryItem);

        filesTabFragment.setInventoryItemId(this.inventoryItem);
        vulnerabilitytabfragment.setInventoryItem(this.inventoryItem);
        log.debug("Set Inventory Item ID in Files Tab Fragment and Software Component in Vulnerability Tab Fragment.");
        // Handle disabling buttons to prevent errors
        parentButton.setEnabled(this.inventoryItem.getParent() != null);
        softwareComponentButton.setEnabled(softwareComponent != null);
        editLicense.setEnabled(softwareComponent != null);
        activateAutocomplete();
        log.debug("Inventory Item Tab Fragment setup complete.");
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
     * Bypasses the strict hasChanges() check to guarantee nested entities (like newly added
     * licenses or copyrights) are properly evaluated and saved by the DataContext.
     *
     * @param event the event object captured when the saveAction is triggered
     */
    @Subscribe("saveAction")
    public void onSaveAction(ActionPerformedEvent event) {
        this.inventoryItem.setExternalNotes(autocompleteAuditNotes.getValue());
        this.inventoryItem.setInventoryName(autocompleteInventoryName.getValue());

        Linking selectedLinking = linkingComboBox.getValue();
        this.inventoryItem.setLinking(Objects.requireNonNullElse(selectedLinking, Linking.NONE).getId());

        SaveContext saveContext = new SaveContext().saving(this.inventoryItem);
        if (this.softwareComponent != null) {
            saveContext.saving(this.softwareComponent);
        }

        EntitySet savedEntities = dataManager.save(saveContext);

        for (Object savedEntity : savedEntities) {
            dataContext.merge(savedEntity);
        }

        if (!savedEntities.isEmpty()) {
            log.debug("Inventory Item {} had {} changes force-saved.", inventoryItem.getInventoryName(), savedEntities.size());

            notifications.create(messages.formatMessage(getClass(),
                            "inventory.save.success",
                            inventoryItem.getInventoryName(), savedEntities.size()))
                    .withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                    .withCloseable(true)
                    .show();

            if (hostView instanceof AuditView) {
                ((AuditView) hostView).refreshInventoryItemDc(inventoryItem.getProject());
            }

            setSaveButtonDirtyState(false);
        } else {
            log.debug("Inventory Item {} has no changes.", inventoryItem.getInventoryName());
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification"))
                    .withPosition(Notification.Position.BOTTOM_END)
                    .show();
        }
    }

    /**
     * Opens a dialog to select and append existing licenses to the associated software component.
     * This method stages the additions within the view's data context, deferring database
     * persistence until the main fragment is saved.
     *
     * @param event the click event triggered by selecting the add license dropdown item
     */
    @Subscribe(id = "editLicense.addLicense")
    public void addLicense(DropdownButtonItem.ClickEvent event) {
        if (softwareComponent != null) {
            DialogWindow<AddLicenseDialog> window = dialogWindow.view(hostView, AddLicenseDialog.class).build();
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
                        dataContext.merge(softwareComponent);
                        setSaveButtonDirtyState(true);
                        infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.LicenseAdd"));
                    }
                }
            });
        }
    }

    /**
     * Initializes a new license entry and links it to the active software component.
     * The new relationship is managed strictly in memory using the local data context.
     *
     * @param event the click event triggered by selecting the create license dropdown item
     */
    @Subscribe(id = "editLicense.createLicense")
    public void createAndAddLicense(DropdownButtonItem.ClickEvent event) {
        if (softwareComponent != null) {
            DialogWindow<CreateLicenseDialog> window = dialogWindow.view(hostView, CreateLicenseDialog.class).build();
            window.getView().setAvailableContent(softwareComponent);
            window.open();

            window.addAfterCloseListener(close -> {
                if (close.closedWith(StandardOutcome.SAVE)) {
                    License newLicense = window.getView().getCreatedLicense();
                    if (newLicense != null) {
                        License trackedLicense = dataContext.merge(newLicense);
                        softwareComponent.getLicenses().add(trackedLicense);
                        licenseDc.getMutableItems().add(trackedLicense);
                        infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.LicenseCreate"));
                    }
                }
            });
        }
    }

    /**
     * Toggles the UI state to allow multiple selection for license removal.
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
     * Severs the link between selected licenses and the software component in memory.
     * Modifies the underlying entity collection without immediately committing changes to the database.
     *
     * @param event the click event triggered by the remove license button
     */
    @Subscribe(id = "removeLicenseButton")
    public void removeLicenses(ClickEvent<JmixButton> event) {
        Set<License> selectedLicenses = licensesDataGrid.getSelectedItems();

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
        if(softwareComponent != null){
            DialogWindow<AddCopyrightDialog> window = dialogWindow.view(hostView, AddCopyrightDialog.class).build();
            window.getView().setAvailableContent(softwareComponent);
            window.open();

            window.addAfterCloseListener(close -> {
                if(close.closedWith(StandardOutcome.SAVE)){
                    List<Copyright> selectedCopyrights = window.getView().getSelectedCopyrights();

                    if (selectedCopyrights != null && !selectedCopyrights.isEmpty()) {
                        for (Copyright copyright : selectedCopyrights) {
                            Copyright trackedCopyright = dataContext.merge(copyright);

                            if (!softwareComponent.getCopyrights().contains(trackedCopyright)) {
                                softwareComponent.getCopyrights().add(trackedCopyright);
                                copyrightDc.getMutableItems().add(trackedCopyright);
                            }
                        }
                        dataContext.merge(softwareComponent);
                        infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.CopyrightAdd"));
                    }
                }
            });
        }
    }

    @Subscribe(id = "editCopyright.createCopyright")
    public void createAndAddCopyright(DropdownButtonItem.ClickEvent event) {
        if (inventoryItem != null && softwareComponent != null) {
            DialogWindow<CreateCopyrightDialog> window = dialogWindow.view(hostView, CreateCopyrightDialog.class).build();
            window.getView().setAvailableContent(inventoryItem);
            window.open();

            window.addAfterCloseListener(close -> {
                if (close.closedWith(StandardOutcome.SAVE)) {
                    Copyright newCopyright = window.getView().getCreatedCopyright();

                    if (newCopyright != null) {
                        Copyright trackedCopyright = dataContext.merge(newCopyright);
                        if (softwareComponent.getCopyrights() == null) {
                            softwareComponent.setCopyrights(new ArrayList<>());
                        }
                        if (!softwareComponent.getCopyrights().contains(trackedCopyright)) {
                            softwareComponent.getCopyrights().add(trackedCopyright);
                        }
                        copyrightDc.getMutableItems().add(trackedCopyright);
                        setSaveButtonDirtyState(true);
                        infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.CopyrightCreate"));
                    }
                }
            });
        }
    }

    @Subscribe(id = "editCopyright.removeCopyright")
    public void toggleRemoveCopyrightMode(DropdownButtonItem.ClickEvent event) {
        deleteCopyrightMode = !deleteCopyrightMode;

        copyrightsDataGrid.setSelectionMode(
                deleteCopyrightMode ? DataGrid.SelectionMode.MULTI : DataGrid.SelectionMode.SINGLE
        );
        removeCopyrightButton.setVisible(deleteCopyrightMode);
    }

    @Subscribe(id = "removeCopyrightButton")
    public void removeCopyrights(ClickEvent<JmixButton> event) {
        Set<Copyright> selectedCopyrights = copyrightsDataGrid.getSelectedItems();

        if (!selectedCopyrights.isEmpty() && softwareComponent != null) {
            for (Copyright copyright : selectedCopyrights) {
                softwareComponent.getCopyrights().remove(copyright);
                copyrightDc.getMutableItems().remove(copyright);
            }
            infoMessage(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.notification.CopyrightRemove"));
        }

        deleteCopyrightMode = false;
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
        log.debug("parent {}", inventoryItem.getParent());
        if (parentField.getValue() != null) {
            DialogWindow<InventoryItemDetailView> dialog = dialogWindow.detail(hostView, InventoryItem.class)
                    .withViewClass(InventoryItemDetailView.class)
                    .editEntity(parentField.getValue()).build();
            dialog.setHeight("90%");
            dialog.setWidth("90%");
            dialog.open();
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

    private void updateCopyrightsFromInventory(InventoryItem inventoryItem){
        List<Copyright> copyrights = copyrightRepository.findByInventoryItem(inventoryItem);
        copyrightDl.setParameter("copyrightsList",copyrights);
        copyrightDl.load();
    }

    private void updateLicensesFromInventoryItem(InventoryItem item){
        List<License> licenses = licenseRepository.findByInventoryItem(item);
        licensesDl.setParameter("licensesList",licenses);
        licensesDl.load();
    }

    @Subscribe(id = "auditReuseButton")
    public void addAuditReuseToInventory(final ClickEvent<Button> event) {
        InventoryItem ReuseItem = findReuseOfInventory(inventoryItem);
        if (ReuseItem != null) {
            String notes = this.inventoryItem.getExternalNotes();
            if (notes == null) {
                this.inventoryItem.setExternalNotes(getTimeStampSeperator() + "<br>" + ReuseItem.getExternalNotes());
            } else {
                this.inventoryItem.setExternalNotes(notes + "<br>" + getTimeStampSeperator() + "<br>" + ReuseItem.getExternalNotes());
            }
            inventoryItemRepository.save(this.inventoryItem);
            autocompleteAuditNotes.setValue(this.inventoryItem.getExternalNotes());
        }
    }

    private InventoryItem findReuseOfInventory(InventoryItem inventoryItem) {

        List<InventoryItem> reuseItems = new ArrayList<>();
        List<Project> projects = projectRepository.findByProjectName(inventoryItem.getProject().getProjectName());
        if(projects.size()>1){
            LocalDateTime time = projects.stream().min(Comparator.comparing(Project::getCreatedAt)).get().getCreatedAt();
            if(!time.equals(inventoryItem.getProject().getCreatedAt())){
                Project beforeProject = projects.stream().filter(p ->
                                p.getCreatedAt().isBefore(inventoryItem.getProject().getCreatedAt()))
                        .max(Comparator.comparing(Project::getCreatedAt)).get();
                reuseItems = inventoryItemRepository.findByBeforeProjectAndInventoryNameAndCurated(beforeProject,
                        inventoryItem.getInventoryName(),true);
            }
        }
        if(!reuseItems.isEmpty()){
            return reuseItems.getFirst();
        }else{
            return null;
        }
    }

    private String getTimeStampSeperator() {
        return "------------- " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + "------------";
    }

    private void infoMessage(String message){
        notifications.create(message)
                .withPosition(Notification.Position.TOP_CENTER)
                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                .withDuration(3000)
                .show();
    }

    private void visibleReuseItem(InventoryItem inventoryItem){
        InventoryItem item = findReuseOfInventory(inventoryItem);
        if(item != null){
            inventoryItemDlReuse.setEntityId(item.getId());
            inventoryItemDlReuse.load();
            showReusefromInventoryItem(item);
            reuseTab.setVisible(true);
        }
    }

    private void showReusefromInventoryItem(InventoryItem item) {

        if (item.getExternalNotes() != null && !item.getExternalNotes().isEmpty()) {
            auditReuseButton.setEnabled(true);
        } else {
            auditReuseButton.setEnabled(false);
        }
        if (item.getSoftwareComponent() != null) {
            softwareComponentReuseField.setValue(item.getSoftwareComponent().getName());
        }
        if (item.getParent() != null) {
            parentReuseID.setValue(item.getParent().getInventoryName());
            parentReuseButton.setEnabled(true);
        }
        filesReuseTabFragment.setInventoryItemId(item);
        filesReuseTabFragment.setHostView(hostView);
        inventoryProjectReuseField.setValue(item.getProject().getProjectName()+" - "+item.getProject().getVersion());
    }

    /**
     * Handles the download action triggered by clicking a button.
     * Validates the provided URL, saves the current data context if valid,
     * and prompts the user with an input dialog to configure and start the download task.
     */
    @Subscribe(id = "downloadBtn")
    private void download(ClickEvent<JmixButton> event) {
        String url = downloadUrlTextField.getValue();

        if (url == null || url.isBlank()) {
            notifications.create(
                            messages.getMessage(
                                    "eu.occtet.bocfrontend.view/inventoryTabFragment.tabSheet.softwareComponent.url.empty.message"
                            )
                    ).withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .show();
            return;
        }
        try {
            new URL(url).toURI();
            dataContext.save();
        } catch (MalformedURLException e) {
            notifications.create(
                            messages.formatMessage(
                                    "eu.occtet.bocfrontend.view",
                                    "inventoryTabFragment.tabSheet.softwareComponent.url.malformed.message",
                                    url
                            )
                    ).withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .show();
            return;
        } catch (URISyntaxException e) {
            notifications.create(
                            messages.formatMessage(
                                    "eu.occtet.bocfrontend.view",
                                    "inventoryTabFragment.tabSheet.softwareComponent.url.syntax.message",
                                    url
                            )
                    ).withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .show();
            return;
        }
        dialogs.createInputDialog(hostView)
                .withHeader(messages.formatMessage("eu.occtet.bocfrontend.view",
                        "inventoryTabFragment.tabSheet.softwareComponent.url.start.download.task",
                        url))
                .withLabelsPosition(Dialogs.InputDialogBuilder.LabelsPosition.TOP)
                .withParameter(InputParameter.booleanParameter("isMainPkg") // important
                        .withDefaultValue(false)
                        .withLabel(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.tabSheet.softwareComponent.url.start.download.task.isMainPkg"))
                        .withRequired(false)
                ).withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        try {
                            natsService.sendToDownload(inventoryItem.getProject().getId(), inventoryItem.getId(), closeEvent.getValue("isMainPkg"));

                            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.tabSheet.softwareComponent.url.start.download.request.sent.title"),
                                            messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.tabSheet.softwareComponent.url.start.download.request.sent.description"))
                                    .withType(Notifications.Type.SUCCESS)
                                    .withPosition(Notification.Position.BOTTOM_END).show();

                        } catch (IOException | JetStreamApiException e) {
                            log.error("NATS Connection failure for project {}: {}", inventoryItem.getProject().getId(), e.getMessage(), e);

                            notifications.create(
                                            messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.tabSheet.softwareComponent.url.start.download.error.title"),
                                            messages.getMessage("eu.occtet.bocfrontend.view/inventoryTabFragment.tabSheet.softwareComponent.url.start.download.error.network.msg")
                                    ).withType(Notifications.Type.ERROR)
                                    .show();
                        }
                    }
                }).withDraggable(true).open();
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    public void onDataContextChange(final DataContext.ChangeEvent event) {
        setSaveButtonDirtyState(dataContext.hasChanges());
    }

    private void setSaveButtonDirtyState(boolean hasChanges) {
        // Framework bug workaround: Manually fetch the button if <suffix> broke the @ViewComponent injection
        if (this.saveButton == null) {
            this.saveButton = (JmixButton) findComponent(this ,"saveButton").orElse(null);
        }

        if (this.saveButton == null) {
            log.warn("Could not locate saveButton in the UI. Skipping color update.");
            return;
        }

        if (hasChanges) {
            saveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            saveButton.setIcon(VaadinIcon.CHECK_CIRCLE.create());
        } else {
            saveButton.removeThemeVariants(ButtonVariant.LUMO_SUCCESS);
            saveButton.setIcon(null);
        }
    }

}
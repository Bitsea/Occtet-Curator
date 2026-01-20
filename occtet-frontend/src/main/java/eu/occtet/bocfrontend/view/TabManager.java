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

package eu.occtet.bocfrontend.view;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.tabs.Tab;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.model.FileResult;
import eu.occtet.bocfrontend.service.FileContentService;
import eu.occtet.bocfrontend.view.audit.AuditView;
import eu.occtet.bocfrontend.view.audit.fragment.InventoryItemTabFragment;
import eu.occtet.bocfrontend.view.audit.fragment.CodeViewerFragment;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.StandardView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages the behavior and operations related to creating, opening, focusing,
 * and closing tabs of different types within a user interface.
 * Tabs can be associated with either `InventoryItem` instances or `File` objects.
 */
public class TabManager {

    private static final Logger log = LogManager.getLogger(TabManager.class);

    // Tab tracking
    private final Map<InventoryItem, Tab> openInventoryTabs = new LinkedHashMap<>();
    private final Map<File, Tab> openFileTabs = new LinkedHashMap<>();

    private final StandardView hostView;
    private final DataContext dataContext;
    private final Dialogs dialogs;
    private final Fragments fragments;
    private final JmixTabSheet mainTabSheet, inventoryItemTabSheet, filesTabSheet;
    private final Tab inventoryItemTabSection, filesTabSection;
    private final FileContentService fileContentService;
    private final Notifications notifications;
    private final Consumer<Serializable> onTabChangeCallback;

    private TabManager(Builder builder) {
        this.fileContentService = Objects.requireNonNull(builder.fileContentService, "fileContentService is required");
        this.notifications = Objects.requireNonNull(builder.notifications, "notifications is required");
        this.fragments = Objects.requireNonNull(builder.fragments, "fragments is required");
        this.dialogs = Objects.requireNonNull(builder.dialogs, "dialogs is required");
        this.hostView = Objects.requireNonNull(builder.hostView, "hostView is required");
        this.dataContext = Objects.requireNonNull(builder.dataContext, "dataContext is required");
        this.mainTabSheet = Objects.requireNonNull(builder.mainTabSheet, "mainTabSheet is required");
        this.inventoryItemTabSection = Objects.requireNonNull(builder.inventoryItemTabSection, "inventoryItemTabSection is required");
        this.inventoryItemTabSheet = Objects.requireNonNull(builder.inventoryItemTabSheet, "inventoryItemTabSheet is required");
        this.filesTabSection = Objects.requireNonNull(builder.filesTabSection, "filesTabSection is required");
        this.filesTabSheet = Objects.requireNonNull(builder.filesTabSheet, "filesTabSheet is required");
        this.onTabChangeCallback = Objects.requireNonNull(builder.onTabChangeCallback, "onTabChangeCallback is required");
    }

    /**
     * Opens or focuses a tab for the given InventoryItem.
     */
    public void openInventoryItemTab(InventoryItem item, boolean selectTab) {
        if (item == null) {
            log.warn("Attempted to open tab for null InventoryItem");
            return;
        }

        if (selectTab) {
            mainTabSheet.setSelectedTab(inventoryItemTabSection);
            if (!inventoryItemTabSection.getElement().isVisible())
                inventoryItemTabSection.getElement().setVisible(true);
        }

        openTabAction(
                openInventoryTabs,
                inventoryItemTabSheet,
                item,
                item.getInventoryName(),
                () -> createInventoryItemFragment(item),
                this::handleClosingInventoryItemTab,
                item.getId(), selectTab
        );
    }

    /**
     * Opens or focuses a tab for the given File object.
     */
    public void openFileTab(File file, boolean selectTab) {
        if (file == null || Boolean.TRUE.equals(file.getIsDirectory())) {
            log.debug("Skipping tab creation for null or directory node");
            return;
        }

        if (selectTab) {
            filesTabSection.setVisible(true);
            mainTabSheet.setSelectedTab(filesTabSection);
        }

        openTabAction(
                openFileTabs,
                filesTabSheet,
                file,
                file.getFileName(),
                () -> createCodeViewerFragment(file),
                tab -> {
                    closeFileTab(file);
                    if (filesTabSheet.getTabCount() == 0) {
                        filesTabSection.setVisible(false);
                    }
                },
                file.getId(),
                selectTab
        );
    }


    /**
     * Generic method to open or focus a tab.
     */
    private <T> void openTabAction(
            Map<T, Tab> openTabsMap,
            JmixTabSheet tabSheet,
            T key,
            String tabTitle,
            Supplier<Component> contentSupplier,
            Consumer<Tab> onClose,
            Serializable identifier,
            boolean selectTab
    ) {

        // Focus existing tab if already open
        if (openTabsMap.containsKey(key)) {
            Tab existingTab = openTabsMap.get(key);
            tabSheet.setSelectedTab(existingTab);
            onTabChangeCallback.accept(identifier);
            log.debug("Focused existing tab: {}", tabTitle);
            return;
        }

        // Create new tab
        Component content = contentSupplier.get();
        Tab tab = tabSheet.add(tabTitle, content);

        // Add close button
        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.getElement().setAttribute("aria-label", "Close tab");
        closeButton.addClickListener(click -> {
            if (onClose != null) {
                onClose.accept(tab);
            }
        });
        tab.addComponentAtIndex(1, closeButton);

        openTabsMap.put(key, tab);

        if (selectTab) {
            tabSheet.setSelectedTab(tab);
            onTabChangeCallback.accept(identifier);
        }

        log.debug("Opened new tab (selected={}): {}", selectTab, tabTitle);
    }


    private Component createInventoryItemFragment(InventoryItem item) {
        InventoryItemTabFragment fragment = fragments.create(hostView, InventoryItemTabFragment.class);
        fragment.setHostView((AuditView) hostView);
        log.debug("Opening inventory item: {}", item.getInventoryName());
        fragment.setInventoryItem(item);
        return fragment;
    }

    private Component createCodeViewerFragment(File file) {
        CodeViewerFragment fragment = fragments.create(hostView, CodeViewerFragment.class);

        FileResult result = fileContentService.getFileContent(file.getAbsolutePath());
        log.debug("Opening file: {}", file.getAbsolutePath());
        switch (result) {
            case FileResult.Success(String content, String fileName) ->
                    fragment.setCodeEditorContent(content, fileName);
            case FileResult.Failure(String errorMessage) -> {
                log.warn("Failed to load file content: {}", errorMessage);
                notifications.create(errorMessage)
                        .withPosition(Notification.Position.BOTTOM_END)
                        .withThemeVariant(NotificationVariant.LUMO_ERROR)
                        .show();
            }
        }

        return fragment;
    }

    /**
     * Handles the closing of an InventoryItem tab with unsaved changes check.
     */
    private void handleClosingInventoryItemTab(Tab tab) {
        openInventoryTabs.entrySet().stream()
                .filter(entry -> entry.getValue().equals(tab))
                .findFirst()
                .ifPresent(entry -> {
                    InventoryItem item = entry.getKey();
                    InventoryItemTabFragment fragment =
                            (InventoryItemTabFragment) inventoryItemTabSheet.getComponent(tab);

                    if (fragment != null && dataContext.isModified(fragment.getInventoryItem())) {
                        showUnsavedChangesDialog(item);
                    } else {
                        closeInventoryItemTab(item);
                    }
                });
    }

    private void showUnsavedChangesDialog(InventoryItem item) {
        dialogs.createOptionDialog()
                .withHeader("Unsaved Changes")
                .withText("Do you want to discard your changes to \"" + item.getInventoryName() + "\"?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES)
                                .withText("Discard")
                                .withHandler(e -> closeInventoryItemTab(item)),
                        new DialogAction(DialogAction.Type.NO)
                                .withText("Cancel")
                ).open();
    }

    /**
     * Closes the specified InventoryItem tab.
     */
    private void closeInventoryItemTab(InventoryItem item) {
        Tab tab = openInventoryTabs.remove(item);
        if (tab != null) {
            inventoryItemTabSheet.remove(tab);
            log.debug("Closed inventory item tab: {}", item.getInventoryName());
        }
        onTabChangeCallback.accept(getActiveTabIdentifier());
    }

    /**
     * Closes the specified FileTreeNode tab.
     */
    private void closeFileTab(File file) {
        Tab tab = openFileTabs.remove(file);
        if (tab != null) {
            filesTabSheet.remove(tab);
            log.debug("Closed file tab: {}", file.getFileName());
        }
        onTabChangeCallback.accept(getActiveTabIdentifier());
    }

    /**
     * Closes all currently open tabs.
     */
    public void closeAllTabs() {
        // Create copies to avoid ConcurrentModificationException
        List<InventoryItem> inventoryItems = new ArrayList<>(openInventoryTabs.keySet());
        List<File> fileNodes = new ArrayList<>(openFileTabs.keySet());

        inventoryItems.forEach(this::closeInventoryItemTab);
        fileNodes.forEach(this::closeFileTab);

        log.debug("Closed all tabs");
    }

    /**
     * Checks if any tabs are currently open.
     */
    public boolean hasOpenTabs() {
        return !openInventoryTabs.isEmpty() || !openFileTabs.isEmpty();
    }

    /**
     * Selects a tab based on its identifier.
     */
    public void selectTab(Serializable identifier) {
        if (identifier == null) {
            log.debug("Cannot select tab: identifier is null");
            return;
        }

        if (identifier instanceof Long itemId) {
            boolean foundInInv = selectInventoryItemTab(itemId);
            if (!foundInInv)
                selectFileTab(itemId);
        } else {
            log.warn("Unknown identifier type: {}", identifier.getClass());
        }
    }

    private boolean selectInventoryItemTab(Long itemId) {
        Optional<Map.Entry<InventoryItem, Tab>> match = openInventoryTabs.entrySet().stream()
                .filter(inventoryItemTabEntry -> inventoryItemTabEntry.getKey().getId()==itemId)
                .findFirst();

        if (match.isPresent()) {
            mainTabSheet.setSelectedTab(inventoryItemTabSection);
            inventoryItemTabSheet.setSelectedTab(match.get().getValue());
            onTabChangeCallback.accept(itemId);
            log.debug("Selected inventory item tab: {}", itemId);
            return true;
        }
        return false;
    }

    private void selectFileTab(Long fileId) {
        openFileTabs.entrySet().stream()
                .filter(entry -> entry.getKey().getId()==fileId)
                .findFirst()
                .ifPresentOrElse(
                        entry -> {
                            mainTabSheet.setSelectedTab(filesTabSection);
                            filesTabSheet.setSelectedTab(entry.getValue());
                            onTabChangeCallback.accept(fileId);
                            log.debug("Selected file tab: {}", fileId);
                        },
                        () -> log.debug("No open tab found for file ID: {}", fileId)
                );
    }

    /**
     * Returns the list of open inventory item IDs.
     */
    public List<Long> getOpenInventoryItemIds() {
        return openInventoryTabs.keySet().stream()
                .map(InventoryItem::getId)
                .toList();
    }

    public List<Long> getOpenFileIds() {
        return openFileTabs.keySet().stream()
                .map(File::getId)
                .toList();
    }

    /**
     * Returns the identifier of the currently active tab.
     */
    public Serializable getActiveTabIdentifier() {
        Tab selectedMainTab = mainTabSheet.getSelectedTab();

        if (selectedMainTab == null) return null;

        if (selectedMainTab.equals(inventoryItemTabSection)) {
            return getActiveInventoryItemId();
        } else if (selectedMainTab.equals(filesTabSection)) {
            return getActiveFileId();
        }

        return null;
    }

    private Long getActiveInventoryItemId() {
        Tab selectedTab = inventoryItemTabSheet.getSelectedTab();
        if (selectedTab == null) {
            return null;
        }

        return openInventoryTabs.entrySet().stream()
                .filter(e -> e.getValue().equals(selectedTab))
                .map(e -> e.getKey().getId())
                .findFirst()
                .orElse(null);
    }

    private Long getActiveFileId() {
        Tab selectedTab = filesTabSheet.getSelectedTab();
        if (selectedTab == null) {
            return null;
        }

        return openFileTabs.entrySet().stream()
                .filter(e -> e.getValue().equals(selectedTab))
                .map(e -> e.getKey().getId()) // Return Long
                .findFirst()
                .orElse(null);
    }

    /**
     * Builder for creating a fully initialized TabManager.
     */
    public static class Builder {
        private final Fragments fragments;
        private final Dialogs dialogs;
        private final FileContentService fileContentService;
        private final Notifications notifications;

        private StandardView hostView;
        private DataContext dataContext;
        private JmixTabSheet mainTabSheet;
        private Tab inventoryItemTabSection;
        private JmixTabSheet inventoryItemTabSheet;
        private Tab filesTabSection;
        private JmixTabSheet filesTabSheet;
        private Consumer<Serializable> onTabChangeCallback;

        public Builder(Fragments fragments, Dialogs dialogs,
                       FileContentService fileContentService, Notifications notifications) {
            this.fragments = Objects.requireNonNull(fragments);
            this.dialogs = Objects.requireNonNull(dialogs);
            this.fileContentService = Objects.requireNonNull(fileContentService);
            this.notifications = Objects.requireNonNull(notifications);
        }

        public Builder withHostView(StandardView hostView) {
            this.hostView = hostView;
            return this;
        }

        public Builder withDataContext(DataContext dataContext) {
            this.dataContext = dataContext;
            return this;
        }

        public Builder withMainTabSheet(JmixTabSheet mainTabSheet) {
            this.mainTabSheet = mainTabSheet;
            return this;
        }

        public Builder withInventoryItemTabSection(Tab inventoryItemTabSection) {
            this.inventoryItemTabSection = inventoryItemTabSection;
            return this;
        }

        public Builder withInventoryItemTabSheet(JmixTabSheet inventoryItemTabSheet) {
            this.inventoryItemTabSheet = inventoryItemTabSheet;
            return this;
        }

        public Builder withFilesTabSection(Tab filesTabSection) {
            this.filesTabSection = filesTabSection;
            return this;
        }

        public Builder withFilesTabSheet(JmixTabSheet filesTabSheet) {
            this.filesTabSheet = filesTabSheet;
            return this;
        }

        public Builder withTabChangeCallback(Consumer<Serializable> onTabChangeCallback) {
            this.onTabChangeCallback = onTabChangeCallback;
            return this;
        }

        public TabManager build() {
            return new TabManager(this);
        }
    }
}
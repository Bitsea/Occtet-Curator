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

import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.factory.FileFactory;
import eu.occtet.bocfrontend.service.FileContentService;
import eu.occtet.bocfrontend.view.audit.AuditView;
import eu.occtet.bocfrontend.view.file.FileListView;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Messages;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonVariant;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * The FilesTabFragment class represents a fragment of a user interface for managing and interacting
 * with code locations in a vertical layout. It provides functionality to add, view, edit, delete.
 */
@FragmentDescriptor("files-tab-fragment.xml")
public class FilesTabFragment extends Fragment<VerticalLayout>{

    private static final Logger log = LogManager.getLogger(FilesTabFragment.class);

    private View<?> hostView;
    private InventoryItem inventoryItem;

    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private CollectionLoader<File> fileDl;
    @ViewComponent
    private DataGrid<File> fileDataGrid;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private FileFactory fileFactory;
    @Autowired
    private Notifications notifications;
    @Autowired
    private FileContentService fileContentService;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private Messages messages;
    @Autowired
    private DialogWindows dialogWindows;


    public void setHostView(View<?> hostView) {
        this.hostView = hostView;

        fileDataGrid.setDropMode(GridDropMode.ON_GRID);

        // Listeners for the files dragged from the file tree grid in `audit view`
        fileDataGrid.addDropListener(event -> {
            if (hostView instanceof AuditView) {
                AuditView auditView = (AuditView) hostView;

                List<File> draggedFiles = auditView.getCurrentDraggedFiles();

                if (draggedFiles != null && !draggedFiles.isEmpty()) {
                    handleDroppedFiles(draggedFiles);
                }
            }
        });
    }

    public void setInventoryItemId(InventoryItem inventoryItem) {
        this.inventoryItem = dataContext.merge(inventoryItem);
        fileDl.setParameter("inventoryItem", this.inventoryItem);
        fileDl.load();
    }

    @Subscribe(id = "filesEditButton.addFile")
    public void addFile(DropdownButtonItem.ClickEvent event) {
        dialogWindows.lookup(hostView, File.class)
                .withViewId("File.list")
                .withViewConfigurer(view -> {
                    if (view instanceof FileListView fileListView) {
                        fileListView.setProject(inventoryItem.getProject());
                    }
                })
                .withSelectHandler(selectedFiles -> {
                    List<File> filesToUpdate = new ArrayList<>();
                    for (File file : selectedFiles) {
                        File reloadedFile = dataManager.load(File.class)
                                .id(file.getId())
                                .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("inventoryItems"))
                                .one();
                        if (reloadedFile.getInventoryItems() == null){
                            reloadedFile.setInventoryItems(new HashSet<>());
                        }
                        reloadedFile.addInventoryItem(inventoryItem);
                        filesToUpdate.add(reloadedFile);
                    }
                    if (!filesToUpdate.isEmpty()) {
                        dataManager.saveAll(filesToUpdate);
                        notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.add.success"))
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                .show();
                        fileDl.load();
                    }
                })
                .open();
    }

    @Supply(to = "fileDataGrid.options", subject = "renderer")
    protected Renderer<File> fileRenderer() {
        return new ComponentRenderer<>(file -> {
            DropdownButton optionsButton = uiComponents.create(DropdownButton.class);
            optionsButton.setDropdownIndicatorVisible(false);
            optionsButton.setIcon(VaadinIcon.ELLIPSIS_DOTS_H.create());
            optionsButton.addThemeVariants(DropdownButtonVariant.LUMO_ICON, DropdownButtonVariant.LUMO_SMALL, DropdownButtonVariant.LUMO_TERTIARY);
            optionsButton.setWidth("40px");

            optionsButton.addItem("copy", messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.button.copy"), clickEvent -> copyFile(file));
            optionsButton.addItem("view", messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.button.view"), clickEvent -> viewFile(file));
            optionsButton.addItem("remove", messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.button.remove"), clickEvent -> removeFile(file));

            return optionsButton;
        });
    }
    private void viewFile(File file) {
        if (file != null){
            if (file.getPhysicalPath() != null){
                if (!file.getPhysicalPath().isBlank()) {
                    if (hostView instanceof AuditView auditView) {
                        auditView.getTabManager().openFileTab(file, true);
                    } else {
                        log.error("Host view is not AuditView, cannot open tab.");
                    }
                }
            } else {
                notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.message.error"))
                        .withPosition(Notification.Position.BOTTOM_END)
                        .withThemeVariant(NotificationVariant.LUMO_ERROR)
                        .withDuration(3000)
                        .show();
            }
        }
    }

    private void copyFile(File file) {
        UiComponentUtils.copyToClipboard(file.getProjectPath())
                .then(successResult -> notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.copy.notification")
                                + "\nFile: " + file.getProjectPath())
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                .show(),
                        errorResult -> notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.copy.notification.error"))
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR)
                                .show());
    }

    private void removeFile(File file) {
        String confirmationMessage = messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.remove.message") +
                "\nFile: " + file.getProjectPath() + "\nInventory item: " + inventoryItem;

        dialogs.createOptionDialog()
                .withHeader(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.remove.header"))
                .withText(confirmationMessage)
                .withActions(
                        new DialogAction(DialogAction.Type.YES).withHandler(event -> {
                            File reloadedFile = dataManager.load(File.class)
                                    .id(file.getId())
                                    .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("inventoryItems"))
                                    .one();
                            if (reloadedFile.getInventoryItems() != null) {
                                reloadedFile.getInventoryItems().removeIf(item -> item.getId().equals(inventoryItem.getId()));
                                dataManager.save(reloadedFile);
                            }
                            fileDl.load();

                            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.remove.success"))
                                    .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                    .show();
                        }),
                        new DialogAction(DialogAction.Type.NO)
                ).open();
    }

    /**
     * Recursively retrieves all successors of a given file, starting from the specified starting point.
    */
    private Set<File> getAllSuccessor(File startingPoint){
        Set<File> successors = new HashSet<>();

        if (!startingPoint.getIsDirectory()){
            successors.add(startingPoint);
            return successors;
        }

        Set<File> children = fileRepository.findFilesByParent(startingPoint);
        if (children == null){
            return successors;
        }
        for (File child : children){
            if (!child.getIsDirectory()){
                successors.add(child);
                continue;
            }
            successors.addAll(getAllSuccessor(child));
        }

        return successors;
    }

    private void handleDroppedFiles(List<File> draggedFiles) {
        Set<File> allSuccessors = new HashSet<>();
        for (File draggedFile : draggedFiles) {
            allSuccessors.addAll(getAllSuccessor(draggedFile));
        }

        List<File> filesToSave = new ArrayList<>();
        for (File file : allSuccessors) {
            if (file.getInventoryItems() == null) {
                file.setInventoryItems(new HashSet<>());
            }

            boolean alreadyLinked = file.getInventoryItems().stream().anyMatch(ii -> ii.getId().equals(inventoryItem.getId()));

            if (!alreadyLinked) {
                file.addInventoryItem(inventoryItem);
                filesToSave.add(file);
            }
        }

        if (!filesToSave.isEmpty()) {
            dataManager.saveAll(filesToSave);
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.files.add.success")).withThemeVariant(NotificationVariant.LUMO_SUCCESS).show();
            fileDl.load();
        }
    }
}
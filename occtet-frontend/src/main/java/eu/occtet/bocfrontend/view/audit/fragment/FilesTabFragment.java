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

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.factory.FileFactory;
import eu.occtet.bocfrontend.service.FileContentService;
import eu.occtet.bocfrontend.view.audit.AuditView;
import io.jmix.core.Messages;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.UiComponentUtils;
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

import java.util.List;
import java.util.Optional;

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
    private FileRepository codeLocationRepository;
    @Autowired
    private Messages messages;

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
    }

    public void setInventoryItemId(InventoryItem inventoryItem) {
        this.inventoryItem = dataContext.merge(inventoryItem);
        fileDl.setParameter("inventoryItem", this.inventoryItem);
        fileDl.load();
    }

    @Subscribe(id = "codeloctionsEditButton.addCodeLocation")
    public void addCodeLocation(DropdownButtonItem.ClickEvent event) {
        dialogs.createInputDialog(hostView)
                .withHeader(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.edit.header"))
                .withParameters(
                        InputParameter.stringParameter("filePath").withLabel(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.filePath")).withRequired(true)
                                .withRequiredMessage(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.edit.message.required")),
                        InputParameter.intParameter("from").withRequired(false).withLabel(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.from")).withDefaultValue(0),
                        InputParameter.intParameter("to").withRequired(false).withLabel(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.to")).withDefaultValue(0)
                ).withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        String filePath = closeEvent.getValue("filePath");
                        Integer from = closeEvent.getValue("from");
                        Integer to = closeEvent.getValue("to");
                        //TODO fix this!
                        //codeLocationFactory.create(inventoryItem, filePath, from, to);
                        //codeLocationDl.load();
                        notifications.create("Not yet implemented").show();
                    }
                }).open();
    }

    @Supply(to = "fileDataGrid.options", subject = "renderer")
    protected Renderer<File> codeLocationRenderer() {
        return new ComponentRenderer<>(file -> {
            DropdownButton optionsButton = uiComponents.create(DropdownButton.class);
            optionsButton.setDropdownIndicatorVisible(false);
            optionsButton.setIcon(VaadinIcon.ELLIPSIS_DOTS_H.create());
            optionsButton.addThemeVariants(DropdownButtonVariant.LUMO_ICON, DropdownButtonVariant.LUMO_SMALL, DropdownButtonVariant.LUMO_TERTIARY);
            optionsButton.setWidth("40px");

            optionsButton.addItem("copy", messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.button.copy"), clickEvent -> copyCodeLocation(file));
            optionsButton.addItem("view", messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.button.view"), clickEvent -> viewCodeLocation(file));
            optionsButton.addItem("edit", messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.button.edit"), clickEvent -> editCodeLocation(file));
            optionsButton.addItem("delete", messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.button.delete"), clickEvent -> deleteCodeLocation(file));

            return optionsButton;
        });
    }
    private void viewCodeLocation(File codeLocation) {
        Optional<File> fileOpt = fileContentService.findFileEntityForCodeLocation(codeLocation, inventoryItem);

        if (fileOpt.isPresent()) {
            File file = fileOpt.get();
            if (hostView instanceof AuditView auditView) {
                auditView.getTabManager().openFileTab(file, true);
            } else {
                log.error("Host view is not AuditView, cannot open tab.");
            }
        } else {
            notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.message.error"))
                    .withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
                    .withDuration(3000)
                    .show();
        }
    }

    private void copyCodeLocation(File file) {
        UiComponentUtils.copyToClipboard(file.getProjectPath())
                .then(successResult -> notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.copy.notification"))
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                .show(),
                        errorResult -> notifications.create(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.copy.notification.error"))
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR)
                                .show());
    }

    private void editCodeLocation(File file){
        dialogs.createInputDialog(hostView)
                .withHeader("Enter values")
                .withParameters(
                        InputParameter.stringParameter("filePath").withLabel(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.filePath")).withDefaultValue(file.getProjectPath())
                ).withActions(DialogActions.OK_CANCEL)
                .withCloseListener( closeEvent ->{
                    if (!closeEvent.closedWith(DialogOutcome.OK)){
                        return;
                    }
                    String filePath = closeEvent.getValue("filePath");
                    file.setProjectPath(filePath);
                    codeLocationRepository.save(file);
                    log.debug("Code location {} edited.", file.getId());
                }).open();
    }

    private void deleteCodeLocation(File file){
        List<Copyright> associatedCopyrights = copyrightRepository.findCopyrightsByFilesIn(List.of(file));
        String message = messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.delete.message")+"\n"+file.getProjectPath();
        if (!associatedCopyrights.isEmpty()) {
            message += "\n\n" + associatedCopyrights.size() + messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.delete.message.empty");
        }
        dialogs.createOptionDialog().withHeader(messages.getMessage("eu.occtet.bocfrontend.view/filesTabFragment.codeLocation.delete.header"))
                .withText(message)
                .withActions(
                        new DialogAction(DialogAction.Type.YES).withHandler(event -> {
                            //TODO fix this!
                            //codeLocationRepository.delete(file);
                            //copyrightRepository.deleteAll(associatedCopyrights);
                            //codeLocationDl.load();
                            notifications.create("Not yet implemented").show();
                        }),
                        new DialogAction(DialogAction.Type.NO)
                ).open();
    }
}
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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import eu.occtet.bocfrontend.dao.CodeLocationRepository;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.factory.CodeLocationFactory;
import eu.occtet.bocfrontend.model.FileResult;
import eu.occtet.bocfrontend.service.FileContentService;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
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

/**
 * The FilesTabFragment class represents a fragment of a user interface for managing and interacting
 * with code locations in a vertical layout. It provides functionality to add, view, edit, delete,
 * and switch between a grid and code viewer view mode.
 */
@FragmentDescriptor("files-tab-fragment.xml")
public class FilesTabFragment extends Fragment<VerticalLayout>{

    private static final Logger log = LogManager.getLogger(FilesTabFragment.class);

    private View<?> hostView;
    private InventoryItem inventoryItem;
    boolean viewMode = false;

    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private CollectionLoader<CodeLocation> codeLocationDl;
    @ViewComponent
    private VerticalLayout codeLocationViewBox;
    @ViewComponent
    private VerticalLayout codeViewerBox;
    @ViewComponent
    private JmixButton switchButton;
    @ViewComponent
    private CodeViewerFragment codeViewer;

    @Autowired
    private Dialogs dialogs;
    @Autowired
    private CodeLocationFactory codeLocationFactory;
    @Autowired
    private Notifications notifications;
    @Autowired
    private FileContentService fileContentService;
    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    private CodeLocationRepository codeLocationRepository;

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
    }

    public void setInventoryItemId(InventoryItem inventoryItem) {
        this.inventoryItem = dataContext.merge(inventoryItem);

        codeLocationDl.setParameter("inventoryItem", this.inventoryItem);
        codeLocationDl.load();
    }

    /**
     * Handles the addition of a new code location for the current inventory item.
     * Opens an input dialog to collect information such as file path and optional line number range.
     *
     * @param event the click event triggered by the UI component, representing the user interaction
     */
    @Subscribe(id = "codeloctionsEditButton.addCodeLocation")
    public void addCodeLocation(DropdownButtonItem.ClickEvent event) {
        dialogs.createInputDialog(hostView)
                .withHeader("Enter values")
                .withParameters(
                        InputParameter.stringParameter("filePath").withLabel("File Path").withRequired(true)
                                .withRequiredMessage("Provide a path (absolute or relative), e.g. /usr/local/file.txt or ./file.txt"),
                        InputParameter.intParameter("from").withRequired(false).withLabel("From").withDefaultValue(0),
                        InputParameter.intParameter("to").withRequired(false).withLabel("To").withDefaultValue(0)
                ).withActions(DialogActions.OK_CANCEL)
                .withCloseListener( closeEvent ->{
                    if (!closeEvent.closedWith(DialogOutcome.OK)){
                        return;
                    }
                    String filePath = closeEvent.getValue("filePath");
                    Integer from = closeEvent.getValue("from");
                    Integer to = closeEvent.getValue("to");
                    codeLocationFactory.create(inventoryItem,filePath,from,to);
                    codeLocationDl.load();
                }).open();
    }

    @Subscribe(id = "switchButton")
    public void onSwitchButtonClick(ClickEvent<Button> event) {
        switchFileTabMode();
    }

    @Supply(to = "codeLocationDataGrid.options", subject = "renderer")
    protected Renderer<CodeLocation> codeLocationRenderer() {
        return new ComponentRenderer<>(codeLocation -> {
            DropdownButton optionsButton = uiComponents.create(DropdownButton.class);
            optionsButton.setDropdownIndicatorVisible(false);
            optionsButton.setIcon(VaadinIcon.ELLIPSIS_DOTS_H.create());
            optionsButton.addThemeVariants(DropdownButtonVariant.LUMO_ICON, DropdownButtonVariant.LUMO_SMALL, DropdownButtonVariant.LUMO_TERTIARY);
            optionsButton.setWidth("40px");

            // option buttons
            optionsButton.addItem("copy", "Copy", clickEvent -> copyCodeLocation(codeLocation));
            optionsButton.addItem("view", "View", clickEvent -> viewCodeLocation(codeLocation));
            optionsButton.addItem("edit", "edit", clickEvent -> editCodeLocation(codeLocation));
            optionsButton.addItem("delete", "delete", clickEvent -> deleteCodeLocation(codeLocation));

            return optionsButton;
        });
    }

    private void copyCodeLocation(CodeLocation codeLocation) {
        UiComponentUtils.copyToClipboard(codeLocation.getFilePath())
                .then(successResult -> notifications.create("Text copied!")
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_SUCCESS)
                                .show(),
                        errorResult -> notifications.create("Copy failed!")
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withThemeVariant(NotificationVariant.LUMO_ERROR)
                                .show());
    }

    private void viewCodeLocation(CodeLocation codeLocation) {
        FileResult fileResult = fileContentService.getFileContentOfCodeLocation(codeLocation, inventoryItem);
        if (fileResult instanceof FileResult.Success(String content, String fileName)){
            codeViewer.setCodeEditorContent(content, fileName);
            switchFileTabMode();
        } else if (fileResult instanceof FileResult.Failure(String errorMessage)) {
            log.warn("Could not view code location: {}", errorMessage);
            notifications.create(errorMessage)
                    .withPosition(Notification.Position.BOTTOM_END)
                    .withThemeVariant(NotificationVariant.LUMO_ERROR)
//                    .withCloseable(true)
                    .withDuration(6000)
                    .show();
        }
    }

    private void editCodeLocation(CodeLocation codeLocation){
        dialogs.createInputDialog(hostView)
                .withHeader("Enter values")
                .withParameters(
                        InputParameter.stringParameter("filePath").withLabel("File Path").withDefaultValue(codeLocation.getFilePath()),
                        InputParameter.intParameter("from").withRequired(false).withLabel("From").withDefaultValue(codeLocation.getLineNumber()),
                        InputParameter.intParameter("to").withRequired(false).withLabel("To").withDefaultValue(codeLocation.getLineNumberTo())
                ).withActions(DialogActions.OK_CANCEL)
                .withCloseListener( closeEvent ->{
                    if (!closeEvent.closedWith(DialogOutcome.OK)){
                        return;
                    }
                    String filePath = closeEvent.getValue("filePath");
                    Integer from = closeEvent.getValue("from");
                    Integer to = closeEvent.getValue("to");
                    codeLocation.setFilePath(filePath);
                    codeLocation.setLineNumber(from);
                    codeLocation.setLineNumberTo(to);
                    codeLocationRepository.save(codeLocation);
                    log.debug("Code location {} edited.", codeLocation.getId());
                }).open();
    }

    private void deleteCodeLocation(CodeLocation codeLocation){
        List<Copyright> associatedCopyrights = copyrightRepository.findCopyrightsByCodeLocation(codeLocation);
        String message = "Are you sure you want to delete this code location?\n"+codeLocation.getFilePath();
        if (!associatedCopyrights.isEmpty()) {
            message += "\n\n" + associatedCopyrights.size() + " associated copyrights will also be deleted.";
        }
        dialogs.createOptionDialog().withHeader("Delete code location")
                .withText(message)
                .withActions(
                        new DialogAction(DialogAction.Type.YES).withHandler(event -> {
                            // TODO handle deletion: Should copyrights be deleted as well? Please revise
                            codeLocationRepository.delete(codeLocation);
                            copyrightRepository.deleteAll(associatedCopyrights);
                        }),
                        new DialogAction(DialogAction.Type.NO)
                ).open();
    }

    private void switchFileTabMode(){
        viewMode = !viewMode;
        codeLocationViewBox.setVisible(!viewMode);
        codeViewerBox.setVisible(viewMode);
        if (codeViewerBox.isVisible()) {
            switchButton.setIcon(VaadinIcon.FILE_TEXT.create());
            switchButton.setText("Switch back to grid");
        } else {
            switchButton.setIcon(VaadinIcon.FILE_CODE.create());
            switchButton.setText("Switch to code viewer");
        }
    }
}
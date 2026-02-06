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

package eu.occtet.bocfrontend.view.project;

import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.AppConfigurationRepository;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfigKey;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfiguration;
import eu.occtet.bocfrontend.entity.appconfigurations.SearchTermsProfile;
import eu.occtet.bocfrontend.service.Utilities;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.Messages;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.stream.Collectors;

@Route(value = "projects/:id", layout = MainView.class)
@ViewController(id = "Project.detail")
@ViewDescriptor(path = "project-detail-view.xml")
@EditedEntityContainer("projectDc")
public class ProjectDetailView extends StandardDetailView<Project> {

    @ViewComponent
    private DataGrid<SearchTermsProfile> searchTermsProfilesDataGrid;
    @Autowired
    private Messages messages;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Utilities utilities;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private AppConfigurationRepository appConfigurationRepository;
    @Autowired
    private Notifications notifications;

    @Subscribe
    protected void onInit(InitEvent event) {
        initHeaderForDataGrid(searchTermsProfilesDataGrid, messages.getMessage("eu.occtet.bocfrontend.view.project/Project.h2.searchTermsProfile"));
    }

    private <E> void initHeaderForDataGrid(DataGrid<E> dataGrid, String title){
        HeaderRow headerRow = dataGrid.prependHeaderRow();
        HeaderRow.HeaderCell headerCell;
        if (dataGrid.getColumns().size() <= 1){
            headerCell = headerRow.getCell(dataGrid.getColumns().getFirst());
        } else {
            Collection<HeaderRow.HeaderCell> headerCells = dataGrid.getColumns().stream().map(headerRow::getCell).collect(Collectors.toList());

            headerCell = headerRow.join(headerCells);
        }
        Span titleSpan = uiComponents.create(Span.class);
        titleSpan.setText(title);
        titleSpan.setClassName("occtet-dialog-title");
        HorizontalLayout layout = new HorizontalLayout(titleSpan);
        layout.setWidthFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerCell.setComponent(layout);
    }

    @Subscribe("searchTermsProfilesDataGrid.showTerms")
    public void onShowTerms(ActionPerformedEvent event){
        SearchTermsProfile selected = searchTermsProfilesDataGrid.getSingleSelectedItem();
        if (selected == null) return;
        String formattedTerms = utilities.convertListToText(selected.getSearchTerms(), "; ");
        if (formattedTerms.isEmpty()){
            formattedTerms = "(No search terms found)";
        }
        dialogs.createMessageDialog()
                .withHeader(messages.getMessage("eu.occtet.bocfrontend.view.project/projectDetailView.messageDialogShow.searchTerms"))
                .withText(formattedTerms)
                .withCloseOnOutsideClick(true)
                .withCloseOnEsc(true)
                .open();
    }

    @Subscribe
    protected void onBeforeSave(BeforeSaveEvent event){
        AppConfiguration globalBasePath =
                appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH).orElse(null);

        if (globalBasePath == null ||
                globalBasePath.getValue() == null ||
                globalBasePath.getValue().isBlank()) {

            notifications.create(
                            messages.getMessage(
                                    "eu.occtet.bocfrontend.view.project/Project.globalPathNotSet.WarningMsg"
                            )
                    )
                    .withPosition(Notification.Position.TOP_CENTER)
                    .withThemeVariant(NotificationVariant.LUMO_WARNING)
                    .show();

            event.preventSave();
        }
    }
}
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

package eu.occtet.bocfrontend.view.copyright;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.view.dialog.AddLicenseDialog;
import eu.occtet.bocfrontend.view.dialog.AddLicenseToCopyrightDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import eu.occtet.bocfrontend.view.softwareComponent.SoftwareComponentDetailView;
import io.jmix.core.DataManager;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButtonItem;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.InstanceLoader;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@Route(value = "copyrights/:id", layout = MainView.class)
@ViewController(id = "Copyright.detail")
@ViewDescriptor(path = "copyright-detail-view.xml")
@EditedEntityContainer("copyrightDc")
public class CopyrightDetailView extends StandardDetailView<Copyright> {

    private static final Logger log = LogManager.getLogger(CopyrightDetailView.class);


    @Autowired
    private DialogWindows dialogWindow;
    @Autowired
    private Notifications notifications;

    @ViewComponent
    private CollectionContainer<License> licenseDc;

    @ViewComponent
    private JmixButton removeLicenseButton;
    @ViewComponent
    private DataGrid<License> licensesDataGrid;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        updateLicenseGrid();
    }






    @Subscribe(id = "addLicense", subject = "clickListener")
    public void onAddLicenseClick(final ClickEvent<JmixButton> event) {

            DialogWindow<AddLicenseToCopyrightDialog> window = dialogWindow.view(this, AddLicenseToCopyrightDialog.class).build();
            window.getView().setAvailableContent(this.getEditedEntity());
            window.open();
            window.addAfterCloseListener(close ->
                    updateLicenseGrid());

        notifications.create("Licenses added.")
                .withPosition(Notification.Position.BOTTOM_END)
                .show();
    }

    public void updateLicenseGrid(){
        log.debug("update licenses {}", getEditedEntity().getLicenses().size());
        licenseDc.setItems(getEditedEntity().getLicenses());
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
        Copyright current= this.getEditedEntity();
        if (!selectedLicenses.isEmpty()) {
            current.getLicenses().removeAll(selectedLicenses);
            copyrightRepository.save(current);
            updateLicenseGrid();

            notifications.create("Licenses removed.")
                    .withPosition(Notification.Position.BOTTOM_END)
                    .show();
        }

        licensesDataGrid.setSelectionMode(DataGrid.SelectionMode.SINGLE);
        licensesDataGrid.deselectAll();
    }

}
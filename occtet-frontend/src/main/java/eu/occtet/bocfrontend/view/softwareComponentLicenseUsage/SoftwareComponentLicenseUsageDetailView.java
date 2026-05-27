/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https:www.apache.orglicensesLICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *   License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.view.softwareComponentLicenseUsage;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponentLicenseUsage;
import eu.occtet.bocfrontend.view.audit.fragment.InventoryItemTabFragment;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Route(value = "usage-licenses/:id", layout = MainView.class)
@ViewController(id = "SoftwareComponentLicenseUsage.detail")
@ViewDescriptor(path = "usage-license-detail-view.xml")
@EditedEntityContainer("licenseDc")
public class SoftwareComponentLicenseUsageDetailView extends StandardDetailView<SoftwareComponentLicenseUsage> {

    private static final Logger log = LogManager.getLogger(SoftwareComponentLicenseUsageDetailView.class);


    @ViewComponent
    private TextArea licenseTextField;

    @ViewComponent
    private TextField licenseNameField;

    private String firstText;
    private String firstName;


    @Subscribe
    public void onReady(ReadyEvent event) {
        SoftwareComponentLicenseUsage sc = getEditedEntity();
        firstName= sc.getEffectiveName();
        licenseNameField.setValue(firstName);
        firstText = sc.getEffectiveText();
        licenseTextField.setValue(firstText);
    }

    @Subscribe
    public void onBeforeSave(BeforeSaveEvent event) {
        // see if text has changed, compare to standard
        String currentText = licenseTextField.getValue();
        String currentName = licenseNameField.getValue();
        SoftwareComponentLicenseUsage entity = getEditedEntity();
        String templateText = entity.getTemplate() != null ? entity.getTemplate().getTemplateText() : "";
        String templateName = entity.getTemplate() != null ? entity.getTemplate().getLicenseName() : "";

        if(currentText != null && currentText.equals(templateText) && currentName != null && currentName.equals(templateName)){
            entity.setUsageText(null);
            entity.setCustomName(null);
            entity.setIsModified(false);
        }

        if(currentText != null && currentText.equals(templateText)){
            entity.setUsageText(null);
        } else if (currentText != null && !currentText.equals(firstText)) {
            entity.setUsageText(currentText);
            entity.setIsModified(true);
        } else if (currentText == null || currentText.isEmpty()) {
            entity.setUsageText(null);
            entity.setIsModified(true);
        }

        if(currentName != null && currentName.equals(templateName)){
            entity.setCustomName(null);
        } else if (currentName != null && !currentName.equals(firstName)) {
            entity.setCustomName(currentName);
            entity.setIsModified(true);
        } else if (currentName == null || currentName.isEmpty()) {
            entity.setCustomName(null);
            entity.setIsModified(true);
        }

    }



}
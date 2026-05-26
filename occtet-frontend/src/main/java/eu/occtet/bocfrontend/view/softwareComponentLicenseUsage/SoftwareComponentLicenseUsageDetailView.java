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
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;

@Route(value = "usage-licenses/:id", layout = MainView.class)
@ViewController(id = "UsageLicense.detail")
@ViewDescriptor(path = "usage-license-detail-view.xml")
@EditedEntityContainer("licenseDc")
public class SoftwareComponentLicenseUsageDetailView extends StandardDetailView<SoftwareComponentLicenseUsage> {

    @ViewComponent
    private TextArea licenseTextField;

    @ViewComponent
    private TextField licenseNameField;


    @Subscribe
    public void onReady(ReadyEvent event) {
        SoftwareComponentLicenseUsage sc = getEditedEntity();
        licenseNameField.setValue(sc.getEffectiveName());
        String effectiveText = sc.getEffectiveText();
        licenseTextField.setValue(effectiveText);
    }

    @Subscribe
    public void onBeforeSave(BeforeSaveEvent event) {
        // see if text has changed, compare to standard
        String currentText = licenseTextField.getValue();
        String currentName = licenseNameField.getValue();
        String templateText = getEditedEntity().getTemplate().getTemplateText();
        String templateName = getEditedEntity().getTemplate().getLicenseName();

        if (!currentText.equals(templateText)) {
            getEditedEntity().setUsageText(currentText);
        }
        if (!currentName.equals(templateName)) {
            getEditedEntity().setCustomName(currentName);
        }

    }

}
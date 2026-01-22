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

package eu.occtet.bocfrontend.view.appconfigurationprofile;

import com.vaadin.flow.component.textfield.TextArea;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfigurationProfile;
import eu.occtet.bocfrontend.service.AppConfigurationProfileService;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@ViewController(id = "AppConfigurationProfileDetailView")
@ViewDescriptor(path = "app-configuration-profile-detail-view.xml")
@EditedEntityContainer("appConfigurationProfileDc")
@DialogMode(width = "90%", height = "90%")
public class AppConfigurationProfileDetailView extends StandardDetailView<AppConfigurationProfile> {

    @Autowired
    private AppConfigurationProfileService profileService;

    @ViewComponent
    private TextArea searchTermsField;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        AppConfigurationProfile profile = getEditedEntity();
        String text = profileService.convertListToText(profile.getSearchTerms());
        searchTermsField.setValue(text);
    }

    @Subscribe
    public void onBeforeSave(StandardDetailView.BeforeSaveEvent event) {
        AppConfigurationProfile profile = getEditedEntity();
        profile.setSearchTerms(profileService.convertTextToList(searchTermsField.getValue()));
    }
}
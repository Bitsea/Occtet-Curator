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

package eu.occtet.bocfrontend.view.searchtermsprofile;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import eu.occtet.bocfrontend.entity.appconfigurations.SearchTermsProfile;
import eu.occtet.bocfrontend.factory.InfoButtonFactory;
import eu.occtet.bocfrontend.service.Utilities;
import io.jmix.core.Messages;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@ViewController(id = "SearchTermsProfileDetailView")
@ViewDescriptor(path = "search-terms-profile-detail-view.xml")
@DialogMode(width = "90%", height = "90%")
@EditedEntityContainer("searchTermsProfileDc")
public class SearchTermsProfileDetailView extends StandardDetailView<SearchTermsProfile> {

    @Autowired
    private Utilities utilities;
    @Autowired
    private Messages messages;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private InfoButtonFactory infoButtonFactory;

    @ViewComponent
    private TextArea searchTermsField;

    public static final String SEPARATOR = "\n";

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        SearchTermsProfile profile = getEditedEntity();
        String text = utilities.convertListToText(profile.getSearchTerms(), SEPARATOR);
        addInfoButtonToHeaderOfTextArea();
        searchTermsField.setValue(text);
    }

    @Subscribe
    public void onBeforeSave(StandardDetailView.BeforeSaveEvent event) {
        SearchTermsProfile profile = getEditedEntity();
        profile.setSearchTerms(utilities.convertTextToList(searchTermsField.getValue(), SEPARATOR));
    }

    private void addInfoButtonToHeaderOfTextArea(){
        Span exp = uiComponents.create(Span.class);
        exp.setText(messages.getMessage("eu.occtet.bocfrontend.view.searchtermsprofile/SearchTermsProfileDetailView" +
                ".explanation"));
        JmixButton infoButton = infoButtonFactory.createInfoButtonFromComponent(exp, null, null);
        infoButton.getStyle().set("color", "var(--lumo-primary-color)");
        infoButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE);
        searchTermsField.setSuffixComponent(infoButton);
    }
}
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

package eu.occtet.bocfrontend.view.appconfiguration.editorprofilefragment;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.occtet.bocfrontend.entity.appconfigurations.AppConfigurationProfile;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;

@FragmentDescriptor("editor-profile-fragment.xml")
public class EditorProfileFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private CollectionLoader<AppConfigurationProfile> appConfigurationProfileDl;

    @Subscribe
    public void onReady(ReadyEvent event) {
        appConfigurationProfileDl.load();
    }
}
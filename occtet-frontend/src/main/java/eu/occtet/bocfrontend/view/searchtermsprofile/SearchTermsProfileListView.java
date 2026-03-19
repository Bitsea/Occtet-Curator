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

import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.appconfigurations.SearchTermsProfile;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.*;


@ViewController("SearchTermsProfile.list")
@ViewDescriptor("search-terms-profile-list-view.xml")
@LookupComponent("searchTermsProfilesDataGrid")
@DialogMode(width = "60em", height = "45em")
public class SearchTermsProfileListView extends StandardListView<SearchTermsProfile> {
}
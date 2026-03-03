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


import eu.occtet.bocfrontend.entity.*;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import javax.annotation.Nonnull;



@FragmentDescriptor("OverviewOrtTabFragment.xml")
public class OverviewOrtTabFragment extends Fragment<JmixTabSheet>{

    private static final Logger log = LogManager.getLogger(OverviewOrtTabFragment.class);

    private Project project;
    private View<?> hostView;

    @ViewComponent
    private DataContext dataContext;

    public void setProjectOrtOverview(@Nonnull Project project){
        this.project = dataContext.merge(project);
        setOrtInformation(this.project);
    }

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
    }

    private void setOrtInformation(Project project){



    }
}

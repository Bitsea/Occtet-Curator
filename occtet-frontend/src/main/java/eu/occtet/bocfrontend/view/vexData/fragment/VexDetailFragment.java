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

package eu.occtet.bocfrontend.view.vexData.fragment;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.occtet.bocfrontend.entity.VexData;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.view.View;

public class VexDetailFragment extends Fragment<VerticalLayout> {

    protected VexData vexData;
    protected View<?> hostView;

    public void setVexData(VexData vexData) {
        this.vexData = vexData;
    }

    public void setHostView(View<?> hostView) {
        this.hostView = hostView;
    }
}

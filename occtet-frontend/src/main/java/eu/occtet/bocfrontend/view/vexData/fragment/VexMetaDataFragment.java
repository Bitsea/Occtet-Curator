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

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.factory.VexDataFactory;
import eu.occtet.bocfrontend.model.vexModels.VexComponent;
import eu.occtet.bocfrontend.model.vexModels.VexComponentType;
import eu.occtet.bocfrontend.model.vexModels.VexMetadata;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;


@FragmentDescriptor("vex-metadata-fragment.xml")
public class VexMetaDataFragment extends VexDetailFragment {


    private static final Logger log = LogManager.getLogger(VexMetaDataFragment.class);

    @ViewComponent
    private Html timeStamp;

    @ViewComponent
    private ComboBox type;

    @ViewComponent
    private TextField componentName;
    @ViewComponent
    private TextField componentVersion;

    @Autowired
    private VexDataFactory vexDataFactory;

    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostReady(final View.ReadyEvent event) {
        log.debug("make ready");
        timeStamp.setHtmlContent(vexData.getTimeStamp().toString());
        componentName.setValue(vexData.getSoftwareComponent().getName());
        componentVersion.setValue(vexData.getSoftwareComponent().getVersion());
        changeMetaDataValues(vexData, type.getElement().toString());

    }

    @Subscribe("type")
    public void onTypeComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<VexComponentType>, VexComponentType> event) {
        changeMetaDataValues(vexData, event.getValue().getId());

    }

    private void changeMetaDataValues(VexData vexData, String type ){
        VexMetadata vexMetadata = new VexMetadata(vexData.getTimeStamp(), new VexComponent(type, componentName.getValue(), componentVersion.getValue()));
        vexDataFactory.addMetaDataAsJson(vexData, vexMetadata);
    }

}

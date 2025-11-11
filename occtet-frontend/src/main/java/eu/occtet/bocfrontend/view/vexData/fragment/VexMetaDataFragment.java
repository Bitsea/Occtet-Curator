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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.factory.VexDataFactory;
import eu.occtet.bocfrontend.model.vexModels.VexComponent;
import eu.occtet.bocfrontend.model.vexModels.VexComponentType;
import eu.occtet.bocfrontend.model.vexModels.VexMetadata;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;


@FragmentDescriptor("vex-metadata-fragment.xml")
public class VexMetaDataFragment extends VexDetailFragment {


    private static final Logger log = LogManager.getLogger(VexMetaDataFragment.class);


    @ViewComponent
    private ComboBox<String> type;

    @ViewComponent
    private TextField name;
    @ViewComponent
    private TextField version;

    @Autowired
    private VexDataFactory vexDataFactory;

    @ViewComponent
    private InstanceContainer<SoftwareComponent> softwareComponentDc;


    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostReady(final View.ReadyEvent event) {
        softwareComponentDc.setItem(vexData.getSoftwareComponent());
        List<String> typeList= new ArrayList<>();
        for(VexComponentType vt: VexComponentType.values()){
           typeList.add(vt.name());
        }
        type.setItems(typeList);
        changeMetaDataValues(vexData, type.getValue());

    }

    @Subscribe("type")
    public void onTypeComponentValueChange(final AbstractField.ComponentValueChangeEvent<JmixComboBox<String>, String> event) {
        changeMetaDataValues(vexData, event.getValue());
    }

    private void changeMetaDataValues(VexData vexData, String type ){
        VexMetadata vexMetadata = new VexMetadata(vexData.getTimeStamp().toLocalTime().toString(), new VexComponent(type, name.getValue(), version.getValue()));
        vexDataFactory.addMetaDataAsJson(vexData, vexMetadata);
    }

}

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

package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.entity.Vulnerability;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * Factory class for creating of finding VEX data objects.
 */
@Component
public class VexDataFactory {
    @Autowired
    private DataManager dataManager;

    public VexData create(SoftwareComponent softwareComponent){
        VexData vexData = dataManager.create(VexData.class);
        vexData.setSoftwareComponent(softwareComponent);
        vexData.setVulnerability(softwareComponent.getVulnerabilities());
        dataManager.save(vexData);
        return vexData;
    }

}

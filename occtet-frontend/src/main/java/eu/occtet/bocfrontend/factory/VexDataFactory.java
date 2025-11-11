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

import com.google.gson.Gson;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.VexData;
import eu.occtet.bocfrontend.entity.Vulnerability;
import eu.occtet.bocfrontend.model.vexModels.VexVulnerability;
import eu.occtet.bocfrontend.model.vexModels.Vulnerabilites;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * Factory class for creating of finding VEX data objects.
 */
@Component
public class VexDataFactory {
    @Autowired
    private DataManager dataManager;

    /**
     * create a new VexData object with software component and its vulnerabilities
     * @param softwareComponent
     * @return
     */
    public VexData create(SoftwareComponent softwareComponent){
        VexData vexData = dataManager.create(VexData.class);
        vexData.setSoftwareComponent(softwareComponent);
        vexData.setVulnerability(softwareComponent.getVulnerabilities());
        dataManager.save(vexData);
        return vexData;
    }

    /**
     * add software component and vulnerabilities to the vexData
     * @param vexData
     * @param softwareComponent
     * @param vulnerabilities
     * @return
     */
    public VexData addVexData(VexData vexData, SoftwareComponent softwareComponent, List<Vulnerability> vulnerabilities){
        vexData.setSoftwareComponent(softwareComponent);
        vexData.setVulnerability(vulnerabilities);
        return vexData;
    }

    /**
     * add generic data to the vexData. This will be converted to json format before.
     *
     * @param vexData
     * @param data
     * @return
     */
    public VexData addMetaDataAsJson(VexData vexData, Object data) {
        Gson gson = new Gson();
        vexData.setMetaData(gson.toJson(data));
        return vexData;
    }

    /**
     * add generic data to the vexData. This will be converted to json format before.
     *
     * @param vexData
     * @param data
     * @return
     */
    public VexData addVulnerabilityDataAsJson(VexData vexData, List<VexVulnerability> data) {
        Gson gson = new Gson();
        vexData.setVulnerabilities(gson.toJson(data));
        return vexData;
    }

}

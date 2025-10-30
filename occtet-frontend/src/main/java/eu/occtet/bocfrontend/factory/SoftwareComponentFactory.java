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

import eu.occtet.bocfrontend.entity.License;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SoftwareComponentFactory {

    @Autowired
    private DataManager dataManager;

    public SoftwareComponent create(@Nonnull String name, @Nonnull String version, @Nonnull String purl,
                                    boolean curated, List<License> licenses){
        SoftwareComponent softwareComponent = dataManager.create(SoftwareComponent.class);

        softwareComponent.setName(name);
        softwareComponent.setVersion(version);
        softwareComponent.setPurl(purl);
        softwareComponent.setCurated(curated);
        softwareComponent.setLicenses(licenses);

        return dataManager.save(softwareComponent);
    }

    public SoftwareComponent create(String name, String version){
        return create(name, version, "", false, new ArrayList<>());
    }
}
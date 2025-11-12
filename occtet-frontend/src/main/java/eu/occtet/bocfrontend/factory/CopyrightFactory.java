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


import eu.occtet.bocfrontend.entity.CodeLocation;
import eu.occtet.bocfrontend.entity.Copyright;
import io.jmix.core.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyrightFactory {

    @Autowired
    private DataManager dataManager;

    public Copyright create(String copyrightName, CodeLocation codeLocation, boolean isCurated, boolean isGarbage){

        Copyright copyright = dataManager.create(Copyright.class);
        copyright.setCopyrightText(copyrightName);
        copyright.setCodeLocation(codeLocation);
        copyright.setCurated(isCurated);
        copyright.setGarbage(isGarbage);

        return dataManager.save(copyright);
    }
}

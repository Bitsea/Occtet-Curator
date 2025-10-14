/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.License;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseFactory {

    @Autowired
    private DataManager dataManager;

    public License create(@Nonnull Integer priority, @Nonnull String licenseType, @Nonnull String licenseText,
                          @Nonnull String licenseName, @Nonnull String detailsUrl, boolean isModified,
                          boolean curated, boolean isSpdx){
        License license = dataManager.create(License.class);

        license.setPriority(priority);
        license.setLicenseType(licenseType);
        license.setLicenseText(licenseText);
        license.setLicenseName(licenseName);
        license.setDetailsUrl(detailsUrl);
        license.setModified(isModified);
        license.setCurated(curated);
        license.setSpdx(isSpdx);

        return dataManager.save(license);
    }

    public License create(@Nonnull String licenseType, @Nonnull String licenseText, @Nonnull String licenseName){
        return create(0, licenseType, licenseText, licenseName, "", false, false, false);
    }

    public License create(@Nonnull String licenseType,@Nonnull String licenseText,@Nonnull String licenseName, @Nonnull String detailUrl, boolean isSpdx){
        return create(0,licenseType,licenseText,licenseName,detailUrl,false,false,isSpdx);
    }
}
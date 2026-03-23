/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.factory;

import eu.occtet.bocfrontend.entity.TemplateLicense;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TemplateLicenseFactory {

    @Autowired
    private DataManager dataManager;

    public TemplateLicense create(@Nonnull Integer priority, @Nonnull String licenseType, @Nonnull String templateText,
                                  @Nonnull String licenseName, @Nonnull String detailsUrl, boolean isSpdx) {
        TemplateLicense license = dataManager.create(TemplateLicense.class);

        license.setPriority(priority);
        license.setLicenseType(licenseType);
        license.setTemplateText(templateText);
        license.setLicenseName(licenseName);
        license.setDetailsUrl(detailsUrl);
        license.setIsSpdx(isSpdx);

        return dataManager.save(license);
    }

    public TemplateLicense create(@Nonnull String licenseType, @Nonnull String templateText, @Nonnull String licenseName) {
        return create(0, licenseType, templateText, licenseName, "", false);
    }

    public TemplateLicense create(@Nonnull String licenseType, @Nonnull String templateText, @Nonnull String licenseName,
                                  @Nonnull String detailUrl, boolean isSpdx) {
        return create(0, licenseType, templateText, licenseName, detailUrl, isSpdx);
    }
}

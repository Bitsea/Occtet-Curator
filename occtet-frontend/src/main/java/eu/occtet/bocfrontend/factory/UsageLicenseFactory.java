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

import com.github.javaparser.quality.Nullable;
import eu.occtet.bocfrontend.entity.SoftwareComponent;
import eu.occtet.bocfrontend.entity.TemplateLicense;
import eu.occtet.bocfrontend.entity.UsageLicense;
import io.jmix.core.DataManager;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UsageLicenseFactory {

    @Autowired
    private DataManager dataManager;

    public UsageLicense create(@Nonnull String usageText, boolean isModified, boolean curated,
                               @Nullable SoftwareComponent softwareComponent, @Nullable TemplateLicense template) {
        UsageLicense license = dataManager.create(UsageLicense.class);

        license.setUsageText(usageText);
        license.setModified(isModified);
        license.setCurated(curated);
        license.setSoftwareComponent(softwareComponent);
        license.setTemplate(template);

        return dataManager.save(license);
    }


    public UsageLicense create(@Nonnull String usageText, boolean isModified, boolean curated) {
        return create(usageText, isModified, curated, null, null);
    }
}

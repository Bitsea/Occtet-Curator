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

package eu.occtet.boc.spdx.service.handler;

import eu.occtet.boc.entity.Copyright;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.License;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.spdx.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.SpdxSnippet;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SnippetHandler {

    private static final Logger log = LogManager.getLogger(SnippetHandler.class);

    @Autowired
    private LicenseHandler licenseHandler;

    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private CopyrightService copyrightService;

    public void enrichComponentFromSnippet(SpdxSnippet snippet,
                                           Map<String, InventoryItem> fileMap,
                                           Map<String, License> licenseCache,
                                           Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc
    ) throws InvalidSPDXAnalysisException {

        SpdxFile snippetFile = snippet.getSnippetFromFile();
        if (snippetFile == null) return;

        InventoryItem item = fileMap.get(snippetFile.getId());
        if (item == null || item.getSoftwareComponent() == null) {
            return;
        }

        SoftwareComponent component = item.getSoftwareComponent();
        boolean componentUpdated = false;

        String snippetCopyright = snippet.getCopyrightText();
        if (snippetCopyright != null && !snippetCopyright.isEmpty()
                && !"NOASSERTION".equals(snippetCopyright) && !"NONE".equals(snippetCopyright)) {

            boolean exists = component.getCopyrights() != null && component.getCopyrights().stream()
                    .anyMatch(c -> c.getCopyrightText().equals(snippetCopyright));

            if (!exists) {
                Copyright copyright = copyrightService.findOrCreateBatch(Set.of(snippetCopyright)).get(snippetCopyright);
                if (copyright != null) {
                    component.getCopyrights().add(copyright);
                    componentUpdated = true;
                }
            }
        }

        AnyLicenseInfo concluded = snippet.getLicenseConcluded();
        if (concluded != null && !concluded.isNoAssertion(concluded) && !concluded.isNoAssertion(concluded)) {
            List<License> licenses = licenseHandler.createLicenses(concluded, licenseCache, licenseInfosExtractedSpdxDoc);
            for (License license : licenses) {
                if (!component.getLicenses().contains(license)) {
                    component.addLicense(license);
                    componentUpdated = true;
                }
            }
        }

        if (componentUpdated) {
            softwareComponentService.update(component);
        }
    }
}

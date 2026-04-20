/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.spdx.service.handler;

import eu.occtet.boc.entity.*;
import eu.occtet.boc.spdx.context.SpdxImportContext;
import eu.occtet.boc.spdx.converter.SpdxConverter;
import eu.occtet.boc.spdx.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.core.queries.CoreAttributeGroup;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxConstantsCompatV2;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.SpdxSnippet;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

@Service
public class SnippetHandler {

    private static final Logger log = LogManager.getLogger(SnippetHandler.class);

    @Autowired
    private LicenseHandler licenseHandler;
    @Autowired
    SpdxConverter spdxConverter;
    @Autowired
    private SoftwareComponentService softwareComponentService;
    @Autowired
    private CopyrightService copyrightService;

    /**
     * Orchestrates the processing of all snippets found in the SPDX document.
     * Iterates through the model store, converts to entities, and enriches components.
     */
    public void processAllSnippets(SpdxImportContext context) {
        log.info("Starting snippet processing...");

        try {
            Stream<?> rawStream = SpdxModelFactory.getSpdxObjects(
                    context.getSpdxDocument().getModelStore(),
                    context.getSpdxDocument().getCopyManager(),
                    SpdxConstantsCompatV2.CLASS_SPDX_SNIPPET,
                    context.getSpdxDocument().getDocumentUri(),
                    null
            );

            Stream<SpdxSnippet> snippetStream = rawStream
                    .filter(obj -> obj instanceof SpdxSnippet)
                    .map(obj -> (SpdxSnippet) obj);

            snippetStream.forEach(snippet -> {
                try {
                    processSingleSnippet(snippet, context);
                } catch (Exception e) {
                    log.error("Failed to process snippet: {}. Skipping...", snippet.getId(), e);

                    if (context.getSpdxDocumentRoot().getSnippets() != null) {
                        context.getSpdxDocumentRoot().getSnippets()
                                .removeIf(s -> s.getSpdxId() != null && s.getSpdxId().equals(snippet.getId()));
                    }
                }
            });

            log.info("Snippet processing completed.");
        }catch (InvalidSPDXAnalysisException e) {
            log.error("Failed to process snippets. Skipping...", e);
        }
    }

    /**
     * Creates an isolated transaction for a single snippet.
     * If this fails, only this specific snippet is rolled back.
     */
    private void processSingleSnippet(SpdxSnippet snippet, SpdxImportContext context) {
        spdxConverter.convertSnippets(snippet, context.getSpdxDocumentRoot());

        enrichComponentFromSnippet(
                snippet,
                context.getFileToInventoryItemMap(),
                context.getLicenseCache(),
                context.getExtractedLicenseInfos(),
                context.getProject().getOrganization()
        );
    }

    /**
     * Enriches the SoftwareComponent associated with the snippet's file with
     * copyright and license info found in the snippet.
     */
    private void enrichComponentFromSnippet(SpdxSnippet snippet,
                                            Map<String, InventoryItem> fileMap,
                                            Map<String, License> licenseCache,
                                            Collection<ExtractedLicenseInfo> licenseInfosExtractedSpdxDoc,
                                            Organization organization
    ) {SpdxFile snippetFile;
        try {
            snippetFile = snippet.getSnippetFromFile();
            if (snippetFile == null) return;
        } catch (InvalidSPDXAnalysisException e) {
            log.warn("Snippet {} references an invalid or missing file. Skipping enrichment.", snippet.getId());
            return;
        }
        try {

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
                    Copyright copyright =
                            copyrightService.findOrCreateBatch(Set.of(snippetCopyright),
                                    organization).get(snippetCopyright);
                    if (copyright != null) {
                        if (component.getCopyrights() == null) {
                            component.setCopyrights(new java.util.ArrayList<>());
                        }
                        component.getCopyrights().add(copyright);
                        componentUpdated = true;
                    }
                }
            }

            AnyLicenseInfo concluded = snippet.getLicenseConcluded();
            if (concluded != null && !concluded.isNoAssertion(concluded) && !concluded.isNoAssertion(concluded)) {
                List<License> licenses = licenseHandler.createLicenses(concluded, licenseCache,
                        licenseInfosExtractedSpdxDoc, organization);

                if (component.getLicenses() == null) {
                    component.setLicenses(new ArrayList<>());
                }

                for (UsageLicense newUsage : usageLicenses) {
                    // Prevent duplicates by checking if this component already uses this Template ID
                    boolean alreadyExists = component.getLicenses().stream()
                            .anyMatch(existing -> existing.getTemplate().getLicenseType()
                                    .equals(newUsage.getTemplate().getLicenseType()));

                    if (!alreadyExists) {
                        newUsage.setSoftwareComponent(component); // Bind the component
                        component.getLicenses().add(newUsage);
                        componentUpdated = true;
                    }
                }
            }

            if (componentUpdated) {
                softwareComponentService.update(component);
            }
        } catch (InvalidSPDXAnalysisException e) {
            log.error("Failed to process snippet: {}. Skipping...", snippet.getId(), e);
        }
    }
}

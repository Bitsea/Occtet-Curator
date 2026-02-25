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

package eu.occtet.boc.spdx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class JsonSanitizer {

    private static final Logger log = LogManager.getLogger(JsonSanitizer.class);

    /**
     * Scans the JSON tree for defined SPDXIDs and removes references (like relationships)
     * pointing to IDs that do not exist.
     */
    public byte[] sanitizeSpdxJson(JsonNode rootNode, ObjectMapper mapper, byte[] originalBytes) {
        try {

            Set<String> validIds = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);

            if (rootNode.has("SPDXID")) {
                validIds.add(rootNode.get("SPDXID").asText());
            }

            if (rootNode.has("hasExtractedLicensingInfos") && rootNode.get("hasExtractedLicensingInfos").isArray()) {
                ArrayNode licenses = (ArrayNode) rootNode.get("hasExtractedLicensingInfos");
                ArrayNode deduplicatedLicenses = mapper.createArrayNode();

                for (JsonNode license : licenses) {
                    if (license.has("licenseId")) {
                        String licenseId = license.get("licenseId").asText();
                        if (!validIds.contains(licenseId)) {
                            validIds.add(licenseId);
                            deduplicatedLicenses.add(license);
                        } else {
                            log.warn("Scrubbing duplicate extracted license definition: {}", licenseId);
                        }
                    }
                }
                ((ObjectNode) rootNode).set("hasExtractedLicensingInfos", deduplicatedLicenses);
            }

            String[] elementArrays = {"packages", "files", "snippets"};
            for (String arrayName : elementArrays) {
                if (rootNode.has(arrayName) && rootNode.get(arrayName).isArray()) {
                    ArrayNode elements = (ArrayNode) rootNode.get(arrayName);
                    ArrayNode deduplicatedElements = mapper.createArrayNode();

                    for (JsonNode element : elements) {
                        if (element.has("SPDXID")) {
                            String spdxId = element.get("SPDXID").asText();
                            if (!validIds.contains(spdxId)) {
                                validIds.add(spdxId);
                                deduplicatedElements.add(element);
                            } else {
                                log.warn("Scrubbing duplicate {} definition: {}", arrayName, spdxId);
                            }
                        }
                    }
                    ((ObjectNode) rootNode).set(arrayName, deduplicatedElements);
                }
            }

            if (rootNode.has("relationships") && rootNode.get("relationships").isArray()) {
                ArrayNode relationships = (ArrayNode) rootNode.get("relationships");
                ArrayNode validRelationships = mapper.createArrayNode();

                for (JsonNode rel : relationships) {
                    String sourceId = rel.path("spdxElementId").asText();
                    String targetId = rel.path("relatedSpdxElement").asText();

                    if (isValidReference(sourceId, validIds) && isValidReference(targetId, validIds)) {
                        validRelationships.add(rel);
                    } else {
                        log.warn("Scrubbing invalid relationship in JSON: {} -> {}", sourceId, targetId);
                    }
                }
                ((ObjectNode) rootNode).set("relationships", validRelationships);
            }

            if (rootNode.has("packages") && rootNode.get("packages").isArray()) {
                for (JsonNode pkg : rootNode.get("packages")) {
                    if (pkg.has("hasFiles") && pkg.get("hasFiles").isArray()) {
                        ArrayNode files = (ArrayNode) pkg.get("hasFiles");
                        ArrayNode validFiles = mapper.createArrayNode();
                        for (JsonNode fileId : files) {
                            if (isValidReference(fileId.asText(), validIds)) {
                                validFiles.add(fileId);
                            } else {
                                log.warn("Scrubbing missing file reference {} from package {}", fileId.asText(), pkg.path("SPDXID").asText());
                            }
                        }
                        ((ObjectNode) pkg).set("hasFiles", validFiles);
                    }
                }
            }

            if (rootNode.has("documentDescribes") && rootNode.get("documentDescribes").isArray()) {
                ArrayNode describes = (ArrayNode) rootNode.get("documentDescribes");
                ArrayNode validDescribes = mapper.createArrayNode();
                for (JsonNode descId : describes) {
                    if (isValidReference(descId.asText(), validIds)) {
                        validDescribes.add(descId);
                    } else {
                        log.warn("Scrubbing missing documentDescribes reference: {}", descId.asText());
                    }
                }
                ((ObjectNode) rootNode).set("documentDescribes", validDescribes);
            }

            if (rootNode.has("snippets") && rootNode.get("snippets").isArray()) {
                for (JsonNode snippet : rootNode.get("snippets")) {
                    if (snippet.has("snippetFromFile")) {
                        String fileRef = snippet.get("snippetFromFile").asText();
                        if (!isValidReference(fileRef, validIds)) {
                            log.warn("Scrubbing invalid snippetFromFile reference {} from snippet {}",
                                    fileRef, snippet.path("SPDXID").asText());
                            ((ObjectNode) snippet).remove("snippetFromFile");
                        }
                    }
                }
            }

            return mapper.writeValueAsBytes(rootNode);
        } catch (Exception e) {
            log.warn("Failed to sanitize SPDX JSON, falling back to original payload", e);
            return originalBytes;
        }
    }

    /**
     * Validates if an ID is safe to keep.
     */
    private boolean isValidReference(String id, Set<String> validIds) {
        if (id == null || id.isEmpty()) return false;

        return validIds.contains(id) || id.contains(":") || "NONE".equals(id) || "NOASSERTION".equals(id);
    }
}

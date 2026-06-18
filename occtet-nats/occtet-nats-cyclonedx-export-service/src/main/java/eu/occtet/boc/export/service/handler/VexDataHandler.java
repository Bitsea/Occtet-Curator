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

package eu.occtet.boc.export.service.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.VexData;
import eu.occtet.boc.entity.Vulnerability;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VexDataHandler {


    private static final Logger log = LogManager.getLogger(VexDataHandler.class);



    public org.cyclonedx.model.vulnerability.Vulnerability mapToCycloneDxVulnerability(Vulnerability vuln,
            VexData vex, List<InventoryItem> affectedItems) {
        log.debug("handling vulnerabilities or (if existing) vexData");

        ObjectMapper objectMapper= new ObjectMapper();

        org.cyclonedx.model.vulnerability.Vulnerability cdVuln = new org.cyclonedx.model.vulnerability.Vulnerability();

        // Base Data Mapping
        cdVuln.setId(vuln.getVulnerabilityId()); // z.B. CVE-2021-44228
        cdVuln.setDescription(vuln.getDescription());
        cdVuln.setDetail(vuln.getSummary());

        // severity & risk score
        if (vuln.getSeverity() != null || vuln.getRiskScore() != null) {
            org.cyclonedx.model.vulnerability.Vulnerability.Rating rating =
                    new org.cyclonedx.model.vulnerability.Vulnerability.Rating();

            // set severity
            if (vuln.getSeverity() != null) {
                rating.setSeverity(org.cyclonedx.model.vulnerability.Vulnerability.Rating.Severity.fromString(vuln.getSeverity().toLowerCase()));
            }

            // set risk score
            if (vuln.getRiskScore() != null) {
                rating.setScore(vuln.getRiskScore());
                rating.setMethod(org.cyclonedx.model.vulnerability.Vulnerability.Rating.Method.CVSSV3);
            }

            cdVuln.setRatings(List.of(rating));
        }

        if (vuln.getExploitability() != null) {
            String detail = (cdVuln.getDetail() != null ? cdVuln.getDetail() : "")
                    + "\n[Exploitability Score: " + vuln.getExploitability() + "]";
            cdVuln.setDetail(detail);
        }

        // connect do softwareComponent
        List<org.cyclonedx.model.vulnerability.Vulnerability.Affect> affectsList = new ArrayList<>();
        for (InventoryItem item : affectedItems) {
            org.cyclonedx.model.vulnerability.Vulnerability.Affect affects = new org.cyclonedx.model.vulnerability.Vulnerability.Affect();
            affects.setRef(item.getInventoryName());
            affectsList.add(affects);
        }
        cdVuln.setAffects(affectsList);

        // parse vexdata
        if (vex != null && vex.getVulnerabilities() != null && !vex.getVulnerabilities().isBlank()) {
            try {
                JsonNode rootNode = objectMapper.readTree(vex.getVulnerabilities());

                // search von analysis
                JsonNode analysisNode = rootNode.path("analysis");
                if (!analysisNode.isMissingNode()) {
                    org.cyclonedx.model.vulnerability.Vulnerability.Analysis cdAnalysis = new org.cyclonedx.model.vulnerability.Vulnerability.Analysis();

                    // state
                    if (analysisNode.has("state")) {
                        cdAnalysis.setState(org.cyclonedx.model.vulnerability.Vulnerability.Analysis.State.fromString(analysisNode.get("state").asText().toLowerCase()));
                    }
                    // Justification
                    if (analysisNode.has("justification")) {
                        cdAnalysis.setJustification(org.cyclonedx.model.vulnerability.Vulnerability.Analysis.Justification.fromString(analysisNode.get("justification").asText().toLowerCase()));
                    }
                    // Detail
                    if (analysisNode.has("detail")) {
                        cdAnalysis.setDetail(analysisNode.get("detail").asText());
                    }

                    cdVuln.setAnalysis(cdAnalysis);
                }
            } catch (Exception e) {
                //fallback state
                org.cyclonedx.model.vulnerability.Vulnerability.Analysis fallbackAnalysis = new org.cyclonedx.model.vulnerability.Vulnerability.Analysis();
                fallbackAnalysis.setState(org.cyclonedx.model.vulnerability.Vulnerability.Analysis.State.IN_TRIAGE);
                fallbackAnalysis.setDetail("VEX JSON metadata parsing failed: " + e.getMessage());
                cdVuln.setAnalysis(fallbackAnalysis);
            }
        }

        return cdVuln;
    }
}

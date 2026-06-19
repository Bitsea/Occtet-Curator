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

package eu.occtet.boc.export.service;

import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.export.service.handler.ComponentHandler;
import eu.occtet.boc.model.CycloneDxExportWorkData;
import eu.occtet.boc.service.ProgressReportingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.metadata.ToolInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;


@Transactional
@Service
public class ExportService  extends ProgressReportingService  {

    private static final Logger log = LogManager.getLogger(ExportService.class);

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    AnswerService answerService;
    @Autowired
    private ComponentHandler componentHandler;

    @Value("${sbom.spdx.creators.tool.name}")
    private String toolName;

    public boolean process(CycloneDxExportWorkData cycloneDxExportWorkData) {
        log.info("exporting CycloneDX!");
        return createDocument(cycloneDxExportWorkData);
    }

    /**
     * Creates an CycloneDX document by processing project and document data, merging entity changes,
     * adding file elements, packages, relationships, and serializing the result to an output format.
     *
     * @param cycloneDxExportWorkData Data object containing necessary parameters such as project ID,
     *                           SPDX document ID, object store key, and progress notification references.
     * @return {@code true} if the CycloneDX document creation and serialization were successful;
     *         {@code false} otherwise.
     */
    private boolean createDocument(CycloneDxExportWorkData cycloneDxExportWorkData) {
        try {
            log.debug("creating CycloneDX document:");
            Bom bom = new Bom();
            log.debug("show workdata: {}", cycloneDxExportWorkData);

            log.info("fetching project with id: {}", cycloneDxExportWorkData.getProjectId());
            Optional<Project> projectOpt = projectRepository.findById(cycloneDxExportWorkData.getProjectId());
            if (projectOpt.isEmpty()) {
                log.error("Project with id {} not found", cycloneDxExportWorkData.getProjectId());
                return false;
            }
            notifyProgress(10,"Project fetched");

            Project project = projectOpt.get();

            Boolean enriched = cycloneDxExportWorkData.getEnrichment();
            log.debug("LicenseText enrichment with Copyright: {}",enriched);

            notifyProgress(20,"set up for component handling");
            createMetaDataForBOM(bom,project, cycloneDxExportWorkData.getSerialNumber(), cycloneDxExportWorkData.getVersion());
            bom = componentHandler.handleComponents(project,(percent) -> notifyProgress(20 + percent, "processing components"), bom, enriched);

            if(bom==null){
                log.error("No InventoryItems existing");
                notifyProgress(0, "no existing InventoryItems");
                return false;
            }
            notifyProgress(90,"handled components");

            try {
                BomJsonGenerator generator = BomGeneratorFactory.createJson(Version.VERSION_16, bom);

                // generate json
                String jsonString = generator.toJsonString();

                // convert to utf_8
                byte[] bomBytes = jsonString.getBytes(StandardCharsets.UTF_8);
                String objectStoreKey = cycloneDxExportWorkData.getObjectStoreKey();

                log.info("Serialized CycloneDX SBOM JSON. Sending {} bytes to Object Store with key: {}", bomBytes.length, objectStoreKey);

                log.debug("objectkeyStore : {}", objectStoreKey);
                answerService.putIntoBucket(objectStoreKey, bomBytes);

            } catch (Exception e) {
                log.error("Failed to serialize or upload SBOM JSON to Object Store", e);

            }
            notifyProgress(100,"completed");
            return true;
        } catch (Exception e) {
            log.error("Unexpected error during CycloneDX creation", e);
            return false;
        }
    }


    private void createMetaDataForBOM(Bom bom, Project project, String serialNumber, Integer version){
        Metadata metadata= new Metadata();
        metadata.setTimestamp(new Date());


        bom.setSerialNumber(serialNumber);
        bom.setVersion(version);

        ToolInformation tools = new ToolInformation();
        Component toolComponent = new Component();
        toolComponent.setType(Component.Type.APPLICATION);
        toolComponent.setGroup("eu.occtet");
        toolComponent.setName("bitsea-occtet-curator");
        toolComponent.setVersion("1.0");
        toolComponent.setPublisher(project.getOrganization().getOrganizationName());
        toolComponent.setDescription("Curator - audit tool");

        tools.setComponents(List.of(toolComponent));
        metadata.setToolChoice(tools);


        bom.setMetadata(metadata);
    }








}
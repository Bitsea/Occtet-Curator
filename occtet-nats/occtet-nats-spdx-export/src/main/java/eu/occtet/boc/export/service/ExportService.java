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

package eu.occtet.boc.export.service;

import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.spdxV2.CreationInfoEntity;
import eu.occtet.boc.entity.spdxV2.Package;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.entity.spdxV2.SpdxFileEntity;
import eu.occtet.boc.export.dao.ProjectRepository;
import eu.occtet.boc.export.dao.spdxV2.SpdxDocumentRootRepository;
import eu.occtet.boc.model.SpdxExportWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.*;
import org.spdx.library.model.v2.enumerations.RelationshipType;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExportService extends BaseWorkDataProcessor {

    private static final Logger log = LogManager.getLogger(ExportService.class);

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    SpdxDocumentRootRepository spdxDocumentRootRepository;

    @Override
    public boolean process(SpdxExportWorkData spdxExportWorkData){
        log.info("exporting spdx!");
        return createDocument(spdxExportWorkData);
    }

    private boolean createDocument(SpdxExportWorkData spdxExportWorkData){
        try {

            // setup for spdx library need to be called once before any spdx model objects are accessed
            SpdxModelFactory.init();
            InMemSpdxStore modelStore = new InMemSpdxStore();

            Optional<Project> project = projectRepository.findById(UUID.fromString(spdxExportWorkData.getProjectId()));
            if (project.isEmpty()) {
                return false;
            }
            Optional<SpdxDocumentRoot> spdxDocumentRootOpt = spdxDocumentRootRepository.findBySPDXID(spdxExportWorkData.getSpdxDocumentId());
            if (spdxDocumentRootOpt.isEmpty()) {
                return false;
            }
            SpdxDocumentRoot spdxDocumentRoot = spdxDocumentRootOpt.get();
            String documentUri = spdxDocumentRoot.getDocumentUri();
            SpdxDocument spdxDocument = new SpdxDocument(modelStore, documentUri, null, true);

            CreationInfoEntity creationInfoEntity = spdxDocumentRoot.getCreationInfo();
            //TODO get username and any other creators?
            spdxDocument.setCreationInfo(
                    spdxDocument.createCreationInfo(
                            Arrays.asList("Tool: Occtet-Curator", "<Insert User>"),
                            new Date().toString())
            );
            spdxDocument.getCreationInfo().setComment(creationInfoEntity.getComment());
            spdxDocument.setSpecVersion(Version.CURRENT_SPDX_VERSION);
            spdxDocument.setName(spdxDocumentRoot.getName());
            spdxDocument.setDataLicense(LicenseInfoFactory.parseSPDXLicenseStringCompatV2(
                    spdxDocumentRoot.getDataLicense(), modelStore, documentUri, null));

            spdxDocumentRoot.getHasExtractedLicensingInfos().forEach(
                    extractedLicense ->
                    {
                        try {
                            spdxDocument.addExtractedLicenseInfos(
                                    new ExtractedLicenseInfo(extractedLicense.getLicenseId(), extractedLicense.getExtractedText())
                            );
                        } catch (InvalidSPDXAnalysisException e) {
                            log.error("error occurred while trying to add extractedLicenseInfo: {}", e.toString());
                            throw new RuntimeException(e);
                        }
                    }
            );

            Collection<ExternalDocumentRef> externalRefs = new ArrayList<>();
            spdxDocumentRoot.getExternalDocumentRefs().forEach(
                    externalRef ->
                    {
                        try {
                            //TODO confirm creation here is proper?
                            externalRefs.add(
                                    new ExternalDocumentRef(modelStore, documentUri, externalRef.getExternalDocumentId(),null, true)
                            );
                        } catch (InvalidSPDXAnalysisException e) {
                            log.error("error occurred while trying to add external refs: {}", e.toString());
                            throw new RuntimeException(e);
                        }
                    }
            );

            //TODO finish creating spdxObjects from these: add info from audit
            List<SpdxPackage> packages = createSpdxPackages(spdxDocumentRoot.getPackages());
            List<SpdxFile> files = createSpdxFiles(spdxDocumentRoot.getFiles());

            //TODO fix setting relationship target: has to be done after creating all the other spdx objects
            spdxDocumentRoot.getRelationships().forEach(
                    relationship ->
                    {
                        try {
                            Relationship spdxRelationship = new Relationship(modelStore, documentUri, relationship.getSpdxElementId(), null, true);
                            spdxRelationship.setRelationshipType(RelationshipType.valueOf(relationship.getRelationshipType()));
                            //spdxRelationship.setRelatedSpdxElement(relationship.getRelatedSpdxElement());
                            spdxRelationship.setComment(relationship.getComment());
                            spdxDocument.addRelationship(spdxRelationship);
                        } catch (InvalidSPDXAnalysisException e) {
                            log.error("error occurred while trying to add relationship: {}", e.toString());
                            throw new RuntimeException(e);
                        }
                    }
            );

            spdxDocument.setExternalDocumentRefs(externalRefs);

            return true;
        } catch (Exception e) {
            log.error("unexpected error occurred during spdx document creation: {}", e.toString());
            return false;
        }
    }

    private List<SpdxPackage> createSpdxPackages(List<Package> pkgEnteties){
        List<SpdxPackage> packages = new ArrayList<>();
        return packages;
    }

    private List<SpdxFile> createSpdxFiles(List<SpdxFileEntity> fileEnteties){
        List<SpdxFile> files = new ArrayList<>();
        return files;
    }
}

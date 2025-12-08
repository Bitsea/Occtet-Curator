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

import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.spdxV2.*;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.export.dao.InventoryItemRepository;
import eu.occtet.boc.export.dao.ProjectRepository;
import eu.occtet.boc.export.dao.spdxV2.SpdxDocumentRootRepository;
import eu.occtet.boc.model.SpdxExportWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.library.LicenseInfoFactory;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.*;
import org.spdx.library.model.v2.enumerations.ChecksumAlgorithm;
import org.spdx.library.model.v2.enumerations.ReferenceCategory;
import org.spdx.library.model.v2.enumerations.RelationshipType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class ExportService extends BaseWorkDataProcessor {

    private static final Logger log = LogManager.getLogger(ExportService.class);

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    SpdxDocumentRootRepository spdxDocumentRootRepository;
    @Autowired
    InventoryItemRepository inventoryItemRepository;

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
            MultiFormatStore jsonStore = new MultiFormatStore(modelStore, MultiFormatStore.Format.JSON_PRETTY);

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

            List<SpdxPackage> packages = createSpdxPackages(spdxDocumentRoot.getPackages() , spdxDocumentRoot.getFiles(), spdxDocument, modelStore, documentUri);

            spdxDocumentRoot.getRelationships().forEach(
                    relationship ->
                    {
                        try {
                            Relationship spdxRelationship = new Relationship(modelStore, documentUri, relationship.getSpdxElementId(), null, true);
                            spdxRelationship.setRelationshipType(RelationshipType.valueOf(relationship.getRelationshipType()));
                            spdxRelationship.setRelatedSpdxElement(
                                    findRelatedElement(relationship.getRelatedSpdxElement(), packages));
                            spdxRelationship.setComment(relationship.getComment());
                            spdxDocument.addRelationship(spdxRelationship);
                        } catch (InvalidSPDXAnalysisException e) {
                            log.error("error occurred while trying to add relationship: {}", e.toString());
                            throw new RuntimeException(e);
                        }
                    }
            );
            spdxDocument.setExternalDocumentRefs(externalRefs);

            try (FileOutputStream out = new FileOutputStream("sbom.spdx.json")) {
                // The store serializes the specific document URI to the output stream
                jsonStore.serialize(out, spdxDocument);
                log.info("JSON SBOM created successfully!");
            } catch (IOException e) {
                log.error("unexpected error occurred while trying to create spdx json file: {}", e.toString());
            }

            return true;
        } catch (Exception e) {
            log.error("unexpected error occurred during spdx document creation: {}", e.toString());
            return false;
        }
    }

    private List<SpdxPackage> createSpdxPackages(List<SpdxPackageEntity> pkgEntities, List<SpdxFileEntity> fileEntities, SpdxDocument spdxDocument, IModelStore modelStore, String documentUri) {
        List<SpdxPackage> packages = new ArrayList<>();
        try {
            for (SpdxPackageEntity pkgEntity : pkgEntities) {
                SpdxPackage spdxPackage = spdxDocument.createPackage(
                        pkgEntity.getSpdxId(),
                        pkgEntity.getName(),
                        LicenseInfoFactory.parseSPDXLicenseStringCompatV2(pkgEntity.getLicenseConcluded()),
                        pkgEntity.getCopyrightText(),
                        LicenseInfoFactory.parseSPDXLicenseStringCompatV2(pkgEntity.getLicenseDeclared())
                ).build();
                spdxPackage.setDownloadLocation(pkgEntity.getDownloadLocation());
                spdxPackage.setFilesAnalyzed(pkgEntity.getFileNames().isEmpty());
                for (ChecksumEntity checksumEntity : pkgEntity.getChecksums()) {
                    spdxPackage.addChecksum(
                            spdxPackage.createChecksum(ChecksumAlgorithm.valueOf(checksumEntity.getAlgorithm()),
                                    checksumEntity.getChecksumValue()));
                }
                for (ExternalRefEntity externalRefEntity : pkgEntity.getExternalRefs()) {
                    ExternalRef externalRef = new ExternalRef(modelStore, documentUri, SpdxConstantsCompatV2.CLASS_SPDX_EXTERNAL_REFERENCE + externalRefEntity.getReferenceLocator(), null, true);
                    externalRef.setReferenceLocator(externalRefEntity.getReferenceLocator());
                    externalRef.setReferenceType(new ReferenceType(externalRef.getReferenceType()));
                    externalRef.setReferenceCategory(ReferenceCategory.valueOf(externalRefEntity.getReferenceCategory()));
                    externalRef.setComment(externalRefEntity.getComment());
                    spdxPackage.addExternalRef(externalRef);
                }
                SpdxPackageVerificationCode pkgVerificationCode = new SpdxPackageVerificationCode(modelStore, documentUri, SpdxConstantsCompatV2.CLASS_SPDX_VERIFICATIONCODE + pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue(), null, true);
                pkgVerificationCode.setValue(pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue());
                pkgVerificationCode.getExcludedFileNames().addAll(pkgEntity.getPackageVerificationCode().getPackageVerificationCodeExcludedFiles());
                spdxPackage.setPackageVerificationCode(new SpdxPackageVerificationCode());

                Optional<InventoryItem> inventoryItem = inventoryItemRepository.findBySpdxId(pkgEntity.getSpdxId());
                if(inventoryItem.isPresent()){
                    //TODO incorporate more of the audit notes
                    spdxPackage.setComment(inventoryItem.get().getExternalNotes());
                }
                fileEntities.forEach(
                        fileEntity -> {
                            try {
                                spdxPackage.addFile(createSpdxFile(fileEntity, spdxDocument));
                            } catch (InvalidSPDXAnalysisException e) {
                                log.error("Unexpected error while trying to add files to spdx packages: {}", e.toString());
                            }
                        }
                );
                packages.add(spdxPackage);
            }
        }catch (Exception e){
            log.error("Unexpected error while trying to create spdx packages: {}", e.toString());
        }
        return packages;

    }

    private SpdxFile createSpdxFile(SpdxFileEntity fileEntity, SpdxDocument spdxDocument){
        try {
            List<AnyLicenseInfo> seenLicenses = new ArrayList<>();
            for (String license : fileEntity.getLicenseInfoInFiles()) {
                seenLicenses.add(LicenseInfoFactory.parseSPDXLicenseStringCompatV2(license));
            }

            return spdxDocument.createSpdxFile(
                    fileEntity.getSpdxId(),
                    fileEntity.getFileName(),
                    LicenseInfoFactory.parseSPDXLicenseStringCompatV2(fileEntity.getLicenseConcluded()),
                    seenLicenses,
                    fileEntity.getCopyrightText(),
                    spdxDocument.createChecksum(
                            ChecksumAlgorithm.valueOf(fileEntity.getChecksums().getFirst().getAlgorithm()),
                            fileEntity.getChecksums().getFirst().getChecksumValue())
                    )
                    .build();
        } catch (Exception e) {
            log.error("unexpected error occurred while trying to create spdxFile: {}", e.toString());
            return null;
        }
    }

    private SpdxElement findRelatedElement(String id, List<SpdxPackage> pkgs){
        if (pkgs != null) {
            for (SpdxPackage pkg : pkgs) {
                if (id.equals(pkg.getId())) {
                    return pkg;
                }
            }
        }
        return null;
    }
}

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
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.dao.SpdxDocumentRootRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
@Service
public class ExportService extends BaseWorkDataProcessor {

    private static final Logger log = LogManager.getLogger(ExportService.class);

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    SpdxDocumentRootRepository spdxDocumentRootRepository;
    @Autowired
    InventoryItemRepository inventoryItemRepository;
    @Autowired
    AnswerService answerService;

    @Override
    public boolean process(SpdxExportWorkData spdxExportWorkData) {
        log.info("exporting spdx!");
        return createDocument(spdxExportWorkData);
    }

    private boolean createDocument(SpdxExportWorkData spdxExportWorkData) {
        try {
            SpdxModelFactory.init();
            InMemSpdxStore modelStore = new InMemSpdxStore();
            MultiFormatStore jsonStore = new MultiFormatStore(modelStore, MultiFormatStore.Format.JSON_PRETTY);

            log.info("fetching project with id: {}", spdxExportWorkData.getProjectId());
            Optional<Project> project = projectRepository.findById(Long.parseLong(spdxExportWorkData.getProjectId()));
            if (project.isEmpty()) return false;

            log.info("fetching document with id: {}", spdxExportWorkData.getSpdxDocumentId());
            Optional<SpdxDocumentRoot> spdxDocumentRootOpt = spdxDocumentRootRepository.findByDocumentUri(spdxExportWorkData.getSpdxDocumentId());
            if (spdxDocumentRootOpt.isEmpty()) return false;

            SpdxDocumentRoot spdxDocumentRoot = spdxDocumentRootOpt.get();
            String documentUri = spdxDocumentRoot.getDocumentUri();
            SpdxDocument spdxDocument = new SpdxDocument(modelStore, documentUri, null, true);

            Map<String, SpdxElement> elementMap = new HashMap<>();
            elementMap.put(spdxDocument.getId(), spdxDocument);
            elementMap.put("SPDXRef-DOCUMENT", spdxDocument);

            log.info("create creationInfo");
            // TODO get user and other creators?
            CreationInfoEntity creationInfoEntity = spdxDocumentRoot.getCreationInfo();
            spdxDocument.setCreationInfo(
                    spdxDocument.createCreationInfo(
                            Arrays.asList("Tool: Occtet-Curator", "<Insert User>"),
                            Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
            );
            spdxDocument.getCreationInfo().setLicenseListVersion(creationInfoEntity.getLicenseListVersion());
            spdxDocument.getCreationInfo().setComment(creationInfoEntity.getComment());
            spdxDocument.setComment(spdxDocumentRoot.getComment());
            spdxDocument.setSpecVersion(Version.CURRENT_SPDX_VERSION);
            spdxDocument.setName(spdxDocumentRoot.getName());
            spdxDocument.setDataLicense(LicenseInfoFactory.parseSPDXLicenseStringCompatV2(
                    spdxDocumentRoot.getDataLicense(), modelStore, documentUri, null));

            log.info("create extractedLicense info");
            spdxDocumentRoot.getHasExtractedLicensingInfos().forEach(extractedLicense -> {
                try {
                    ExtractedLicenseInfo extractedInfo = new ExtractedLicenseInfo(modelStore, documentUri, extractedLicense.getLicenseId(), null, true);
                    extractedInfo.setExtractedText(extractedLicense.getExtractedText());
                    spdxDocument.addExtractedLicenseInfos(extractedInfo);
                } catch (InvalidSPDXAnalysisException e) {
                    log.error("Error adding extractedLicenseInfo", e);
                }
            });

            log.info("create ExternalDocumentRefs");
            Collection<ExternalDocumentRef> externalRefs = new ArrayList<>();
            spdxDocumentRoot.getExternalDocumentRefs().forEach(externalRef -> {
                try {
                    log.info("creating external ref for: {}", externalRef.getExternalDocumentId());
                    externalRefs.add(new ExternalDocumentRef(modelStore, documentUri, externalRef.getExternalDocumentId(), null, true));
                } catch (InvalidSPDXAnalysisException e) {
                    log.error("Error adding external refs", e);
                }
            });
            spdxDocument.setExternalDocumentRefs(externalRefs);

            log.info("map files");
            Map<String, SpdxFileEntity> fileEntityMap = spdxDocumentRoot.getFiles().stream()
                    .collect(Collectors.toMap(SpdxFileEntity::getSpdxId, Function.identity(), (existing, replacement) -> existing));

            log.info("start converting packages");
            List<SpdxPackage> packages = new ArrayList<>();
            for (SpdxPackageEntity pkgEntity : spdxDocumentRoot.getPackages()) {
                SpdxPackage pkg = createSpdxPackage(pkgEntity, fileEntityMap, spdxDocument, modelStore, documentUri, project.get(), elementMap);
                if (pkg != null) {
                    packages.add(pkg);
                    elementMap.put(pkg.getId(), pkg);
                }
            }

            spdxDocumentRoot.getRelationships().forEach(relationship -> {
                try {
                    log.info("relationship: {} to {}", relationship.getSpdxElementId(), relationship.getRelatedSpdxElement());
                    String uniqueRelationshipId = SpdxConstantsCompatV2.SPDX_ELEMENT_REF_PRENUM + "Relationship-" + UUID.randomUUID();
                    Relationship spdxRelationship = new Relationship(modelStore, documentUri, uniqueRelationshipId, null, true);

                    spdxRelationship.setRelationshipType(RelationshipType.valueOf(relationship.getRelationshipType()));
                    spdxRelationship.setComment(relationship.getComment());

                    SpdxElement relatedElement = elementMap.get(relationship.getRelatedSpdxElement());
                    if (relatedElement != null) {
                        spdxRelationship.setRelatedSpdxElement(relatedElement);
                    } else {
                        log.error("Could not find RELATED element with ID: {}", relationship.getRelatedSpdxElement());
                        return;
                    }

                    SpdxElement sourceElement = elementMap.get(relationship.getSpdxElementId());
                    if (sourceElement != null) {
                        sourceElement.addRelationship(spdxRelationship);
                    } else {
                        log.error("Could not find SOURCE element for relationship: {}", relationship.getSpdxElementId());
                    }

                } catch (InvalidSPDXAnalysisException e) {
                    log.error("Error adding relationship", e);
                }
            });

            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 java.io.BufferedOutputStream bufferedOut = new java.io.BufferedOutputStream(out)) {
                jsonStore.serialize(bufferedOut, spdxDocument);
                bufferedOut.flush();
                String objectStoreKey = spdxExportWorkData.getSpdxDocumentId();

                log.info("Serialized SBOM JSON. Sending {} bytes to Object Store with key: {}", out.size(), objectStoreKey);

                answerService.putIntoBucket(objectStoreKey, out.toByteArray());
            }

            return true;
        } catch (Exception e) {
            log.error("Unexpected error during SPDX creation", e);
            return false;
        }
    }

    private SpdxPackage createSpdxPackage(SpdxPackageEntity pkgEntity,
                                          Map<String, SpdxFileEntity> fileEntityMap,
                                          SpdxDocument spdxDocument,
                                          IModelStore modelStore,
                                          String documentUri,
                                          Project project,
                                          Map<String, SpdxElement> elementMap) {
        log.info("create pkg: {}", pkgEntity.getSpdxId());
        try {
            SpdxPackage.SpdxPackageBuilder builder = spdxDocument.createPackage(
                    pkgEntity.getSpdxId(),
                    pkgEntity.getName(),
                    LicenseInfoFactory.parseSPDXLicenseStringCompatV2(pkgEntity.getLicenseConcluded(), modelStore, documentUri, null),
                    pkgEntity.getCopyrightText(),
                    LicenseInfoFactory.parseSPDXLicenseStringCompatV2(pkgEntity.getLicenseDeclared(), modelStore, documentUri, null)
            );

            builder.setFilesAnalyzed(pkgEntity.isFilesAnalyzed());
            builder.setDownloadLocation(pkgEntity.getDownloadLocation());

            if (!pkgEntity.getVersionInfo().isEmpty()) builder.setVersionInfo(pkgEntity.getVersionInfo());
            if (!pkgEntity.getHomepage().isEmpty()) builder.setHomepage(pkgEntity.getHomepage());
            if (!pkgEntity.getSummary().isEmpty()) builder.setSummary(pkgEntity.getSummary());
            if (!pkgEntity.getDescription().isEmpty()) builder.setDescription(pkgEntity.getDescription());
            if (!pkgEntity.getOriginator().isEmpty()) builder.setOriginator(pkgEntity.getOriginator());
            if (!pkgEntity.getSupplier().isEmpty()) builder.setSupplier(pkgEntity.getSupplier());

            if (pkgEntity.isFilesAnalyzed() && pkgEntity.getPackageVerificationCode() != null) {
                SpdxPackageVerificationCode pkgVerificationCode = new SpdxPackageVerificationCode(modelStore, documentUri, SpdxConstantsCompatV2.CLASS_SPDX_VERIFICATIONCODE + pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue(), null, true);
                pkgVerificationCode.setValue(pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue());
                pkgVerificationCode.getExcludedFileNames().addAll(pkgEntity.getPackageVerificationCode().getPackageVerificationCodeExcludedFiles());
                builder.setPackageVerificationCode(pkgVerificationCode);
            }

            SpdxPackage spdxPackage = builder.build();

            for (ChecksumEntity checksumEntity : pkgEntity.getChecksums()) {
                spdxPackage.addChecksum(spdxPackage.createChecksum(ChecksumAlgorithm.valueOf(checksumEntity.getAlgorithm()), checksumEntity.getChecksumValue()));
            }

            for (ExternalRefEntity externalRefEntity : pkgEntity.getExternalRefs()) {
                String extRefId = SpdxConstantsCompatV2.CLASS_SPDX_EXTERNAL_REFERENCE + UUID.randomUUID();
                ExternalRef externalRef = new ExternalRef(modelStore, documentUri, extRefId, null, true);
                externalRef.setReferenceLocator(externalRefEntity.getReferenceLocator());
                externalRef.setReferenceType(new ReferenceType(externalRefEntity.getReferenceType()));
                externalRef.setReferenceCategory(ReferenceCategory.valueOf(externalRefEntity.getReferenceCategory()));
                externalRef.setComment(externalRefEntity.getComment());
                spdxPackage.addExternalRef(externalRef);
            }

            //TODO more auditing
            List<InventoryItem> inventoryItems = inventoryItemRepository.findBySpdxIdAndProject(pkgEntity.getSpdxId(), project);
            if (!inventoryItems.isEmpty() && inventoryItems.getFirst() != null) {
                spdxPackage.setComment(inventoryItems.getFirst().getExternalNotes());
            }

            if (pkgEntity.getFileNames() != null) {
                for (String fileSpdxId : pkgEntity.getFileNames()) {
                    SpdxFileEntity fileEntity = fileEntityMap.get(fileSpdxId);
                    if (fileEntity != null) {
                        SpdxFile spdxFile = createSpdxFile(fileEntity, spdxDocument);
                        if (spdxFile != null) {
                            spdxPackage.addFile(spdxFile);
                            elementMap.put(spdxFile.getId(), spdxFile);
                        }
                    }
                }
            }

            return spdxPackage;
        } catch (Exception e) {
            log.error("Error creating package {}", pkgEntity.getName(), e);
            return null;
        }
    }

    private SpdxFile createSpdxFile(SpdxFileEntity fileEntity, SpdxDocument spdxDocument) {
        log.info("creating file: {}", fileEntity.getSpdxId());
        IModelStore modelStore = spdxDocument.getModelStore();
        String documentUri = spdxDocument.getDocumentUri();
        try {
            List<AnyLicenseInfo> seenLicenses = new ArrayList<>();
            for (String license : fileEntity.getLicenseInfoInFiles()) {
                seenLicenses.add(LicenseInfoFactory.parseSPDXLicenseStringCompatV2(license, modelStore, documentUri, null));
            }

            if (fileEntity.getChecksums() == null || fileEntity.getChecksums().isEmpty()) {
                log.error("Skipping file {} (ID: {}): No checksums available.", fileEntity.getFileName(), fileEntity.getSpdxId());
                return null;
            }

            return spdxDocument.createSpdxFile(
                    fileEntity.getSpdxId(),
                    fileEntity.getFileName(),
                    LicenseInfoFactory.parseSPDXLicenseStringCompatV2(fileEntity.getLicenseConcluded(), modelStore, documentUri, null),
                    seenLicenses,
                    fileEntity.getCopyrightText(),
                    spdxDocument.createChecksum(
                            ChecksumAlgorithm.valueOf(fileEntity.getChecksums().getFirst().getAlgorithm()),
                            fileEntity.getChecksums().getFirst().getChecksumValue())
            ).build();
        } catch (Exception e) {
            log.error("Error creating SpdxFile: {}", fileEntity.getFileName(), e);
            return null;
        }
    }
}
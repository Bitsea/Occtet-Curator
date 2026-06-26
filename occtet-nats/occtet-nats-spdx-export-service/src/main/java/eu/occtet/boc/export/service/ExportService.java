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
import eu.occtet.boc.dao.SpdxDocumentRootRepository;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.spdxV2.*;
import eu.occtet.boc.model.SpdxExportWorkData;
import eu.occtet.boc.service.ProgressReportingService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Transactional
@Service
public class ExportService  extends ProgressReportingService  {

    private static final Logger log = LogManager.getLogger(ExportService.class);

    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    SpdxDocumentRootRepository spdxDocumentRootRepository;
    @Autowired
    AnswerService answerService;
    @Autowired
    private MergeService mergeService;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Value("${sbom.spdx.creators.tool.name}")
    private String toolName;

    public boolean process(SpdxExportWorkData spdxExportWorkData) {
        log.info("exporting SPDX!");
        return createDocument(spdxExportWorkData);
    }

    /**
     * Creates an SPDX document by processing project and document data, merging entity changes,
     * adding file elements, packages, relationships, and serializing the result to an output format.
     *
     * @param spdxExportWorkData Data object containing necessary parameters such as project ID,
     *                           SPDX document ID, object store key, and progress notification references.
     * @return {@code true} if the SPDX document creation and serialization were successful;
     *         {@code false} otherwise.
     */
    private boolean createDocument(SpdxExportWorkData spdxExportWorkData) {
        try {
            log.debug("creating SPDX document:");
            SpdxModelFactory.init();
            InMemSpdxStore modelStore = new InMemSpdxStore();
            MultiFormatStore jsonStore = new MultiFormatStore(modelStore, MultiFormatStore.Format.JSON_PRETTY);

            log.info("fetching project with id: {}", spdxExportWorkData.getProjectId());
            Optional<Project> projectOpt = projectRepository.findById(spdxExportWorkData.getProjectId());
            if (projectOpt.isEmpty()) {
                log.error("Project with id {} not found", spdxExportWorkData.getProjectId());
                return false;
            }
            notifyProgress(10, "Project fetched");

            Project project = projectOpt.get();

            log.info("fetching document with id: {}", spdxExportWorkData.getSpdxDocumentId());

            log.error("Document with id {} not found", spdxExportWorkData.getSpdxDocumentId());
            SpdxDocumentRoot spdxDocumentRoot = new SpdxDocumentRoot();
            spdxDocumentRoot.setPackages(new ArrayList<>());
            spdxDocumentRoot.setName(project.getProjectName());
            //generated documentId for new document
            spdxDocumentRoot.setDocumentUri("https://occtet.eu/spdx/" + project.getOrganization().getOrganizationName() + ":"
                    + project.getProjectName() + "_" + project.getVersion() + "_" + java.util.UUID.randomUUID());
            //this is the standard license for an spdx document!
            spdxDocumentRoot.setDataLicense("CC0-1.0");
            //standard document id
            spdxDocumentRoot.setSpdxId("SPDXRef-DOCUMENT");
            //this is the current spdx version we export
            spdxDocumentRoot.setSpdxVersion("2.3");

            project.setDocumentID(spdxDocumentRoot.getDocumentUri());
            project = projectRepository.save(project);


            log.info("Merging changes to document entities of project: {}", project.getProjectName());
            mergeService.mergeChangesToDocumentEntities(spdxDocumentRoot, project, spdxExportWorkData.getEnrichment());
            notifyProgress(20, "merged changes to document entities");

            Boolean enriched = spdxExportWorkData.getEnrichment();
            log.debug("LicenseText enrichment with Copyright: {}",enriched);

            String documentUri = spdxDocumentRoot.getDocumentUri();
            SpdxDocument spdxDocument = new SpdxDocument(modelStore, documentUri, null, true);

            Map<String, SpdxElement> elementMap = new HashMap<>();
            elementMap.put(spdxDocument.getId(), spdxDocument);
            elementMap.put("SPDXRef-DOCUMENT", spdxDocument);

            log.info("create creationInfo");
            addCreationInfo(spdxDocumentRoot, spdxDocument, project, modelStore, documentUri);
            notifyProgress(30,"created creationInfo");

            log.info("create extractedLicense info");
            addExtractedLicenseInfo(spdxDocumentRoot, spdxDocument, modelStore, documentUri);

            log.info("create ExternalDocumentRefs");
            addExternalRefs(spdxDocumentRoot, spdxDocument, modelStore, documentUri);
            notifyProgress(40,"created externalDocumentRefs");

            log.info("Creating file elements...");
            Set<String> processedFileIds = new HashSet<>();
            for (SpdxFileEntity fileEntity : spdxDocumentRoot.getFiles()) {
                if (!processedFileIds.add(fileEntity.getSpdxId())) continue;

                SpdxFile spdxFile = createSpdxFile(fileEntity, spdxDocument);
                if (spdxFile != null) {
                    elementMap.put(spdxFile.getId(), spdxFile);
                }
            }
            notifyProgress(50, "created file elements");

            log.debug("start converting packages");
            List<SpdxPackage> packages = new ArrayList<>();
            Set<String> processedPackageIds = new HashSet<>();

            for (SpdxPackageEntity pkgEntity : spdxDocumentRoot.getPackages()) {
                if (!processedPackageIds.add(pkgEntity.getSpdxId())) continue;

                SpdxPackage pkg = createSpdxPackage(pkgEntity, spdxDocument, modelStore, documentUri, elementMap);
                if (pkg != null) {
                    packages.add(pkg);
                    elementMap.put(pkg.getId(), pkg);
                }
            }
            log.info("finished converting packages, number of packages: {}", packages.size());

            notifyProgress(70, packages.size() + "packages created");

            addRelations(spdxDocumentRoot, modelStore, documentUri, elementMap);
            notifyProgress(90,"added relationships");

            // Reconstruct the DESCRIBES relationships required by SPDX 2.3
            log.info("create DESCRIBES relationships");
            addingDescribesRelationships(spdxDocument, packages);
            notifyProgress(95, "added DESCRIBES relationships");

            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 java.io.BufferedOutputStream bufferedOut = new java.io.BufferedOutputStream(out)
            ) {
                jsonStore.serialize(bufferedOut, spdxDocument);
                bufferedOut.flush();
                String objectStoreKey = spdxExportWorkData.getObjectStoreKey();

                log.info("Serialized SBOM JSON. Sending {} bytes to Object Store with key: {}", out.size(), objectStoreKey);

                answerService.putIntoBucket(objectStoreKey, out.toByteArray());
            }
            notifyProgress(100,"completed");
            return true;
        } catch (Exception e) {
            log.error("Unexpected error during SPDX creation", e);
            return false;
        }
    }

    private void addingDescribesRelationships(SpdxDocument spdxDocument,List<SpdxPackage> convertedPackages){
        if (convertedPackages == null || convertedPackages.isEmpty()) {
            log.warn("No packages available to be described by the document.");
            return;
        }

        log.info("create DESCRIBES relationships");
        IModelStore modelStore = spdxDocument.getModelStore();
        String documentUri = spdxDocument.getDocumentUri();

        for (SpdxPackage targetElement : convertedPackages) {
            try {

                String relId = SpdxConstantsCompatV2.SPDX_ELEMENT_REF_PRENUM + "Relationship-" + UUID.randomUUID();
                Relationship describesRel = new Relationship(modelStore, documentUri, relId, null, true);
                describesRel.setRelationshipType(RelationshipType.DESCRIBES);
                describesRel.setRelatedSpdxElement(targetElement);

                // Attach the relationship to the root document
                spdxDocument.addRelationship(describesRel);

                log.debug("Successfully added DESCRIBES relationship for active package: {}", targetElement.getId());
            } catch (InvalidSPDXAnalysisException e) {
                log.error("Error adding DESCRIBES relationship for: {}", targetElement.getId(), e);
            }
        }
    }

    private void addRelations(SpdxDocumentRoot spdxDocumentRoot, InMemSpdxStore modelStore, String documentUri, Map<String, SpdxElement> elementMap){
        log.info("Adding relationships from database entities...");
        spdxDocumentRoot.getRelationships().forEach(relationship -> {
            try {
                log.trace("relationship: {} to {}", relationship.getSpdxElementId(),
                        relationship.getRelatedSpdxElement());
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
    }

    private void addCreationInfo(SpdxDocumentRoot spdxDocumentRoot, SpdxDocument spdxDocument, Project project,InMemSpdxStore modelStore, String documentUri ) throws InvalidSPDXAnalysisException {
        String createAttributeTemplate = "%s: %s %s";
        CreationInfoEntity creationInfoEntity;
        if(spdxDocumentRoot.getCreationInfo()!= null)
            creationInfoEntity = spdxDocumentRoot.getCreationInfo();
        else creationInfoEntity= new CreationInfoEntity();
        spdxDocument.setCreationInfo(
                spdxDocument.createCreationInfo(
                        Arrays.asList(
                                String.format(createAttributeTemplate,
                                        "Person",
                                        project.getProjectContact(),
                                        project.getContactEmail() != null ?
                                                "(" + project.getContactEmail() + ")" : "()"
                                ),
                                String.format(createAttributeTemplate,
                                        "Organization",
                                        project.getOrganization().getOrganizationName(),
                                        project.getOrganization().getOrganizationEmail() != null ?
                                                "(" + project.getOrganization().getOrganizationEmail() + ")" : "()"
                                ),
                                String.format(createAttributeTemplate, "Tool", toolName, "")
                        ),
                        Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
        );
        Objects.requireNonNull(spdxDocument.getCreationInfo()).setLicenseListVersion(creationInfoEntity.getLicenseListVersion());
        spdxDocument.setSpecVersion(Version.CURRENT_SPDX_VERSION);
        spdxDocument.setName(spdxDocumentRoot.getName());
        spdxDocument.setDataLicense(LicenseInfoFactory.parseSPDXLicenseStringCompatV2(
                spdxDocumentRoot.getDataLicense(), modelStore, documentUri, null));
    }

    private void addExtractedLicenseInfo(SpdxDocumentRoot spdxDocumentRoot, SpdxDocument spdxDocument, InMemSpdxStore modelStore, String documentUri){
        spdxDocumentRoot.getHasExtractedLicensingInfos().forEach(extractedLicense -> {
            try {
                String rawLicenseId = extractedLicense.getLicenseId();
                if (rawLicenseId == null || rawLicenseId.isBlank()) {
                    return;
                }


                String spdxValidId = rawLicenseId;
                if (!spdxValidId.startsWith("LicenseRef-")) {
                    spdxValidId = "LicenseRef-" + spdxValidId;
                }


                ExtractedLicenseInfo extractedInfo = new ExtractedLicenseInfo(modelStore, documentUri, spdxValidId, null, true);

                String finalLicenseText = extractedLicense.getExtractedText();
                extractedInfo.setName(extractedLicense.getName() != null ? extractedLicense.getName() : "Custom License");

                extractedInfo.setExtractedText(finalLicenseText != null ? finalLicenseText : "No license text available.");
                spdxDocument.addExtractedLicenseInfos(extractedInfo);
            } catch (InvalidSPDXAnalysisException e) {
                log.error("Error adding extractedLicenseInfo", e);
            }
        });
    }

    private void addExternalRefs(SpdxDocumentRoot spdxDocumentRoot, SpdxDocument spdxDocument, InMemSpdxStore modelStore, String documentUri ){
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
    }

    /**
     * Creates an SPDX package based on the provided {@link SpdxPackageEntity} and related inputs.
     * This method constructs an {@link SpdxPackage} using the details from the given package entity,
     * SPDX document, and model store, while also handling various attributes such as license information,
     * checksums, external references, and included files.
     *
     * @param pkgEntity the entity containing package information to be converted into an {@link SpdxPackage}
     * @param spdxDocument the SPDX document to which the new package will be associated
     * @param modelStore the SPDX model store for persisting SPDX elements and properties
     * @param documentUri the unique URI of the SPDX document in which the package is created
     * @param elementMap a mapping of existing SPDX elements used to map file references to the package
     * @return the created {@link SpdxPackage} if successful, or null if an error occurs during creation
     */
    private SpdxPackage createSpdxPackage(SpdxPackageEntity pkgEntity,
                                          SpdxDocument spdxDocument,
                                          IModelStore modelStore,
                                          String documentUri,
                                          Map<String, SpdxElement> elementMap) {
        log.trace("create pkg: {}", pkgEntity.getSpdxId());
        try {
            SpdxPackage.SpdxPackageBuilder builder = spdxDocument.createPackage(
                    pkgEntity.getSpdxId(),
                    pkgEntity.getName(),
                    LicenseInfoFactory.parseSPDXLicenseStringCompatV2(pkgEntity.getLicenseConcluded(), modelStore, documentUri, null),
                    pkgEntity.getCopyrightText(),
                    LicenseInfoFactory.parseSPDXLicenseStringCompatV2(pkgEntity.getLicenseDeclared(), modelStore, documentUri, null)
            );

            boolean hasValidVerificationCode = pkgEntity.getPackageVerificationCode() != null &&
                    pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue() != null &&
                    !pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue().isEmpty();

            boolean safeFilesAnalyzed = pkgEntity.isFilesAnalyzed() && hasValidVerificationCode;
            builder.setFilesAnalyzed(safeFilesAnalyzed);

            builder.setDownloadLocation(pkgEntity.getDownloadLocation());

            if (!pkgEntity.getVersionInfo().isEmpty()) builder.setVersionInfo(pkgEntity.getVersionInfo());
            if (!pkgEntity.getHomepage().isEmpty()) builder.setHomepage(pkgEntity.getHomepage());
            if (!pkgEntity.getSummary().isEmpty()) builder.setSummary(pkgEntity.getSummary());
            if (!pkgEntity.getDescription().isEmpty()) builder.setDescription(pkgEntity.getDescription());
            if (!pkgEntity.getOriginator().isEmpty()) builder.setOriginator(pkgEntity.getOriginator());
            if (!pkgEntity.getSupplier().isEmpty()) builder.setSupplier(pkgEntity.getSupplier());

            if (hasValidVerificationCode) {
                SpdxPackageVerificationCode pkgVerificationCode = new SpdxPackageVerificationCode(modelStore, documentUri, SpdxConstantsCompatV2.CLASS_SPDX_VERIFICATIONCODE + pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue(), null, true);
                pkgVerificationCode.setValue(pkgEntity.getPackageVerificationCode().getPackageVerificationCodeValue());
                pkgVerificationCode.getExcludedFileNames().addAll(pkgEntity.getPackageVerificationCode().getPackageVerificationCodeExcludedFiles());
                builder.setPackageVerificationCode(pkgVerificationCode);
            }

            SpdxPackage spdxPackage = builder.build();

            Set<String> seenChecksums = new HashSet<>();
            for (ChecksumEntity checksumEntity : pkgEntity.getChecksums()) {
                String uniqueKey = checksumEntity.getAlgorithm() + ":" + checksumEntity.getChecksumValue();
                if (seenChecksums.add(uniqueKey)) {
                    spdxPackage.addChecksum(spdxPackage.createChecksum(ChecksumAlgorithm.valueOf(checksumEntity.getAlgorithm()), checksumEntity.getChecksumValue()));
                }
            }
            Set<String> seenRefs = new HashSet<>();
            for (ExternalRefEntity externalRefEntity : pkgEntity.getExternalRefs()) {
                String uniqueKey = externalRefEntity.getReferenceLocator();
                if (seenRefs.add(uniqueKey)) {
                    String extRefId = SpdxConstantsCompatV2.CLASS_SPDX_EXTERNAL_REFERENCE + UUID.randomUUID();
                    ExternalRef externalRef = new ExternalRef(modelStore, documentUri, extRefId, null, true);
                    externalRef.setReferenceLocator(externalRefEntity.getReferenceLocator());
                    externalRef.setReferenceType(new ReferenceType(externalRefEntity.getReferenceType()));
                    externalRef.setReferenceCategory(ReferenceCategory.valueOf(externalRefEntity.getReferenceCategory()));

                    if (externalRefEntity.getComment() != null && !externalRefEntity.getComment().isBlank()) {
                        externalRef.setComment(externalRefEntity.getComment());
                    }

                    spdxPackage.addExternalRef(externalRef);
                }
            }

            if (pkgEntity.getFileNames() != null) {
                log.trace("Start adding files to package: {},...", pkgEntity.getName());
                for (String fileSpdxId : pkgEntity.getFileNames()) {
                    SpdxElement cachedFile = elementMap.get(fileSpdxId);
                    if (cachedFile instanceof SpdxFile) {
                        spdxPackage.addFile((SpdxFile) cachedFile);
                    }
                }
                log.trace("finished adding files to package: {}, number of files added: {}", pkgEntity.getName(),
                        pkgEntity.getFileNames().size());
            }

            return spdxPackage;
        } catch (Exception e) {
            log.error("Error creating package {}", pkgEntity.getName(), e);
            return null;
        }
    }

    /**
     * Creates an SPDX file object based on the given SpdxFileEntity and SpdxDocument.
     *
     * @param fileEntity the entity containing details about the file, such as SPDX ID, file name,
     *                   license information, copyright, and checksum.
     * @param spdxDocument the SPDX document to which the file will be added; it provides the model
     *                     store and document URI for constructing the SPDX file.
     * @return the created SpdxFile object or null if an error occurs or the file cannot be created
     *         (e.g., due to missing checksum information).
     */
    private SpdxFile createSpdxFile(SpdxFileEntity fileEntity, SpdxDocument spdxDocument) {
        log.trace("creating file: {}", fileEntity.getSpdxId());
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
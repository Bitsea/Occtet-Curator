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

package eu.occtet.boc.spdx.converter;

import eu.occtet.boc.entity.spdxV2.*;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.*;
import org.spdx.library.model.v2.Annotation;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.SpdxSnippet;
import org.spdx.library.model.v2.enumerations.FileType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.spdx.library.model.v2.pointer.StartEndPointer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SpdxConverter {

    @Autowired
    SpdxDocumentRootRepository spdxDocumentRootRepository;

    private static final Logger log = LogManager.getLogger(SpdxConverter.class);

    /**
     * Converts the top-level metadata of an {@link SpdxDocument} into a persistent {@link SpdxDocumentRoot}.
     * <p>
     * <b>Upsert Logic:</b> This method attempts to find an existing document in the database using the
     * SPDX ID (defaulting to "SPDXRef-DOCUMENT" if missing).
     * <ul>
     * <li>If the document <b>exists</b>, it updates the mutable fields and clears child lists (such as external document references and extracted licensing infos) before repopulating them to avoid duplication.</li>
     * <li>If the document <b>does not exist</b>, a new entity is created.</li>
     * </ul>
     * </p>
     * <p>
     * This method handles global attributes like creation info, data license, and external document references.
     * It saves the entity to the {@link SpdxDocumentRootRepository} immediately.
     * </p>
     *
     * @param spdxDocument The source SPDX document model to be converted.
     * @return The persisted {@link SpdxDocumentRoot} entity (either newly created or updated).
     * @see SpdxDocumentRootRepository#findByDocumentUri(String) (String)
     */
    public SpdxDocumentRoot convertSpdxV2DocumentInformation(SpdxDocument spdxDocument) {
        log.info("converting document");

        try {
            String docId = spdxDocument.getId();
            if (docId == null || docId.trim().isEmpty()) {
                docId = "SPDXRef-DOCUMENT";
            }

            SpdxDocumentRoot spdxDocumentRoot = spdxDocumentRootRepository.findByDocumentUri(spdxDocument.getDocumentUri())
                    .orElse(new SpdxDocumentRoot());

            spdxDocumentRoot.setSpdxVersion(spdxDocument.getSpecVersion());
            spdxDocumentRoot.setSpdxId(docId);
            spdxDocumentRoot.setName(spdxDocument.getName().orElse(""));
            spdxDocumentRoot.setDataLicense(spdxDocument.getDataLicense().toString());
            spdxDocumentRoot.setComment(spdxDocument.getComment().orElse(""));
            spdxDocumentRoot.setDocumentUri(spdxDocument.getDocumentUri());

            if (spdxDocumentRoot.getExternalDocumentRefs() == null) {
                spdxDocumentRoot.setExternalDocumentRefs(new ArrayList<>());
            }
            List<ExternalDocumentRefEntity> refList = spdxDocumentRoot.getExternalDocumentRefs();
            refList.clear();

            for (ExternalDocumentRef ref : spdxDocument.getExternalDocumentRefs()) {
                ExternalDocumentRefEntity refEntity = new ExternalDocumentRefEntity();
                refEntity.setSpdxDocument(spdxDocumentRoot);
                refEntity.setExternalDocumentId(ref.getDocumentUri());
                if (ref.getSpdxDocument().isPresent())
                    refEntity.setSpdxDocumentExternal(ref.getSpdxDocument().get().getId());
                if (ref.getChecksum().isPresent()) {
                    ChecksumEntity checksumEntity = new ChecksumEntity();
                    checksumEntity.setAlgorithm(ref.getChecksum().get().getAlgorithm().toString());
                    checksumEntity.setChecksumValue(ref.getChecksum().get().getValue());
                    refEntity.setChecksum(checksumEntity);
                }
                refList.add(refEntity);
            }

            if (spdxDocumentRoot.getCreationInfo() == null) {
                spdxDocumentRoot.setCreationInfo(new CreationInfoEntity());
            }
            CreationInfoEntity creationInfoEntity = spdxDocumentRoot.getCreationInfo();
            SpdxCreatorInformation creationInfo = spdxDocument.getCreationInfo();
            creationInfoEntity.setComment(creationInfo.getComment().orElse(""));
            creationInfoEntity.setCreators(StringUtils.join(creationInfo.getCreators(),","));
            creationInfoEntity.setLicenseListVersion(creationInfo.getLicenseListVersion().orElse(""));
            creationInfoEntity.setCreated(creationInfo.getCreated());

            if (spdxDocumentRoot.getHasExtractedLicensingInfos() == null) {
                spdxDocumentRoot.setHasExtractedLicensingInfos(new ArrayList<>());
            } else {
                spdxDocumentRoot.getHasExtractedLicensingInfos().clear();
            }

            if (spdxDocumentRoot.getHasExtractedLicensingInfos() == null) {
                spdxDocumentRoot.setHasExtractedLicensingInfos(new ArrayList<>());
            }
            List<ExtractedLicensingInfoEntity> infoEntities = spdxDocumentRoot.getHasExtractedLicensingInfos();
            infoEntities.clear();

            for (ExtractedLicenseInfo extractedLicenseInfo : spdxDocument.getExtractedLicenseInfos()) {
                ExtractedLicensingInfoEntity infoEntity = new ExtractedLicensingInfoEntity();
                infoEntity.setComment(extractedLicenseInfo.getComment());
                infoEntity.setSpdxDocument(spdxDocumentRoot);
                infoEntity.setName(extractedLicenseInfo.getName());
                infoEntity.setExtractedText(extractedLicenseInfo.getExtractedText());
                infoEntity.setSeeAlsos(new ArrayList<>(extractedLicenseInfo.getSeeAlso()));
                infoEntity.setLicenseId(extractedLicenseInfo.getLicenseId());
                infoEntities.add(infoEntity);
            }

            return spdxDocumentRoot;

        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx document to entity: {}", e.toString());
            return new SpdxDocumentRoot(); // Fail-safe return
        }
    }

    /**
     * Converts an {@link SpdxPackage} model into a persistent {@link SpdxPackageEntity}.
     * <p>
     * <b>Upsert Logic:</b> Checks the provided {@code spdxDocumentRoot} to see if it already contains
     * a package with the same SPDX ID.
     * <ul>
     * <li>If found, the existing entity is updated. The child lists (ExternalRefs, Annotations, Checksums) are cleared and repopulated with the new data.</li>
     * <li>If not found, a new {@link SpdxPackageEntity} is created and added to the document's package list.</li>
     * </ul>
     * </p>
     * <p>
     * This method persists data such as package name, version, download location, verification codes,
     * and file names associated with the package.
     * </p>
     *
     * @param spdxPackage      The source SPDX package model to convert.
     * @param spdxDocumentRoot The parent {@link SpdxDocumentRoot} entity to which this package belongs.
     * @return The persisted {@link SpdxPackageEntity} (either newly created or updated).
     */
    public SpdxPackageEntity convertPackage(SpdxPackage spdxPackage,
                                            SpdxDocumentRoot spdxDocumentRoot,
                                            Map<String, SpdxPackageEntity> packageLookupMap) { // Added Map param
        log.info("converting package: {}", spdxPackage.getId());
        try {
            if (spdxDocumentRoot.getPackages() == null) {
                spdxDocumentRoot.setPackages(new ArrayList<>());
            }

            String spdxId = spdxPackage.getId();
            SpdxPackageEntity spdxPackageEntity = packageLookupMap.get(spdxId);

            boolean isNew = false;
            if (spdxPackageEntity == null) {
                spdxPackageEntity = new SpdxPackageEntity();
                isNew = true;
            }

            // Set standard fields
            spdxPackageEntity.setSpdxDocument(spdxDocumentRoot);
            spdxPackageEntity.setName(spdxPackage.getName().orElse(""));
            spdxPackageEntity.setSpdxId(spdxId);
            spdxPackageEntity.setVersionInfo(spdxPackage.getVersionInfo().orElse(""));
            spdxPackageEntity.setCopyrightText(spdxPackage.getCopyrightText());
            spdxPackageEntity.setDownloadLocation(spdxPackage.getDownloadLocation().orElse(""));
            spdxPackageEntity.setLicenseConcluded(spdxPackage.getLicenseConcluded().toString());
            spdxPackageEntity.setLicenseDeclared(spdxPackage.getLicenseDeclared().toString());
            spdxPackageEntity.setHomepage(spdxPackage.getHomepage().orElse(""));
            spdxPackageEntity.setSummary(spdxPackage.getSummary().orElse(""));
            spdxPackageEntity.setDescription(spdxPackage.getDescription().orElse(""));
            spdxPackageEntity.setOriginator(spdxPackage.getOriginator().orElse(""));
            spdxPackageEntity.setSupplier(spdxPackage.getSupplier().orElse(""));

            // Handle External Refs
            if (spdxPackageEntity.getExternalRefs() == null) {
                spdxPackageEntity.setExternalRefs(new ArrayList<>());
            }
            spdxPackageEntity.getExternalRefs().clear();
            for (ExternalRef externalRef : spdxPackage.getExternalRefs()) {
                ExternalRefEntity externalRefEntity = new ExternalRefEntity();
                externalRefEntity.setComment(externalRef.getComment().orElse(""));
                externalRefEntity.setPkg(spdxPackageEntity);
                externalRefEntity.setReferenceCategory(externalRef.getReferenceCategory().toString());
                externalRefEntity.setReferenceType(externalRef.getReferenceType().toString());
                externalRefEntity.setReferenceLocator(externalRef.getReferenceLocator());
                spdxPackageEntity.getExternalRefs().add(externalRefEntity);
            }

            // Handle Annotations
            if (spdxPackageEntity.getAnnotations() == null) {
                spdxPackageEntity.setAnnotations(new ArrayList<>());
            }
            spdxPackageEntity.getAnnotations().clear();
            for (Annotation annotation : spdxPackage.getAnnotations()) {
                AnnotationEntity annotationEntity = new AnnotationEntity();
                annotationEntity.setAnnotationDate(annotation.getAnnotationDate());
                annotationEntity.setComment(annotation.getComment());
                annotationEntity.setAnnotationType(annotation.getType());
                annotationEntity.setAnnotator(annotation.getAnnotator());
                annotationEntity.setSpdxElementId(annotation.getId());
                annotationEntity.setPkg(spdxPackageEntity);
                spdxPackageEntity.getAnnotations().add(annotationEntity);
            }

            // Handle Checksums
            if (spdxPackageEntity.getChecksums() == null) {
                spdxPackageEntity.setChecksums(new ArrayList<>());
            }
            spdxPackageEntity.getChecksums().clear();
            for (Checksum checksum : spdxPackage.getChecksums()) {
                ChecksumEntity checksumEntity = new ChecksumEntity();
                checksumEntity.setAlgorithm(checksum.getAlgorithm().toString());
                checksumEntity.setChecksumValue(checksum.getValue());
                checksumEntity.setPkg(spdxPackageEntity);
                spdxPackageEntity.getChecksums().add(checksumEntity);
            }

            // Handle License Info From Files
            List<String> licenseInfoFiles = new ArrayList<>();
            for (AnyLicenseInfo anyLicenseInfo : spdxPackage.getLicenseInfoFromFiles()) {
                licenseInfoFiles.add(anyLicenseInfo.toString());
            }
            spdxPackageEntity.setLicenseInfoFromFiles(licenseInfoFiles);

            PackageVerificationCodeEntity packageVerificationCodeEntity = getPackageVerificationCodeEntity(spdxPackage);
            spdxPackageEntity.setPackageVerificationCode(packageVerificationCodeEntity);

            List<String> fileNames = new ArrayList<>();
            spdxPackage.getFiles().forEach(spdxFile -> fileNames.add(spdxFile.getId()));
            spdxPackageEntity.setFileNames(fileNames);
            spdxPackageEntity.setFilesAnalyzed(spdxPackage.isFilesAnalyzed());

            if (isNew) {
                spdxDocumentRoot.getPackages().add(spdxPackageEntity);
                packageLookupMap.put(spdxId, spdxPackageEntity);
            }


            return spdxPackageEntity;

        } catch (Exception e) {
            log.error("error while converting spdx package to entity: {}", e.toString());
            return new SpdxPackageEntity();
        }
    }

    public Snippet convertSnippets(SpdxSnippet libSnippet, SpdxDocumentRoot parentDoc) {
        Snippet entity = new Snippet();
        try {
            entity.setSpdxDocument(parentDoc);
            entity.setSPDXID(libSnippet.getId());
            entity.setName(libSnippet.getName().orElse(null));
            entity.setCopyrightText(libSnippet.getCopyrightText());

            entity.setSnippetFromFile(libSnippet.getSnippetFromFile().getId());

            entity.setLicenseConcluded(libSnippet.getLicenseConcluded().toString());
            List<String> licenses = libSnippet.getLicenseInfoFromFiles().stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            entity.setLicenseInfoInSnippets(licenses);


            List<Range> entityRanges = getRanges(libSnippet);
            entity.setRanges(entityRanges);

        } catch (Exception e) {
            log.error("error while converting snippets: {}", e.toString());
            return null;
        }

        return entity;
    }

    private static List<Range> getRanges(SpdxSnippet libSnippet) throws InvalidSPDXAnalysisException {
        List<Range> entityRanges = new ArrayList<>();

        StartEndPointer byteRange = libSnippet.getByteRange();
        Range range = new Range();
        range.setType(byteRange.getType());
        range.setReference(byteRange.getStartPointer().getReference().getDocumentUri());
        entityRanges.add(range);

        Optional<StartEndPointer> lineRangeOpt = libSnippet.getLineRange();
        if (lineRangeOpt.isPresent()) {
            Range lineRange = new Range();
            lineRange.setType(lineRangeOpt.get().getType());
            lineRange.setReference(lineRangeOpt.get().getStartPointer().getDocumentUri());
            entityRanges.add(lineRange);
        }
        return entityRanges;
    }

    /**
     * Helper method to extract the package verification code from an {@link SpdxPackage}.
     * <p>
     * If the verification code is present in the model, it extracts the value and the list of excluded files.
     * If not present, it returns an entity with empty values.
     * </p>
     *
     * @param spdxPackage The source SPDX package model.
     * @return A {@link PackageVerificationCodeEntity} containing the code and excluded files (or empty defaults).
     * @throws InvalidSPDXAnalysisException If an error occurs while accessing SPDX model properties.
     */
    private PackageVerificationCodeEntity getPackageVerificationCodeEntity(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        PackageVerificationCodeEntity packageVerificationCodeEntity;
        if (spdxPackage.getPackageVerificationCode().isPresent()) {
            packageVerificationCodeEntity = new PackageVerificationCodeEntity();
            packageVerificationCodeEntity.setPackageVerificationCodeValue(spdxPackage.getPackageVerificationCode().get().getValue());
            List<String> excludedFiles = new ArrayList<>(spdxPackage.getPackageVerificationCode().get()
                    .getExcludedFileNames());
            packageVerificationCodeEntity.setPackageVerificationCodeExcludedFiles(excludedFiles);
        } else {
            packageVerificationCodeEntity = new PackageVerificationCodeEntity();
            packageVerificationCodeEntity.setPackageVerificationCodeValue("");
            packageVerificationCodeEntity.setPackageVerificationCodeExcludedFiles(new ArrayList<>());
        }
        return packageVerificationCodeEntity;
    }

    /**
     * Converts an {@link SpdxFile} model into a persistent {@link SpdxFileEntity}.
     * <p>
     * <b>Upsert Logic:</b> Checks the {@code spdxDocumentRoot} for an existing file with the same SPDX ID.
     * <ul>
     * <li>If found, updates the file's attributes and clears/repopulates checksums to prevent duplicates.</li>
     * <li>If not found, creates a new file entity and adds it to the document's file list.</li>
     * </ul>
     * </p>
     *
     * @param spdxFile         The source SPDX file model to convert.
     * @param spdxDocumentRoot The parent {@link SpdxDocumentRoot} entity to which this file belongs.
     * @return The persisted {@link SpdxFileEntity} (either newly created or updated).
     */
    public SpdxFileEntity convertFile(SpdxFile spdxFile, SpdxDocumentRoot spdxDocumentRoot) {
        try {

            if (spdxDocumentRoot.getFiles() == null) {
                spdxDocumentRoot.setFiles(new ArrayList<>());
            }

            log.info("now converting spdxFile {}", spdxFile.getId());

            SpdxFileEntity spdxFileEntity = spdxDocumentRoot.getFiles().stream()
                    .filter(f -> f.getSpdxId() != null && f.getSpdxId().equals(spdxFile.getId()))
                    .findFirst()
                    .orElse(null);

            boolean isNew = false;
            if (spdxFileEntity == null) {
                spdxFileEntity = new SpdxFileEntity();
                isNew = true;
            }

            spdxFileEntity.setFileName(spdxFile.getName().orElse(""));
            spdxFileEntity.setSpdxDocument(spdxDocumentRoot);
            spdxFileEntity.setSpdxId(spdxFile.getId());
            spdxFileEntity.setCopyrightText(spdxFile.getCopyrightText());
            spdxFileEntity.setLicenseConcluded(spdxFile.getLicenseConcluded().toString());
            log.debug("populated {}", spdxFileEntity);


            if (spdxFileEntity.getChecksums() == null) {
                spdxFileEntity.setChecksums(new ArrayList<>());
            }
            spdxFileEntity.getChecksums().clear();
            for (Checksum checksum : spdxFile.getChecksums()) {
                ChecksumEntity checksumEntity = new ChecksumEntity();
                checksumEntity.setAlgorithm(checksum.getAlgorithm().toString());
                checksumEntity.setChecksumValue(checksum.getValue());
                checksumEntity.setSpdxFile(spdxFileEntity);
                spdxFileEntity.getChecksums().add(checksumEntity);
            }

            List<String> fileTypes = new ArrayList<>();
            for (FileType fileType : spdxFile.getFileTypes()) {
                fileTypes.add(fileType.getLongName());
            }
            spdxFileEntity.setFileTypes(fileTypes);

            List<String> licenseInfoFromFiles = new ArrayList<>();
            for (AnyLicenseInfo licenseInfo : spdxFile.getLicenseInfoFromFiles()) {
                licenseInfoFromFiles.add(licenseInfo.toString());
            }
            spdxFileEntity.setLicenseInfoInFiles(licenseInfoFromFiles);

            if (isNew) {
                spdxDocumentRoot.getFiles().add(spdxFileEntity);
                log.debug("added new spdxFileEntity to documentRoot: {}", spdxFileEntity);
            }

            return spdxFileEntity;

        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx file to entity: {}", e.toString());
            return new SpdxFileEntity();
        }
    }

    /**
     * Converts an SPDX {@link Relationship} model into a persistent {@link RelationshipEntity}.
     * <p>
     * <b>Check for Existence:</b> Before creating a new relationship, this method checks if a relationship
     * already exists between the source element and the target element (or null target) with the same
     * relationship type.
     * <ul>
     * <li>If an exact match exists, the existing relationship is returned (no duplicate creation).</li>
     * <li>If no match exists, a new relationship entity is created, added to the document, and persisted.</li>
     * </ul>
     * </p>
     *
     * @param relationship     The source relationship model defining the link between SPDX elements.
     * @param spdxDocumentRoot The parent {@link SpdxDocumentRoot} entity.
     * @param spdxPackage      The package acting as the source of this relationship (used for the source ID).
     * @return The persisted {@link RelationshipEntity} (either newly created or the existing match).
     */
    public RelationshipEntity convertRelationShip(Relationship relationship, SpdxDocumentRoot spdxDocumentRoot, SpdxPackage spdxPackage) {
        // log.info("converting relationship");
        try {
            if (spdxDocumentRoot.getRelationships() == null) {
                spdxDocumentRoot.setRelationships(new ArrayList<>());
            }

            String targetId = relationship.getRelatedSpdxElement().isPresent() ? relationship.getRelatedSpdxElement().get().getId() : null;
            String type = relationship.getRelationshipType().toString();

            boolean exists = spdxDocumentRoot.getRelationships().stream()
                    .anyMatch(r -> r.getSpdxElementId().equals(spdxPackage.getId())
                            && ((r.getRelatedSpdxElement() == null && targetId == null) || (r.getRelatedSpdxElement() != null && r.getRelatedSpdxElement().equals(targetId)))
                            && r.getRelationshipType().equals(type));

            if (exists) {
                return spdxDocumentRoot.getRelationships().stream()
                        .filter(r -> r.getSpdxElementId().equals(spdxPackage.getId()) && r.getRelatedSpdxElement().equals(targetId) && r.getRelationshipType().equals(type))
                        .findFirst().orElse(null);
            }

            RelationshipEntity relationshipEntity = new RelationshipEntity();
            relationshipEntity.setSpdxDocument(spdxDocumentRoot);
            relationshipEntity.setComment(relationship.getComment().orElse(""));
            relationshipEntity.setSpdxElementId(spdxPackage.getId());
            if (targetId != null)
                relationshipEntity.setRelatedSpdxElement(targetId);
            relationshipEntity.setRelationshipType(type);

            spdxDocumentRoot.getRelationships().add(relationshipEntity);
            return relationshipEntity;
        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx relationship to entity: {}", e.toString());
            return new RelationshipEntity();
        }
    }
}
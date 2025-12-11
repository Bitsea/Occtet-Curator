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

package eu.occtet.boc.spdx.coverter;

import eu.occtet.boc.entity.spdxV2.*;
import eu.occtet.boc.entity.spdxV2.SpdxPackageEntity;
import eu.occtet.boc.spdx.dao.spdxV2.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.*;
import org.spdx.library.model.v2.Annotation;
import org.spdx.library.model.v2.ExternalRef;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.enumerations.FileType;
import org.spdx.library.model.v2.license.AnyLicenseInfo;
import org.spdx.library.model.v2.license.ExtractedLicenseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpdxConverter {

    @Autowired
    SpdxDocumentRootRepository spdxDocumentRootRepository;

    private static final Logger log = LogManager.getLogger(SpdxConverter.class);

    /**
     * Converts the top-level metadata of an {@link SpdxDocument} into a persistent {@link SpdxDocumentRoot}.
     * <p>
     * This method initializes the document root entity and processes global document attributes, including:
     * <ul>
     * <li><strong>Creation Information:</strong> Creators, timestamps, and tool versions.</li>
     * <li><strong>External Document References:</strong> Links to other SPDX documents and their checksums.</li>
     * <li><strong>Extracted Licensing Info:</strong> Non-standard licenses found within the document.</li>
     * </ul>
     * <p>
     * <strong>Note:</strong> This method focuses solely on document-level metadata. It does <em>not</em> convert
     * Package, File, or Relationship data; those must be handled by subsequent calls to specific conversion methods.
     * </p>
     *
     * @param spdxDocument The source SPDX document model to convert.
     * @return The newly created and persisted {@link SpdxDocumentRoot} entity.
     */
    public SpdxDocumentRoot convertSpdxV2DocumentInformation(SpdxDocument spdxDocument){
       log.info("converting document");
        SpdxDocumentRoot spdxDocumentRoot = new SpdxDocumentRoot();
        try {
            spdxDocumentRoot.setSpdxVersion(spdxDocument.getSpecVersion());
            String docId = spdxDocument.getId();
            if (docId == null || docId.trim().isEmpty()) {
                docId = "SPDXRef-DOCUMENT";
            }
            spdxDocumentRoot.setSpdxId(docId);
            spdxDocumentRoot.setName(spdxDocument.getName().orElse(""));
            spdxDocumentRoot.setDataLicense(spdxDocument.getDataLicense().toString());
            spdxDocumentRoot.setComment(spdxDocument.getComment().orElse(""));
            spdxDocumentRoot.setDocumentUri(spdxDocument.getDocumentUri());

            List<ExternalDocumentRefEntity> refList = new ArrayList<>();

            for(ExternalDocumentRef ref: spdxDocument.getExternalDocumentRefs()){
                ExternalDocumentRefEntity refEntity = new ExternalDocumentRefEntity();
                refEntity.setId(Long.valueOf(ref.getId()));
                refEntity.setSpdxDocument(spdxDocumentRoot);
                refEntity.setExternalDocumentId(ref.getDocumentUri());
                if(ref.getSpdxDocument().isPresent())
                    refEntity.setSpdxDocumentExternal(ref.getSpdxDocument().get().getId());
                if(ref.getChecksum().isPresent()) {
                    ChecksumEntity checksumEntity = new ChecksumEntity();
                    checksumEntity.setAlgorithm(ref.getChecksum().get().getAlgorithm().toString());
                    checksumEntity.setChecksumValue(ref.getChecksum().get().getValue());
                    refEntity.setChecksum(checksumEntity);
                }
                refList.add(refEntity);
            }
            spdxDocumentRoot.setExternalDocumentRefs(refList);

            CreationInfoEntity creationInfoEntity = new CreationInfoEntity();
            SpdxCreatorInformation creationInfo = spdxDocument.getCreationInfo();
            creationInfoEntity.setComment(creationInfo.getComment().orElse(""));
            creationInfoEntity.setCreators(new ArrayList<>(creationInfo.getCreators()));
            creationInfoEntity.setLicenseListVersion(creationInfo.getLicenseListVersion().orElse(""));
            creationInfoEntity.setCreated(creationInfo.getCreated());
            spdxDocumentRoot.setCreationInfo(creationInfoEntity);

            List<ExtractedLicensingInfoEntity> infoEntities = new ArrayList<>();
            for(ExtractedLicenseInfo extractedLicenseInfo: spdxDocument.getExtractedLicenseInfos()){
                ExtractedLicensingInfoEntity infoEntity = new ExtractedLicensingInfoEntity();
                infoEntity.setComment(extractedLicenseInfo.getComment());
                infoEntity.setSpdxDocument(spdxDocumentRoot);
                infoEntity.setName(extractedLicenseInfo.getName());
                infoEntity.setExtractedText(extractedLicenseInfo.getExtractedText());
                infoEntity.setSeeAlsos(new ArrayList<>(extractedLicenseInfo.getSeeAlso()));
                infoEntity.setLicenseId(extractedLicenseInfo.getLicenseId());
                //TODO implement this
                //infoEntity.setCrossRefs();
                infoEntities.add(infoEntity);
            }
            spdxDocumentRoot.setHasExtractedLicensingInfos(infoEntities);

            //TODO implement this
            //spdxDocumentRoot.setSnippets();

            spdxDocumentRootRepository.save(spdxDocumentRoot);
        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx document to entity: {}", e.toString());
        }
        return spdxDocumentRoot;
    }

    /**
     * Converts an {@link SpdxPackage} model into a persistent {@link SpdxPackageEntity} entity.
     * <p>
     * This method maps core package attributes (Name, ID, Version, License) and persists
     * associated child entities including External References, Annotations, Checksums,
     * and Verification Codes. It also triggers the conversion of associated relationships.
     * </p>
     *
     * @param spdxPackage      The source SPDX package model to convert.
     * @param spdxDocumentRoot The parent database entity representing the SPDX document
     * that this package belongs to.
     * @return The newly created and persisted {@link SpdxPackageEntity} entity.
     */
    public SpdxPackageEntity convertPackage(SpdxPackage spdxPackage, SpdxDocumentRoot spdxDocumentRoot){
        log.info("converting package");
        SpdxPackageEntity spdxPackageEntity = new SpdxPackageEntity();
        try {
            spdxPackageEntity.setSpdxDocument(spdxDocumentRoot);
            spdxPackageEntity.setName(spdxPackage.getName().orElse(""));
            spdxPackageEntity.setSpdxId(spdxPackage.getId());
            spdxPackageEntity.setVersionInfo(spdxPackage.getVersionInfo().orElse(""));
            spdxPackageEntity.setCopyrightText(spdxPackage.getCopyrightText());
            spdxPackageEntity.setDownloadLocation(spdxPackage.getDownloadLocation().orElse(""));
            spdxPackageEntity.setLicenseConcluded(spdxPackage.getLicenseConcluded().toString());
            spdxPackageEntity.setLicenseDeclared(spdxPackage.getLicenseDeclared().toString());

            List<ExternalRefEntity> externalRefEntities = new ArrayList<>();
            for(ExternalRef externalRef: spdxPackage.getExternalRefs()){
                ExternalRefEntity externalRefEntity = new ExternalRefEntity();
                externalRefEntity.setComment(externalRef.getComment().orElse(""));
                externalRefEntity.setPkg(spdxPackageEntity);
                externalRefEntity.setReferenceCategory(externalRef.getReferenceCategory().toString());
                externalRefEntity.setReferenceType(externalRef.getReferenceType().toString());
                externalRefEntity.setReferenceLocator(externalRef.getReferenceLocator());
                externalRefEntities.add(externalRefEntity);
            }
            spdxPackageEntity.setExternalRefs(externalRefEntities);
            List<AnnotationEntity> annotationEntities = new ArrayList<>();
            for(Annotation annotation: spdxPackage.getAnnotations()){
                AnnotationEntity annotationEntity = new AnnotationEntity();
                annotationEntity.setAnnotationDate(annotation.getAnnotationDate());
                annotationEntity.setComment(annotation.getComment());
                annotationEntity.setAnnotationType(annotation.getType());
                annotationEntity.setAnnotator(annotation.getAnnotator());
                annotationEntity.setSpdxElementId(annotation.getId());
                annotationEntities.add(annotationEntity);
            }
            spdxPackageEntity.setAnnotations(annotationEntities);
            List<ChecksumEntity> checksumEntities = new ArrayList<>();
            for(Checksum checksum: spdxPackage.getChecksums()){
                ChecksumEntity checksumEntity = new ChecksumEntity();
                checksumEntity.setAlgorithm(checksum.getAlgorithm().toString());
                checksumEntity.setChecksumValue(checksum.getValue());
                checksumEntity.setPkg(spdxPackageEntity);
                checksumEntities.add(checksumEntity);
            }
            spdxPackageEntity.setChecksums(checksumEntities);

            List<String> licenseInfoFiles = new ArrayList<>();
            for(AnyLicenseInfo anyLicenseInfo: spdxPackage.getLicenseInfoFromFiles()){
                licenseInfoFiles.add(anyLicenseInfo.toString());
            }
            spdxPackageEntity.setLicenseInfoFromFiles(licenseInfoFiles);

            PackageVerificationCodeEntity packageVerificationCodeEntity = getPackageVerificationCodeEntity(spdxPackage);
            spdxPackageEntity.setPackageVerificationCode(packageVerificationCodeEntity);

            List<String> fileNames = new ArrayList<>();
            spdxPackage.getFiles().forEach(
                    spdxFile -> fileNames.add(spdxFile.getId())
            );
            spdxPackageEntity.setFileNames(fileNames);

            spdxPackageEntity.setSpdxDocument(spdxDocumentRoot);
            if (spdxDocumentRoot.getPackages() == null) {
                spdxDocumentRoot.setPackages(new ArrayList<>());
            }

            spdxPackageEntity.setFilesAnalyzed(spdxPackage.isFilesAnalyzed());
            spdxDocumentRoot.getPackages().add(spdxPackageEntity);
            spdxDocumentRootRepository.save(spdxDocumentRoot);
            return spdxPackageEntity;

        } catch (Exception e) {
            log.error("error while converting spdx package to entity: {}", e.toString());
            return spdxPackageEntity;
        }
    }

    private PackageVerificationCodeEntity getPackageVerificationCodeEntity(SpdxPackage spdxPackage) throws InvalidSPDXAnalysisException {
        PackageVerificationCodeEntity packageVerificationCodeEntity;
        if(spdxPackage.getPackageVerificationCode().isPresent()) {
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
     * This method extracts file metadata, including checksums, file types, and
     * license information found within the file. The resulting entity is saved
     * to the repository and linked to the provided document root.
     * </p>
     *
     * @param spdxFile         The source SPDX file model to convert.
     * @param spdxDocumentRoot The parent database entity representing the SPDX document
     * that this file belongs to.
     * @return The newly created and persisted {@link SpdxFileEntity} entity.
     */
    public SpdxFileEntity convertFile(SpdxFile spdxFile, SpdxDocumentRoot spdxDocumentRoot){
        log.info("converting file");
        SpdxFileEntity spdxFileEntity = new SpdxFileEntity();
        try {
            spdxFileEntity.setFileName(spdxFile.getName().orElse(""));
            spdxFileEntity.setSpdxDocument(spdxDocumentRoot);
            spdxFileEntity.setSpdxId(spdxFile.getId());
            spdxFileEntity.setCopyrightText(spdxFile.getCopyrightText());
            spdxFileEntity.setLicenseConcluded(spdxFile.getLicenseConcluded().toString());

            List<ChecksumEntity> checksumEntities = new ArrayList<>();
            for(Checksum checksum: spdxFile.getChecksums()){
                ChecksumEntity checksumEntity = new ChecksumEntity();
                checksumEntity.setAlgorithm(checksum.getAlgorithm().toString());
                checksumEntity.setChecksumValue(checksum.getValue());
                checksumEntities.add(checksumEntity);
                checksumEntity.setSpdxFile(spdxFileEntity);
            }
            spdxFileEntity.setChecksums(checksumEntities);

            List<String> fileTypes = new ArrayList<>();
            for(FileType fileType: spdxFile.getFileTypes()){
                fileTypes.add(fileType.getLongName());
            }
            spdxFileEntity.setFileTypes(fileTypes);

            List<String> licenseInfoFromFiles = new ArrayList<>();
            for(AnyLicenseInfo licenseInfo: spdxFile.getLicenseInfoFromFiles()){
                licenseInfoFromFiles.add(licenseInfo.toString());
            }
            spdxFileEntity.setLicenseInfoInFiles(licenseInfoFromFiles);

            spdxDocumentRoot.getFiles().add(spdxFileEntity);
            spdxDocumentRootRepository.save(spdxDocumentRoot);
            return spdxFileEntity;

        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx file to entity: {}", e.toString());
            return spdxFileEntity;
        }

    }

    /**
     * Converts an SPDX {@link Relationship} model into a persistent {@link RelationshipEntity}.
     * <p>
     * This method captures the relationship type (e.g., DESCRIBES, CONTAINS), the
     * related element IDs, and any associated comments, persisting them to the
     * relationship repository.
     * </p>
     *
     * @param relationship     The source relationship model defining the link between SPDX elements.
     * @param spdxDocumentRoot The parent database entity representing the SPDX document
     * context for this relationship.
     * @return The newly created and persisted {@link RelationshipEntity} entity.
     */
    public RelationshipEntity convertRelationShip(Relationship relationship, SpdxDocumentRoot spdxDocumentRoot, SpdxPackage spdxPackage){
        log.info("converting relationship");
        RelationshipEntity relationshipEntity = new RelationshipEntity();
        try {
            relationshipEntity.setSpdxDocument(spdxDocumentRoot);
            relationshipEntity.setComment(relationship.getComment().orElse(""));
            relationshipEntity.setSpdxElementId(spdxPackage.getId());
            if(relationship.getRelatedSpdxElement().isPresent())
                relationshipEntity.setRelatedSpdxElement(relationship.getRelatedSpdxElement().get().getId());
            relationshipEntity.setRelationshipType(relationship.getRelationshipType().toString());

            spdxDocumentRoot.getRelationships().add(relationshipEntity);
            spdxDocumentRootRepository.save(spdxDocumentRoot);
            return relationshipEntity;
        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx relationship to entity: {}", e.toString());
            return relationshipEntity;
        }
    }
}
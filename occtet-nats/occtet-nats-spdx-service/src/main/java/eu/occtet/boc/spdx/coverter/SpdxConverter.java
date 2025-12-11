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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SpdxConverter {

    @Autowired
    SpdxDocumentRootRepository spdxDocumentRootRepository;

    private static final Logger log = LogManager.getLogger(SpdxConverter.class);

    public SpdxDocumentRoot convertSpdxV2DocumentInformation(SpdxDocument spdxDocument){
        log.info("converting document");

        try {
            String docId = spdxDocument.getId();
            if (docId == null || docId.trim().isEmpty()) {
                docId = "SPDXRef-DOCUMENT";
            }

            SpdxDocumentRoot spdxDocumentRoot = spdxDocumentRootRepository.findBySpdxId(docId)
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

            for(ExternalDocumentRef ref: spdxDocument.getExternalDocumentRefs()){
                ExternalDocumentRefEntity refEntity = new ExternalDocumentRefEntity();
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

            if (spdxDocumentRoot.getCreationInfo() == null) {
                spdxDocumentRoot.setCreationInfo(new CreationInfoEntity());
            }
            CreationInfoEntity creationInfoEntity = spdxDocumentRoot.getCreationInfo();
            SpdxCreatorInformation creationInfo = spdxDocument.getCreationInfo();
            creationInfoEntity.setComment(creationInfo.getComment().orElse(""));
            creationInfoEntity.setCreators(new ArrayList<>(creationInfo.getCreators()));
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

            for(ExtractedLicenseInfo extractedLicenseInfo: spdxDocument.getExtractedLicenseInfos()){
                ExtractedLicensingInfoEntity infoEntity = new ExtractedLicensingInfoEntity();
                infoEntity.setComment(extractedLicenseInfo.getComment());
                infoEntity.setSpdxDocument(spdxDocumentRoot);
                infoEntity.setName(extractedLicenseInfo.getName());
                infoEntity.setExtractedText(extractedLicenseInfo.getExtractedText());
                infoEntity.setSeeAlsos(new ArrayList<>(extractedLicenseInfo.getSeeAlso()));
                infoEntity.setLicenseId(extractedLicenseInfo.getLicenseId());
                infoEntities.add(infoEntity);
            }

            spdxDocumentRootRepository.save(spdxDocumentRoot);
            return spdxDocumentRoot;

        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx document to entity: {}", e.toString());
            return new SpdxDocumentRoot(); // Fail-safe return
        }
    }

    public SpdxPackageEntity convertPackage(SpdxPackage spdxPackage, SpdxDocumentRoot spdxDocumentRoot){
        log.info("converting package");
        try {
            if (spdxDocumentRoot.getPackages() == null) {
                spdxDocumentRoot.setPackages(new ArrayList<>());
            }

            SpdxPackageEntity spdxPackageEntity = spdxDocumentRoot.getPackages().stream()
                    .filter(p -> p.getSpdxId() != null && p.getSpdxId().equals(spdxPackage.getId()))
                    .findFirst()
                    .orElse(null);

            boolean isNew = false;
            if (spdxPackageEntity == null) {
                spdxPackageEntity = new SpdxPackageEntity();
                isNew = true;
            }

            spdxPackageEntity.setSpdxDocument(spdxDocumentRoot);
            spdxPackageEntity.setName(spdxPackage.getName().orElse(""));
            spdxPackageEntity.setSpdxId(spdxPackage.getId());
            spdxPackageEntity.setVersionInfo(spdxPackage.getVersionInfo().orElse(""));
            spdxPackageEntity.setCopyrightText(spdxPackage.getCopyrightText());
            spdxPackageEntity.setDownloadLocation(spdxPackage.getDownloadLocation().orElse(""));
            spdxPackageEntity.setLicenseConcluded(spdxPackage.getLicenseConcluded().toString());
            spdxPackageEntity.setLicenseDeclared(spdxPackage.getLicenseDeclared().toString());


            if(spdxPackageEntity.getExternalRefs() == null) {
                spdxPackageEntity.setExternalRefs(new ArrayList<>());
            }
            spdxPackageEntity.getExternalRefs().clear();
            for(ExternalRef externalRef: spdxPackage.getExternalRefs()){
                ExternalRefEntity externalRefEntity = new ExternalRefEntity();
                externalRefEntity.setComment(externalRef.getComment().orElse(""));
                externalRefEntity.setPkg(spdxPackageEntity);
                externalRefEntity.setReferenceCategory(externalRef.getReferenceCategory().toString());
                externalRefEntity.setReferenceType(externalRef.getReferenceType().toString());
                externalRefEntity.setReferenceLocator(externalRef.getReferenceLocator());
                spdxPackageEntity.getExternalRefs().add(externalRefEntity);
            }

            if(spdxPackageEntity.getAnnotations() == null) {
                spdxPackageEntity.setAnnotations(new ArrayList<>());
            }
            spdxPackageEntity.getAnnotations().clear();

            for(Annotation annotation: spdxPackage.getAnnotations()){
                AnnotationEntity annotationEntity = new AnnotationEntity();
                annotationEntity.setAnnotationDate(annotation.getAnnotationDate());
                annotationEntity.setComment(annotation.getComment());
                annotationEntity.setAnnotationType(annotation.getType());
                annotationEntity.setAnnotator(annotation.getAnnotator());
                annotationEntity.setSpdxElementId(annotation.getId());
                annotationEntity.setPkg(spdxPackageEntity);
                spdxPackageEntity.getAnnotations().add(annotationEntity);
            }

            if(spdxPackageEntity.getChecksums() == null) {
                spdxPackageEntity.setChecksums(new ArrayList<>());
            }
            spdxPackageEntity.getChecksums().clear();

            for(Checksum checksum: spdxPackage.getChecksums()){
                ChecksumEntity checksumEntity = new ChecksumEntity();
                checksumEntity.setAlgorithm(checksum.getAlgorithm().toString());
                checksumEntity.setChecksumValue(checksum.getValue());
                checksumEntity.setPkg(spdxPackageEntity);
                spdxPackageEntity.getChecksums().add(checksumEntity);
            }

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

            spdxPackageEntity.setFilesAnalyzed(spdxPackage.isFilesAnalyzed());

            if (isNew) {
                spdxDocumentRoot.getPackages().add(spdxPackageEntity);
            }

            spdxDocumentRootRepository.save(spdxDocumentRoot);
            return spdxPackageEntity;

        } catch (Exception e) {
            log.error("error while converting spdx package to entity: {}", e.toString());
            return new SpdxPackageEntity();
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

    public SpdxFileEntity convertFile(SpdxFile spdxFile, SpdxDocumentRoot spdxDocumentRoot){
        log.info("converting file");
        try {
            if (spdxDocumentRoot.getFiles() == null) {
                spdxDocumentRoot.setFiles(new ArrayList<>());
            }

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


            if(spdxFileEntity.getChecksums() == null) {
                spdxFileEntity.setChecksums(new ArrayList<>());
            }
            spdxFileEntity.getChecksums().clear();
            for(Checksum checksum: spdxFile.getChecksums()){
                ChecksumEntity checksumEntity = new ChecksumEntity();
                checksumEntity.setAlgorithm(checksum.getAlgorithm().toString());
                checksumEntity.setChecksumValue(checksum.getValue());
                checksumEntity.setSpdxFile(spdxFileEntity);
                spdxFileEntity.getChecksums().add(checksumEntity);
            }

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

            if (isNew) {
                spdxDocumentRoot.getFiles().add(spdxFileEntity);
            }

            spdxDocumentRootRepository.save(spdxDocumentRoot);
            return spdxFileEntity;

        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx file to entity: {}", e.toString());
            return new SpdxFileEntity();
        }
    }

    public RelationshipEntity convertRelationShip(Relationship relationship, SpdxDocumentRoot spdxDocumentRoot, SpdxPackage spdxPackage){
        log.info("converting relationship");
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
            if(targetId != null)
                relationshipEntity.setRelatedSpdxElement(targetId);
            relationshipEntity.setRelationshipType(type);

            spdxDocumentRoot.getRelationships().add(relationshipEntity);
            spdxDocumentRootRepository.save(spdxDocumentRoot);
            return relationshipEntity;
        } catch (InvalidSPDXAnalysisException e) {
            log.error("error while converting spdx relationship to entity: {}", e.toString());
            return new RelationshipEntity();
        }
    }
}
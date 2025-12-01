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

package eu.occtet.boc.spdx.utlities;

import eu.occtet.boc.entity.spdxV2.*;
import eu.occtet.boc.entity.spdxV2.Package;
import eu.occtet.boc.spdx.coverter.SpdxConverter;
import eu.occtet.boc.spdx.dao.spdxV2.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.SpdxPackage;
import org.spdx.storage.simple.InMemSpdxStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        SpdxConverter.class, ExternalDocumentRefRepository.class, ChecksumRepository.class, CreationInfoRepository.class,
        ExtractedLicensingInfoRepository.class, SpdxDocumentRootRepository.class, ExternalRefRepository.class,
        AnnotationRepository.class, PackageRepository.class, SpdxFileRepository.class, RelationshipRepository.class

})
@EnableJpaRepositories(basePackages = {
        "eu.occtet.boc.spdx.dao"
} )
@EntityScan(basePackages = {
        "eu.occtet.boc.entity"
})
@ExtendWith(MockitoExtension.class)
public class SpdxConverterTest {

    @MockitoBean
    private SpdxDocumentRootRepository spdxDocumentRootRepository;

    @Autowired
    SpdxConverter spdxConverter;

    SpdxDocument spdxDocument;

    private static final Logger log = LogManager.getLogger(SpdxConverterTest.class);

    @BeforeEach
    public void setUp(){
        try {
            // setup for spdx library need to be called once before any spdx model objects are accessed
            SpdxModelFactory.init();
            InMemSpdxStore modelStore = new InMemSpdxStore();
            MultiFormatStore inputStore = new MultiFormatStore(modelStore, MultiFormatStore.Format.JSON);

            byte[] bytes = Thread.currentThread().getContextClassLoader().getResourceAsStream("synthetic-scan-result-expected-output.spdx.json").readAllBytes();

            InputStream inputStream = new ByteArrayInputStream(bytes);
            spdxDocument = inputStore.deSerialize(inputStream, false);
        }catch (Exception e){
            Assertions.fail("Error occurred during setup");
        }
    }

    @Test
    public void convertDocument(){
        Mockito.when(spdxDocumentRootRepository.save(Mockito.any()))
                .thenReturn(new SpdxDocumentRoot());

        SpdxDocumentRoot spdxDocumentRoot = spdxConverter.convertSpdxV2DocumentInformation(spdxDocument);
        //Check correctness of base info
        Assertions.assertEquals("SPDX-2.3", spdxDocumentRoot.getSpdxVersion());
        Assertions.assertEquals("SPDXRef-DOCUMENT", spdxDocumentRoot.getSPDXID());
        Assertions.assertEquals("some document name", spdxDocumentRoot.getName());
        Assertions.assertEquals("CC0-1.0" ,spdxDocumentRoot.getDataLicense());
        Assertions.assertEquals("some document comment", spdxDocumentRoot.getComment());
        Assertions.assertEquals("<REPLACE_DOCUMENT_NAMESPACE>", spdxDocumentRoot.getDocumentUri());

        //Check externalDocumentRefs
        Assertions.assertTrue(spdxDocumentRoot.getExternalDocumentRefs().isEmpty());

        //Check correctness of creation info
        CreationInfoEntity creationInfoEntity = spdxDocumentRoot.getCreationInfo();
        Assertions.assertNotNull(creationInfoEntity);
        Assertions.assertEquals("some creation info comment", creationInfoEntity.getComment());
        Assertions.assertFalse(creationInfoEntity.getCreators().isEmpty());
        Assertions.assertEquals("Person: some creation info person", creationInfoEntity.getCreators().get(0));
        Assertions.assertEquals("Organization: some creation info organization", creationInfoEntity.getCreators().get(1));
        Assertions.assertEquals("Tool: ort-<REPLACE_ORT_VERSION>", creationInfoEntity.getCreators().get(2));
        Assertions.assertEquals("<REPLACE_LICENSE_LIST_VERSION>", creationInfoEntity.getLicenseListVersion());
        Assertions.assertEquals("<REPLACE_CREATION_DATE_AND_TIME>", creationInfoEntity.getCreated());


        //Check externalLicensingInfos
        List<ExtractedLicensingInfoEntity> infoEntities = spdxDocumentRoot.getHasExtractedLicensingInfos();
        Assertions.assertFalse(infoEntities.isEmpty());
        ExtractedLicensingInfoEntity infoEntity = infoEntities.getFirst();
        Assertions.assertEquals("", infoEntity.getComment());
        Assertions.assertEquals("To anyone who acknowledges that the file \"sRGB Color Space Profile.icm\" \nis provided \"AS IS\" WITH NO EXPRESS OR IMPLIED WARRANTY:\npermission to use, copy and distribute this file for any purpose is hereby \ngranted without fee, provided that the file is not changed including the HP \ncopyright notice tag, and that the name of Hewlett-Packard Company not be \nused in advertising or publicity pertaining to distribution of the software \nwithout specific, written prior permission. Hewlett-Packard Company makes \nno representations about the suitability of this software for any purpose.",
                infoEntity.getExtractedText());
        Assertions.assertEquals("LicenseRef-scancode-srgb", infoEntity.getLicenseId());
    }

    @Test
    public void convertPackage(){
        try {
            Mockito.when(spdxDocumentRootRepository.save(Mockito.any()))
                    .thenReturn(new SpdxDocumentRoot());

            SpdxDocumentRoot spdxDocumentRoot = new SpdxDocumentRoot();
            SpdxPackage spdxPackage = (SpdxPackage) spdxDocument.getDocumentDescribes().toArray()[0];

            Package pkg = spdxConverter.convertPackage(spdxPackage, spdxDocumentRoot);

            Assertions.assertEquals(spdxDocumentRoot, pkg.getSpdxDocument());
            Assertions.assertEquals("proj1", pkg.getName());
            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1", pkg.getSPDXID());
            Assertions.assertEquals("0.0.1", pkg.getVersionInfo());
            Assertions.assertEquals("https://github.com/path/proj1-repo.git", pkg.getDownloadLocation());
            Assertions.assertEquals("NONE", pkg.getCopyrightText());
            Assertions.assertEquals("NOASSERTION", pkg.getLicenseConcluded());
            Assertions.assertEquals("MIT", pkg.getLicenseDeclared());
            Assertions.assertEquals(spdxDocumentRoot, pkg.getSpdxDocument());
            Assertions.assertFalse(spdxDocumentRoot.getPackages().isEmpty());

        } catch (Exception e) {
            Assertions.fail("An error occurred: " + e);
        }
    }

    @Test
    public void convertFileTest(){
        try {
            Mockito.when(spdxDocumentRootRepository.save(Mockito.any()))
                    .thenReturn(new SpdxDocumentRoot());

            SpdxDocumentRoot spdxDocumentRoot = new SpdxDocumentRoot();
            SpdxPackage spdxPackage = (SpdxPackage) spdxDocument.getDocumentDescribes().toArray()[0];
            SpdxFile spdxFile = (SpdxFile) spdxPackage.getFiles().toArray()[0];
            SpdxFileEntity spdxFileEntity = spdxConverter.convertFile(spdxFile, spdxDocumentRoot);

            Assertions.assertEquals("SPDXRef-File-4", spdxFileEntity.getSPDXID());
            Assertions.assertEquals("LICENSE", spdxFileEntity.getFileName());
            Assertions.assertEquals("NONE", spdxFileEntity.getCopyrightText());
            Assertions.assertEquals("NOASSERTION", spdxFileEntity.getLicenseConcluded());
            Assertions.assertEquals("GPL-2.0-only WITH NOASSERTION", spdxFileEntity.getLicenseInfoInFiles().getFirst());
            Assertions.assertEquals(spdxDocumentRoot,spdxFileEntity.getSpdxDocument());
            Assertions.assertFalse(spdxDocumentRoot.getFiles().isEmpty());

            ChecksumEntity checksum = spdxFileEntity.getChecksums().getFirst();
            Assertions.assertEquals("SHA1", checksum.getAlgorithm());
            Assertions.assertEquals("0398ccd0f49298b10a3d76a47800d2ebecd49859", checksum.getChecksumValue());

        } catch (Exception e) {
            Assertions.fail("An error occurred: " + e);
        }
    }

    @Test
    public void convertRelationshipTest(){
        try {
            Mockito.when(spdxDocumentRootRepository.save(Mockito.any()))
                    .thenReturn(new SpdxDocumentRoot());

            SpdxDocumentRoot spdxDocumentRoot = new SpdxDocumentRoot();
            Relationship relationship = (Relationship) spdxDocument.getRelationships().toArray()[0];

            SpdxPackage spdxPackage = new SpdxPackage("SPDXRef-Package-Go-gopkg.in.yaml.v3-3.0.1");
            RelationshipEntity relationshipEntity = spdxConverter.convertRelationShip(relationship, spdxDocumentRoot, spdxPackage);

            Assertions.assertEquals("SPDXRef-Package-Go-gopkg.in.yaml.v3-3.0.1",relationshipEntity.getSpdxElementId());
            Assertions.assertEquals("DESCRIBES", relationshipEntity.getRelationshipType());
            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1", relationshipEntity.getRelatedSpdxElement());
            Assertions.assertEquals(spdxDocumentRoot, relationshipEntity.getSpdxDocument());
            Assertions.assertFalse(spdxDocumentRoot.getRelationships().isEmpty());

        } catch (Exception e) {
            Assertions.fail("An error occurred: " + e);
        }
    }
}

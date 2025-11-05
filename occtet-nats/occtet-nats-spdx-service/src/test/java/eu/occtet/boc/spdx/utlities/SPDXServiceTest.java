/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

package eu.occtet.boc.spdx.utlities;

import eu.occtet.boc.model.SpdxWorkData;
import eu.occtet.boc.spdx.dao.InventoryItemRepository;
import eu.occtet.boc.spdx.dao.LicenseRepository;
import eu.occtet.boc.spdx.dao.ProjectRepository;
import eu.occtet.boc.spdx.dao.SoftwareComponentRepository;
import eu.occtet.boc.spdx.factory.*;
import eu.occtet.boc.spdx.service.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Objects;

@DataJpaTest
@ContextConfiguration(classes = {SpdxService.class, SoftwareComponentService.class, SoftwareComponentRepository.class,
        CopyrightService.class, InventoryItemService.class, LicenseService.class, CodeLocationService.class,
        ProjectRepository.class, LicenseRepository.class, InventoryItemRepository.class, SoftwareComponentFactory.class,
        CopyrightFactory.class, CodeLocationFactory.class, InventoryItemFactory.class,
        LicenseFactory.class
})
@EnableJpaRepositories(basePackages = "eu.occtet.boc.spdx.dao")
@EntityScan(basePackages = "eu.occtet.boc.entity")
@ExtendWith(MockitoExtension.class)
public class SPDXServiceTest {

    @Autowired
    private SpdxService spdxService;

    @MockitoBean
    private AnswerService answerService;



   // @Test // FIXME commented out until issue #32 is fixed
    public void parseDocumentTest(){
        Mockito.mock(answerService.sendToDownload("url","loc","ver"));
        //Mockito.mock(answerService.prepareAnswers(Mockito.anyList(),true,false));

        try {

            SpdxWorkData spdxWorkData = new SpdxWorkData();
            spdxWorkData.setJsonSpdx(new String(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("synthetic-scan-result-expected-output.spdx.json")).readAllBytes()));
            spdxWorkData.setProjectId("1");

            //boolean result = spdxService.parseDocument(spdxWorkData);

            // FIXME as this function only returns a boolean, we need to check the database for the correct results.
            // this propably requires the (local h2 in-memory database) to be populated beforehand.


            /*
            Project testProject = projectRepository.findAll().getFirst();
            List<SoftwareComponent> softwareComponents1 = softwareComponentRepository.findById(softwareComponents.getFirst());
            SoftwareComponent testComponent = softwareComponents1.getFirst();
            InventoryItem relatedItem = inventoryItemRepository.findBySoftwareComponentAndProject(testComponent, testProject).getFirst();

            //are name and version correct?
            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1", testComponent.getName());
            Assertions.assertEquals("0.0.1", testComponent.getVersion());
            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1 0.0.1 (MIT)", relatedItem.getInventoryName());

            //are licenses correct?
            List<License> licenses = testComponent.getLicenses();
            Assertions.assertFalse(licenses.stream().filter(s -> s.getLicenseName().equals("MIT") && s.isSpdx()).toList().isEmpty());

            //are copyrights correct?
            List<Copyright> copyrights = relatedItem.getCopyrights();
            Assertions.assertFalse(copyrights.stream().filter(s -> s.getCopyrightText().equals("NONE")).toList().isEmpty());

            //are parents correct?
            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1", inventoryItemRepository.findBySpdxID("SPDXRef-Package-Maven-pkg4-grp-pkg4-0.0.1").getFirst().getParent().getSpdxId());
            */
        } catch (Exception e) {
            Assertions.fail("An unexpected error occured: " + e);
        }
    }
}

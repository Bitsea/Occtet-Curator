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

import eu.occtet.boc.entity.*;
import eu.occtet.boc.model.SpdxWorkData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

//TODO fix
@SpringBootTest
@AutoConfigureDataJpa
public class SPDXUtilitiesTest {


//    @Test
//    public void spdxConsumptionTest(){
//        try {
//
//            SpdxWorkData spdxWorkData = new SpdxWorkData();
//            spdxWorkData.setJsonSpdx(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("synthetic-scan-result-expected-output.spdx.json")).readAllBytes());
//            spdxWorkData.setProject(new Project());
//            spdxWorkData.setRootInventoryItem(new InventoryItem());
//
//            List<UUID> softwareComponents = spdxUtilities.parseDocument(spdxWorkData);
//
//            if (softwareComponents == null || softwareComponents.isEmpty()) {
//                Assertions.fail("Failed to create any softwareComponents");
//            }
//            Project testProject = projectRepository.findAll().getFirst();
//            List<SoftwareComponent> softwareComponents1 = softwareComponentRepository.findById(softwareComponents.getFirst());
//            SoftwareComponent testComponent = softwareComponents1.getFirst();
//            InventoryItem relatedItem = inventoryItemRepository.findBySoftwareComponentAndProject(testComponent, testProject).getFirst();
//
//            //are name and version correct?
//            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1", testComponent.getName());
//            Assertions.assertEquals("0.0.1", testComponent.getVersion());
//            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1 0.0.1 (MIT)", relatedItem.getInventoryName());
//
//            //are licenses correct?
//            List<License> licenses = testComponent.getLicenses();
//            Assertions.assertFalse(licenses.stream().filter(s -> s.getLicenseName().equals("MIT") && s.isSpdx()).toList().isEmpty());
//
//            //are copyrights correct?
//            List<Copyright> copyrights = relatedItem.getCopyrights();
//            Assertions.assertFalse(copyrights.stream().filter(s -> s.getCopyrightText().equals("NONE")).toList().isEmpty());
//
//            //are parents correct?
//            Assertions.assertEquals("SPDXRef-Project-Maven-proj1-grp-proj1-0.0.1", inventoryItemRepository.findBySpdxID("SPDXRef-Package-Maven-pkg4-grp-pkg4-0.0.1").getFirst().getParent().getSpdxId());
//        } catch (Exception e) {
//            Assertions.fail("An unexpected error occured: " + e);
//        }
//    }
}

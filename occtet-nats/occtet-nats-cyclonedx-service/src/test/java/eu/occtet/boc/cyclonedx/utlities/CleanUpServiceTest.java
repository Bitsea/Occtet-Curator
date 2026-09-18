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

package eu.occtet.boc.cyclonedx.utlities;

import eu.occtet.boc.config.TestEclipseLinkJpaConfiguration;
import eu.occtet.boc.cyclonedx.service.CleanUpService;
import eu.occtet.boc.dao.*;
import eu.occtet.boc.entity.*;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfigType;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {
        CleanUpService.class,
        TestEclipseLinkJpaConfiguration.class
})
@EnableJpaRepositories(basePackages = {"eu.occtet.boc.dao"})
@EntityScan(basePackages = "eu.occtet.boc.entity")
@EnableJpaAuditing
@ActiveProfiles("test")
public class CleanUpServiceTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CleanUpService cleanUpService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private CopyrightRepository copyrightRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AppConfigurationRepository appConfigurationRepository;

    @TempDir
    Path tempBaseDir;

    private Project testProject;
    private Organization testOrg;

    @BeforeEach
    public void setUp() {
        testOrg = new Organization();
        testOrg.setOrganizationName("TestOrg");
        organizationRepository.save(testOrg);

        testProject = new Project("TestProject");
        testProject.setVersion("1.0.0");
        testProject.setOrganization(testOrg);
        testProject = projectRepository.save(testProject);

        AppConfiguration basePathConfig = new AppConfiguration();
        basePathConfig.setConfigKey(AppConfigKey.GENERAL_BASE_PATH);
        basePathConfig.setValue(tempBaseDir.toAbsolutePath().toString());
        basePathConfig.setDataType(AppConfigType.STRING);
        appConfigurationRepository.save(basePathConfig);
    }

    @Test
    public void testCleanUpFileTree_ShouldDeletePhysicalFilesAndDatabaseRecords() throws IOException {
        // 1. Create directory structure on disk
        String folderName = testProject.getProjectName() + "_" + testProject.getVersion();
        Path projectDir = tempBaseDir.resolve(folderName);
        log.debug("Creating test project directory at: {}", projectDir);
        Path subDir = projectDir.resolve("src").resolve("main");
        Files.createDirectories(subDir);
        Path testFile1 = subDir.resolve("App.java");
        Path testFile2 = subDir.resolve("Utils.java");
        Files.writeString(testFile1, "public class App {}");
        Files.writeString(testFile2, "public class Utils {}");

        Assertions.assertTrue(Files.exists(testFile1));
        Assertions.assertTrue(Files.exists(testFile2));

        // 2. Create entities in database
        File parentDirEntity = new File();
        parentDirEntity.setProjectPath("src/main");
        parentDirEntity.setFileName("main");
        parentDirEntity.setIsDirectory(true);
        parentDirEntity.setProject(testProject);
        parentDirEntity = fileRepository.save(parentDirEntity);

        File fileEntity1 = new File();
        fileEntity1.setProjectPath("src/main/App.java");
        fileEntity1.setFileName("App.java");
        fileEntity1.setIsDirectory(false);
        fileEntity1.setParent(parentDirEntity);
        fileEntity1.setProject(testProject);
        fileEntity1 = fileRepository.save(fileEntity1);

        File fileEntity2 = new File();
        fileEntity2.setProjectPath("src/main/Utils.java");
        fileEntity2.setFileName("Utils.java");
        fileEntity2.setIsDirectory(false);
        fileEntity2.setParent(parentDirEntity);
        fileEntity2.setProject(testProject);
        fileEntity2 = fileRepository.save(fileEntity2);

        // 3. Link InventoryItem & Copyright to files
        InventoryItem inventoryItem = new InventoryItem("Item1", testProject, null, testOrg);
        inventoryItem = inventoryItemRepository.save(inventoryItem);
        fileEntity1.getInventoryItems().add(inventoryItem);
        fileRepository.save(fileEntity1);

        Copyright copyright = new Copyright("Copyright (C) 2026", new HashSet<>(Set.of(fileEntity1, fileEntity2)), testOrg);
        copyrightRepository.save(copyright);

        List<File> filesBefore = fileRepository.findAllByProject(testProject);
        Assertions.assertEquals(3, filesBefore.size());

        // 4. Perform Cleanup
        cleanUpService.cleanUpFileTree(testProject);

        // 5. Verify physical directory is deleted
        Assertions.assertFalse(Files.exists(projectDir));

        // 6. Verify database records are deleted
        List<File> filesAfter = fileRepository.findAllByProject(testProject);
        Assertions.assertTrue(filesAfter.isEmpty(), "All files for project should be deleted from DB");
    }
}

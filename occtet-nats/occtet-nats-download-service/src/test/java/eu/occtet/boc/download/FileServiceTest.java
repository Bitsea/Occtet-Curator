/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.download;


import eu.occtet.boc.download.dao.FileRepository;
import eu.occtet.boc.download.factory.FileFactory;
import eu.occtet.boc.download.service.FileService;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@ContextConfiguration(classes = {FileService.class, FileFactory.class, FileRepository.class})
@EnableJpaRepositories(basePackages = "eu.occtet.boc.download.dao")
@EntityScan(basePackages = "eu.occtet.boc.entity")
@EnableJpaAuditing
@ActiveProfiles("test")
public class FileServiceTest {


    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileRepository fileRepository;

    @Test
    void testCreatingEntitiesFromPathOfMainPackage(@TempDir Path tempDir) throws IOException {
        // Setup Filesystem
        Path parent1 = Files.createDirectories(tempDir.resolve("parent1")); // Project Root
        Path subFolder = Files.createDirectories(parent1.resolve("subFolder1"));
        Files.createFile(parent1.resolve("rootFile1.txt"));
        Files.createFile(subFolder.resolve("childFile1.txt"));

        // Setup Project
        Project project = new Project();
        project.setProjectName("TestProject");
        project.setBasePath(parent1.toAbsolutePath().toString());
        entityManager.persistAndFlush(project);

        fileService.createEntitiesFromPath(project, null, parent1, true);

        List<File> allFiles = fileRepository.findAll();
        assertEquals(4, allFiles.size());

        // Test Child File
        File childFile = allFiles.stream()
                .filter(f -> f.getFileName().equals("childFile1.txt"))
                .findFirst().orElseThrow();
        assertNotNull(childFile.getParent());
        assertEquals("subFolder1", childFile.getParent().getFileName());
        assertEquals("subFolder1/childFile1.txt", childFile.getRelativePath());

        // Test SubFolder
        File subFolderEntity = childFile.getParent();
        assertTrue(subFolderEntity.getIsDirectory());
        assertEquals("subFolder1", subFolderEntity.getRelativePath());

        // Test Root File
        File rootFile = allFiles.stream()
                .filter(f -> f.getFileName().equals("rootFile1.txt"))
                .findFirst().orElseThrow();

        assertEquals("rootFile1.txt", rootFile.getRelativePath());
        assertEquals(project.getId(), rootFile.getProject().getId());

        assertNotNull(rootFile.getParent(), "Root file should have the main package folder as parent");
        assertEquals("parent1", rootFile.getParent().getFileName());
    }

    @Test
    void testCreatingEntitiesFromPathOfDependencyPackage(@TempDir Path tempDir) throws IOException {
        Path parent1 = Files.createDirectories(tempDir.resolve("parent1"));
        Path dependencies = Files.createDirectories(parent1.resolve("dependencies"));
        Path subFolder = Files.createDirectories(dependencies.resolve("subFolder2"));
        Files.createFile(subFolder.resolve("childFile2.txt"));

        Project project = new Project();
        project.setProjectName("TestProject");
        project.setBasePath(parent1.toAbsolutePath().toString());
        entityManager.persistAndFlush(project);

        fileService.createEntitiesFromPath(project, null, subFolder, false);

        List<File> allFiles = fileRepository.findAll();
        assertEquals(3, allFiles.size());

        File childFile = allFiles.stream()
                .filter(f -> f.getFileName().equals("childFile2.txt"))
                .findFirst().orElseThrow();

        assertNotNull(childFile.getParent());
        assertEquals("subFolder2", childFile.getParent().getFileName());
        assertEquals("childFile2.txt", childFile.getRelativePath());

        File subFolderEntity = childFile.getParent();
        assertTrue(subFolderEntity.getIsDirectory());
        assertEquals("", subFolderEntity.getRelativePath()); // relative starts always after project dir which is why
        // this should be empty in this case

        File dependenciesEntity = subFolderEntity.getParent();
        assertNotNull(dependenciesEntity, "Parent 'dependencies' folder should have been auto-created");
        assertEquals("dependencies", dependenciesEntity.getRelativePath());
        assertTrue(dependenciesEntity.getIsDirectory());
    }
}



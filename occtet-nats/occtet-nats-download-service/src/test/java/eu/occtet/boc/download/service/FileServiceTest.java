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

package eu.occtet.boc.download.service;


import eu.occtet.boc.config.TestEclipseLinkJpaConfiguration;
import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.download.factory.FileFactory;
import eu.occtet.boc.entity.File;
import eu.occtet.boc.entity.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {FileService.class, FileFactory.class, FileRepository.class, TestEclipseLinkJpaConfiguration.class})
@EnableJpaRepositories(basePackages = "eu.occtet.boc.dao")
@EntityScan(basePackages = "eu.occtet.boc.entity")
@EnableJpaAuditing
@ActiveProfiles("test")
class FileServiceTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileRepository fileRepository;

    private Project testProject;

    @BeforeEach
    void setup() {
        testProject = new Project();
        testProject.setProjectName("TestProject");
        testProject = entityManager.persistAndFlush(testProject);
    }

    @Test
    void testScanFromProjectRoot(@TempDir Path tempDir) throws IOException {
        // Structure:
        // tempDir/projectRoot
        //    |-- src/
        //        |-- Main.java
        //    |-- README.md

        Path projectRoot = Files.createDirectories(tempDir.resolve("projectRoot"));
        Path srcDir = Files.createDirectories(projectRoot.resolve("src"));
        Files.createFile(srcDir.resolve("Main.java"));
        Files.createFile(projectRoot.resolve("README.md"));

        String projectPathString = projectRoot.toAbsolutePath().toString();

        fileService.createEntitiesFromPath(testProject, null, projectRoot, projectPathString);

        List<File> files = fileRepository.findAll();
        assertEquals(4, files.size());

        File mainJava = files.stream().filter(f -> f.getFileName().equals("Main.java")).findFirst().orElseThrow();
        assertEquals("src/Main.java", mainJava.getArtifactPath());
        assertTrue(mainJava.getProjectPath().endsWith("src/Main.java"));
        assertNotNull(mainJava.getPhysicalPath());
    }

    @Test
    void testScanDependencySubFolder(@TempDir Path tempDir) throws IOException {
        // Structure:
        // tempDir/projectRoot
        //    |-- dependencies/
        //        |-- lib-a/  <-- SCAN START
        //            |-- lib.jar

        Path projectRoot = Files.createDirectories(tempDir.resolve("projectRoot"));
        Path depFolder = Files.createDirectories(projectRoot.resolve("dependencies"));
        Path libFolder = Files.createDirectories(depFolder.resolve("lib-a"));
        Files.createFile(libFolder.resolve("lib.jar"));

        String projectPathString = projectRoot.toAbsolutePath().toString();

        fileService.createEntitiesFromPath(testProject, null, libFolder, projectPathString);

        List<File> files = fileRepository.findAll();

        File libJar = files.stream().filter(f -> f.getFileName().equals("lib.jar")).findFirst().orElseThrow();
        File libA = files.stream().filter(f -> f.getFileName().equals("lib-a")).findFirst().orElseThrow();

        assertEquals("lib.jar", libJar.getArtifactPath());
        assertTrue(libJar.getProjectPath().endsWith("dependencies/lib-a/lib.jar"));
        assertTrue(libA.getProjectPath().endsWith("dependencies/lib-a"));

        assertNotNull(libA.getParent());
        assertEquals("dependencies", libA.getParent().getFileName());
    }

    @Test
    void testUpdateExistingSpdxEntity(@TempDir Path tempDir) throws IOException {
        // Structure:
        // tempDir/projectRoot
        //    |-- LICENSE (Exists on disk)

        Path projectRoot = Files.createDirectories(tempDir.resolve("projectRoot"));
        Files.createFile(projectRoot.resolve("LICENSE"));
        String projectPathString = projectRoot.toAbsolutePath().toString();

        File spdxEntityPlaceholder = new File();
        spdxEntityPlaceholder.setProject(testProject);
        spdxEntityPlaceholder.setFileName("LICENSE");
        spdxEntityPlaceholder.setArtifactPath("LICENSE");
        spdxEntityPlaceholder.setPhysicalPath(null);
        spdxEntityPlaceholder.setIsDirectory(false);
        spdxEntityPlaceholder = fileRepository.saveAndFlush(spdxEntityPlaceholder);
        Long originalId = spdxEntityPlaceholder.getId();

        fileService.createEntitiesFromPath(testProject, null, projectRoot, projectPathString);

        List<File> allFiles = fileRepository.findAll();

        File updatedFile = allFiles.stream()
                .filter(f -> f.getFileName().equals("LICENSE"))
                .findFirst()
                .orElseThrow();

        assertEquals(originalId, updatedFile.getId());
        assertNotNull(updatedFile.getPhysicalPath());
        assertEquals(projectRoot.resolve("LICENSE").toAbsolutePath().toString(), updatedFile.getPhysicalPath());
    }
}
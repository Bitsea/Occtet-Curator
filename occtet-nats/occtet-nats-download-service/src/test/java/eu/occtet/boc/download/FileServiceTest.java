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
public class FileServiceTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileRepository fileRepository;


    @Test
    void testCreatingEntitiesFromPath(@TempDir Path tempDir) throws IOException {
        Path parent1 = Files.createDirectories(tempDir.resolve("parent1")); // Project Root
        Path subFolder = Files.createDirectories(parent1.resolve("subFolder"));
        Files.createFile(parent1.resolve("rootFile.txt"));
        Files.createFile(subFolder.resolve("childFile.txt"));

        Project project = new Project();
        project.setProjectName("TestProject");
        project.setBasePath(parent1.toAbsolutePath().toString());
        entityManager.persistAndFlush(project);

        fileService.createEntitiesFromPath(project, parent1);
        List<File> allFiles = fileRepository.findAll();
        assertEquals(3, allFiles.size());

        // test hierarchy
        File childFile = allFiles.stream()
                .filter(f -> f.getFileName().equals("childFile.txt"))
                .findFirst().orElseThrow();
        assertNotNull(childFile.getParent());
        assertEquals("subFolder", childFile.getParent().getFileName());
        assertEquals("subFolder/childFile.txt", childFile.getRelativePath());

        File subFolderEntity = childFile.getParent();

        assertNull(subFolderEntity.getParent());
        assertTrue(subFolderEntity.getDirectory());
        assertEquals("subFolder", subFolderEntity.getRelativePath());

        File rootFile = allFiles.stream()
                .filter(f -> f.getFileName().equals("rootFile.txt"))
                .findFirst().orElseThrow();

        assertNull(rootFile.getParent());
        assertEquals("rootFile.txt", rootFile.getRelativePath());
        assertEquals(project.getId(), rootFile.getProject().getId());
    }
}


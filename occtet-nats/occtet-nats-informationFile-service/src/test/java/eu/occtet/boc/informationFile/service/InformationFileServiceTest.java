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

package eu.occtet.boc.informationFile.service;

import eu.occtet.boc.entity.InformationFile;
import eu.occtet.boc.informationFile.dao.InformationFileDao;
import eu.occtet.boc.informationFile.dao.InformationFileRepository;
import eu.occtet.boc.informationFile.factory.InformationFileFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DataJpaTest
@ContextConfiguration(classes = {InformationFileService.class,InformationFileFactory.class,
        InformationFileRepository.class, InformationFileDao.class})
@EnableJpaRepositories(basePackages = "eu.occtet.boc.informationFile.dao")
@EntityScan(basePackages = "eu.occtet.boc.entity")
@ExtendWith(MockitoExtension.class)
public class InformationFileServiceTest {

    @MockitoBean
    private VectorStore vectorStore;

    @Autowired
    private InformationFileService informationFileService;

    @Autowired
    private InformationFileRepository informationFileRepository;


    @Test
    void testUploadFiles(){

        Path path = Path.of("src","test","resources/testData/bad-copyrights.txt");
        String context = "copyright";

        assertTrue(informationFileService.uploadFiles(path.toFile().getAbsolutePath(),context));
        InformationFile informationFile = informationFileRepository.findByFileName("bad-copyrights.txt").getFirst();

        assertEquals("bad-copyrights.txt",informationFile.getFileName());
        assertEquals("copyright",informationFile.getContext());
    }
}

/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.service;


import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.factory.FileFactory;
import eu.occtet.bocfrontend.model.FileResult;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(AuthenticatedAsAdmin.class)
@SpringBootTest
public class FileContentServiceTest {

    private static final Logger log = LogManager.getLogger(FileContentServiceTest.class);

    @Autowired
    private FileContentService fileContentService;
    @Autowired
    private FileFactory fileFactory;

    @Test
    void getFileContent_fromRelativePath_succeeds() {
        InventoryItem rootItem = mock(InventoryItem.class);
        Project project = mock(Project.class);

        String projectRootPath = Paths.get("").toAbsolutePath().toString();
        when(rootItem.getParent()).thenReturn(null);

        File file = fileFactory.create(new InventoryItem(),
                "src/test/resources/FileContentTestFile",project);

        FileResult result = fileContentService.getFileContent(projectRootPath);

        assertInstanceOf(FileResult.Success.class, result);
        FileResult.Success successResult = (FileResult.Success) result;

        String expectedContent = "Hello World!";

        assertEquals(expectedContent, successResult.content());
    }
}

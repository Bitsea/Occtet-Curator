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

import eu.occtet.bocfrontend.entity.Configuration;
import eu.occtet.bocfrontend.entity.CuratorTask;
import eu.occtet.bocfrontend.importer.TaskManager;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;


@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
@Transactional
public class ConfigurationServiceTest {


    @Autowired
    protected DataManager dataManager;
    @Autowired
    private ConfigurationService configurationService;

    private Configuration configuration;
    private CuratorTask curatorTaskFlexera;
    private CuratorTask curatorTaskSpdx;

    private static final String FLEXERA_FILE_NAME = "src/test/resources/foss_report_sample.xlsx";
    private static final String KEY_FOR_UPLOAD_CONFIG = "fileName";
    private static final String KEY_FOR_BOOLEAN_CONFIG1 = "UseLicenseMatcher";
    private static final String KEY_FOR_BOOLEAN_CONFIG2 = "UseFalseCopyrightFilter";
    private static final String KEY_FOR_BASE_PATH_CONFIG = "basePathForRelativePath";

    @BeforeEach
    void setUp() {
        this.configuration = dataManager.create(Configuration.class);

        // Flexera
        this.curatorTaskFlexera = dataManager.create(CuratorTask.class);
        curatorTaskFlexera.setTaskName("Flexera_Report_Import");
        // SPDX
        this.curatorTaskSpdx= dataManager.create(CuratorTask.class);
        curatorTaskSpdx.setTaskName("SPDX_Import");
    }

    @Test
    void test_getTypeOfConfiguration(){
        // Tests for Flexera
        assertEquals(
                Configuration.Type.FILE_UPLOAD,
                configurationService.getTypeOfConfiguration(
                        "fileName", curatorTaskFlexera)
        );
        assertEquals(
                Configuration.Type.BOOLEAN,
                configurationService.getTypeOfConfiguration(
                        "UseLicenseMatcher", curatorTaskFlexera)
        );
        assertEquals(
                Configuration.Type.BOOLEAN,
                configurationService.getTypeOfConfiguration(
                        "UseFalseCopyrightFilter", curatorTaskFlexera)
        );
        // Tests for SPDX
    }

    @Test
    void test_handleConfigForValidFileUpload(){
        // Test for Flexera
        File uploadFile = new File(FLEXERA_FILE_NAME);
        try (FileInputStream uploadFileInputStream = new FileInputStream(uploadFile)){

            byte[] uploadFileValue = new byte[uploadFileInputStream.available()];

            boolean result =
                    configurationService.handleConfig(
                            configuration,
                            KEY_FOR_UPLOAD_CONFIG, // Key for the configuration
                            uploadFileValue,
                            uploadFile.getName(),
                            false,
                            null,
                            curatorTaskFlexera
                    );

            assertTrue(result);
            assertEquals(uploadFileValue.length, configuration.getUpload().length);
            assertEquals("foss_report_sample.xlsx", configuration.getValue());
        } catch (Exception e) {
            assert false : "Exception: " + e;
        }
        // Test for SPDX
    }

    @Test
    void test_handleConfigForInvalidFileUpload(){
        // Test for Flexera
        try{
            boolean resultForValid =
                    configurationService.handleConfig(
                            configuration,
                            KEY_FOR_UPLOAD_CONFIG, // Key for the configuration
                            new byte[0],
                            "",
                            false,
                            null,
                            curatorTaskSpdx
                    );

            assertFalse(resultForValid);
        } catch (Exception e) {
            assert false : "Exception: " + e;
        }
        // Test for SPDX
    }


    @Test
    void test_handleConfigForBoolean(){
        boolean result = configurationService.handleConfig(
                configuration,
                KEY_FOR_BOOLEAN_CONFIG1,
                null,
                null,
                true,
                null,
                curatorTaskFlexera
        );
        assertTrue(result);
        assertEquals("true", configuration.getValue());
        result = configurationService.handleConfig(
                configuration,
                KEY_FOR_BOOLEAN_CONFIG2,
                null,
                null,
                false,
                null,
                curatorTaskFlexera
        );
        assertTrue(result);
        assertEquals("false", configuration.getValue());
    }
}

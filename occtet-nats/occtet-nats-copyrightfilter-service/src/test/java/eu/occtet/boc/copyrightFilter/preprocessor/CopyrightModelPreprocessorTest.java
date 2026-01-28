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

package eu.occtet.boc.copyrightFilter.preprocessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
//@AutoConfigureDataJpa
// NOTICE: If you comment out the next two lines, the test will use the DB connection configured in test/resources/application.properties
//@AutoConfigureTestEntityManager
//@AutoConfigureTestDatabase
public class CopyrightModelPreprocessorTest {

    private static final Logger log = LogManager.getLogger(CopyrightPreprocessor.class);



    //@Test
    public void trimCopyrightYmlWorkingTest(){
        CopyrightPreprocessor copyrightPreprocessor= new CopyrightPreprocessor();
        Path mainPath = Paths.get( "src", "test", "resources", "scan-result-test2.yml");
        String basePath = mainPath.toFile().getAbsolutePath();
        assert !(copyrightPreprocessor.trimCopyrightYml(basePath).isEmpty());

    }

    //@Test
    public void trimCopyrightYmlExceptionTest(){
        CopyrightPreprocessor copyrightPreprocessor= new CopyrightPreprocessor();
        assertNull(copyrightPreprocessor.trimCopyrightYml("no file here"));
    }

    //@Test
    public void trimCopyrightYmlEmptyFileTest(){
        Path mainPath = Paths.get( "src", "test", "resources", "empty-test.yml");
        String basePath = mainPath.toFile().getAbsolutePath();
        CopyrightPreprocessor copyrightPreprocessor= new CopyrightPreprocessor();
        assert copyrightPreprocessor.trimCopyrightYml(basePath).isEmpty();
    }








}

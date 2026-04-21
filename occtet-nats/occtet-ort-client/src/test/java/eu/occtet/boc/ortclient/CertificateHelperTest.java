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

package eu.occtet.boc.ortclient;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.util.Collection;
import static org.springframework.test.util.AssertionErrors.assertEquals;


@ActiveProfiles("test")
public class CertificateHelperTest {

    //@Test
    public void testLoadCertificate() {
        //for the test to work, change this with a path to where your real certificates are loaded
        Path resourceDirectory = Paths.get("src","test","resources", "cacerts", "test.de.cert");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath();

        Collection<? extends Certificate> certificates =CertificateHelper.loadCertificates(absolutePath);
        assertEquals("size of certificates",1, certificates.size());
    }
}

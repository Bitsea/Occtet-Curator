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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;

public final class CertificateHelper {

    private static final Logger log = LogManager.getLogger(CertificateHelper.class);


    public static Collection<? extends Certificate> loadCertificates(String path) {
        log.info("Helper: loadCertificates from path: {}", path);
        try {
            Path filePath = Paths.get(path);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<Certificate> allCertificates = new ArrayList<>();
            if (Files.isRegularFile(filePath)) {
                log.info("is file: {}", filePath);
                try (InputStream inputStream = Files.newInputStream(filePath)) {
                    allCertificates.addAll(
                            certificateFactory.generateCertificates(inputStream)
                    );
                }
            } else if (Files.isDirectory(filePath)) {
                log.info("is directory: {}", filePath);
                if(Files.getFileStore(filePath).getBlockSize()>0) {
                    long num= Files.getFileStore(filePath).getBlockSize();
                    log.info("list {}", Files.getFileStore(filePath).getBlockSize());
                }
                Files.list(filePath)
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String filename = p.getFileName().toString().toLowerCase();
                            return filename.endsWith(".pem") ||
                                    filename.endsWith(".crt") ||
                                    filename.endsWith(".cert");
                        })
                        .forEach(certFile -> {
                            try (InputStream inputStream = Files.newInputStream(certFile)) {
                                allCertificates.addAll(
                                        certificateFactory.generateCertificates(inputStream)
                                );
                                log.info("added certificates from file: {}", certFile.getFileName());
                            } catch (Exception e) {
                                log.info("Failed to load: " + certFile);
                            }
                        });
            }
            log.info("return {} certificates", allCertificates.size());
            return allCertificates;
        } catch (Exception e) {
            log.info("Failed to load certificates from path {}, error: {}", path, e.getMessage());
            return null;
        }
    }
}

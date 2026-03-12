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

package eu.occtet.boc.spdx.service;

import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.dao.FileRepository;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class CleanUpService {

    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private AppConfigurationRepository appConfigurationRepository;

    private static final Logger log = LoggerFactory.getLogger(CleanUpService.class);


    /**
     * Cleans up the file tree associated with the given project.
     * @param project
     */
    public void cleanUpFileTree(Project project) {

        String globalBasePath = appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH)
                .map(AppConfiguration::getValue)
                .orElseThrow(() -> new RuntimeException("General Base Path not configured!"));
        String folderName = project.getProjectName() + "_" + project.getId();
        Path projectDir = Paths.get(globalBasePath).resolve(folderName);
        deleteProjectDirectory(projectDir);
        log.debug("Cleaning up directory {}", projectDir);
        //deleting all entities in the file tree associated with the project
        fileRepository.deleteAllByProject(project);



    }

    private void deleteProjectDirectory(Path projectRoot) {
        if (!Files.exists(projectRoot)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(projectRoot)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.error("Failed to delete file or directory: {}, exception: {}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk through the project directory: {} exception: {}", projectRoot, e.getMessage());
            throw new RuntimeException("Failed to delete project directory " + projectRoot, e);
        }
    }
}

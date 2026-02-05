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

import com.github.packageurl.PackageURL;
import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.download.factory.DownloadStrategyFactory;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
import eu.occtet.boc.model.DownloadServiceWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class DownloadManager extends BaseWorkDataProcessor {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired private ProjectRepository projectRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private AppConfigurationRepository appConfigurationRepository;

    @Autowired private DownloadStrategyFactory downloadStrategyFactory;
    @Autowired private ArchiveService archiveService;
    @Autowired private FileService fileService;

    @Override
    @Transactional
    public boolean process(DownloadServiceWorkData data) {
        log.info("Starting download process for Item {}", data.getInventoryItemId());
        try{
            Project project = projectRepository.findById(data.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project with id " + data.getProjectId() + " not found"));
            InventoryItem inventoryItem = inventoryItemRepository.findById(data.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("InventoryItem with id " + data.getInventoryItemId() + " not found"));
            SoftwareComponent softwareComponent = inventoryItem.getSoftwareComponent();
            if (softwareComponent == null) throw new RuntimeException("SoftwareComponent for InventoryItem with id " + data.getInventoryItemId() + " not found");

            Path targetDir = calculateTargetPath(project.getProjectName() ,project.getId());
            Path downloadedPath = null;

            // Process download
            // Attempt using downloadLocation
            if (softwareComponent.getDetailsUrl() != null) {
                try {
                    URL durl = new URI(softwareComponent.getDetailsUrl()).toURL();
                    Optional<DownloadStrategy> strategy = downloadStrategyFactory.findForUrl(durl, softwareComponent.getVersion());
                    if (strategy.isPresent()) {
                        log.info("Downloading via URL using {}", strategy.get().getClass().getSimpleName());
                        downloadedPath = strategy.get().download(durl, targetDir);
                    }
                } catch (Exception e) {
                    log.warn("Failed to download via URL, falling back...", e.getMessage());
                }
            }
            // Attempt using PURL
            if (softwareComponent.getPurl() != null && downloadedPath == null){
                try {
                    PackageURL purl = new PackageURL(softwareComponent.getPurl());
                    Optional<DownloadStrategy> strategy = downloadStrategyFactory.findForPurl(purl);
                    if (strategy.isPresent()) {
                        log.info("Downloading via PURL using {}", strategy.get().getClass().getSimpleName());
                        downloadedPath = strategy.get().download(purl, targetDir);
                    }
                } catch (Exception e) {
                    log.warn("Failed to download via PURL, falling back...", e.getMessage());
                }
            }
            // Attempt using name/version
            if (softwareComponent.getName() != null && downloadedPath == null){
                try {
                    Optional<DownloadStrategy> strategy = downloadStrategyFactory.findForName(softwareComponent.getName(), softwareComponent.getVersion());
                    if (strategy.isPresent()) {
                        log.info("Downloading via Name lookup using {}", strategy.get().getClass().getSimpleName());
                        downloadedPath = strategy.get().download(softwareComponent.getName(), softwareComponent.getVersion(), targetDir);
                    }
                } catch (Exception e) {
                    log.warn("Failed to download via Name/Version, falling back...", e.getMessage());
                }
            }

            if (downloadedPath == null){
                log.error("All download strategies failed for item {}", data.getInventoryItemId());
                return false;
            }

            if (Files.isRegularFile(downloadedPath)){
                archiveService.unpack(downloadedPath, targetDir);
            }

            // TODO get rid of arg `targetDir.toString()` unneeded plus why is it using String instead of path
            fileService.createEntitiesFromPath(project, inventoryItem, downloadedPath, targetDir.toString());
            return true;
        } catch (Exception e) {
            log.error("Process failed", e.getMessage());
            return false;
        }
    }

    private Path calculateTargetPath(String projectName, Long projectId) throws RuntimeException{
        AppConfiguration globalBasePath = appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH)
                .orElseThrow(() -> new RuntimeException("System base path is not set in the configuration"));
        String folderName = projectName + "_" + projectId;
        return Paths.get(globalBasePath.getValue()).resolve(folderName);
    }
}

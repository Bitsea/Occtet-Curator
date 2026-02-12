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
import eu.occtet.boc.download.strategies.DownloadStrategy;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.SoftwareComponent;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
import eu.occtet.boc.model.DownloadServiceWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import eu.occtet.boc.util.ExternalNotesConstants;
import eu.occtet.boc.util.FileConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class DownloadManager extends BaseWorkDataProcessor {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired private ProjectRepository projectRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private AppConfigurationRepository appConfigurationRepository;

    @Autowired private DownloadStrategyFactory downloadStrategyFactory;
    @Autowired private ArchiveService archiveService;
    @Autowired private FileService fileService;

    private static final String SAFE_FILENAME_REGEX = "[^a-zA-Z0-9.\\-_]";

    @Override
    @Transactional
    public boolean process(DownloadServiceWorkData data) {
        log.info("Starting download process");

        Path downloadedPath = null;
        Path projectBaseDir = null; // project root folder
        Path finalComponentDir = null; // Component folder

        try{
            AppConfiguration globalBasePath = appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH)
                    .orElseThrow(() -> new RuntimeException("System base path is not set in the configuration"));
            if (globalBasePath.getValue() == null || globalBasePath.getValue().isBlank())
                throw new RuntimeException("System base path is not set in the configuration");

            Project project = projectRepository.findById(data.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project with id " + data.getProjectId() + " not found"));
            InventoryItem inventoryItem = inventoryItemRepository.findById(data.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("InventoryItem with id " + data.getInventoryItemId() + " not found"));
            SoftwareComponent softwareComponent = inventoryItem.getSoftwareComponent();
            if (softwareComponent == null) throw new RuntimeException("SoftwareComponent for InventoryItem with id " + data.getInventoryItemId() + " not found");

            boolean isMainPkg = Boolean.TRUE.equals(data.getIsMainPackage());

            // calc base project path (e.g., /data/Project_101)
            projectBaseDir = calculateTargetPath(globalBasePath.getValue(), project.getProjectName(), project.getId());

            // Structure: [project] / [dependencies?] / [component_name] / [version]
            Path workingPath = projectBaseDir;

            if (!isMainPkg) {
                log.debug("Resolving dependencies folder for item {}", data.getInventoryItemId());
                workingPath = workingPath.resolve(FileConstants.DEPENDENCIES_FOLDER_NAME);
            }
            String canonicalName = resolveCanonicalDirectoryName(softwareComponent);
            String safeSoftwareComponentName = sanitizeFilename(canonicalName, "unknown_component_" + data.getInventoryItemId());

            String safeComponentVersion = sanitizeFilename(softwareComponent.getVersion(), "unknown_version");

            finalComponentDir = workingPath.resolve(safeSoftwareComponentName).resolve(safeComponentVersion);

            // Process download
            // Attempt using downloadLocation
            if (softwareComponent.getDetailsUrl() != null) {
                try {
                    URL durl = new URI(softwareComponent.getDetailsUrl()).toURL();

                    List<DownloadStrategy> candidates = downloadStrategyFactory.findForUrl(durl, softwareComponent.getVersion());

                    for (DownloadStrategy strategy : candidates) {
                        try {
                            log.info("Attempting download via URL using {}", strategy.getClass().getSimpleName());
                            downloadedPath = strategy.download(durl, softwareComponent.getVersion(), finalComponentDir);

                            if (downloadedPath != null) {
                                break;
                            }
                        } catch (Exception e) {
                            log.warn("Strategy {} failed to download. Trying next strategy... Error: {}",
                                    strategy.getClass().getSimpleName(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Critical error resolving URL strategies: {}", e.getMessage());
                }
            }
            // Attempt using PURL
            if (softwareComponent.getPurl() != null && downloadedPath == null){
                try {
                    PackageURL purl = new PackageURL(softwareComponent.getPurl());
                    List<DownloadStrategy> candidates = downloadStrategyFactory.findForPurl(purl);

                    for (DownloadStrategy strategy : candidates) {
                        try {
                            log.info("Attempting download via PURL using {}", strategy.getClass().getSimpleName());
                            downloadedPath = strategy.download(purl, finalComponentDir);
                            if (downloadedPath != null) break;
                        } catch (Exception e) {
                            log.warn("Strategy {} failed. Error: {}", strategy.getClass().getSimpleName(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to process PURL: {}", e.getMessage());
                }
            }
            // Attempt using name/version
            if (softwareComponent.getName() != null && downloadedPath == null){
                try {
                    List<DownloadStrategy> candidates = downloadStrategyFactory.findForName(softwareComponent.getName(), softwareComponent.getVersion());

                    for (DownloadStrategy strategy : candidates) {
                        try {
                            log.info("Attempting download via Name lookup using {}", strategy.getClass().getSimpleName());
                            downloadedPath = strategy.download(softwareComponent.getName(),
                                    softwareComponent.getVersion(), finalComponentDir);
                            if (downloadedPath != null) break;
                        } catch (Exception e) {
                            log.warn("Strategy {} failed. Error: {}", strategy.getClass().getSimpleName(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to process Name lookup: {}", e.getMessage());
                }
            }

            if (downloadedPath == null){
                log.error("All download strategies failed for item {}", data.getInventoryItemId());
                String updatedNotes = inventoryItem.getExternalNotes();
                updatedNotes += ExternalNotesConstants.SECTION_SEPARATOR +
                        ExternalNotesConstants.WARNING_AUDITOR_ATTENTION_REQ + 
                        ExternalNotesConstants.DOWNLOAD_SERVICE_FAILURE_MSG;
                inventoryItem.setExternalNotes(updatedNotes);
                inventoryItem.setHasTodos(true);
                inventoryItemRepository.save(inventoryItem);
                log.debug("InventoryItem '{}' audit notes updated with WARNING message: {}", inventoryItem.getId(),
                        updatedNotes);
                return false;
            }

            if (Files.isRegularFile(downloadedPath)){
                archiveService.unpack(downloadedPath, finalComponentDir);
            }

            fileService.createEntitiesFromPath(
                    project,
                    finalComponentDir,
                    projectBaseDir.toString()
            );

            return true;
        } catch (Exception e) {
            log.error("Process failed: {}", e.getMessage());
            return false;
        } finally {
            if (downloadedPath != null) {
                log.debug("Cleaning up, deleting the source archive: {}", downloadedPath.getFileName());
                try {
                    Files.deleteIfExists(downloadedPath);
                } catch (IOException ioe) {
                    log.warn("Failed to delete temporary file {} after failed download: {}", downloadedPath, ioe.getMessage());
                }
            }
        }
    }

    private Path calculateTargetPath(String globalBasePath,String projectName, Long projectId) throws RuntimeException{
        String folderName =  projectName + "_" + projectId;
        return Paths.get(globalBasePath).resolve(folderName);
    }

    private String sanitizeFilename(String input, String fallback) {
        if (input == null || input.isBlank()) return fallback;
        return input.replaceAll(SAFE_FILENAME_REGEX, "_");
    }

    /**
     * Resolves a canonical folder name to prevent duplicates.
     * Priority: PURL Name -> Git Repo Name -> Component Name
     */
    private String resolveCanonicalDirectoryName(SoftwareComponent component) {
        if (component.getPurl() != null && !component.getPurl().isBlank()) {
            try {
                PackageURL purl = new PackageURL(component.getPurl());
                log.debug("Choosing PURL name as component name: {}", purl.getName());
                return purl.getName();
            } catch (Exception e) {
                log.warn("Invalid PURL for component {}: {}", component.getId(), e.getMessage());
            }
        }

        String url = component.getDetailsUrl();
        if (url != null && !url.isBlank()) {
            // https://github.com/Bitsea/-> Occtet-Curator <-
            String clean = url.trim();
            if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
            if (clean.endsWith(".git")) clean = clean.substring(0, clean.length() - 4);

            int lastSlash = clean.lastIndexOf('/');
            if (lastSlash != -1 && lastSlash < clean.length() - 1) {
                log.debug("Choosing last segment of URL as component name: {}", clean.substring(lastSlash + 1));
                return clean.substring(lastSlash + 1);
            }
        }

        return component.getName();
    }
}

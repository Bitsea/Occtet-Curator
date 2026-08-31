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

package eu.occtet.boc.download.service;

import com.github.packageurl.PackageURL;
import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.download.factory.DownloadStrategyFactory;
import eu.occtet.boc.download.strategies.DownloadStrategy;
import eu.occtet.boc.download.utils.StoragePathResolver;
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
import java.util.stream.Stream;

@Service
public class DownloadManager extends BaseWorkDataProcessor {

    private final Logger log = LogManager.getLogger(this.getClass());

    @Autowired private ProjectRepository projectRepository;
    @Autowired private InventoryItemRepository inventoryItemRepository;
    @Autowired private AppConfigurationRepository appConfigurationRepository;

    @Autowired private DownloadStrategyFactory downloadStrategyFactory;
    @Autowired private ArchiveService archiveService;
    @Autowired private FileService fileService;
    @Autowired private StoragePathResolver storagePathResolver;

    private static final String SAFE_FILENAME_REGEX = "[^a-zA-Z0-9.\\-_]";

    @Override
    @Transactional
    public boolean process(DownloadServiceWorkData data) {
        log.info("Starting download process for project ID {}, item ID {}", data.getProjectId(), data.getInventoryItemId());

        Path downloadedPath = null;
        try {
            Path baseResolvedPath = resolveBaseSystemPath();

            Project project = projectRepository.findById(data.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project with id " + data.getProjectId() + " not found"));
            InventoryItem inventoryItem = inventoryItemRepository.findById(data.getInventoryItemId())
                    .orElseThrow(() -> new RuntimeException("InventoryItem with id " + data.getInventoryItemId() + " not found"));
            SoftwareComponent softwareComponent = inventoryItem.getSoftwareComponent();
            if (softwareComponent == null) {
                throw new RuntimeException("SoftwareComponent for InventoryItem with id " + data.getInventoryItemId() + " not found");
            }

            boolean isMainPkg = Boolean.TRUE.equals(data.getIsMainPackage());
            Path projectBaseDir = calculateTargetPath(baseResolvedPath, project.getProjectName(), project.getVersion());

            // main-repo already exists, skip download and just create entities from existing files
            if (isMainPkg && isProjectDirectoryAlreadyPopulated(projectBaseDir)) {
                log.info("Main repository code already exists in {}. Skipping duplicate download for item {}", projectBaseDir, inventoryItem.getId());
                fileService.createEntitiesFromPath(project, projectBaseDir, projectBaseDir.toString(), inventoryItem);
                return true;
            }


            Path finalComponentDir = resolveTargetDirectory(data.getInventoryItemId(), softwareComponent,isMainPkg, projectBaseDir);

            // check strategies for download
            downloadedPath = executeDownloadStrategies( softwareComponent, finalComponentDir);

            if (downloadedPath == null) {
                handleDownloadFailure(inventoryItem, softwareComponent);
                return false;
            }

            if (Files.isRegularFile(downloadedPath)) {
                try {
                    archiveService.unpack(downloadedPath, finalComponentDir);
                } catch (Exception e) {
                    log.error("Failed to unpack downloaded file for item {}: {}", inventoryItem.getInventoryName(), e.getMessage());
                    handleDownloadFailure(inventoryItem, softwareComponent);
                    return false;
                }
            }

            fileService.createEntitiesFromPath(project, finalComponentDir, projectBaseDir.toString(), inventoryItem);
            inventoryItemRepository.save(inventoryItem);
            return true;

        } catch (Exception e) {
            log.error("Process failed: {}", e.getMessage(), e);
            return false;
        } finally {
            cleanupTempArchive(downloadedPath);
        }
    }

    private Path resolveBaseSystemPath() {
        AppConfiguration globalBasePath = appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH)
                .orElseThrow(() -> new RuntimeException("System base path is not set in the configuration"));

        if (globalBasePath.getValue() == null || globalBasePath.getValue().isBlank()) {
            throw new RuntimeException("System base path is not set in the configuration");
        }

        Path resolved = storagePathResolver.resolveSystemPath(globalBasePath.getValue());
        log.debug("UI path '{}' resolved to '{}'", globalBasePath.getValue(), resolved);
        return resolved;
    }

    private Path resolveTargetDirectory(Long inventoryItemId, SoftwareComponent component, boolean isMainPkg, Path projectBaseDir) {
        if (isMainPkg) {
            log.debug("Using project base directory for main package download: {}", projectBaseDir);
            return projectBaseDir;
        }

        Path workingPath = projectBaseDir.resolve(FileConstants.DEPENDENCIES_FOLDER_NAME);
        String canonicalName = resolveCanonicalDirectoryName(component);
        String safeComponentName = sanitizeFilename(canonicalName, "unknown_component_" + inventoryItemId);
        String safeComponentVersion = sanitizeFilename(component.getVersion(), "unknown_version");

        return workingPath.resolve(safeComponentName).resolve(safeComponentVersion);
    }


    private Path executeDownloadStrategies( SoftwareComponent component, Path targetDir) {
        Path downloadedPath = tryDownloadViaUrl( component, targetDir);

        if (downloadedPath == null && component.getPurl() != null) {
            downloadedPath = tryDownloadViaPurl(component, targetDir);
        }

        if (downloadedPath == null && component.getName() != null) {
            downloadedPath = tryDownloadViaName(component, targetDir);
        }

        return downloadedPath;
    }

    private Path tryDownloadViaUrl(SoftwareComponent component, Path targetDir) {
        if (component.getDetailsUrl() == null || component.getDetailsUrl().isBlank()) return null;

        try {
            String rawUrl = component.getDetailsUrl().trim();
            String cleanUrl = rawUrl.startsWith("git+") ? rawUrl.substring(4) : rawUrl;
            URL durl = new URI(cleanUrl).toURL();

            List<DownloadStrategy> candidates = downloadStrategyFactory.findForUrl(durl, component.getVersion());
            for (DownloadStrategy strategy : candidates) {
                try {
                    log.debug("Attempting download via URL using {}", strategy.getClass().getSimpleName());
                    Path path = strategy.download(durl, component.getVersion(), targetDir);
                    if (path != null) return path;
                } catch (Exception e) {
                    log.warn("Strategy {} failed: {}", strategy.getClass().getSimpleName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Critical error resolving URL strategies: {}", e.getMessage());
        }
        return null;
    }

    private Path tryDownloadViaPurl(SoftwareComponent component, Path targetDir) {
        try {
            PackageURL purl = new PackageURL(component.getPurl());
            List<DownloadStrategy> candidates = downloadStrategyFactory.findForPurl(purl);
            for (DownloadStrategy strategy : candidates) {
                try {
                    log.info("Attempting download via PURL using {}", strategy.getClass().getSimpleName());
                    Path path = strategy.download(purl, targetDir);
                    if (path != null) return path;
                } catch (Exception e) {
                    log.warn("Strategy {} failed: {}", strategy.getClass().getSimpleName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process PURL: {}", e.getMessage());
        }
        return null;
    }

    private Path tryDownloadViaName(SoftwareComponent component, Path targetDir) {
        try {
            List<DownloadStrategy> candidates = downloadStrategyFactory.findForName(component.getName(), component.getVersion());
            for (DownloadStrategy strategy : candidates) {
                try {
                    log.info("Attempting download via Name lookup using {}", strategy.getClass().getSimpleName());
                    Path path = strategy.download(component.getName(), component.getVersion(), targetDir);
                    if (path != null) return path;
                } catch (Exception e) {
                    log.warn("Strategy {} failed: {}", strategy.getClass().getSimpleName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process Name lookup: {}", e.getMessage());
        }
        return null;
    }

    private void handleDownloadFailure(InventoryItem inventoryItem, SoftwareComponent component) {
        log.error("All download strategies failed for item {}", inventoryItem.getInventoryName());

        String existingNotes = inventoryItem.getExternalNotes() != null ? inventoryItem.getExternalNotes() : "";
        String updatedNotes = existingNotes + ExternalNotesConstants.SECTION_SEPARATOR +
                ExternalNotesConstants.WARNING_AUDITOR_ATTENTION_REQ +
                ExternalNotesConstants.DOWNLOAD_SERVICE_FAILURE_MSG +
                "\nAffected download URL: " + component.getDetailsUrl() +
                ExternalNotesConstants.SECTION_SEPARATOR;

        inventoryItem.setExternalNotes(updatedNotes);
        inventoryItem.setHasTodos(true);
        inventoryItemRepository.save(inventoryItem);
    }

    private void cleanupTempArchive(Path downloadedPath) {
        if (downloadedPath != null && Files.isRegularFile(downloadedPath)) {
            log.debug("Cleaning up temporary archive file: {}", downloadedPath.getFileName());
            try {
                Files.deleteIfExists(downloadedPath);
            } catch (IOException ioe) {
                log.warn("Failed to delete temporary file {}: {}", downloadedPath, ioe.getMessage());
            }
        }
    }

    private boolean isProjectDirectoryAlreadyPopulated(Path projectBaseDir) {
        if (!Files.exists(projectBaseDir)) return false;
        try (Stream<Path> stream = Files.list(projectBaseDir)) {
            // ignore the dependencies folder, check if any other files or directories exist
            return stream.anyMatch(path -> !path.getFileName().toString().equals(FileConstants.DEPENDENCIES_FOLDER_NAME));
        } catch (IOException e) {
            return false;
        }
    }

    private Path calculateTargetPath(Path baseResolvedPath, String projectName, String projectVersion) {
        String safeVersion = sanitizeFilename(projectVersion, "unknown_version");
        String folderName = projectName + "_" + safeVersion;
        return baseResolvedPath.resolve(folderName);
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
                String purlName = purl.getName();

                // go-specific correction: if the PURL name is just a major version (e.g., "v1"),
                // use the last segment of the namespace instead
                if ("golang".equalsIgnoreCase(purl.getType()) && purlName != null && purlName.matches("^v\\d+$")) {
                    String namespace = purl.getNamespace();
                    if (namespace != null && !namespace.isBlank()) {
                        int lastSlash = namespace.lastIndexOf('/');
                        purlName = (lastSlash != -1) ? namespace.substring(lastSlash + 1) : namespace;
                        log.debug("Adjusted Golang PURL name from major version to: {}", purlName);
                    }
                } else {
                    log.debug("Choosing PURL name as component name: {}", purlName);
                }

                return purlName;
            } catch (Exception e) {
                log.warn("Invalid PURL for component {}: {}", component.getId(), e.getMessage());
            }
        }

        String url = component.getDetailsUrl();
        if (url != null && !url.isBlank()) {
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

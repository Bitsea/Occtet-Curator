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
package eu.occtet.boc.download.service;


import eu.occtet.boc.dao.AppConfigurationRepository;
import eu.occtet.boc.dao.InventoryItemRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.download.controller.GitRepoController;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.appconfigurations.AppConfigKey;
import eu.occtet.boc.entity.appconfigurations.AppConfiguration;
import eu.occtet.boc.model.DownloadServiceWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service responsible for handling the download of files associated with an {@link InventoryItem} within a {@link Project}.
 * <p>
 * <b>Workflow Steps:</b>
 * <ol>
 * <li><b>Validation:</b> Verifies that the associated Project and InventoryItem exist in the database.</li>
 * <li><b>URL Resolution:</b> Resolves the raw download URL (e.g., git+, ssh://, standard HTTP) into a direct download link (typically a ZIP archive).
 * <ul>
 * <li>Handles protocol normalization (e.g., git+ssh to git+https).</li>
 * <li>Resolves GitHub tags to archive URLs via {@link GitRepoController}.</li>
 * <li><b>Security:</b> Enforces a strict protocol whitelist (HTTP/HTTPS/GIT) to prevent crashes on unsupported schemes (SVN/CVS/HG).</li>
 * </ul>
 * </li>
 * <li><b>Download & Extraction:</b>
 * <ul>
 * <li>Downloads the artifact to a temporary sandbox directory.</li>
 * <li>Extracts the contents securely (preventing Zip Slip).</li>
 * <li>Flattens the directory structure if a single wrapper folder is detected (e.g., from GitHub archives).</li>
 * <li>Moves the files to the final destination directory structure.</li>
 * </ul>
 * </li>
 * <li><b>Entity Creation:</b> Invokes {@link FileService} to scan the downloaded files and create corresponding File entities in the database, linking them to the InventoryItem.</li>
 * </ol>
 * </p>
 */
@Service
public class DownloadService extends BaseWorkDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    @Autowired
    private GitRepoController gitRepoController;
    @Autowired
    private FileService fileService;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private AppConfigurationRepository appConfigurationRepository;

    /**
     * Orchestrates the download workflow for a specific {@link DownloadServiceWorkData} task.
     * <p>
     * This method validates the existence of the target Project and InventoryItem,
     * resolves the appropriate download URL, and triggers the physical download and extraction.
     * If successful, it delegates to {@link FileService} to map the file system to database entities.
     *
     * @param workData contains the contextual information (URL, version, project ID) required for the download
     * @return true if the process completes successfully or is skipped intentionally; false if validation fails
     * @throws RuntimeException if the download or extraction process encounters an unrecoverable error
     */
    @Override
    public boolean process(DownloadServiceWorkData workData) {
        try {
            log.debug("Processing download task for URL: {} -> {}", workData.getDownloadURL());

            Project project = projectRepository.findById(workData.getProjectId()).orElse(null);
            InventoryItem inventoryItem = inventoryItemRepository.findById(workData.getInventoryItemId()).orElse(null);

            String globalBasePath = appConfigurationRepository.findByConfigKey(AppConfigKey.GENERAL_BASE_PATH)
                    .map(AppConfiguration::getValue)
                    .orElseThrow(() -> new RuntimeException("General Base Path not configured!"));
            String folderName = project.getProjectName() + "_" + project.getId();
            Path projectDir = Paths.get(globalBasePath).resolve(folderName);

            if (Files.notExists(projectDir)) {
                Files.createDirectories(projectDir);
            }

            String path = projectDir.toString();

            if (project == null) {
                log.error("Aborting download: Project not found for ID {}", workData.getProjectId());
                return false;
            }
            if (inventoryItem == null) {
                log.error("Aborting download: InventoryItem not found for ID {}", workData.getInventoryItemId());
                return false;
            }
            if (inventoryItem.getSoftwareComponent() == null) {
                log.error("Aborting download: InventoryItem has no SoftwareComponent associated with it");
                return false;
            }
            Path downloadedPath = performDownload(workData, path, inventoryItem.getSoftwareComponent().getVersion());

            if (downloadedPath == null) {
                return true; // Handled (skipped intentionally)
            }

            log.debug("Download successful; calling FileService to create entities from path: {}",
                    path + File.separator + inventoryItem.getSoftwareComponent().getName());
            fileService.createEntitiesFromPath(project, inventoryItem, downloadedPath, path);
            return true;

        } catch (Exception e) {
            log.error("Download workflow failed for {}: {}", workData.getDownloadURL(), e.getMessage());
            throw new RuntimeException("Download failed", e);
        }
    }

    /**
     * Executes the physical download and extraction of the artifact.
     *
     * @param workData the task details
     * @return the {@link Path} to the final directory containing the extracted files, or null if skipped
     * @throws IOException if file system operations fail
     * @throws InterruptedException if the Git resolution process is interrupted
     */
    private Path performDownload(DownloadServiceWorkData workData, String path, String version) throws IOException,
            InterruptedException {
        log.debug("Performing download for URL: {} -> {}", workData.getDownloadURL(), path);
        String url = workData.getDownloadURL();

        String downloadUrl = resolveDownloadUrl(url, version);

        if (downloadUrl == null) {
            log.warn("Skipping download: URL resolved to null or is unsupported for '{}'", url);
            return null;
        }

        // Calculate target path: [BasePath] / [dependencies?] / [LibName] / [Version]
        Path baseLocation = Paths.get(path);
        if (Boolean.FALSE.equals(workData.getIsMainPackage())) {
            baseLocation = baseLocation.resolve("dependencies");
        }

        String libName = extractCleanLibraryName(url);
        Path targetDir = baseLocation.resolve(libName).resolve(version);

        return downloadAndUnpack(downloadUrl, targetDir);
    }

    /**
     * Resolves the raw input URL into a direct HTTP download link.
     * <p>
     * This method normalizes Git URLs to HTTP, handles version-tag resolution via the GitHub API,
     * and filters out unsupported legacy VCS protocols (CVS, SVN, Mercurial, Bazaar) to prevent
     * runtime crashes.
     *
     * @param rawUrl  the URL provided by the upstream service (e.g., "git+ssh://...")
     * @param version the version tag to resolve if the URL is a Git repository
     * @return a valid HTTP/HTTPS URL string for the archive, or null if the protocol is unsupported
     */
    private String resolveDownloadUrl(String rawUrl, String version) throws IOException, InterruptedException {
        log.debug("Resolving download URL for '{}' -> '{}'", rawUrl, version);
        if (rawUrl == null || rawUrl.isEmpty() ||
                "NONE".equalsIgnoreCase(rawUrl) ||
                "NOASSERTION".equalsIgnoreCase(rawUrl)) {
            return null;
        }

        String effectiveUrl = rawUrl.trim();

        // Normalize SSH/Git schemes to HTTP for easier handling
        if (effectiveUrl.startsWith("git+ssh://")) {
            effectiveUrl = effectiveUrl.replace("git+ssh://", "git+https://");
        } else if (effectiveUrl.startsWith("ssh://")) {
            effectiveUrl = effectiveUrl.replace("ssh://", "git+https://");
        } else if (effectiveUrl.startsWith("https://") && effectiveUrl.endsWith(".git")) {
            effectiveUrl = "git+" + effectiveUrl;
        }

        // Whitelist Check: Only allow protocols we explicitly know how to handle.
        // This implicitly drops CVS, SVN, HG, BZR, etc.
        boolean isSupported = effectiveUrl.startsWith("http://") ||
                effectiveUrl.startsWith("https://") ||
                effectiveUrl.startsWith("git+https://") ||
                effectiveUrl.startsWith("git+http://");

        if (!isSupported) {
            log.warn("Skipping unsupported protocol: {}", rawUrl);
            return null;
        }

        if (effectiveUrl.startsWith("git+")) {
            log.debug("Resolving Git URL to Archive: {}", effectiveUrl);
            return resolveGitToZipUrl(effectiveUrl, version);
        }

        log.debug("Resolving direct URL: {}", effectiveUrl);
        return effectiveUrl;
    }

    private String resolveGitToZipUrl(String gitUrl, String version) throws IOException, InterruptedException {
        // remove "git+" prefix
        String cleanUrl = gitUrl.startsWith("git+") ? gitUrl.substring(4) : gitUrl;
        if (cleanUrl.endsWith("/")) cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);

        // Basic parsing for expected format: https://github.com/owner/repo.git or https://github.com/owner/repo
        if (!cleanUrl.contains("github.com")) {
            log.warn("Non-GitHub Git URLs are not fully supported yet for Zip download: {}", gitUrl);
            // Return cleanUrl to attempt a direct download (fallback), though likely to fail if it's a repo page
            return cleanUrl;
        }

        String[] parts = cleanUrl.split("/");
        if (parts.length < 2) throw new IOException("Invalid Git URL structure: " + gitUrl);
        String repoWithGit = parts[parts.length - 1];
        String owner = parts[parts.length - 2];
        String repo = repoWithGit.endsWith(".git") ? repoWithGit.substring(0, repoWithGit.length() - 4) : repoWithGit;

        String resolved = gitRepoController.resolveGitToZipUrl(owner, repo, version);
        if (resolved == null || resolved.isEmpty()) {
            throw new IOException("GitRepoController returned 404 for " + owner + "/" + repo + " @ " + version);
        }
        log.info("Resolved Git URL to Archive: {}", resolved);
        return resolved;
    }

    private String extractCleanLibraryName(String url) {
        String clean = url.startsWith("git+") ? url.substring(4) : url;

        if (clean.contains("?")) clean = clean.substring(0, clean.indexOf("?"));
        if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);

        String filename = clean.substring(clean.lastIndexOf('/') + 1);

        // Clean common extensions
        filename = filename.replace(".git", "")
                .replace("-sources.jar", "")
                .replace(".jar", "")
                .replace(".zip", "")
                .replace(".tar.gz", "")
                .replace(".tgz", "");

        // Strip trailing version numbers (e.g. "lib-1.2.3" -> "lib")
        return filename.replaceAll("-[0-9.]+$", "");
    }

    /**
     * Downloads an archive to a temporary sandbox, extracts it, and moves the content to the final location.
     * <p>
     * This method uses a "sandbox" approach: files are never extracted directly to the target.
     * Instead, they are extracted to a temp folder, allowing us to inspect the structure
     * (e.g., flattening wrapper folders) before committing to the final path.
     */
    private Path downloadAndUnpack(String downloadUrl, Path targetDir) throws IOException {
        Path tempDownloadDir = null;
        Path downloadTmpFile = null;

        try {
            tempDownloadDir = Files.createTempDirectory("occtet_sandbox_");
            String fileName = FilenameUtils.getName(downloadUrl);
            if (fileName == null || fileName.isEmpty()) fileName = "downloaded_archive";

            downloadTmpFile = tempDownloadDir.resolve(fileName);

            log.info("Downloading artifact from: {}", downloadUrl);
            downloadToPath(downloadUrl, downloadTmpFile);

            if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                unpackTar(downloadTmpFile, tempDownloadDir);
            } else {
                unpackZip(downloadTmpFile, tempDownloadDir);
            }

            Files.deleteIfExists(downloadTmpFile);

            moveToFinalDestination(tempDownloadDir, targetDir);

            log.info("Installation successful at: {}", targetDir);
            return targetDir;

        } finally {
            if (tempDownloadDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(tempDownloadDir);
                } catch (IOException e) {
                    log.warn("Failed to clean up temp directory: {}", tempDownloadDir);
                }
            }
        }
    }

    private void downloadToPath(String urlString, Path destination) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(180000); // 3 minutes

        int responseCode = connection.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String newUrl = connection.getHeaderField("Location");
            downloadToPath(newUrl, destination);
            return;
        }

        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Server returned HTTP " + responseCode + " for URL: " + urlString);
        }

        try (InputStream in = connection.getInputStream();
             OutputStream out = new FileOutputStream(destination.toFile())) {
            in.transferTo(out);
        }
    }

    private void moveToFinalDestination(Path sourceDir, Path targetDir) throws IOException {
        if (Files.notExists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        List<Path> contents;
        try (Stream<Path> stream = Files.list(sourceDir)) {
            contents = stream
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .toList();
        }

        // Logic to flatten "Wrapper Folder" (e.g., github-repo-v1.0/)
        if (contents.size() == 1 && Files.isDirectory(contents.get(0))) {
            Path wrapperDir = contents.get(0);
            log.debug("Wrapper folder detected ({}); flattening structure.", wrapperDir.getFileName());
            moveRecursive(wrapperDir, targetDir);
        } else {
            moveRecursive(sourceDir, targetDir);
        }
    }

    private void moveRecursive(Path source, Path target) throws IOException {
        if (Files.notExists(target)) {
            Files.createDirectories(target);
        }

        try (Stream<Path> stream = Files.list(source)) {
            for (Path srcPath : stream.toList()) {
                Path destinationPath = target.resolve(srcPath.getFileName());
                if (Files.isDirectory(srcPath)) {
                    moveRecursive(srcPath, destinationPath);
                } else {
                    Files.move(srcPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * Extracts a ZIP archive while protecting against Zip Slip vulnerabilities.
     */
    private void unpackZip(Path archive, Path outputDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            String canonicalDest = outputDir.toFile().getCanonicalPath();

            while ((entry = zis.getNextEntry()) != null) {
                if (shouldSkipEntry(entry.getName())) {
                    continue;
                }

                File outFile = outputDir.resolve(entry.getName()).toFile();
                String canonicalOut = outFile.getCanonicalPath();

                // Security Check: Ensure the file is inside the target directory
                if (!canonicalOut.startsWith(canonicalDest + File.separator)) {
                    throw new IOException("Security Error: Zip Slip detected for entry " + entry.getName());
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();
                    if (!parent.exists()) parent.mkdirs();
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        zis.transferTo(out);
                    }
                }
            }
        }
    }

    private void unpackTar(Path archive, Path outputDir) throws IOException {
        try (InputStream fi = Files.newInputStream(archive);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(fi);
             TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            String canonicalDest = outputDir.toFile().getCanonicalPath();

            while ((entry = ti.getNextTarEntry()) != null) {
                if (shouldSkipEntry(entry.getName())) {
                    continue;
                }

                File outFile = outputDir.resolve(entry.getName()).toFile();
                String canonicalOut = outFile.getCanonicalPath();

                // Security Check
                if (!canonicalOut.startsWith(canonicalDest + File.separator)) {
                    throw new IOException("Security Error: Zip Slip detected for entry " + entry.getName());
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();
                    if (!parent.exists()) parent.mkdirs();
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        ti.transferTo(out);
                    }
                }
            }
        }
    }

    // Helper to identify purely metadata entries in archives
    private boolean shouldSkipEntry(String name) {
        return name.equals(".") || name.equals("./") || name.equals("..") || name.equals("../");
    }
}
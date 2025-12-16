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


import eu.occtet.boc.download.controller.GitRepoController;
import eu.occtet.boc.download.dao.InventoryItemRepository;
import eu.occtet.boc.download.dao.ProjectRepository;
import eu.occtet.boc.entity.InventoryItem;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.model.DownloadServiceWorkData;
import eu.occtet.boc.service.BaseWorkDataProcessor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
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
import java.util.UUID;
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
 * <li>Skips invalid or unsupported protocols (e.g., CVS, SVN, NONE).</li>
 * </ul>
 * </li>
 * <li><b>Download & Extraction:</b>
 * <ul>
 * <li>Downloads the artifact to a temporary sandbox directory.</li>
 * <li>Extracts the contents (supporting ZIP and TAR.GZ formats).</li>
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

    @Override
    public boolean process(DownloadServiceWorkData workData) {
        log.debug("DownloadService: downloading data from {} to {}", workData.getUrl(), workData.getLocation());

        try {
            Project project = projectRepository.findById(UUID.fromString(workData.getProjectId())).orElse(null);
            InventoryItem inventoryItem =
                    inventoryItemRepository.findById(UUID.fromString(workData.getInventoryItemId())).orElse(null);
            if (project == null) {
                log.error("Project not found for id: {}", workData.getProjectId());
                return false;
            }
            if (inventoryItem == null){
                log.warn("InventoryItem not found for id: {}", workData.getInventoryItemId());
            }
            // Execute download logic
            Path downloadedPath = performDownload(workData);
            if (downloadedPath == null) {
                return true;
            } else {
                fileService.createEntitiesFromPath(project, inventoryItem, downloadedPath, workData.getIsMainPackage());
                return true;
            }
        } catch (Exception e) {
            log.error("Download failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Downloads and extracts a package to a target directory based on the specified work data.
     *
     * @param workData the data containing details such as URL, version, and target location
     * @return the path to the directory where the package was downloaded*/
    private Path performDownload(DownloadServiceWorkData workData) throws IOException, InterruptedException {
        String url = workData.getUrl();
        String version = workData.getVersion();

        String downloadUrl = resolveDownloadUrl(url, version);

        if (downloadUrl == null) {
            log.warn("Skipping download. Invalid or empty URL: '{}'", url);
            return null;
        }
        // Calculate clean target dir; structure: [BasePath]/?dependencies?/[LibName]/[Version]
        Path baseLocation = Paths.get(workData.getLocation());
        if (Boolean.FALSE.equals(workData.getIsMainPackage())) {
            baseLocation = baseLocation.resolve("dependencies");
        }

        String libName = extractCleanLibraryName(url);
        Path targetDir = baseLocation.resolve(libName).resolve(version); // /[LibName]/[Version]

        return downloadAndUnpack(downloadUrl, targetDir);
    }

    /**
     * Resolves a download URL based on the provided raw URL and version. This method handles
     * various URL formats, such as git-based URLs, legacy VCS URLs, and standard file download URLs.
     * Unsupported or invalid URLs will resolve to null.
     *
     * @param rawUrl the raw URL to be resolved; it may correspond to links like
     *               git repositories, file downloads, or legacy VCS schemes.
     * @param version the version to use for constructing a specific URL in the case of git-based repositories.
     * @return the resolved download URL as a String, or null if the input is invalid or corresponds to
     *         unsupported legacy VCS formats.
     * @throws IOException if an IO exception occurs during URL resolution processing.
     * @throws InterruptedException if the current thread is interrupted during processing.
     */
    private String resolveDownloadUrl(String rawUrl, String version) throws IOException, InterruptedException {
        if (rawUrl == null ||
                rawUrl.equalsIgnoreCase("NONE") ||
                rawUrl.equalsIgnoreCase("NOASSERTION") ||
                rawUrl.isEmpty()) {
            return null;
        }
        // Handle Legacy/Unsupported VCS immediately
        if (rawUrl.startsWith("cvs") || rawUrl.contains("cvs.codehaus.org") || rawUrl.startsWith("svn")) {
            log.warn("Skipping Unsupported Legacy VCS (CVS/SVN): {}", rawUrl);
            return null;
        }

        String effectiveUrl = rawUrl;
        // Normalization: Treat "git+ssh" and plain "https://... .git" as "git+" logic
        if (effectiveUrl.startsWith("git+ssh://")) {
            effectiveUrl = effectiveUrl.replace("git+ssh://", "git+https://");
        }
        else if (effectiveUrl.startsWith("ssh://")) {
            effectiveUrl = effectiveUrl.replace("ssh://", "git+https://");
        }
        // Handle plain "https://github.com/foo/bar.git" as if it were "git+https"
        else if (effectiveUrl.startsWith("https://") && effectiveUrl.endsWith(".git")) {
            effectiveUrl = "git+" + effectiveUrl;
        }
        // If it is (or became) a "git+" URL, resolve it via API to get the ZIP URL
        if (effectiveUrl.startsWith("git+")) {
            return resolveGitToZipUrl(effectiveUrl, version);
        }
        // Standard File Download (Maven artifacts, etc.)
        return effectiveUrl;
    }

    private String resolveGitToZipUrl(String gitUrl, String version) throws IOException, InterruptedException {
        // remove "git+" prefix
        String cleanUrl = gitUrl.startsWith("git+") ? gitUrl.substring(4) : gitUrl;
        if (cleanUrl.endsWith("/")) cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);

        // Basic parsing for expected format: https://github.com/owner/repo.git or https://github.com/owner/repo
        if (!cleanUrl.contains("github.com")) {
            log.warn("Non-GitHub Git URLs are not fully supported yet for Zip download: {}", gitUrl);
            return cleanUrl;
        }
        // split to parts
        String[] parts = cleanUrl.split("/");
        if (parts.length < 2) throw new IOException("Invalid Git URL structure: " + gitUrl);
        String repoWithGit = parts[parts.length - 1];
        String owner = parts[parts.length - 2];
        String repo = repoWithGit.endsWith(".git") ? repoWithGit.substring(0, repoWithGit.length() - 4) : repoWithGit;
        // find actual ZIP URL
        String resolved = gitRepoController.getGitRepository(owner, repo, version);
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
     * Downloads to a temp folder, unpacks, flattens, and moves to target.
     * This approach ensures the final folder structure is clean.
     */
    private Path downloadAndUnpack(String downloadUrl, Path targetDir) throws IOException {
        Path tempDownloadDir = null;
        Path downloadTmpFile = null;

        try {
            // Create a fresh temp directory and file for this operation
            tempDownloadDir = Files.createTempDirectory("occtet_sandbox_");
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
            downloadTmpFile = tempDownloadDir.resolve("archive_" + UUID.randomUUID() + "_" + fileName);

            log.info("Downloading from: {}", downloadUrl);

            // Perform Download
            downloadToPath(downloadUrl, downloadTmpFile);

            // Unpack into the Sandbox Dir
            if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                unpackTar(downloadTmpFile, tempDownloadDir);
            } else {
                unpackZip(downloadTmpFile, tempDownloadDir);
            }

            // Delete the archive file so it doesn't get moved to destination
            Files.delete(downloadTmpFile);

            // Flatten and Move to Final Destination
            moveToFinalDestination(tempDownloadDir, targetDir);

            log.info("Successfully installed to: {}", targetDir);
            return targetDir;

        } finally {
            // Cleanup Temp Dir (Sandbox)
            if (tempDownloadDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(tempDownloadDir);
                } catch (IOException e) {
                    log.warn("Failed to delete temp dir: {}", tempDownloadDir);
                }
            }
        }
    }

    private void downloadToPath(String urlString, Path destination) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(60000*3); // 3min read timeout for larger files

        int responseCode = connection.getResponseCode();

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

        // Logic to flatten "Wrapper Folder" (e.g. google-auto-86f3a0)
        if (contents.size() == 1 && Files.isDirectory(contents.get(0))) {
            Path wrapperDir = contents.get(0);
            log.debug("Detected wrapper folder: {}. Flattening...", wrapperDir.getFileName());
            // Move children of wrapper to target
            moveRecursive(wrapperDir, targetDir);
        } else {
            // Move everything directly
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
                    // Move file (replace if exists to handle re-downloads)
                    Files.move(srcPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }


    private void unpackZip(Path archive, Path outputDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            String canonicalDest = outputDir.toFile().getCanonicalPath();

            while ((entry = zis.getNextEntry()) != null) {
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
}
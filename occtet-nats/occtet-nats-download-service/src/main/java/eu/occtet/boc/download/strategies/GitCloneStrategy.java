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

package eu.occtet.boc.download.strategies;

import com.github.packageurl.PackageURL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Strategy for cloning Git repositories (GitHub, GitLab, Bitbucket, self-hosted Git, etc.)
 * and checking out the specified version/tag/commit.
 */
@Component
@Order(3)
public class GitCloneStrategy implements DownloadStrategy {

    private final Logger log = LogManager.getLogger(this.getClass());
    private static final int TIMEOUT_MINUTES = 5;

    @Override
    public boolean canHandle(URL durl, String version) {
        if (durl == null) return false;
        String urlString = durl.toString().toLowerCase();
        boolean canHandle = urlString.contains(".git") ||
                urlString.startsWith("git://") ||
                urlString.startsWith("http://") ||
                urlString.startsWith("https://");
        log.debug("Checking if {} can handle URL: {}. Result: {}", this.getClass().getSimpleName(), durl, canHandle);
        return canHandle;
    }

    @Override
    public boolean canHandle(PackageURL purl) {
        if (purl == null) return false;
        boolean canHandle = "git".equalsIgnoreCase(purl.getType()) ||
                "github".equalsIgnoreCase(purl.getType()) ||
                "gitlab".equalsIgnoreCase(purl.getType()) ||
                "bitbucket".equalsIgnoreCase(purl.getType());
        log.debug("Checking if {} can handle PURL: {}. Result: {}", this.getClass().getSimpleName(), purl, canHandle);
        return canHandle;
    }

    @Override
    public Path download(URL durl, String version, Path targetDirectory) throws IOException {
        String rawUrl = durl.toString();
        log.info("Executing {}.download for URL: {} @ {}", this.getClass().getSimpleName(), rawUrl, version);

        String cloneUrl = rawUrl;
        String ref = version;

        // If URL contains @ separator for ref/commit (e.g. https://github.com/org/repo.git@commitHash)
        if (rawUrl.contains("@") && !rawUrl.startsWith("git@") && !rawUrl.startsWith("ssh://git@")) {
            int atIndex = rawUrl.lastIndexOf('@');
            cloneUrl = rawUrl.substring(0, atIndex);
            String urlRef = rawUrl.substring(atIndex + 1);
            if (!urlRef.isBlank()) {
                ref = urlRef;
            }
        }

        if (cloneUrl.startsWith("git+")) {
            cloneUrl = cloneUrl.substring(4);
        }

        return executeGitClone(cloneUrl, ref, targetDirectory);
    }

    @Override
    public Path download(PackageURL purl, Path targetDirectory) throws IOException {
        String cloneUrl;
        String ref = purl.getVersion();

        if ("github".equalsIgnoreCase(purl.getType())) {
            cloneUrl = String.format("https://github.com/%s/%s.git", purl.getNamespace(), purl.getName());
        } else if ("gitlab".equalsIgnoreCase(purl.getType())) {
            cloneUrl = String.format("https://gitlab.com/%s/%s.git", purl.getNamespace(), purl.getName());
        } else if ("bitbucket".equalsIgnoreCase(purl.getType())) {
            cloneUrl = String.format("https://bitbucket.org/%s/%s.git", purl.getNamespace(), purl.getName());
        } else {
            cloneUrl = purl.getQualifiers() != null ? purl.getQualifiers().get("repository_url") : null;
            if (cloneUrl == null) {
                throw new IOException("Cannot determine clone URL for PURL: " + purl);
            }
        }

        return executeGitClone(cloneUrl, ref, targetDirectory);
    }

    private Path executeGitClone(String cloneUrl, String ref, Path targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory);

        Path sandboxDir = Files.createTempDirectory("occtet_git_clone_");
        log.info("Cloning {} (ref: {}) into sandbox {}", cloneUrl, ref, sandboxDir);

        try {
            boolean cloneSuccess = false;

            // 1. Try shallow clone with specific branch/tag if ref is provided
            if (ref != null && !ref.isBlank() && !"NONE".equalsIgnoreCase(ref) && !"NOASSERTION".equalsIgnoreCase(ref)) {
                log.info("Attempting shallow clone with branch/tag: {}", ref);
                List<String> shallowBranchCmd = List.of("git", "clone", "--depth", "1", "--branch", ref, cloneUrl, sandboxDir.toString());
                int exitCode = runProcess(shallowBranchCmd, null);
                if (exitCode == 0) {
                    cloneSuccess = true;
                } else {
                    log.warn("Shallow clone with --branch {} failed (might be a commit SHA). Falling back to full clone + checkout...", ref);
                    FileSystemUtils.deleteRecursively(sandboxDir);
                    Files.createDirectories(sandboxDir);
                }
            }

            // 2. If branch clone failed or no branch ref, clone with depth and checkout ref
            if (!cloneSuccess) {
                log.info("Attempting clone of {}", cloneUrl);
                List<String> cloneCmd = List.of("git", "clone", "--depth", "50", cloneUrl, sandboxDir.toString());
                int exitCode = runProcess(cloneCmd, null);
                if (exitCode != 0) {
                    FileSystemUtils.deleteRecursively(sandboxDir);
                    Files.createDirectories(sandboxDir);
                    List<String> fullCloneCmd = List.of("git", "clone", cloneUrl, sandboxDir.toString());
                    exitCode = runProcess(fullCloneCmd, null);
                    if (exitCode != 0) {
                        throw new IOException("Failed to git clone from " + cloneUrl);
                    }
                }

                if (ref != null && !ref.isBlank() && !"NONE".equalsIgnoreCase(ref) && !"NOASSERTION".equalsIgnoreCase(ref)) {
                    log.info("Checking out ref: {}", ref);
                    List<String> checkoutCmd = List.of("git", "checkout", ref);
                    int checkoutExit = runProcess(checkoutCmd, sandboxDir);
                    if (checkoutExit != 0) {
                        log.warn("Failed to checkout ref {}, using default cloned branch", ref);
                    }
                }
            }

            // 3. Remove .git folder so Git metadata is not indexed in file tree
            Path gitDir = sandboxDir.resolve(".git");
            if (Files.exists(gitDir)) {
                log.debug("Removing .git metadata directory from sandbox");
                FileSystemUtils.deleteRecursively(gitDir);
            }

            // 4. Move files from sandbox to targetDirectory
            moveContents(sandboxDir, targetDirectory);
            log.info("Successfully cloned and moved repository contents to {}", targetDirectory);

            return targetDirectory;
        } finally {
            FileSystemUtils.deleteRecursively(sandboxDir);
        }
    }

    private void moveContents(Path source, Path target) throws IOException {
        if (Files.notExists(target)) {
            Files.createDirectories(target);
        }
        try (Stream<Path> stream = Files.list(source)) {
            for (Path srcPath : stream.toList()) {
                Path destinationPath = target.resolve(srcPath.getFileName());
                if (Files.isDirectory(srcPath)) {
                    moveContents(srcPath, Files.createDirectories(destinationPath));
                } else {
                    Files.move(srcPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private int runProcess(List<String> command, Path workingDir) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.redirectErrorStream(true);

        log.debug("Executing command: {}", String.join(" ", command));
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        try {
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("Command timed out after {} minutes: {}", TIMEOUT_MINUTES, String.join(" ", command));
                return -1;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.debug("Command failed with exit code {}. Output: {}", exitCode, output.toString().trim());
            }
            return exitCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Process interrupted: " + String.join(" ", command), e);
        }
    }
}

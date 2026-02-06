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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A service for handling archive files, including unpacking and processing of ZIP, JAR, and TAR.GZ files.
 * <br>
 * The service identifies supported archive file formats based on their extensions:
 * <ul>
 *     <li>ZIP and JAR archives (with .zip or .jar extensions)</li>
 *     <li>TAR.GZ archives (with .tar.gz extension)</li>
 * </ul><br>
 */
@Service
public class ArchiveService {

    private final Logger log = LogManager.getLogger(this.getClass());

    private final static String ZIP_EXTENSION = ".zip";
    private final static String TAR_GZ_EXTENSION = ".tar.gz";
    private final static String JAR_EXTENSION = ".jar";

    public void unpack(Path sourceArchive, Path finalTargetDir) throws IOException {
        Path sandboxDir = Files.createTempDirectory("occtet_sandbox_");
        log.debug("Sandboxing extraction for {} to {}", sourceArchive, sandboxDir);

        try {
            String filename = sourceArchive.getFileName().toString().toLowerCase();
            if (filename.endsWith(ZIP_EXTENSION) || filename.endsWith(JAR_EXTENSION)) {
                unpackZip(sourceArchive, sandboxDir);
            } else if (filename.endsWith(TAR_GZ_EXTENSION)) {
                unpackTarGz(sourceArchive, sandboxDir);
            } else {
                throw new IOException("Unsupported archive type: " + filename);
            }
            moveToFinalDestination(sandboxDir, finalTargetDir);
        } finally {
            FileSystemUtils.deleteRecursively(sandboxDir);
        }
    }

    private void moveToFinalDestination(Path sandbox, Path target) throws IOException {
        log.debug("Moving contents of {} to {}", sandbox, target);
        if (Files.notExists(target)) Files.createDirectories(target);

        List<Path> contents;

        // Filter hidden files
        try (Stream<Path> stream = Files.list(sandbox)) { // TODO remove if hidden files are needed
            contents = stream.filter(p -> !p.getFileName().toString().startsWith(".")).toList();
        }
        if (contents.size() == 1 && Files.isDirectory(contents.getFirst())) {
            Path wrapperDir = contents.getFirst();
            log.debug("Moving children of {} to {}", wrapperDir, target);
            moveChildren(wrapperDir, target);
        } else {
            log.debug("Moving children of {} to {}", sandbox, target);
            moveChildren(sandbox, target);
        }
    }

    private void moveChildren(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.list(source)) {
            for (Path srcPath : stream.toList()) {
                Path destinationPath = target.resolve(srcPath.getFileName());
                if (Files.isDirectory(srcPath)) {
                    moveChildren(srcPath, Files.createDirectories(destinationPath));
                } else {
                    Files.move(srcPath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void unpackZip(Path archive, Path outputDir) throws IOException {
        log.debug("Unpacking zip, Extracting {} to {}", archive, outputDir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(archive))){
            ZipEntry zipEntry;
            String canonicalDest = outputDir.toFile().getCanonicalPath();
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (shouldSkip(zipEntry.getName())) continue;

                File outFile = outputDir.resolve(zipEntry.getName()).toFile();
                if (!outFile.getCanonicalPath().startsWith(canonicalDest + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }
                if (zipEntry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        zis.transferTo(out);
                    }
                }
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void unpackTarGz(Path archive, Path outputDir) throws IOException {
        log.debug("Unpacking tar, Extracting {} to {}", archive, outputDir);
        try (InputStream inputStream = Files.newInputStream(archive);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(inputStream);
             TarArchiveInputStream ti = new TarArchiveInputStream(gzi)){
            TarArchiveEntry tarArchiveEntry;
            String canonicalDest = outputDir.toFile().getCanonicalPath();
            while ((tarArchiveEntry = ti.getNextEntry()) != null){
                if (shouldSkip(tarArchiveEntry.getName())) continue;
                File outFile = outputDir.resolve(tarArchiveEntry.getName()).toFile();
                if (!outFile.getCanonicalPath().startsWith(canonicalDest + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + tarArchiveEntry.getName());
                }
                if (tarArchiveEntry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        ti.transferTo(out);
                    }
                }
            }
        }
    }

    private boolean shouldSkip(String name) {
        // TODO determine what to skip
        return false;
    }
}

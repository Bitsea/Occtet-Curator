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
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PackageService {

    private static final Logger log = LoggerFactory.getLogger(PackageService.class);

    @Autowired
    private GitRepoController gitRepoController;

    private File tmpFile;

    private final static Path tmpLocation = Path.of("src","main","resources/TmpFilePackage");

    public void unpackZipFile(String url, String location) {

        String fileName = url.substring(url.lastIndexOf('/')+1);
        log.info("File name: {}",fileName);
        log.info("Location: {}",location);
        setTmpFile(url,fileName);

        try{
            Path savePath = Paths.get(location);
            if(Files.notExists(savePath)){
                Files.createDirectories(Path.of(savePath.toFile().getAbsolutePath()));
            }

            ZipInputStream zip = new ZipInputStream(new FileInputStream(tmpFile));
            ZipEntry entry = zip.getNextEntry();
            File folder = new File(savePath.toFile().getAbsolutePath(),fileName);

            while(entry != null){
                File outFile = new File(folder, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parentFile = outFile.getParentFile();
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                }
                try {
                    FileOutputStream outStream = new FileOutputStream(outFile);
                    zip.transferTo(outStream);
                }catch (Exception e){
                    log.error(e.getMessage());
                }
                entry = zip.getNextEntry();
            }zip.close();
            Files.deleteIfExists(tmpFile.toPath());
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public void unpackTarGzFile(String url, String location){

        String fileName = url.substring(url.lastIndexOf('/')+1);
        setTmpFile(url,fileName);

        try {
            Path savePath = Paths.get(location);
            if(Files.notExists(savePath)){
                Files.createDirectories(Path.of(savePath.toFile().getAbsolutePath()));
            }

            GzipCompressorInputStream gz = new GzipCompressorInputStream(new FileInputStream(tmpFile));
            TarArchiveInputStream tar = new TarArchiveInputStream(gz);
            TarArchiveEntry entry = tar.getNextTarEntry();
            File folder = new File(savePath.toFile().getAbsolutePath(),fileName);

            while(entry != null) {
                File outFile = new File(folder, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parentFile = outFile.getParentFile();
                    if (parentFile != null && !parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    try{
                        OutputStream outStream = new FileOutputStream(outFile);
                        tar.transferTo(outStream);
                    }catch (Exception e){
                        log.error(e.getMessage());
                    }
                }
                entry = tar.getNextTarEntry();
            }tar.close();
            Files.deleteIfExists(tmpFile.toPath());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void unpackGitZipRepo(String url, String location,String version){

        try{
            String[] splitUrl = url.split("/");
            String owner = splitUrl[splitUrl.length-2];
            String repoGit = splitUrl[splitUrl.length-1];
            int index = repoGit.lastIndexOf('.');
            String repo = repoGit.substring(0,index);

            String gitUrl = gitRepoController.getGitRepository(owner,repo,version);
            log.info("Git url: {}",gitUrl);

            if(gitUrl.isEmpty()){
                log.error("Git url is null or empty...");
            }else{
                unpackZipFile(gitUrl,location);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void setTmpFile(String urlString, String fileName) {

        try {
            if(Files.notExists(tmpLocation)){
                Files.createDirectories(Path.of(tmpLocation.toFile().getAbsolutePath()));
            }
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            InputStream inputStream = connection.getInputStream();
            tmpFile = new File(tmpLocation.toFile().getAbsolutePath(),fileName);
            FileOutputStream outputStream = new FileOutputStream(tmpFile);
            inputStream.transferTo(outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}


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
import eu.occtet.boc.download.dao.ProjectRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


@Service
@Transactional
public class DownloadService extends BaseWorkDataProcessor {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    @Autowired
    private GitRepoController gitRepoController;
    @Autowired
    private FileService fileService;
    @Autowired
    private ProjectRepository projectRepository;

    private String gitUrl;

    private final static Path tmpLocation = Path.of("src","main","resources/TmpFilePackage");

    @Override
    public boolean process(DownloadServiceWorkData workData) {
        log.debug("DownloadService: downloading data from {} to {}", workData.getUrl(),workData.getLocation());
        Path downloadedPath = storeData(workData);
        if (downloadedPath != null) {
            Project project = projectRepository.findById(UUID.fromString(workData.getProjectId()))
                    .orElse(null);

            if (project != null) {
                fileService.createEntitiesFromPath(project, downloadedPath);
                return true;
            }
        }
        return false;
    }

    public Path storeData(DownloadServiceWorkData workData){

        try{
            String analyzedUrl = analyzingUrl(workData.getUrl());

            if(analyzedUrl.equals("git")) {
                getGitUrl(workData.getUrl(), workData.getVersion());
                return downloadFile(gitUrl,workData.getLocation());
            }else{
                return downloadFile(workData.getUrl(), workData.getLocation());
            }
        }catch (Exception e){
            log.error(e.getMessage());
            return null;
        }
    }

    private String analyzingUrl(String url){
        int index = url.lastIndexOf('.');
        return url.substring(index+1);
    }

    private void getGitUrl(String url, String version){
        try{
            String[] splitUrl = url.split("/");
            String owner = splitUrl[splitUrl.length-2];
            String repoGit = splitUrl[splitUrl.length-1];
            int index = repoGit.lastIndexOf('.');
            String repo = repoGit.substring(0,index);

            gitUrl = gitRepoController.getGitRepository(owner,repo,version);
            log.info("Git url: {}",gitUrl);

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private Path downloadFile(String urlString, String location){

        try{
            String fileName = urlString.substring(urlString.lastIndexOf('/')+1);
            if(Files.notExists(tmpLocation)){
                Files.createDirectories(Path.of(tmpLocation.toFile().getAbsolutePath()));
            }

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            InputStream inputStream = connection.getInputStream();
            File tmpFile = new File(tmpLocation.toFile().getAbsolutePath(),fileName);
            FileOutputStream outputStream = new FileOutputStream(tmpFile);
            inputStream.transferTo(outputStream);
            inputStream.close();
            outputStream.close();

            Path savePath = Paths.get(location);
            if(Files.notExists(savePath)){
                Files.createDirectories(Path.of(savePath.toFile().getAbsolutePath()));
            }

            File targetFolder = new File(savePath.toFile().getAbsolutePath(),fileName);

            if(fileName.contains(".")){
                if(fileName.substring(fileName.lastIndexOf('.')).equals(".gz")){
                    unpackTarFile(tmpFile, savePath, fileName);
                }else{
                    unpackZipFile(tmpFile,savePath,fileName);
                }
            }else{
                unpackZipFile(tmpFile,savePath,fileName);
            }
            log.info("File downloaded: {}",fileName);
            log.info("File downloaded and unpacked to: {}", targetFolder.getAbsolutePath());
            return targetFolder.toPath();
        }catch(Exception e) {
            log.error("Error in download file; {}",e.getMessage());
            return null;
        }
    }

    private void unpackZipFile(File tmpFile, Path path, String fileName){

        try{
            ZipInputStream zis = new ZipInputStream(new FileInputStream(tmpFile));
            ZipEntry entry = zis.getNextEntry();
            File folder = new File(path.toFile().getAbsolutePath(),fileName);

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
                        zis.transferTo(outStream);
                        outStream.close();
                    }catch (Exception e){
                        log.error(e.getMessage());
                    }
                }
                entry = zis.getNextEntry();
            }zis.close();
            Files.deleteIfExists(tmpFile.toPath());
        }catch(Exception e){
            log.error("Error in unpackZipFile: {}",e.getMessage());
        }
    }

    private void unpackTarFile(File tmpFile, Path path, String fileName){

        try {

            GzipCompressorInputStream gz = new GzipCompressorInputStream(new FileInputStream(tmpFile));
            TarArchiveInputStream tar = new TarArchiveInputStream(gz);
            TarArchiveEntry entry = tar.getNextTarEntry();
            File folder = new File(path.toFile().getAbsolutePath(),fileName);

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
                        outStream.close();
                    }catch (Exception e){
                        log.error(e.getMessage());
                    }
                }
                entry = tar.getNextTarEntry();
            }tar.close();
            Files.deleteIfExists(tmpFile.toPath());
        } catch (Exception e) {
            log.error("Error in unpackTarFile: {}",e.getMessage());
        }
    }
}
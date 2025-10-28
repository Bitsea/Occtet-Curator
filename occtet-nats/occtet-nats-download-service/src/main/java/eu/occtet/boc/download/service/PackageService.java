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
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PackageService {

    private static final Logger log = LoggerFactory.getLogger(PackageService.class);

    @Autowired
    private GitRepoController gitRepoController;

    public void unpackZipFile(String url, String location) {

        String fileName = location.substring(url.lastIndexOf('/'),location.length()-1);
        File file = getZipFile(url,fileName);

        try{
            ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
            ZipEntry entry = zip.getNextEntry();

            while(entry != null){
                File outFile = new File(location, entry.getName());

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
            }
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }

    public void unpackTarGzFile(String url, String location){

        String fileName = location.substring(url.lastIndexOf('/'),location.length()-1);
        File file = getZipFile(url,fileName);

        try {
            GzipCompressorInputStream gz = new GzipCompressorInputStream(new FileInputStream(file));
            TarArchiveInputStream tar = new TarArchiveInputStream(gz);
            TarArchiveEntry entry = tar.getNextTarEntry();

            while(entry != null) {
                File outFile = new File(location, entry.getName());

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
            }
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

            URI uri = gitRepoController.getGitRepository(owner,repo,version);
            unpackZipFile(uri.toString(),location);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private File getZipFile(String urlString, String fileName) {

        File file = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            InputStream inputStream = connection.getInputStream();
            file = new File(fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            inputStream.transferTo(outputStream);
            inputStream.close();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return file;
    }
}


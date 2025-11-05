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
package eu.occtet.boc.informationFile.service;

import eu.occtet.boc.informationFile.dao.InformationFileDao;
import eu.occtet.boc.informationFile.dao.InformationFileRepository;
import eu.occtet.boc.entity.InformationFile;
import eu.occtet.boc.informationFile.factory.InformationFileFactory;
import eu.occtet.boc.informationFile.retriever.RagHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class InformationFileService {

    @Autowired
    private InformationFileRepository informationFileRepository;

    @Autowired
    private InformationFileFactory informationFileFactory;

    @Autowired
    private InformationFileDao informationFileDao;

    @Autowired
    VectorStore vectorStore;

    private static final Logger log = LogManager.getLogger(InformationFileService.class);
    private static final int LIMIT= 20;

    /**
     * upload files as information for ai from a directory and given context
     * @param path
     * @param context
     * @return
     */
    public Boolean uploadFiles(String path, String context){
        List<InformationFile> files = new ArrayList<>();
        try {
            log.debug("searching for files {}", path);
            List<String> filePaths = iterateProject(path);
            HashMap<String, String> contents = readFileContent(filePaths);
            for (String p : contents.keySet()) {
                InformationFile infoFile = informationFileFactory.createInfoFile(getFileName(p), context, contents.get(p), p);
                informationFileRepository.save(infoFile);
                files.add(infoFile);
                Document d = new Document(infoFile.getContent(), Map.of("fileName", infoFile.getFileName()));
                d.getMetadata().put("filePath", infoFile.getFileName());
                d.getMetadata().put("context", infoFile.getContext());
                vectorStore.add(RagHelper.splitCustomized(List.of(d)));

            }
        }catch(Exception e){
            log.debug("something went wrong when reading {} / message: {}", path, e.getMessage());
        }
        log.debug("added {} files into DB", files.size());
        return !files.isEmpty();
    }

    private HashMap<String, String>  readFileContent(List<String> path){
        HashMap<String, String> contents= new HashMap<>();
        for(String p: path) {
            log.debug("Reading {}", p);
            StringBuilder resultStringBuilder = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(p)));
                String line;
                while ((line = br.readLine()) != null) {
                    resultStringBuilder.append(line).append("\n");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.debug("CONTENT {}", resultStringBuilder);
            contents.put(p, resultStringBuilder.toString());
        }

        return contents;



    }

    private List<String> iterateProject(String filePath) {

        List<String> filePaths = new ArrayList<>();
        try {
            Path path = Paths.get(filePath);
            log.debug("Analyzing files in path {}", path.toString());
            File dir = new File(filePath);
            if(!dir.isFile() && (!dir.isDirectory() || !dir.canRead()) ) {
                log.debug("cannot analyze non-directory or non-readable file {}",dir);
                return null;
            }
            if(dir.isFile()){
                log.debug("add path {}", dir.getAbsolutePath());
                filePaths.add(dir.getAbsolutePath());
            } else {
                for (File f : dir.listFiles()) {
                    if (f.isFile() && f.canRead() && !f.isDirectory()) {
                        filePaths.add(f.getAbsolutePath());

                    } else if (f.isDirectory() && f.canRead()) {
                        // recursion
                        log.debug("analyzing directory {}...", f.getAbsolutePath());
                        filePaths.addAll(iterateProject(f.getAbsolutePath()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not load file {} for analyzing java content {}", filePath, e);
            return null;
        }

        return filePaths;
    }

    private String getFileName(String path){
        String pattern = Pattern.quote(FileSystems.getDefault().getSeparator());
        String[] pArray = path.split(pattern);
        String fileName= pArray[pArray.length-1];
        log.debug("fileName {}", fileName);
        return fileName;
    }


    /**
     * retrieve data from normal DB via similarity search for context
     * @param context
     * @return
     */
    public List<InformationFile> retrieveDataByContext(String context){

        List<Pair<Long, Float>> files= informationFileDao.findInformationFileContentSimilarity(context, LIMIT);
        log.debug("retrieved files from db {}", files.size());
        return files.stream().map(
                uuidFloatPair ->{
                    InformationFile infoF=informationFileRepository.findById(uuidFloatPair.getKey()).get();
                    return infoF;
                }).toList();

    }
}


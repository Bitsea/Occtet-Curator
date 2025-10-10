package eu.occtet.boc.ai.copyrightFilter.service;


import eu.occtet.boc.ai.copyrightFilter.dao.InformationFileDao;
import eu.occtet.boc.ai.copyrightFilter.dao.InformationFileRepository;
import eu.occtet.boc.ai.copyrightFilter.factory.InformationFileFactory;
import eu.occtet.boc.ai.copyrightFilter.retriever.RagHelper;
import eu.occtet.boc.entity.InformationFile;
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
public class InformationFilesService {

    @Autowired
    private InformationFileRepository informationFileRepository;

    @Autowired
    private InformationFileFactory informationFileFactory;

    @Autowired
    private InformationFileDao informationFileDao;

    @Autowired
    VectorStore vectorStore;

    private static final Logger log = LogManager.getLogger(InformationFilesService.class);
    private static final int LIMIT= 20;


    /**
     * retrieve data from normal DB via similarity search for context
     * @param context
     * @return
     */
    public List<InformationFile> retrieveDataByContext(String context){

        List<Pair<UUID, Float>> files= informationFileDao.findInformationFileContentSimilarity(context, LIMIT);
        log.debug("retrieved files from db {}", files.size());
        return files.stream().map(
                        uuidFloatPair ->{
                            InformationFile infoF=informationFileRepository.findById(uuidFloatPair.getKey()).get();
                            return infoF;
                        }).toList();

    }

}

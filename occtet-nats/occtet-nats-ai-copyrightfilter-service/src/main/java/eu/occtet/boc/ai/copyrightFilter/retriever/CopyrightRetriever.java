package eu.occtet.boc.ai.copyrightFilter.retriever;


import eu.occtet.boc.ai.copyrightFilter.service.InformationFilesService;
import eu.occtet.boc.entity.InformationFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CopyrightRetriever {

    private static final Logger log = LogManager.getLogger(CopyrightRetriever.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private InformationFilesService informationFilesService;

    public CopyrightRetriever(){}

    /**
     * Method for performing similaritySearch on the configured VectorStore.
     * Mainly used for testing the functionality of the VectorStore.
     * @param topK
     * @param similarityThreshold
     * @return
     */
    public List<Document> similaritySearch( int topK, double similarityThreshold, String query, String filterExpression) {

        log.debug("similaritySearch");
        SearchRequest searchRequest = buildSearchRequest(topK, similarityThreshold, query, filterExpression);

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.info("successfully performed similaritySearch size list: {}  ", results.size());
        for(Document d: results){
            log.debug("CONTENT {}", d.getFormattedContent());

        }
        return results;
    }

    /**
     * Builds a QuestionAnswerAdvisor for use by the LLM. The Advisor performs similarity search over all documents within the configured VectorStore
     * @param topK max number of results returned by the advisor
     * @param similarityThreshold Score to filter documents by: Only documents with similarity score equal or greater than the 'threshold' will be returned.  0.0 means any similarity is accepted. Value of 1.0 means an exact match is required.
     * @return a new QuestionAnswerAdvisor instance
     */
    public QuestionAnswerAdvisor buildQuestionAnswerAdvisor(int topK, double similarityThreshold, String query, String filterExpression){

        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .userTextAdvise(query + "question_answer_context")
                .searchRequest(buildSearchRequest(topK, similarityThreshold, query, filterExpression))
                .build();
        log.info("successfully created a QuestionAnswerAdvisor for the llm");
       return questionAnswerAdvisor;
    }

    private SearchRequest buildSearchRequest(int topK, double similarityThreshold, String query, String filterexpression){
        return SearchRequest.builder()
                .filterExpression(filterexpression)
                .topK(topK)
                .query(query)
                .similarityThreshold(similarityThreshold)
                .build();
    }

    /**
     * loading copyright files into the vector store with a context helping for embedding and retrieval
     * has to be changed, so the path to documents is not hardcoded
     * @param context
     * @throws IOException
     */
    public void loadVectorStore(String context)  {
        List<Document> documentList = new ArrayList<>();

        List<InformationFile> files = informationFilesService.retrieveDataByContext(context);
        //if there are no files no upload needed
        if(!files.isEmpty()) {
            log.debug("Files from DB {} first filename {}", files.size(), files.getFirst().getFileName());

            List<Document> documents = new ArrayList<>();
            try {
                for (InformationFile f : files) {
                    Document d = new Document(f.getContent(), Map.of("fileName", f.getFileName()));
                    d.getMetadata().put("filePath", f.getFileName());
                    d.getMetadata().put("context", f.getContext());
                    documents.add(d);
                 }

            } catch (Exception e) {
                log.error("Error while reading the files : {}", e.getMessage());
            }
            log.debug("Files added to list {} metadata {}", documents.size(), documents.getFirst().getMetadata());

            documentList = RagHelper.splitCustomized(documents);

            log.debug("documentlist {} metadata {}", documentList.size(), documentList.getFirst().getMetadata());

            vectorStore.add(documentList);
        }
        else{
            log.info("No files found to load into Vector-DB");
        }
    }


}

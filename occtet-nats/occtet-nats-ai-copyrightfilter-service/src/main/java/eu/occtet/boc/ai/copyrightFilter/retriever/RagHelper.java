package eu.occtet.boc.ai.copyrightFilter.retriever;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;


public final class RagHelper {

    private static final Logger log = LogManager.getLogger(RagHelper.class);


    /**
     * default
     * splits the documents before putting them into the vector store
     * @param documents
     * @return
     */
    public static List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    /**
     * customized chunksize etc.
     * splits the documents before putting them into the vector store
     * @param documents
     * @return
     */
    public static List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(350, 100, 10, 500, true);
        for(Document d: documents) {
            log.debug("2 Files added to list {} metadata {}", documents.size(), d.getMetadata());
        }
        return splitter.apply(documents);
    }
}

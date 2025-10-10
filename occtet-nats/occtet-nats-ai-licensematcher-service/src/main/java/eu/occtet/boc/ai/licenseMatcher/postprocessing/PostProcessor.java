package eu.occtet.boc.ai.licenseMatcher.postprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PostProcessor {


    private static final Logger log = LoggerFactory.getLogger(PostProcessor.class);
    private static final String CONFIG_NAME_FALSE_CP="copyright-garbage.yml";
    private static final int thinkingCounter= 8;



    /**
     * models often have a <think>thinking part</think> in their response, here it gets deleted
     * @param response
     * @return
     */
    public String deleteThinking(String response){
        log.debug("delete thinking part: {}", response);
        String noThinking="";

        if (response.contains("<think>")) {
            noThinking = response.replace(response.substring(response.indexOf("<think>"), response.indexOf("</think>") + thinkingCounter), "");

            log.debug("NoThinking: {}", noThinking);
        } else noThinking= response;


        return noThinking;
    }

    /**
     * the String list is the concatenated responses of the ai, here the responses get cleaned
     * and copyrights getting separately into a new list
     * @param result
     * @return list of copyright strings
     */
    public List<String> cleanResults(String result){
        try {

            // concatenate the responses
            List<String> copyrights = new ArrayList<>();

            //remove start and end brackets in sub responses to have a valid json at the end
            String[] resultList = result.split("\\|\\|\\|");
            for (String r : resultList) {
                r = r.trim().replace("\n", "");
                if (!r.isEmpty())
                    copyrights.add(r);
            }

            List<String> noDuplicates= copyrights.stream().distinct().toList();
            List<String> finalList = new ArrayList<>();
           for(String s: noDuplicates) {
               if(!(s.length()<2)) {
                   log.debug("String added '{}'",s);
                   finalList.add(s);
               }
           }
           log.debug("final list size {}", finalList.size());
            return finalList;
        }catch(Exception e){
            log.error("result could not be processed: {}", e.getMessage());
            return null;
        }
    }

}

package eu.occtet.boc.ai.licenseMatcher.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromptFactory {
    private static final Logger log = LoggerFactory.getLogger(PromptFactory.class);


    public Prompt createLicenseMatcherPrompt(String userMessage, String url){
        try {

            Message userMsg = new UserMessage(userMessage);

            String systemText = """
                    Compare two texts by the following rules.
                    Compare the license text from the user to the original license text provided by\s
                    the SPDXLicenseDetail object. Use your tool calling to retrieve the SPDXLicenseDetail from the web page of the license, use this {url} for retrieving. You have to use the standardLicenseTemplate from the SPDXLicenseDetail object.\s
                    In this template are two special kind of text passages, that you have to discern in the license text, which are marked in the standardLicenseTemplate like that:
                    The omittable text passage:Some licenses have text that can simply be ignored. The intent here is to avoid the inclusion of certain text that is superfluous or irrelevant in regards to the substantive license text resulting in a non-match where the license is otherwise an exact match (e.g., directions on how to apply the license or other similar exhibits). In these cases, there should be a positive license match.
                   \s
                                               The license should be considered a match if the text indicated is present and matches OR the text indicated is missing altogether.
                   \s
                                               The following XML tag is used to implement this guideline: <beginOptional>
                   \s
                                               For example: <beginOptional>Apache License Version 2.0, January 2004 http://www.apache.org/licenses/</endOptional>
                   \s
                    The replaceable text passage: Some licenses include text that refers to the specific copyright holder or author, yet the rest of the license is exactly the same. The intent here is to avoid the inclusion of a specific name in one part of the license resulting in a non-match where the license is otherwise an exact match to the legally substantive terms (e.g., the third clause and disclaimer in the BSD licenses, or the third, fourth, and fifth clauses of Apache-1.1). In these cases, there should be a positive license match.
                   \s
                                                 The text indicated as such can be replaced with similar values (e.g., a different name or generic term; different date) and still be considered a positive match. This rule also applies to text-matching in official license headers, see Guideline: official license headers.
                   \s
                                                 The following XML tag is used to implement this guideline. <var> with 3 attributes:
                   \s
                                                     match - a POSIX extended regular expression (ERE) to match the replaceable text
                                                     name - an identifier for the variable text unique to the license XML document, like a regex pattern
                                                     original - placeholder in original text, which can be modified
                   \s
                                                 The original text is enclosed within the beginning and ending alt tags.
                   \s
                                                 For example: <var;name=\\"copyright\\";original=\\"Copyright (C) YEAR by AUTHOR EMAIL  \\";match=\\".(0,5000)\\">
                   \s
                                                 The original replaceable text appears on the SPDX License List webpage in red text.
                                                \s
                    Otherwise the license text should be the same verbatim text (exception for the two passages). The text should be in the same order, e.g., differently ordered paragraphs would not be considered a match.
                   \s
                    Besides that information, you are given the result of the SPDX license matcher, which is comparing license texts rule-based. The DifferenceDescription result
                    object is showing the differences this rule-based method found, which do not match the original license text. Use it to help you find the exact words or text passage, that are not fitting.\s
                   
                   \s""";


            SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
            Message systemMessage = systemPromptTemplate.createMessage(Map.of("url", url));
            Prompt prompt = new Prompt(List.of(systemMessage, userMsg));
            log.debug("created prompt");
            return prompt;
        }catch(Exception e){
            log.error("There is an error in prompting {}", e.getMessage());
            return null;
        }




    }

}

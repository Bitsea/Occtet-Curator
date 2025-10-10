package eu.occtet.boc.licenseMatcher.factory;

import org.spdx.utility.compare.CompareTemplateOutputHandler;
import org.springframework.stereotype.Component;

@Component
public class QuestionFactory {

    public String createLicenseMatcherUserQuestion(CompareTemplateOutputHandler.DifferenceDescription result, String licenseText) {



            String userMessage = "Use the SPDXLicenseDetail object from your tool to confirm, " +
                    "if the license text from the user corresponds to the original SPDX license text." +
                    "Answer with the word 'true', if the license text is equivalent. Answer with the word 'false', if the license text is not equivalent. " +
                    "Also add the the differing text passage if the answer is 'false'. Add the sign '|||' between answer and the differing text passages. " +
                    "Do not add explanations or any comments to 'false' or 'right' and the differing passage" +
                    "Here is the DifferenceDescription object, which contains the differences found by the rule-based approach to find the modified text passages: " + result +
                    ". Here is the license text to compare: " + licenseText;

            return userMessage;

        }

    }

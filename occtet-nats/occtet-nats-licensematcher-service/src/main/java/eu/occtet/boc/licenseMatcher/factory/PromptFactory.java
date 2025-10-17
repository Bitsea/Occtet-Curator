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

package eu.occtet.boc.licenseMatcher.factory;

import org.spdx.utility.compare.CompareTemplateOutputHandler;
import org.springframework.stereotype.Component;

@Component
public class PromptFactory {


    public String createUserMessage(CompareTemplateOutputHandler.DifferenceDescription result){



            return  "Use the SPDXLicenseDetail object from your tool to confirm, " +
                    "if the license text from the user corresponds to the original SPDX license text." +
                    "Answer with the word 'true', if the license text is equivalent. Answer with the word 'false', if the license text is not equivalent. " +
                    "Also add the the differing text passage if the answer is 'false'. Add the sign '|||' between answer and the differing text passages. " +
                    "Do not add explanations or any comments to 'false' or 'right' and the differing passage" +
                    "Here is the DifferenceDescription object, which contains the differences found by the rule-based approach to find the modified text passages: " + result;
    }

}

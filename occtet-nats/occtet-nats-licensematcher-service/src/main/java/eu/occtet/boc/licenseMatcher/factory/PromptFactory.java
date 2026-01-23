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



            return  "You must use the standardLicenseTemplate from the SPDXLicenseDetail objectm which you must retrieve with your tool, to compare, " +
                    "if the license text from the user corresponds to the original SPDX license text." +
                    "After retrieving SPDXLicenseDetail and comparing the given licensetext with it, do the following:" +
                    "1. Quote the licenseId from the tool result" +
                    "2. Quote 3 exact sentences from standardLicenseTemplate" +
                    "3. Point out at least one concrete difference or state \"NO DIFFERENCE FOUND\"" +
                    "4. Final verdict: MATCH / NO MATCH" +
                    "Here is the DifferenceDescription, which contains the differences found by the rule-based approach to find the modified text passages: "
                    + result.getDifferenceMessage() +
                    " and the list of lines with differences from the spdx matcher: " + result.getDifferences();
    }

}

/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.processRun.factory;

import eu.occtet.boc.entity.OrtViolation;
import eu.occtet.boc.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class OrtViolationFactory {

    public OrtViolation createOrtViolation(String message, String rule, String severity, String purl, String howToFix,
                                           String license, String licenseSource, Project project){
        OrtViolation ortViolation= new OrtViolation(message, rule, severity, purl, howToFix, license, licenseSource, false, project);

        return ortViolation;
    }
}

/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 *   SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 */

package eu.occtet.boc.spdx.coverter;

import eu.occtet.boc.entity.spdxV2.Package;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import org.spdx.library.model.v2.Relationship;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.SpdxPackage;
import org.springframework.stereotype.Service;

@Service
public class SpdxConverter {

    /**
     * Takes an spdxDocument and creates all entities directly relevant to the document as a whole.
     * Does not cover package, file and relationship information.
     * @param spdxDocument
     * @return true if the process was successful, false otherwise
     */
    public boolean convertSpdxV2DocumentInformation(SpdxDocument spdxDocument){
        SpdxDocumentRoot  spdxDocumentRoot = new SpdxDocumentRoot();
        return true;
    }

    /**
     *
     * @param spdxPackage
     * @return
     */
    public boolean convertPackage(SpdxPackage spdxPackage){
        Package spdxPackageEntity = new Package();
        return true;
    }

    /**
     *
     * @param spdxFile
     * @return
     */
    public boolean convertFile(SpdxFile spdxFile){
        eu.occtet.boc.entity.spdxV2.SpdxFile spdxFileEntity = new eu.occtet.boc.entity.spdxV2.SpdxFile();
        return true;
    }

    /**
     *
     * @param relationship
     * @return
     */
    public boolean convertRelationShip(Relationship relationship){
        eu.occtet.boc.entity.spdxV2.Relationship relationshipEntity = new eu.occtet.boc.entity.spdxV2.Relationship();
        return true;
    }
}
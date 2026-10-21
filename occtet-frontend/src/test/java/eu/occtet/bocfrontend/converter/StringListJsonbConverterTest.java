/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.converter;

import eu.occtet.bocfrontend.entity.appconfigurations.SearchTermsProfile;
import eu.occtet.bocfrontend.entity.spdxV2.CreationInfoEntity;
import eu.occtet.bocfrontend.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.bocfrontend.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.EntitySet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class StringListJsonbConverterTest {

    @Autowired
    private DataManager dataManager;

    private final List<Object> saved = new ArrayList<>();

    @AfterEach
    void tearDown() {
        saved.forEach(dataManager::remove);
        saved.clear();
    }

    // The JSONB column of SearchTermsProfile goes through the converter.
    @Test
    void test_searchTermsAreSavedAndLoaded() {
        SearchTermsProfile profile = dataManager.create(SearchTermsProfile.class);
        profile.setName("string-list-jsonb-converter-test");
        profile.setSearchTerms(List.of("copyright", "(c)"));
        profile = dataManager.save(profile);
        saved.add(profile);

        SearchTermsProfile loaded = dataManager.load(SearchTermsProfile.class).id(profile.getId()).one();
        assertEquals(List.of("copyright", "(c)"), loaded.getSearchTerms());
    }

    // The values of an element collection of strings are plain strings: the converter must not be
    // applied to them.
    @Test
    void test_spdxDocumentDescribesIsSavedAndLoaded() {
        CreationInfoEntity creationInfo = dataManager.create(CreationInfoEntity.class);
        creationInfo.setCreated("2026-10-20T00:00:00Z");
        creationInfo.setCreators("Tool: StringListJsonbConverterTest");

        SpdxDocumentRoot root = dataManager.create(SpdxDocumentRoot.class);
        root.setSpdxId("SPDXRef-DOCUMENT");
        root.setSpdxVersion("SPDX-2.3");
        root.setDataLicense("CC0-1.0");
        root.setName("string-list-jsonb-converter-test");
        root.setDocumentUri("https://example.org/string-list-jsonb-converter-test");
        root.setCreationInfo(creationInfo);
        root.setSpdxDocumentDescribes(List.of("SPDXRef-Package-a", "SPDXRef-Package-b"));

        EntitySet entities = dataManager.save(creationInfo, root);
        SpdxDocumentRoot document = entities.get(root);
        saved.add(document);

        SpdxDocumentRoot loaded = dataManager.load(SpdxDocumentRoot.class).id(document.getId()).one();
        assertEquals(List.of("SPDXRef-Package-a", "SPDXRef-Package-b"), loaded.getSpdxDocumentDescribes());
    }
}

/*
 *  Copyright (C) 2025 Bitsea GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https:www.apache.orglicensesLICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 *
 *
 */

package eu.occtet.boc.fileindexing;

import eu.occtet.boc.fileindexing.service.OpenSearchInfraService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.ExistsIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;

import static org.mockito.Mockito.*;

@SpringBootTest(classes = {OpenSearchInfraService.class})
@ExtendWith(MockitoExtension.class)
public class OpenSearchInfraServiceTest {

    @MockitoBean
    private OpenSearchClient client;
    @MockitoBean
    private OpenSearchIndicesClient indicesClient;
    @Autowired
    private OpenSearchInfraService openSearchInfraService;

    @Test
    void shouldCreateIndexTemplateWhenItDoesNotExist() throws IOException {
        when(client.indices()).thenReturn(indicesClient);
        BooleanResponse falseResponse = new BooleanResponse(false);
        when(indicesClient.existsIndexTemplate(any(ExistsIndexTemplateRequest.class)))
                .thenReturn(falseResponse);
        PutIndexTemplateResponse putResponse = mock(PutIndexTemplateResponse.class);
        when(putResponse.acknowledged()).thenReturn(true);
        when(putResponse.toJsonString()).thenReturn("{\"acknowledged\":true}");
        when(indicesClient.putIndexTemplate(any(PutIndexTemplateRequest.class)))
                .thenReturn(putResponse);
        openSearchInfraService.ensureIndexTemplate();
        verify(indicesClient).existsIndexTemplate(any(ExistsIndexTemplateRequest.class));
        verify(indicesClient).putIndexTemplate(any(PutIndexTemplateRequest.class));
    }
    @Test
    void shouldDoNothingWhenTemplateAlreadyExists() throws IOException {
        when(client.indices()).thenReturn(indicesClient);

        BooleanResponse trueResponse = new BooleanResponse(true);
        when(indicesClient.existsIndexTemplate(any(ExistsIndexTemplateRequest.class)))
                .thenReturn(trueResponse);

        openSearchInfraService.ensureIndexTemplate();

        verify(indicesClient).existsIndexTemplate(any(ExistsIndexTemplateRequest.class));
        verify(indicesClient, never()).putIndexTemplate(any(PutIndexTemplateRequest.class));
    }
}

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

package eu.occtet.boc.export;

import eu.occtet.boc.export.service.AnswerService;
import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.ObjectStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnswerServiceTest {

    @Mock
    private Connection natsConnection;

    @Mock
    private ObjectStore objectStore;

    @InjectMocks
    private AnswerService answerService;

    @Test
    void putIntoBucket_ValidData_StoresSuccessfully() throws IOException, JetStreamApiException, NoSuchAlgorithmException {
        String testFileName = "test-sbom.json";
        byte[] testData = "{}".getBytes();

        when(natsConnection.objectStore("file-bucket")).thenReturn(objectStore);

        answerService.putIntoBucket(testFileName, testData);

        verify(natsConnection, times(1)).objectStore("file-bucket");

        verify(objectStore, times(1)).put(eq(testFileName), any(InputStream.class));
    }

    @Test
    void putIntoBucket_NatsThrowsException_HandlesGracefully() throws IOException, JetStreamApiException, NoSuchAlgorithmException {
        String testFileName = "test-sbom.json";
        byte[] testData = "{}".getBytes();

        when(natsConnection.objectStore("file-bucket")).thenReturn(objectStore);

        when(objectStore.put(eq(testFileName), any(InputStream.class))).thenThrow(new IOException("NATS Connection Refused"));

        answerService.putIntoBucket(testFileName, testData);

        verify(natsConnection, times(1)).objectStore("file-bucket");
        verify(objectStore, times(1)).put(eq(testFileName), any(InputStream.class));
    }
}

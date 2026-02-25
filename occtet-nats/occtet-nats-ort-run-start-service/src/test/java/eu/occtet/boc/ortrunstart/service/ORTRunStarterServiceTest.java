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

package eu.occtet.boc.ortrunstart.service;

import eu.occtet.boc.config.TestEclipseLinkJpaConfiguration;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import eu.occtet.boc.model.ORTStartRunWorkData;
import eu.occtet.boc.ortclient.AuthService;
import eu.occtet.boc.ortclient.OrtClientService;
import eu.occtet.boc.ortclient.TokenResponse;
import jakarta.persistence.EntityManager;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiException;
import org.openapitools.client.api.RunsApi;
import org.openapitools.client.model.OrtRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {ORTRunStarterService.class, ProjectRepository.class, TestEclipseLinkJpaConfiguration.class})
@EnableJpaRepositories(basePackages = "eu.occtet.boc.dao")
@EntityScan(basePackages = "eu.occtet.boc.entity")
@EnableJpaAuditing
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class ORTRunStarterServiceTest{


    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ORTRunStarterService ortRunStarterService;

    @Autowired
    private TestEntityManager entityManager;


    @Test // commented out because it requires a running ORT server and Keycloak instance on localhost.
    public void testStartOrtRun() throws IOException, InterruptedException, ApiException/* throws ApiException*/ {
        Project project= new Project("test");
        project.setId(1234L);
        entityManager.persistAndFlush(project);

        Mockito.lenient().when(projectRepository.findById(any()))
                .thenReturn(Optional.of(project));



        ortRunStarterService.startOrtRun(project.getId(),  "repoName",
                "https://github.com/Bitsea/Occtet-Curator/", "GIT_REPO", "0.3.8-alpha");

    }


    private static final Logger log = LogManager.getLogger(ORTRunStarterService.class);

    String clientId="ort-server";
    private String tokenUrl="http://ort.bitsea.de/realms/master/protocol/openid-connect/token";
    private String username = "ort-admin";
    private String password = "password";


    @Test // commented out because it requires a running ORT server and Keycloak instance on localhost.
    public void startOrtRunTest() throws IOException, InterruptedException, ApiException {
        OrtClientService ortClientService = new OrtClientService("http://ort.bitsea.de");
        AuthService authService = new AuthService(tokenUrl);
        TokenResponse tokenResponse = authService.requestToken(clientId,username,password,"offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        // demo code only! This only gets the run information, but does not start it. We need to figure out how that is done.
        RunsApi runsApi = new RunsApi(apiClient);
        OrtRun run = runsApi.getRun(1234L);
        System.out.println(run);

    }
}
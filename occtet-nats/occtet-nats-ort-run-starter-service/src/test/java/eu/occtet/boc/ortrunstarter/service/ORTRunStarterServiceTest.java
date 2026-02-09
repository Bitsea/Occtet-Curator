package eu.occtet.boc.ortrunstarter.service;

import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.Project;
import eu.occtet.boc.entity.spdxV2.SpdxDocumentRoot;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.client.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ContextConfiguration(classes = {ORTRunStarterService.class, ProjectRepository.class})
@EnableJpaRepositories(basePackages = {
        "eu.occtet.boc.dao"})
@EntityScan(basePackages = {
        "eu.occtet.boc.entity"
})
@ExtendWith(MockitoExtension.class)
public class ORTRunStarterServiceTest extends TestCase {

    private ORTRunStarterService ortRunStarterService = new ORTRunStarterService();
    @Autowired
    private ProjectRepository projectRepository;


    @Autowired
    ApplicationContext ctx;

    @Before
    public void listBeans() {
        String[] names = ctx.getBeanDefinitionNames();
        Arrays.sort(names);
        for (String n : names) {
            System.out.println(n + " -> " + ctx.getBean(n).getClass().getName());
        }
    }

    @Test // commented out because it requires a running ORT server and Keycloak instance on localhost.
    public void testStartOrtRun() throws IOException, InterruptedException, ApiException/* throws ApiException*/ {
        Project project= new Project("testProject");


        projectRepository.save(project);
        ortRunStarterService.startOrtRun(12345, "orgaName", "repoName",
                "https://github.com/Bitsea/Occtet-Curator/tree/main", "GIT_REPO");

    }
}
package eu.occtet.boc.ortclient;

import junit.framework.TestCase;
import org.openapitools.client.ApiClient;
import org.openapitools.client.api.*;
import org.openapitools.client.model.*;

import java.util.Map;

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
public class OrtClientServiceTest extends TestCase {

    /**
     * test&demonstrate how to access ORT server via OrtClientService.
     * This is NOT a unit test and thus not annotated with @Test because it requires a running ORT server with reachable Keycloak instance
     * with the given configuration (default local docker setup).
     * @throws Exception
     */
    public void testOrtClientAccess() throws Exception {

        OrtClientService ortClientService = new OrtClientService("http://localhost:8080");
        //OrtClientService ortClientService = new OrtClientService("https://ort.bitsea.de");
        AuthService authService = new AuthService("http://localhost:8081/realms/master/protocol/openid-connect/token");
        //AuthService authService = new AuthService("https://keycloak.bitsea.de/realms/master/protocol/openid-connect/token");
        TokenResponse tokenResponse = authService.requestToken("ort-server","ort-admin","password","offline_access");
        ApiClient apiClient = ortClientService.createApiClient(tokenResponse);

        VersionsApi versionsApi = new VersionsApi(apiClient);
        Map<String, String> versions = versionsApi.getVersions();
        versions.keySet().forEach(k-> System.out.println("Version key: " + k + " value: " + versions.get(k)));

        OrganizationsApi organizationsApi = new OrganizationsApi(apiClient);
        PagedResponseOrganization orgs = organizationsApi.getOrganizations(null,null,null,null);
        orgs.getData().forEach(o-> System.out.println("Organization: " + o));
        Long organizationId = orgs.getData().get(0).getId();

        PagedResponseProduct organizationProducts = organizationsApi.getOrganizationProducts(organizationId, null, null, null, null);
        organizationProducts.getData().forEach(p-> System.out.println("Organization Product: " + p));
        Long productId = organizationProducts.getData().get(0).getId();

        ProductsApi productsApi = new ProductsApi(apiClient);
        PagedResponseRepository productRepositories = productsApi.getProductRepositories(productId, null, null, null, null);
        productRepositories.getData().forEach(r-> System.out.println("Product Repository: " + r));
        Long repositoryId = productRepositories.getData().get(0).getId();

        RepositoriesApi repositoriesApi = new RepositoriesApi(apiClient);
        PagedResponseOrtRunSummary repositoryRuns = repositoriesApi.getRepositoryRuns(repositoryId, null, null, null);
        repositoryRuns.getData().forEach(r-> System.out.println("Repository Run: " + r));
        Long runId = repositoryRuns.getData().get(0).getId();

        RunsApi runsApi = new RunsApi(apiClient);
        OrtRunStatistics runStatistics = runsApi.getRunStatistics(runId);
        System.out.println("Run Statistics: " + runStatistics);

    }
}
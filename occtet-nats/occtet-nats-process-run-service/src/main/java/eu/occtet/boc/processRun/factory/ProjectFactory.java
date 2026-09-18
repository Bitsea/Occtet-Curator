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

import eu.occtet.boc.dao.OrganizationRepository;
import eu.occtet.boc.dao.ProjectRepository;
import eu.occtet.boc.entity.Organization;
import eu.occtet.boc.entity.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectFactory {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    public Project createProject(String projectName, String organizationName, String version) {
        Organization org = organizationRepository.findByOrganizationName(organizationName)
                .orElseGet(() -> {
                    Organization newOrg = new Organization();
                    newOrg.setOrganizationName(organizationName);
                    return organizationRepository.save(newOrg);
                });

        Project project = new Project();
        project.setProjectName(projectName);
        project.setOrganization(org);
        project.setProjectContact(organizationName);
        project.setVersion(version);
        projectRepository.save(project);
        return project;
    }
}

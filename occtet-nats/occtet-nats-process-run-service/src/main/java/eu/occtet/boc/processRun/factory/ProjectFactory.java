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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectFactory {

    private static final Logger log = LogManager.getLogger(ProjectFactory.class);

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    public Project createProject(String projectName, String organizationName, String version) {
        log.info("Attempting to create project '{}' (version: {}) for organization '{}'", projectName, version, organizationName);

        Organization org = organizationRepository.findByOrganizationName(organizationName)
                .map(existingOrg -> {
                    log.info("Found existing organization '{}' with ID: {}", organizationName, existingOrg.getId());
                    return existingOrg;
                })
                .orElseGet(() -> {
                    log.info("Organization '{}' not found in database. Creating new organization...", organizationName);
                    Organization newOrg = new Organization();
                    newOrg.setOrganizationName(organizationName);
                    Organization savedOrg = organizationRepository.save(newOrg);
                    log.info("Created and saved new organization '{}' with ID: {}", organizationName, savedOrg.getId());
                    return savedOrg;
                });

        Project project = new Project();
        project.setProjectName(projectName);
        project.setOrganization(org);
        project.setProjectContact(organizationName);
        project.setVersion(version);
        Project savedProject = projectRepository.save(project);
        log.info("Successfully created and saved project '{}' with ID: {} (Organization: '{}', ID: {})",
                savedProject.getProjectName(), savedProject.getId(), org.getOrganizationName(), org.getId());
        return savedProject;
    }
}

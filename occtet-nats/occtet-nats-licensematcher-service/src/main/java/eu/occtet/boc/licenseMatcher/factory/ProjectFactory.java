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

package eu.occtet.boc.licenseMatcher.factory;

import eu.occtet.boc.entity.Project;
import eu.occtet.boc.licenseMatcher.dao.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectFactory {

    private final ProjectRepository projectRepository;

    @Autowired
    public ProjectFactory(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project create(String projectName){
        Project project;

        if (projectRepository.findByProjectName(projectName).isEmpty()){
            project = new Project(projectName);
        } else {
            project = projectRepository.findByProjectName(projectName).get(0);
        }
        return projectRepository.save(project);
    }
}

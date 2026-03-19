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

package eu.occtet.bocfrontend.view.file;

import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

@ViewController(id = "File.list")
@ViewDescriptor(path = "file-list-view.xml")
@LookupComponent("filesDataGrid")
@DialogMode(width = "64em")
public class FileListView extends StandardListView<File> {

    @ViewComponent
    private CollectionLoader<File> filesDl;

    private Project project;

    @Subscribe
    public void onInit(InitEvent event) {
    }

    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            filesDl.setParameter("project", project);
        }
        filesDl.load();
    }
}
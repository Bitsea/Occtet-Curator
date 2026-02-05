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

package eu.occtet.bocfrontend.view.audit;

import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.Project;
import eu.occtet.bocfrontend.model.FileReviewedFilterMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.stream.Stream;

/**
 * FileHierarchyProvider is a hierarchical data provider that provides file data
 * for hierarchical structures, such as tree components. This class fetches file
 * data from a specified repository and supports filtering operations to refine
 * the set of visible items in the hierarchy.
 * <p>
 * This provider utilizes the following functionalities:
 * - Filtering based on a provided filter text.
 * - Determination of direct children for a given parent file object.
 * - Counting child elements for a specified parent node.
 * - Determining whether a file has children.
 * - Handling hierarchical queries efficiently with support for pagination.
 */
public class FileHierarchyProvider extends AbstractBackEndHierarchicalDataProvider<File, Void> {

    private static final Logger log = LogManager.getLogger(FileHierarchyProvider.class);


    private final FileRepository fileRepository;
    private final Project project;

    private Boolean targetStatus = null;

    public FileHierarchyProvider(FileRepository fileRepository, Project project) {
        this.fileRepository = fileRepository;
        this.project = project;
    }

    public void setReviewedFilter(FileReviewedFilterMode mode) {
        this.targetStatus = mode.asBoolean();
        this.refreshAll();
    }

    @Override
    protected Stream<File> fetchChildrenFromBackEnd(HierarchicalQuery<File, Void> query) {
        File parent = query.getParent();
        log.debug("Fetching children for parent: {}", parent != null ? parent.getFileName() : "null (roots)");
        Pageable pageable = PageRequest.of(query.getOffset() / query.getLimit(), query.getLimit());

        if (parent == null) {
            return fileRepository.findRootsSorted(project, targetStatus, pageable).stream();
        } else {
            log.debug("Fetching children of {} with reviewed status {}", parent.getFileName(), targetStatus);
            return fileRepository.findChildrenSorted(parent, targetStatus, pageable).stream();
        }
    }

    @Override
    public int getChildCount(HierarchicalQuery<File, Void> query) {
        File parent = query.getParent();
        //log.debug("Counting children {} for parent {}",fileRepository.countRoots(project, targetStatus), parent.getFileName() );
        if (parent == null) {
            return (int) fileRepository.countRoots(project, targetStatus);
        }
        return (int) fileRepository.countChildren(parent, targetStatus);
    }

    @Override
    public boolean hasChildren(File file) {
        return Boolean.TRUE.equals(file.getIsDirectory());
    }
}
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
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

    private final FileRepository fileRepository;
    private final Project project;

    private final Set<UUID> exactMatchIds = new HashSet<>();
    private final Set<UUID> pathIds = new HashSet<>();
    private String filterText = "";

    public FileHierarchyProvider(FileRepository fileRepository, Project project) {
        this.fileRepository = fileRepository;
        this.project = project;
    }

    /**
     * Sets the filter text used to identify files and updates the internal data structures
     * accordingly. Clears existing match and path identifiers and populates them based on
     * the results of the filter text applied to the file repository.
     *
     * @param filterText the filter text to be applied for searching files in the file repository;
     *                   if the filter is non-empty, it retrieves matching files and builds
     *                   hierarchies of path identifiers from the root to the matches.
     */
    public void setFilterText(String filterText) {
        this.filterText = filterText;
        this.exactMatchIds.clear();
        this.pathIds.clear();

        if (hasFilter()) {
            List<File> matches = fileRepository.findRawMatches(project, filterText);

            for (File match : matches) {
                exactMatchIds.add(match.getId());
                pathIds.add(match.getId());

                File current = match.getParent();
                while (current != null) {
                    if (pathIds.contains(current.getId())) {
                        break;
                    }
                    pathIds.add(current.getId());
                    current = current.getParent();
                }
            }
        }
        this.refreshAll();
    }

    /**
     * Fetches the child files of a given parent file from the backend repository based on the hierarchical query.
     * Depending on the filtering criteria and the parent file, this method retrieves a stream of matching file entities.
     *
     * @param query the hierarchical query specifying the parent file and pagination information.
     *              If the query contains no parent, root files are fetched.
     *              The query also provides pagination parameters such as offset and limit.
     * @return a stream of {@link File} objects representing the child files of the specified parent.
     *         An empty stream is returned if no children are found or the filtering criteria exclude all matches.
     */
    @Override
    protected Stream<File> fetchChildrenFromBackEnd(HierarchicalQuery<File, Void> query) {
        File parent = query.getParent();
        Pageable pageable = PageRequest.of(query.getOffset() / query.getLimit(), query.getLimit());

        if (!hasFilter()) {
            return fetchStandardChildren(parent, pageable);
        }

        if (pathIds.isEmpty()) return Stream.empty();

        if (parent == null) {
            return fileRepository.findRootsByIdIn(project, pathIds, pageable).stream();
        }

        UUID parentId = parent.getId();

        if (exactMatchIds.contains(parentId)) {
            return fileRepository.findByParentOrderByIsDirectoryDescFileNameAsc(parent, pageable).stream();
        }

        if (pathIds.contains(parentId)) {
            return fileRepository.findByParentAndIdIn(parent, pathIds, pageable).stream();
        }

        return fileRepository.findByParentOrderByIsDirectoryDescFileNameAsc(parent, pageable).stream();
    }

    @Override
    public int getChildCount(HierarchicalQuery<File, Void> query) {
        File parent = query.getParent();

        if (!hasFilter()) {
            return countStandardChildren(parent);
        }

        if (pathIds.isEmpty()) return 0;

        if (parent == null) {
            return (int) fileRepository.countByProjectAndParentIsNullAndIdIn(project, pathIds);
        }

        UUID parentId = parent.getId();

        if (exactMatchIds.contains(parentId)) {
            return (int) fileRepository.countByParent(parent);
        } else if (pathIds.contains(parentId)) {
            return (int) fileRepository.countByParentAndIdIn(parent, pathIds);
        } else {
            return (int) fileRepository.countByParent(parent);
        }
    }

    private Stream<File> fetchStandardChildren(File parent, Pageable pageable) {
        if (parent == null) {
            return fileRepository.findByProjectAndParentIsNullOrderByIsDirectoryDescFileNameAsc(project, pageable).stream();
        }
        return fileRepository.findByParentOrderByIsDirectoryDescFileNameAsc(parent, pageable).stream();
    }

    private int countStandardChildren(File parent) {
        if (parent == null) {
            return (int) fileRepository.countByProjectAndParentIsNull(project);
        }
        return (int) fileRepository.countByParent(parent);
    }

    @Override
    public boolean hasChildren(File file) {
        return Boolean.TRUE.equals(file.getIsDirectory());
    }

    private boolean hasFilter() {
        return filterText != null && !filterText.isBlank();
    }

    public Set<UUID> getExactMatchIds() {
        return Collections.unmodifiableSet(exactMatchIds);
    }

    public Set<UUID> getPathIds() {
        return Collections.unmodifiableSet(pathIds);
    }
}
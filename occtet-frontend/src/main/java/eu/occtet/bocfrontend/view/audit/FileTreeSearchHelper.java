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

import com.google.common.base.Strings;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.TreeGrid;
import eu.occtet.bocfrontend.comparator.TreePathComparator;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.Project;
import io.jmix.core.Messages;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Helper class for searching and navigating through a hierarchical file tree structure.
 * This class is responsible for handling search functionality within a file tree,
 * updating the view to highlight search results, and enabling navigation between matches.
 */
public class FileTreeSearchHelper {

    private final Logger log = LogManager.getLogger(this.getClass());


    private final FileRepository fileRepository;
    private final TreeGrid<File> treeGrid;
    private final NativeLabel countLabel;
    private final Set<Long> expandedItemIds;
    private final Supplier<Boolean> filterStatusSupplier;
    private final TransactionTemplate transactionTemplate;

    private List<Long> searchResultIds = new ArrayList<>();
    private int currentIndex = -1;
    private String lastSearchText = "";

    private long totalMatchesInDb = 0;

    public FileTreeSearchHelper(FileRepository fileRepository,
                                TreeGrid<File> treeGrid,
                                NativeLabel countLabel,
                                Set<Long> expandedItemIds,
                                Supplier<Boolean> filterStatusSupplier,
                                TransactionTemplate transactionTemplate) {
        this.fileRepository = fileRepository;
        this.treeGrid = treeGrid;
        this.countLabel = countLabel;
        this.expandedItemIds = expandedItemIds;
        this.filterStatusSupplier = filterStatusSupplier;
        this.transactionTemplate = transactionTemplate;

        setupRenderer();
    }

    /**
     * Resets the search helper state. Call this when the Project changes.
     */
    public void reset() {
        this.searchResultIds.clear();
        this.currentIndex = -1;
        this.lastSearchText = "";
        this.totalMatchesInDb = 0;
        updateLabel();
    }

    /**
     * Handles the "Enter" key press event to trigger a search or navigate to the next result.
     * If the provided text differs from the last searched text, a new search is performed.
     * Otherwise, navigates to the next search result.
     *
     * @param currentText the text currently entered that will be used to perform the search
     * @param project the project in which the search or navigation should be performed
     */
    public void onEnterKeyPressed(String currentText, Project project) {
        if (!Objects.equals(currentText, lastSearchText)) {
            performSearch(currentText, project);
        } else {
            next(project);
        }
    }

    /**
     * Executes a search operation for files within the given project based on the specified search text.
     * Updates the list of search results and tracks the current position in the results.
     * If the search text is empty or null, clears the current search results.
     *
     * @param searchText the text to search for in the file names. If null or empty, the search results are cleared.
     * @param project the project to perform the search in. Must not be null.
     */
    public void performSearch(String searchText, Project project) {
        log.info("Performing search for: {}", searchText);

        boolean isNewSearch = !Objects.equals(searchText, lastSearchText);

        this.lastSearchText = searchText;
        this.currentIndex = -1;
        this.searchResultIds.clear();

        if (Strings.isNullOrEmpty(searchText)) {
            updateLabel();
            treeGrid.getDataProvider().refreshAll();
            return;
        }

        List<Long> sortedIds = transactionTemplate.execute(status -> {
            List<File> candidates = fileRepository.findCandidates(
                    project,
                    searchText,
                    filterStatusSupplier.get()
            );
            candidates.sort(new TreePathComparator());
            return candidates.stream().map(File::getId).toList();
        });

        this.searchResultIds = new ArrayList<>(Objects.requireNonNull(sortedIds));
        this.totalMatchesInDb = fileRepository.countCandidates(
                project, searchText, filterStatusSupplier.get()
        );

        if (isNewSearch) {
            treeGrid.getDataProvider().refreshAll();
        }

        if (!searchResultIds.isEmpty()) {
            jumpToMatch(0, project, isNewSearch);
        } else {
            updateLabel();
            if (!isNewSearch) treeGrid.getDataProvider().refreshAll();
        }
    }

    /**
     * Jumps to the next search result
     */
    public void next(Project project) {
        if (searchResultIds.isEmpty()) return;
        int nextIndex = (currentIndex + 1) % searchResultIds.size();
        jumpToMatch(nextIndex, project, false);
    }

    /**
     * Jumps to the previous search result
     */
    public void previous(Project project) {
        if (searchResultIds.isEmpty()) return;
        int prevIndex = (currentIndex - 1 + searchResultIds.size()) % searchResultIds.size();
        jumpToMatch(prevIndex, project, false);
    }

    private void jumpToMatch(int index, Project project, boolean isNewSearch) {
        this.currentIndex = index;
        Long newId = searchResultIds.get(index);
        updateLabel();

        fileRepository.findById(newId).ifPresent(file -> {
            expandParents(file);
            treeGrid.deselectAll();
            treeGrid.select(file);

            treeGrid.getDataProvider().refreshAll();

            int[] pathToTreeIndex = calculatePath(file);
            if (pathToTreeIndex.length > 0) {
                log.debug("Jumping to path: {}", Arrays.toString(pathToTreeIndex));
                treeGrid.scrollToIndex(pathToTreeIndex);
            }
        });
    }

    private void expandParents(File file) {
        List<File> parentsToExpand = new ArrayList<>();
        File parent = file.getParent();
        while (parent != null) {
            parentsToExpand.add(parent);
            parent = parent.getParent();
        }

        Collections.reverse(parentsToExpand);

        for (File p : parentsToExpand) {
            treeGrid.expand(p);
            expandedItemIds.add(p.getId());
        }
    }

    private void updateLabel() {
        if (searchResultIds.isEmpty())
            countLabel.setText("0:0");
        else
            countLabel.setText((currentIndex + 1) + ":" + totalMatchesInDb);
    }

    private void setupRenderer() {
        if (treeGrid.getColumnByKey("fileName") != null) {
            treeGrid.removeColumn(treeGrid.getColumnByKey("fileName"));
        }

        treeGrid.addComponentHierarchyColumn(file -> {
                    Span span = new Span();
                    String text = file.getFileName();
                    log.debug("see filename {}", text);
                    if (Strings.isNullOrEmpty(lastSearchText) || Strings.isNullOrEmpty(text)) {
                        span.setText(text);
                        return span;
                    }

                    boolean isMatch = searchResultIds.contains(file.getId());
                    boolean isCurrent = !searchResultIds.isEmpty()
                            && currentIndex >= 0
                            && searchResultIds.get(currentIndex).equals(file.getId());

                    if (isMatch) {
                        String color = isCurrent ? "#ff9900" : "#ffff00";
                        String html = text.replaceAll("(?i)(" + Pattern.quote(lastSearchText) + ")",
                                "<span style='background-color: " + color + "; color: black; display: inline-block; line-height: 1.2;'>$1</span>");
                        span.getElement().setProperty("innerHTML", html);
                    } else {
                        span.setText(text);
                    }

                    return span;
                })
                .setKey("fileName")
                .setHeader("Files")
                .setSortable(true);
    }

    private int[] calculatePath(File file) {
        // NOTE: the following is robust but had to be done for nested scrolling
        List<Integer> pathIndices = new ArrayList<>();

        // Build the hierarchy stack: [Root, Child, Target]
        List<File> hierarchy = new ArrayList<>();
        File current = file;
        while(current != null) {
            hierarchy.add(current);
            current = current.getParent();
        }
        // Reverse so we start from Root
        Collections.reverse(hierarchy);

        // Calculate the index at each level (Root level, then Child level, etc.)
        for (int i = 0; i < hierarchy.size(); i++) {
            File node = hierarchy.get(i);
            File parent = node.getParent();

            // Fetch siblings to find where 'node' sits among them
            List<File> siblings;
            if (parent == null) {
                // Find roots
                siblings = fileRepository.findRootsSorted(
                        node.getProject(),
                        filterStatusSupplier.get(),
                        Pageable.unpaged()
                );
            } else {
                // Find children of the parent
                siblings = fileRepository.findChildrenSorted(
                        parent,
                        filterStatusSupplier.get(),
                        Pageable.unpaged()
                );
            }

            // Find the index of 'node' in the 'siblings' list
            int index = -1;
            for (int k = 0; k < siblings.size(); k++) {
                if (siblings.get(k).getId()==node.getId()) {
                    index = k;
                    break;
                }
            }

            // If we can't find the file in the DB sort order, we can't scroll to it.
            if (index == -1) return new int[0];

            pathIndices.add(index);
        }

        return pathIndices.stream().mapToInt(Integer::intValue).toArray();
    }
}
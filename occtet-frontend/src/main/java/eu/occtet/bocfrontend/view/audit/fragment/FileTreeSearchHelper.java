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

package eu.occtet.bocfrontend.view.audit.fragment;

import com.google.common.base.Strings;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.File;
import eu.occtet.bocfrontend.entity.Project;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class FileTreeSearchHelper {

    private final FileRepository fileRepository;
    private final TreeGrid<File> treeGrid;
    private final NativeLabel countLabel;
    private final Set<UUID> expandedItemIds;
    private final Supplier<Boolean> filterStatusSupplier;

    private List<UUID> searchResultIds = new ArrayList<>();
    private int currentIndex = -1;
    private String lastSearchText = "";

    public FileTreeSearchHelper(FileRepository fileRepository, TreeGrid<File> treeGrid, NativeLabel countLabel, Set<UUID> expandedItemIds, Supplier<Boolean> filterStatusSupplier) {
        this.fileRepository = fileRepository;
        this.treeGrid = treeGrid;
        this.countLabel = countLabel;
        this.expandedItemIds = expandedItemIds;
        this.filterStatusSupplier = filterStatusSupplier;

        setupRenderer();
    }

    public void performSearch(String searchText, Project project) {
        this.lastSearchText = searchText;
        this.currentIndex = -1;
        this.searchResultIds.clear();

        if (Strings.isNullOrEmpty(searchText)) {
            updateLabel();
            treeGrid.getDataProvider().refreshAll();
            return;
        }

        this.searchResultIds = fileRepository.searchIdsByFileName(project, searchText);

        if (!searchResultIds.isEmpty()) {
            jumpToMatch(0, project);
        } else {
            updateLabel();
        }
        treeGrid.getDataProvider().refreshAll();
    }

    public void next(Project project) {
        if (searchResultIds.isEmpty())
            return;
        int nextIndex = (currentIndex + 1) % searchResultIds.size();
        jumpToMatch(nextIndex, project);
    }

    public void previous(Project project) {
        if (searchResultIds.isEmpty())
            return;
        int prevIndex = (currentIndex - 1 + searchResultIds.size()) % searchResultIds.size();
        jumpToMatch(prevIndex, project);
    }

    private void jumpToMatch(int index, Project project) {
        this.currentIndex = index;
        UUID targetId = searchResultIds.get(index);
        updateLabel();

        fileRepository.findById(targetId).ifPresent(file -> {
            expandParents(file);
            treeGrid.deselectAll();
            treeGrid.select(file);

            int globalIndex = calculateGlobalIndex(file);
            if (globalIndex >= 0) {
                treeGrid.scrollToIndex(globalIndex);
            }
        });
    }

    private void expandParents(File file) {
        File parent = file.getParent();
        while (parent != null) {
            treeGrid.expand(parent);
            expandedItemIds.add(parent.getId()); // Sync view's state
            parent = parent.getParent();
        }
    }

    private void updateLabel() {
        if (searchResultIds.isEmpty())
            countLabel.setText("0:0");
        else
            countLabel.setText((currentIndex + 1) + ":" + searchResultIds.size());
    }

    private int calculateGlobalIndex(File file) {
        Boolean currentFilter = filterStatusSupplier.get();

        List<File> roots = fileRepository.findRootsSorted(file.getProject(), currentFilter, Pageable.unpaged());        Deque<File> stack = new ArrayDeque<>();
        for (int i = roots.size() - 1; i >= 0; i--) {
            stack.push(roots.get(i));
        }
        int counter = 0;
        while (!stack.isEmpty()) {
            File current = stack.pop();
            if (current.getId().equals(file.getId())) {
                return counter;
            }
            counter++;
            if (expandedItemIds.contains(current.getId())) {
                List<File> children = fileRepository.findChildrenSorted(current, currentFilter, Pageable.unpaged());
                for (int j = children.size() - 1; j >= 0; j--) {
                    stack.push(children.get(j));
                }
            }
        }
        return -1;
    }

    private void setupRenderer() {
        treeGrid.getColumnByKey("fileName").setRenderer(new ComponentRenderer<>(file -> {
            Span span = new Span();
            String text = file.getFileName();

            if (Strings.isNullOrEmpty(lastSearchText) || Strings.isNullOrEmpty(text)) {
                span.setText(text);
                return span;
            }

            boolean isMatch = searchResultIds.contains(file.getId());
            boolean isCurrent = !searchResultIds.isEmpty() && currentIndex >= 0 && searchResultIds.get(currentIndex).equals(file.getId());

            if (isMatch) {
                String color = isCurrent ? "#ff9900" : "#ffff00";
                String html = text.replaceAll("(?i)(" + Pattern.quote(lastSearchText) + ")", "<span style='background-color: " + color + "; color: black;'>$1</span>");
                span.getElement().setProperty("innerHTML", html);
            } else {
                span.setText(text);
            }
            return span;
        }));
    }
}

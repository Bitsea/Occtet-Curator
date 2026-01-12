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

package eu.occtet.bocfrontend.comparator;

import eu.occtet.bocfrontend.dao.FileRepository;
import eu.occtet.bocfrontend.entity.File;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A comparator for comparing two {@link File} objects based on their hierarchical
 * tree structure and specific rules for sibling comparison.
 * <p>
 * Files are compared by determining their lineage (ancestry) and comparing each
 * level in their respective paths. Siblings are compared using rules that
 * consider dependency folders, directory status, and file names in a case-insensitive manner.
 * </p>
 *
 * Methods include:
 * <ul>
 * - {@code compare}: Compares the two file objects based on their tree structure.
 * - {@code getLineage}: Retrieves the list representing the file's lineage up to the root.
 * - {@code compareSiblings}: Compares two sibling files based on specific attributes.
 * </ul>
 *
 * This comparator assumes the use of a {@link File} domain entity with attributes
 * such as parent-child relationships and directory/file status.
 */
public class TreePathComparator implements Comparator<File> {

    @Override
    public int compare(File f1, File f2) {
        if (f1.getId().equals(f2.getId())) return 0;

        List<File> path1 = getLineage(f1);
        List<File> path2 = getLineage(f2);

        // Compare level by level
        int depth = Math.min(path1.size(), path2.size());
        for (int i = 0; i < depth; i++){
            File ancestor1 = path1.get(i);
            File ancestor2 = path2.get(i);

            if (!ancestor1.getId().equals(ancestor2.getId())){
                return compareSiblings(ancestor1, ancestor2);
            }
        }

        return Integer.compare(path1.size(), path2.size());
    }

    private List<File> getLineage(File file) {
        List<File> path = new ArrayList<>();
        File current = file;
        while (current != null) {
            path.addFirst(current);
            current = current.getParent();
        }
        return path;
    }

    private int compareSiblings(File s1, File s2) {
        String depName = FileRepository.DEPENDENCY_FOLDER_NAME;

        boolean isDep1 = depName.equalsIgnoreCase(s1.getFileName());
        boolean isDep2 = depName.equalsIgnoreCase(s2.getFileName());
        if (isDep1 != isDep2) return isDep1 ? 1 : -1;

        boolean isDir1 = Boolean.TRUE.equals(s1.getIsDirectory());
        boolean isDir2 = Boolean.TRUE.equals(s2.getIsDirectory());
        if (isDir1 != isDir2) return isDir1 ? -1 : 1;

        String n1 = s1.getFileName() != null ? s1.getFileName() : "";
        String n2 = s2.getFileName() != null ? s2.getFileName() : "";
        return String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
    }
}

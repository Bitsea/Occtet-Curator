/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.model;

import eu.occtet.bocfrontend.entity.CodeLocation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a node in the file tree structure.
 * Can represent either a file or a directory.
 *
 * This class is immutable except for the children list which can be modified
 * during tree construction but should be treated as read-only afterwards.
 */
public class FileTreeNode implements Serializable {

    private final UUID id;
    private final String name;
    private final String fullPath;
    private final FileTreeNode parent;
    private final List<FileTreeNode> children;
    private final CodeLocation codeLocation;
    private final boolean isDirectory;

    public FileTreeNode(String name,
                        String fullPath,
                        FileTreeNode parent,
                        List<FileTreeNode> children,
                        CodeLocation codeLocation,
                        boolean isDirectory) {
        this.id = UUID.randomUUID();
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.fullPath = Objects.requireNonNull(fullPath, "Full path cannot be null");
        this.parent = parent;
        this.children = children != null ? children : new ArrayList<>();
        this.codeLocation = codeLocation;
        this.isDirectory = isDirectory;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullPath() {
        return fullPath;
    }

    public FileTreeNode getParent() {
        return parent;
    }

    public List<FileTreeNode> getChildren() {
        return children;
    }

    public CodeLocation getCodeLocation() {
        return codeLocation;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    @Override
    public String toString() {
        return String.format("FileTreeNode{name='%s', id=%s, path='%s'}",
                name, id, fullPath);
    }
}

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

package eu.occtet.bocfrontend.model;

import eu.occtet.bocfrontend.entity.CodeLocation;

import java.io.Serializable;
import java.util.List;

public class FileTreeNode implements Serializable {

    private final String name;
    private final String fullPath;
    private final FileTreeNode parent;
    private final List<FileTreeNode> children;
    private final CodeLocation codeLocation;
    private final boolean isDirectory;

    public FileTreeNode(String name, String fullPath, FileTreeNode parent, List<FileTreeNode> children, CodeLocation codeLocation, boolean isDirectory) {
        this.name = name;
        this.fullPath = fullPath;
        this.parent = parent;
        this.children = children;
        this.codeLocation = codeLocation;
        this.isDirectory = isDirectory;
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
}

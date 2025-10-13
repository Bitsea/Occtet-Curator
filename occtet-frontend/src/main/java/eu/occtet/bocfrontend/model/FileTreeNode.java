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

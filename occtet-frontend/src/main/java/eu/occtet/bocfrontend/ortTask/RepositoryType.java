package eu.occtet.bocfrontend.ortTask;

public enum RepositoryType {

    GIT("GIT"),
    GIT_REPO("GIT_REPO"),
    MERCURIAL("MERCURIAL"),
    SUBVERSION("SUBVERSION"),
    UNKNOWN("UNKNOWN");

    private final String standardType;

    RepositoryType(String standardType) {
        this.standardType = standardType;
    }

    public String getStandardType() {
        return standardType;
    }
}

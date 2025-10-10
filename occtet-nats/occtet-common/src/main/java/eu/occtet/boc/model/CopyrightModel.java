package eu.occtet.boc.model;

/**
 * Copyright DTO for reading copyrights out of the scan-result.yml file
 */
public class CopyrightModel {

    private String statement;
    private String path;
    private int start_line;
    private int end_line;

    public CopyrightModel() {
    }

    public CopyrightModel(String statement, String path, int start_line, int end_line) {
        this.statement = statement;
        this.path = path;
        this.start_line = start_line;
        this.end_line = end_line;
    }

    public CopyrightModel(String statement, String path) {
        this.statement = statement;
        this.path = path;
        this.start_line = 0;
        this.end_line = 0;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStart_line() {
        return start_line;
    }

    public void setStart_line(int start_line) {
        this.start_line = start_line;
    }

    public int getEnd_line() {
        return end_line;
    }

    public void setEnd_line(int end_line) {
        this.end_line = end_line;
    }
}

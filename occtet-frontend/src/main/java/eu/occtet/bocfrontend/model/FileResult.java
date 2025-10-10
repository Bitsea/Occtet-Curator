package eu.occtet.bocfrontend.model;

public sealed interface FileResult {

    record Success(String content, String fileName) implements FileResult {}

    record Failure(String errorMessage) implements FileResult {}
}

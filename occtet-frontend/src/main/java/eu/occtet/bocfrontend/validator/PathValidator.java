package eu.occtet.bocfrontend.validator;

import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.exception.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class PathValidator implements Validator<String> {

    private static final Logger log = LogManager.getLogger(PathValidator.class);

    @Override
    public void accept(String value) {
        log.debug("PathValidator called with value: {}", value);

        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Path cannot be empty.");
        }

        try {
            Path path = Paths.get(value);
            if (path.isAbsolute()) {
                throw new ValidationException("Path must be relative.");
            }
        } catch (InvalidPathException e) {
            throw new ValidationException("Invalid path format. Please enter a valid relative path.", e);
        }
    }
}

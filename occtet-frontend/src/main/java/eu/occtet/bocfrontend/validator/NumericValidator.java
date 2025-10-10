package eu.occtet.bocfrontend.validator;

import io.jmix.flowui.component.validation.Validator;
import io.jmix.flowui.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class NumericValidator implements Validator<String> {

    @Override
    public void accept(String value) {
        if (value == null) return;
        try {
            Double.parseDouble(value);
        } catch (Exception e) {
            throw new ValidationException("Invalid number");
        }
    }
}

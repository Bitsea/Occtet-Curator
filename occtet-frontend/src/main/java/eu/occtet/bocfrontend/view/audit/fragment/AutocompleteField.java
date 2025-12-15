package eu.occtet.bocfrontend.view.audit.fragment;


import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyDownEvent;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.util.List;
import java.util.stream.Collectors;

public class AutocompleteField extends VerticalLayout {

    private static final Logger log = LogManager.getLogger(AutocompleteField.class);


    private final TextArea input = new TextArea();
    private final ListBox<String> suggestionBox = new ListBox<>();
    private String inputText="";

    private List<String> options;

    public AutocompleteField(String t) {
        log.debug("Initializing AutocompleteField");
        input.setLabel(t);
        input.setSizeFull();
        input.setWidth("100%");
        input.setHeight("100%");
        input.setValueChangeMode(ValueChangeMode.TIMEOUT);
        add(input, suggestionBox);

        suggestionBox.setVisible(false);
    }


    public void initializeField(){
        log.debug("initializeField called");
        input.addValueChangeListener(e -> {
            log.debug("inputText: {}", inputText);
            log.debug("change detected: between value; {},  newValue: {}", e.getValue(), e.getValue().replace(inputText, ""));
            String text = e.getValue();
            String search= text.replace(inputText, "");
            log.debug("Search text extracted: {}", search);
            updateSuggestions(search);

        });
        input.addKeyDownListener(Key.ENTER, this::onEnter);

        suggestionBox.addValueChangeListener(e -> {
            log.debug("Suggestion selected: {}", e.getValue());
            String val = e.getValue();
            if (val != null) {
                log.debug("Inserting selected suggestion: {}", val);
                if(!inputText.isEmpty())
                    input.setValue(inputText + " \n " + val);
                else input.setValue(val);

                inputText= input.getValue();
                suggestionBox.setVisible(false);
            }
        });
    }

    private void onEnter(KeyDownEvent event){
        log.debug("Enter key pressed");
        inputText= input.getValue();
    }

    public void setOptions( List<String> options) {
        this.options =options;
    }

    public void updateSuggestions(String text) {
        log.debug("Updating suggestions for input: {}  \n end", text);
        if (options == null || text.isEmpty()) {
            log.debug("No options available or input is empty");
            suggestionBox.setVisible(false);
            suggestionBox.removeAll();
            return;
        }

            String matchText = text.trim().toLowerCase();
            log.debug("Matching text: {}  \n :end ", matchText);
            List<String> list = options.stream().filter(s -> {
                if (s != null) {
                    s=s.trim();
                    return s.contains(matchText.trim())||s.startsWith(matchText.trim());
                }
                return false;
            }).collect(Collectors.toList());

        log.debug("Filtered suggestions: {}", list.size());
        if (list.isEmpty()) {
            log.debug("empty list");
            suggestionBox.setVisible(false);
            suggestionBox.removeAll();
        } else {
            log.debug("set Items");
            suggestionBox.setItems(list);
            suggestionBox.setVisible(true);
        }
    }

    public String getValue() {
        return input.getValue();
    }

    public void setValue(String value) {
        input.setValue(value);
    }

}

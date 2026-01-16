package eu.occtet.bocfrontend.view.help;



import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Route(value = "help-view", layout = MainView.class)
@ViewController(id = "HelpView")
@ViewDescriptor(path = "help-view.xml")
public class HelpView extends StandardView {

    private static final Logger log = LogManager.getLogger(HelpView.class);

    @ViewComponent
    private VerticalLayout main;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) throws IOException {


        Locale locale = VaadinSession.getCurrent().getLocale();
        String lang = locale.getLanguage();
        String path = "help/help_" + lang + ".html";

        String markdown = loadMarkdown(path);
        Html helpText = new Html(markdown);
        main.add(helpText);
    }

    private String loadMarkdown(String path) {
        log.debug("Loading markdown help file: {}", path);
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Markdown not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
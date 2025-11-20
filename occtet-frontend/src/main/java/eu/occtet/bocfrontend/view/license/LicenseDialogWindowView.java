package eu.occtet.bocfrontend.view.license;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;

@Route(value = "license-dialog-window-view", layout = MainView.class)
@ViewController("LicenseDialogWindowView")
@ViewDescriptor("license-dialog-window-view.xml")
public class LicenseDialogWindowView extends StandardView {
    @ViewComponent
    private JmixTextArea text;

    public String getText() {
        return text.getValue();
    }

    @Subscribe(id = "searchBtn", subject = "clickListener")
    public void onSearchBtnClick(final ClickEvent<JmixButton> event) {
        close(StandardOutcome.SAVE);
    }

    @Subscribe(id = "closeBtn", subject = "clickListener")
    public void onCloseBtnClick(final ClickEvent<JmixButton> event) {
        close(StandardOutcome.DISCARD);
    }
}
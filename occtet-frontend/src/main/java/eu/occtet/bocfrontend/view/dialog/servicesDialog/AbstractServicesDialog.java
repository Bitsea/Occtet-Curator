package eu.occtet.bocfrontend.view.dialog.servicesDialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.StandardView;



public abstract class AbstractServicesDialog extends StandardView {

    protected abstract void onInit(InitEvent event);

    public abstract void sendWorkDataButton(ClickEvent<Button> event);

    public void cancelButton(ClickEvent<Button> event){close(StandardOutcome.CLOSE);}


}

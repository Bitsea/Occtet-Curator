package eu.occtet.bocfrontend.view.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.StandardView;

public abstract class AbstractCreateContentDialog<T> extends StandardView {

    public abstract void setAvailableContent(T content);

    public abstract void addContentButton(ClickEvent<Button> event);

    public void cancelButton(ClickEvent<Button> event){close(StandardOutcome.CLOSE);}
}

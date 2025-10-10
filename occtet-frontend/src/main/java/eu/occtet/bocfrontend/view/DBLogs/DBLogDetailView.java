package eu.occtet.bocfrontend.view.DBLogs;


import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.entity.DBLog;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.view.*;

@Route(value = "dblog/:id", layout = MainView.class)
@ViewController(id = "DBLog.detail")
@ViewDescriptor(path = "dblog-detail-view.xml")
@EditedEntityContainer("dbLogDc")
@DialogMode(width = "64em")
public class DBLogDetailView extends StandardDetailView<DBLog>{

    @Override
    public boolean hasUnsavedChanges() {
        return false;
    }


}

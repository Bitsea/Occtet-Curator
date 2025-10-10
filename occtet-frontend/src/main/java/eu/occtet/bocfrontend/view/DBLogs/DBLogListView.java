package eu.occtet.bocfrontend.view.DBLogs;


import com.google.gson.Gson;
import com.vaadin.flow.component.ClickEvent;
import eu.occtet.boc.model.AILicenseMatcherWorkData;
import eu.occtet.boc.model.AIStatusQueryWorkData;
import eu.occtet.boc.model.WorkTask;
import eu.occtet.boc.model.WorkerStatus;
import eu.occtet.bocfrontend.controller.DBLogController;
import eu.occtet.bocfrontend.dao.DBLogRepository;
import eu.occtet.bocfrontend.entity.DBLog;
import eu.occtet.bocfrontend.service.NatsService;
import eu.occtet.bocfrontend.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.facet.Timer;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import io.nats.client.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;


import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Route(value = "dblog", layout = MainView.class)
@ViewController(id = "DBLog.list")
@ViewDescriptor(path = "dblog-list-view.xml")
@LookupComponent("DBLogListDataGrid")
@DialogMode(width = "64em")
public class DBLogListView extends StandardListView<DBLog> {

    @Autowired
    private DBLogRepository dbrepo;
    @ViewComponent
    private CollectionContainer<DBLog> dbLogDc;
    @ViewComponent
    private DataGrid<DBLog> DBLogListDataGrid;


    @Autowired
    private NatsService natsService;

    @ViewComponent
    JmixButton testConnection;

    @Autowired
    private UiAsyncTasks uiAsyncTasks;

    @Autowired
    private DBLogController dbLogController;

    @Autowired
    private Dialogs dialogs;

    private static final Logger log = LogManager.getLogger(DBLogListView.class);


    @Subscribe
    public void onInit(final InitEvent event) {
        // Initialize the DataGrid with the DBLog items
        List<DBLog> dbLogs = dbrepo.findAll();
        dbLogs.sort(Comparator.comparing(DBLog::getEventDate).reversed());
        dbLogDc.setItems(dbLogs);

    }

    @Subscribe("callApiButtonCopyright")
    protected void onCallApiButtonCopyrightClick(final ClickEvent<JmixButton> event) {
        event.getSource().setText("Calling in Process");

        uiAsyncTasks.supplierConfigurer(this::callCopyrightApi)
                .withResultHandler(api -> {
                    Objects.requireNonNull(DBLogListDataGrid.getAction("refresh"))
                            .actionPerform(event.getSource());
                    event.getSource().setText("Call Backend for Copyrights");
                })
                .withTimeout(600, TimeUnit.SECONDS)
                .supplyAsync();

    }

    @Subscribe("callApiButtonLicense")
    protected void onCallApiButtonLicenseClick(final ClickEvent<JmixButton> event) {
        event.getSource().setText("Calling in Process");

        uiAsyncTasks.supplierConfigurer(this::callLicenseApi)
                .withResultHandler(api -> {
                    Objects.requireNonNull(DBLogListDataGrid.getAction("refresh"))
                            .actionPerform(event.getSource());
                    event.getSource().setText("Call Backend for Licenses");
                })
                .withTimeout(600, TimeUnit.SECONDS)
                .supplyAsync();

    }

    private String callCopyrightApi() {
        return dbLogController.callCopyrightApi();
    }

    private String callLicenseApi() {
        return dbLogController.callLicenseApi();
    }



    @Subscribe("timer")
    public void onTimerTimerAction(final Timer.TimerActionEvent event) {
        Objects.requireNonNull(DBLogListDataGrid.getAction("refresh")).actionPerform(event.getSource().getOwner());
    }

    @Subscribe(id = "testConnection", subject = "clickListener")
    public void onTestConnectionClick(final ClickEvent<JmixButton> event) {
        AIStatusQueryWorkData aiStatusQuery = new AIStatusQueryWorkData();
        aiStatusQuery.setDetails("are you ready to process some tasks?");
        aiStatusQuery.setExpectedStatus("ready");
        LocalDateTime now = LocalDateTime.now();
        long actualTimestamp = now.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        WorkTask workTask = new WorkTask("status_request", "question", actualTimestamp, aiStatusQuery);
        try {
            Gson gson = new Gson();
            String message = gson.toJson(workTask);
            log.debug("sending message to ai service: {}", message);
            natsService.sendWorkMessageToStream("work.ai", message.getBytes(Charset.defaultCharset()));
        }catch(Exception e){
            dialogs.createMessageDialog().withText("Error with Ai service connection: "+ e.getMessage()).open();
        }
    }
}

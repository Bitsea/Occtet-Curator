/*
 *
 *  Copyright (C) 2025 Bitsea GmbH
 *  *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  License-Filename: LICENSE
 * /
 *
 */

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

    @ViewComponent
    JmixButton testConnection;

    private static final Logger log = LogManager.getLogger(DBLogListView.class);


    @Subscribe
    public void onInit(final InitEvent event) {
        // Initialize the DataGrid with the DBLog items
        List<DBLog> dbLogs = dbrepo.findAll();
        dbLogs.sort(Comparator.comparing(DBLog::getEventDate).reversed());
        dbLogDc.setItems(dbLogs);

    }


    @Subscribe("timer")
    public void onTimerTimerAction(final Timer.TimerActionEvent event) {
        Objects.requireNonNull(DBLogListDataGrid.getAction("refresh")).actionPerform(event.getSource().getOwner());
    }

}

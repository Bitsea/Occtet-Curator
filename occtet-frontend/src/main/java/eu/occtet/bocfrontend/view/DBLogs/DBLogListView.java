/*
 * Copyright (C) 2025 Bitsea GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https:www.apache.orglicensesLICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package eu.occtet.bocfrontend.view.DBLogs;


import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import eu.occtet.bocfrontend.dao.LoggingEventRepository;
import eu.occtet.bocfrontend.entity.DBLog;
import eu.occtet.bocfrontend.entity.InventoryItem;
import eu.occtet.bocfrontend.entity.LoggingEvent;
import eu.occtet.bocfrontend.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;


@Route(value = "dblog", layout = MainView.class)
@ViewController(id = "DBLog.list")
@ViewDescriptor(path = "dblog-list-view.xml")
@LookupComponent("dataGrid")
@DialogMode(width = "64em")
public class DBLogListView extends StandardListView<DBLog> {

    @Autowired
    private LoggingEventRepository dbrepo;

    @ViewComponent
    private DataGrid<LoggingEvent> dataGrid;

    @ViewComponent
    private CollectionContainer<LoggingEvent> dbLogDc;

    private static final Logger log = LogManager.getLogger(DBLogListView.class);

    @Supply(to = "dataGrid.timestmp", subject = "renderer")
    private Renderer<LoggingEvent> loggingEventDateRenderer() {
        return new TextRenderer<>(e -> LocalDateTime.ofInstant(Instant.ofEpochMilli(e.getTimestmp()), ZoneId.systemDefault()).toString());
    }

}

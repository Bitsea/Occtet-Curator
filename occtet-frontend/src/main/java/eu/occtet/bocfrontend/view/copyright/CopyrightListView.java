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

package eu.occtet.bocfrontend.view.copyright;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.editor.EditorCloseEvent;
import com.vaadin.flow.component.grid.editor.EditorSaveEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.dao.CopyrightRepository;
import eu.occtet.bocfrontend.entity.Copyright;
import eu.occtet.bocfrontend.service.CopyrightService;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Route(value = "copyrights", layout = MainView.class)
@ViewController(id = "Copyright.list")
@ViewDescriptor(path = "copyright-list-view.xml")
@LookupComponent("copyrightsDataGrid")
@DialogMode(width = "64em")
public class CopyrightListView extends StandardListView<Copyright> {

    private static final Logger log = LogManager.getLogger(CopyrightListView.class);

    @ViewComponent
    private CollectionContainer<Copyright> copyrightsDc;
    @ViewComponent
    private CollectionLoader<Copyright> copyrightsDl;

    @Autowired
    private CopyrightService copyrightService;

    @Autowired
    private Downloader downloader;

    @Autowired
    private Notifications notifications;

    @Autowired
    private CopyrightRepository copyrightRepository;
    @Autowired
    protected UiComponents uiComponents;

    private List<Copyright> copyrightsList;

    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private JmixCheckbox immediateCheckbox;
    @ViewComponent
    private JmixButton saveButton;

    private final Set<Copyright> copyrights = new HashSet<>();

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event){

        copyrightsList = copyrightRepository.findAll();
    }

    @Supply(to = "copyrightsDataGrid.curated", subject = "renderer")
    protected Renderer<Copyright> curatedComponentRenderer() {
        return new ComponentRenderer<>(
                () -> {
                    JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
                    checkbox.setReadOnly(true);
                    return checkbox;
                },
                (checkbox, copyright) -> checkbox.setValue(copyright.isCurated())
        );
    }

    @Supply(to = "copyrightsDataGrid.garbage", subject = "renderer")
    protected Renderer<Copyright> garbageComponentRenderer() {
        return new ComponentRenderer<>(this::createCheckbox);
    }

    private JmixCheckbox createCheckbox(Copyright copyright){
        JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
        checkbox.setReadOnly(true);
        checkbox.setValue(copyright.isGarbage());
        return checkbox;
    }

    @Subscribe("downloadButton")
    public void downloadGarbageCopyrights(ClickEvent<Button> event){

        List<Copyright> copyrightList = copyrightsDc.getItems();
        List<Copyright> garbageList;

        if(event != null && !copyrightList.isEmpty()){
            garbageList = copyrightList.stream().filter(Copyright::getGarbage).toList();
            if(!garbageList.isEmpty()){
                copyrightService.createYML(garbageList);
                downloader.download(copyrightService.getYmlFileRef());
            }else{
                getInfoMessage("No garbage copyrights");
            }
        }
    }

    @Subscribe("uploadFile")
    public void uploadCopyrights(final FileUploadSucceededEvent<FileUploadField> event){

        File file = copyrightService.createFileUploadCopyrights(event);
        if(file != null){
            List<String> garbageCopys = copyrightService.readYML(file);
            if(!garbageCopys.isEmpty()){
                copyrightService.setGarbageCopyrightsInJSON(garbageCopys);
                getInfoMessage("File uploaded successfully");
            }else{
                getInfoMessage("Upload failed!");
            }
        }
    }

    private void getInfoMessage(String message){
        notifications.create(message)
                .withPosition(Notification.Position.TOP_CENTER)
                .withDuration(3000)
                .show();
    }

    @Install(to = "copyrightsDataGrid.@editor", subject = "saveListener")
    private void copyrightsDataGridEditorCloseListener(final EditorSaveEvent<Copyright> editorSaveEvent) {
        log.debug("Editor closed for copyright: {}", editorSaveEvent.getItem().getCopyrightText());
        saveOrEnqueue(editorSaveEvent.getItem());
    }

    @Subscribe(id = "saveButton", subject = "clickListener")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        saveEnqueuedChanges();
    }

    private void saveOrEnqueue(Copyright copyright) {
        log.debug("save or enqueue");
        if (immediateCheckbox.getValue()) {
            saveChanges(copyright);
        } else {
            copyrights.add(copyright);
        }
    }

    private void saveChanges(Copyright copyright) {
        Copyright savedCopyright = dataManager.save(copyright);
        copyrightsDc.replaceItem(savedCopyright);
        notifications.show("Changes saved to the database");
    }

    private void saveEnqueuedChanges() {
        log.debug("saved enqueued changes");
        if (!copyrights.isEmpty()) {
            dataManager.saveAll(copyrights);
            copyrights.clear();
            notifications.show("Changes saved to the database");
            copyrightsDl.load();
        }
    }
    
    
    
}
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
package eu.occtet.bocfrontend.view.dialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import eu.occtet.bocfrontend.service.InformationFileService;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

@ViewController("uploadInformationFileDialog")
@ViewDescriptor("upload-informationFile-dialog.xml")
@DialogMode(width = "600px", height = "350px")
public class UploadInformationFileDialog extends StandardView {

    private static final Logger log = LogManager.getLogger(UploadInformationFileDialog.class);

    @ViewComponent
    private TextField contextTextField;

    @Autowired
    private InformationFileService informationFileService;

    private File infoFile;
    @Autowired
    private Notifications notifications;


    @Subscribe("fileUploadField")
    public void uploadInformationFile(final FileUploadSucceededEvent<FileUploadField> event){
        infoFile = informationFileService.createTempInformationFile(event);
    }

    @Subscribe("startBtn")
    public void uploadFile(final ClickEvent<JmixButton> event){

        String context = contextTextField.getValue();
        if(infoFile != null && !context.isEmpty()) {
            informationFileService.uploadInformationFile(infoFile.getAbsolutePath(),context);
            notifications.create("reading file...")
                    .withPosition(Notification.Position.MIDDLE)
                    .withDuration(3000)
                    .show();
            close(StandardOutcome.CLOSE);
        }
    }
}

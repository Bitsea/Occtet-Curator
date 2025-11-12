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

package eu.occtet.bocfrontend.view.setup;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import eu.occtet.bocfrontend.view.dialog.UploadInformationFileDialog;
import eu.occtet.bocfrontend.view.main.MainView;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "setup-view", layout = MainView.class)
@ViewController(id = "SetupView")
@ViewDescriptor(path = "setup-view.xml")
public class SetupView extends StandardView {

    private static final Logger log = LogManager.getLogger(SetupView.class);

    @Autowired
    private DialogWindows dialogWindow;

    @Subscribe(id = "informationFileBtn")
    public void uploadInformationFile(final ClickEvent<JmixButton> event){

        DialogWindow<UploadInformationFileDialog> window = dialogWindow.view(this, UploadInformationFileDialog.class).build();
        window.open();
    }
}
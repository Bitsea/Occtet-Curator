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

package eu.occtet.bocfrontend.view.codeviewerfragment;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import elemental.json.JsonArray;
import elemental.json.JsonType;
import elemental.json.JsonValue;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.codeeditor.CodeEditor;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.codeeditor.CodeEditorMode;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;

import static com.google.common.io.Files.getFileExtension;

@FragmentDescriptor("code-viewer-fragment.xml")
@JsModule("./js/code-viewer.js")
public class CodeViewerFragment extends Fragment<VerticalLayout> {

    @ViewComponent
    private CodeEditor codeEditor;
    @ViewComponent
    private JmixButton findPrevButton;
    @ViewComponent
    private JmixButton findNextButton;
    @ViewComponent
    private TextField searchField;
    @ViewComponent
    private NativeLabel countLabel;

    @Autowired
    private Notifications notifications;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        // Initialize the editor and disable the default search shortcut
        codeEditor.getElement().executeJs("window.AceEditorUtils.initializeEditor(this);");
        countLabel.setText("0 of 0");
        codeEditor.setReadOnly(true);
    }

    @Subscribe(value = "findNextButton", subject = "clickListener")
    public void onFindNextButtonClick(ClickEvent<JmixButton> event) {
        String query = searchField.getValue();
        if (query != null && !query.isEmpty()) {
            codeEditor.getElement()
                    .executeJs("return window.AceEditorUtils.findTextWithCount(this, $0, $1);", query, false)
                    .then(JsonValue.class, this::updateCountLabel);
        }
    }

    @Subscribe(value = "findPrevButton", subject = "clickListener")
    public void onFindPrevButtonClick(ClickEvent<JmixButton> event) {
        String query = searchField.getValue();
        if (query != null && !query.isEmpty()) {
            codeEditor.getElement()
                    .executeJs("return window.AceEditorUtils.findTextWithCount(this, $0, $1);", query, true)
                    .then(JsonValue.class, this::updateCountLabel);
        }
    }

    private void updateCountLabel(JsonValue result) {
        if (result != null && result.getType() == JsonType.ARRAY) {
            JsonArray array = (JsonArray) result;
            if (array.length() >= 2) {
                int current = (int) array.getNumber(0);
                int total = (int) array.getNumber(1);
                countLabel.setText(current + " of " + total);
            }
        }
    }

    /**
     * Set editor content and detect mode by filename.
     */
    public void setCodeEditorContent(String content, String filename) {
        codeEditor.setValue(content);
        codeEditor.setMode(detectModeByExtension(filename));

        String regexForMarking = "copyright.*";
        String markerColor = "ace_marker_yellow";

        String markerCss = """
        .ace_marker_yellow {
            position: absolute;
            background: rgba(255, 255, 0, 0.4);
            z-index: 20;
        }
    """;
        codeEditor.getElement().executeJs("window.AceEditorUtils.highlightAllOccurrences(this, $0, $1, $2, $3);",
                regexForMarking
                , markerColor, markerCss, true); // set to false if working with regexes is not wanted
    }

//    @Subscribe("fileUploadField")
//    public void onFileUploadFieldFileUploadSucceeded(final FileUploadSucceededEvent<FileUploadField> event) {
//        String fileName = event.getFileName();
//        byte[] fileBytes = event.getSource().getValue();
//        if (fileBytes == null){
//            notifications.create("File content is empty").withType(Notifications.Type.ERROR).show();
//            return;
//        }
//        String content = new String(fileBytes, StandardCharsets.UTF_8);
//        setCodeEditorContent(content, fileName);
//    }

    private CodeEditorMode detectModeByExtension(String filename) {
        String extension = getFileExtension(filename);

        if (extension.equals("c") || extension.equals("h")) return CodeEditorMode.C_CPP;
        if (extension.equals("cpp") || extension.equals("hpp")) return CodeEditorMode.C_CPP;
        if (extension.equals("java")) return CodeEditorMode.JAVA;
        if (extension.equals("js")) return CodeEditorMode.JAVASCRIPT;
        if (extension.equals("ts")) return CodeEditorMode.TYPESCRIPT;
        if (extension.equals("py")) return CodeEditorMode.PYTHON;
        if (extension.equals("xml")) return CodeEditorMode.XML;
        if (extension.equals("html")) return CodeEditorMode.HTML;
        if (extension.equals("css")) return CodeEditorMode.CSS;
        if (extension.equals("md")) return CodeEditorMode.MARKDOWN;
        if (extension.equals("json")) return CodeEditorMode.JSON;
        if (extension.equals("txt")) return CodeEditorMode.TEXT;
        if (extension.equals("yaml")) return CodeEditorMode.YAML;
        if (extension.equals("yml")) return CodeEditorMode.YAML;
        if (extension.equals("sql")) return CodeEditorMode.SQL;
        return CodeEditorMode.TEXT;
    }
}
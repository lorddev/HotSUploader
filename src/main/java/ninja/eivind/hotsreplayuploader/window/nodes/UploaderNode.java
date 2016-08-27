// Copyright 2016 Eivind Vegsundvåg
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ninja.eivind.hotsreplayuploader.window.nodes;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import ninja.eivind.hotsreplayuploader.di.JavaFXController;
import ninja.eivind.hotsreplayuploader.models.ReplayFile;
import ninja.eivind.hotsreplayuploader.models.stringconverters.StatusBinder;
import ninja.eivind.hotsreplayuploader.providers.hotslogs.HotsLogsProvider;
import ninja.eivind.hotsreplayuploader.scene.control.CustomListCellFactory;
import ninja.eivind.hotsreplayuploader.services.UploaderService;
import ninja.eivind.hotsreplayuploader.utils.ReplayFileComparator;
import org.springframework.beans.factory.annotation.Autowired;

public class UploaderNode extends VBox implements JavaFXController {

    @FXML
    private FXMLLoader loader;

    @FXML
    private Label newReplaysCount;

    @FXML
    private ListView<ReplayFile> newReplaysView;

    @FXML
    private Label uploadedReplays;

    @FXML
    private Label status;

    @Autowired
    private StatusBinder statusBinder;

    @Autowired
    private UploaderService uploaderService;

    public UploaderNode() {

    }

    private void bindList() {
        final ObservableList<ReplayFile> files = uploaderService.getFiles();
        newReplaysCount.setText(String.valueOf(files.size()));
        files.addListener((ListChangeListener<ReplayFile>) c -> newReplaysCount.setText(String.valueOf(files.size())));
        newReplaysView.setItems(files.sorted(new ReplayFileComparator()));
        newReplaysView.setCellFactory(new CustomListCellFactory(uploaderService));

        uploadedReplays.textProperty().bind(uploaderService.getUploadedCount());
    }

    @Override
    public void initialize() {
        setupFileHandler();
        if (uploaderService.isIdle()) {
            setIdle();
        }
        bindList();
        status.textProperty().bind(statusBinder.message());
    }

    private void setStatus(final String description, final Paint color) {
        statusBinder.message().setValue(description);
        status.textFillProperty().setValue(color);
    }

    private void setIdle() {
        setStatus("Idle", Paint.valueOf("#38d3ff"));
    }

    private void setMaintenance() {
        setStatus("Maintenance", Paint.valueOf("#FF0000"));
    }

    private void setUploading() {
        setStatus("Uploading", Paint.valueOf("#00B000"));
    }

    private void setError() {
        setStatus("Connection error", Paint.valueOf("#FF0000"));
    }

    private void setupFileHandler() {
        uploaderService.setRestartOnFailure(true);
        uploaderService.setOnSucceeded(event -> {
            if (HotsLogsProvider.isMaintenance()) {
                setMaintenance();
            } else if (uploaderService.isIdle()) {
                setIdle();
            } else {
                setUploading();
            }
        });
        uploaderService.setOnFailed(event -> setError());
        uploaderService.start();
    }
}

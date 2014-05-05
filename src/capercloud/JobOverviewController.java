/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.model.Result;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class JobOverviewController implements Initializable {
    
    @FXML private ComboBox cbJobType;
    @FXML private Button btnRun;
    @FXML private TextArea console;
    @FXML private TextField tfFilepath;
    @FXML private TextField tfJobName;
    @FXML private TableView tvResult;
    @FXML private TableColumn<Result, String> tcJobName;
    @FXML private TableColumn<Result, String> tcResultPath;
    
    @FXML private TabPane tpJobOverview;
    
    private CaperCloud mainApp;
    private final ObservableList strings = FXCollections.observableArrayList(
        "Find new gene", "Find SAP", "Find AS", "RNA-seq");
    
    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
        
        //set result table when we know the mainApp
        tvResult.setItems(mainApp.getResults());
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbJobType.setItems(strings);
        tcJobName.setCellValueFactory(new PropertyValueFactory<Result, String>("jobName"));
        tcResultPath.setCellValueFactory(new PropertyValueFactory<Result, String>("resultPath"));
        SingleSelectionModel<Tab> selectionModel = tpJobOverview.getSelectionModel();
        selectionModel.select(0);
    }    
    
    @FXML
    private void handleRun() {
        try {
            btnRun.setDisable(true);
            console.setText("");
            String command = "/Users/shuai/Bio/tools/tandem-osx-13-09-01-1/bin/tandem " +
                    "/Users/shuai/Bio/tools/tandem-osx-13-09-01-1/bin/input.xml";
            
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            CommandLine commandline = CommandLine.parse(command);
            DefaultExecutor exec = new DefaultExecutor();
            
            exec.setExitValues(null);
            
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream,errorStream);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandline, new ExecuteResultHandler() {
                @Override
                public void onProcessComplete(int i) {
                    try {
                        String out = outputStream.toString("utf8");
                        console.setText(out);
                        
                        mainApp.getResults().add(new Result(tfJobName.getText(), "output filepath"));
                    } catch (UnsupportedEncodingException ex) { 
                        Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        btnRun.setDisable(false);
                    }
                }
                
                @Override
                public void onProcessFailed(ExecuteException ee) {
                    try {
                        String error = errorStream.toString("utf8");
                        console.setText(error);
                    } catch (UnsupportedEncodingException ex) {
                        
                        Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        btnRun.setDisable(false);
                    }
                }
            });
        } catch (IOException ex) {
            Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML
    private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Spectra File");
        File file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());
        if (file != null) {
            tfFilepath.setText(file.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleClear() {
        tfFilepath.setText("");
    }
    
    @FXML
    private void handleAddCloudSettings() {
        mainApp.showPreferenceView(0);
    }
    
    @FXML
    private void handleAddSearchSettings() {
        mainApp.showPreferenceView(1);
    }
    
    @FXML
    private void handleLoadSearchSettings() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Search Settings");
        File file = fileChooser.showOpenDialog(mainApp.getPrimaryStage());
        if (file != null) {
            tfFilepath.setText(file.getAbsolutePath());
        }
    }
}
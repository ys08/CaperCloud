/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.model.FileDescription;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import org.jets3t.service.multithread.S3ServiceMulti;
import org.jets3t.service.security.ProviderCredentials;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class JobOverviewController implements Initializable {
    
    private CaperCloud mainApp;
    private ObservableList<String> nickList = FXCollections.observableArrayList();
    private final ObservableList jobTypes = FXCollections.observableArrayList(
            "Novel Protein", 
            "SAP", 
            "AS",
            "Custom Protein Database"
    );
    //data for localFileTableView
    private ObservableList localFileCache;
    //get it from manage account action
    private ProviderCredentials credentials;
    
    @FXML private TabPane mainTab;
    //File Tab
    @FXML private Button btnManageAccounts;
    @FXML private Button btnTransferPreferences;
    @FXML private Button btnLogout;
    @FXML private Button btnLocalBrowse;
    @FXML private ComboBox cbSwitchAccount;
    @FXML private TableView tvLocal;
    @FXML private TableView tvRemote;
    @FXML private TableView tvTransferLog;
    @FXML private TableColumn tcLocalFilename;
    @FXML private TableColumn tcLocalFilesize;
    @FXML private TableColumn tcLocalModifiedTime;
    
    //Job Tab
    @FXML private ComboBox cbJobType;
    @FXML private BorderPane bpJobType;
    @FXML private TableView tvInput;
    @FXML private TableView tvFixedModifications;
    @FXML private TableView tvVariableModifications;
    @FXML private TableView tvModifications;
    
    //Status Tab
    @FXML private TableView tvJobMonitor;
    @FXML private TableView tvInstanceMonitor;
    
    //Result Tab
    @FXML private TableView tvResults;

    public ObservableList<FileDescription> getLocalFileCache() {
        //lazy init
        if (this.localFileCache == null) {
            this.localFileCache = FXCollections.observableArrayList();
        }
        return localFileCache;
    }

    public void setLocalFileCache(ObservableList<FileDescription> localFileCache) {
        this.localFileCache = localFileCache;
    }

    public ObservableList getNickLists() {
        return nickList;
    }

    public ComboBox getCbSwitchAccount() {
        return cbSwitchAccount;
    }
    
    
    
    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;   
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        //File tab init
        this.cbSwitchAccount.setItems(this.nickList);
        this.tvLocal.setPlaceholder(new Text(""));
        this.tvRemote.setPlaceholder(new Text(""));
        this.tvTransferLog.setPlaceholder(new Text(""));
        //Job tab init
        this.cbJobType.setItems(jobTypes);
        this.tvInput.setPlaceholder(new Text(""));
        this.tvFixedModifications.setPlaceholder(new Text(""));
        this.tvVariableModifications.setPlaceholder(new Text(""));
        this.tvModifications.setPlaceholder(new Text());
        //Status tab init
        this.tvJobMonitor.setPlaceholder(new Text(""));
        this.tvInstanceMonitor.setPlaceholder(new Text(""));
        //Result tab init
        this.tvResults.setPlaceholder(new Text(""));
        
    }    
    @FXML
    private void handleManageAccountsAction() {
        this.mainApp.showLoginView();
    }
    @FXML
    private void handleLocalBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser(); 
        directoryChooser.setTitle("Please choose a folder");
        
        File file = directoryChooser.showDialog(null);
        if(file != null) {
            this.getLocalFileCache().clear();
            for(File f : file.listFiles()) {
                this.getLocalFileCache().add(new FileDescription(f.getName(), f.length(), new Date(f.lastModified()), null));
                this.tvLocal.setItems(this.getLocalFileCache());
            }
        }
        listLocalFiles();
    }
    
    @FXML
    private void handleJobTypeChange(ActionEvent ae) {
        if("Novel Protein".equals(cbJobType.getValue())) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TypeOne.fxml"));
                AnchorPane ap = (AnchorPane) loader.load();
                bpJobType.setCenter(ap);
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       if("SAP".equals(cbJobType.getValue())) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TypeTwo.fxml"));
                AnchorPane ap = (AnchorPane) loader.load();
                bpJobType.setCenter(ap);
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if("AS".equals(cbJobType.getValue())) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TypeThree.fxml"));
                AnchorPane ap = (AnchorPane) loader.load();
                bpJobType.setCenter(ap);
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if("Custom Protein Database".equals(cbJobType.getValue())) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TypeFour.fxml"));
                AnchorPane ap = (AnchorPane) loader.load();
                bpJobType.setCenter(ap);
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void listLocalFiles() {
        this.tcLocalFilename.setCellValueFactory(new PropertyValueFactory<FileDescription, String>("filename"));
        this.tcLocalFilesize.setCellValueFactory(new PropertyValueFactory<FileDescription, Long>("filesize"));
        this.tcLocalModifiedTime.setCellValueFactory(new PropertyValueFactory<FileDescription, Date>("modifiedTime"));
        this.tvLocal.setItems(this.getLocalFileCache());
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.model.DataTransferTask;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;

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
    private ObservableList<File> localFileCache;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private File homeDirectory = new File(System.getProperty("user.home"));
    
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
    @FXML private TextField tfLocalPath;
    
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

    public ObservableList<File> getLocalFileCache() {
        //lazy init
        if (this.localFileCache == null) {
            this.localFileCache = FXCollections.observableArrayList();
        }
        return localFileCache;
    }

    public void setLocalFileCache(ObservableList<File> localFileCache) {
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
        this.tvLocal.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.tvRemote.setPlaceholder(new Text(""));
        this.tvTransferLog.setPlaceholder(new Text(""));
        this.tfLocalPath.setText(this.homeDirectory.getAbsolutePath());
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
        
        //init local table data
        Iterator<File> filesinFolder = FileUtils.iterateFiles(this.homeDirectory, null, false);
        while (filesinFolder.hasNext()) {
            this.getLocalFileCache().add(filesinFolder.next());
        }
        this.getLocalFileCache();
        this.tcLocalFilename.setCellValueFactory(new Callback<CellDataFeatures<File, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<File, String> p) {
                return new SimpleStringProperty(p.getValue().getName());
            }
        });
        this.tcLocalFilesize.setCellValueFactory(new Callback<CellDataFeatures<File, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<File, String> p) {
                long fSize = p.getValue().length();
                DecimalFormat df = new DecimalFormat("#.00");
                String fileSizeString;
                if (fSize < 1024) {
                    fileSizeString = df.format(fSize) + "B";
                } else if (fSize < 1048576) {
                    fileSizeString = df.format(fSize / 1024) + "K";
                } else if (fSize < 1073741824) {
                    fileSizeString = df.format((double) fSize / 1048576) + "M";
                } else {
                    fileSizeString = df.format((double) fSize / 1073741824) + "G";
                }           
                return new SimpleStringProperty(fileSizeString);
            }
        });
        this.tcLocalModifiedTime.setCellValueFactory(new Callback<CellDataFeatures<File, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<File, String> p) {
                Date d = new Date(p.getValue().lastModified());
                return new SimpleStringProperty(sdf.format(d));
            }
        });
        this.tvLocal.setItems(this.getLocalFileCache());
        
        //local table clear selection
        tvLocal.setRowFactory(new Callback<TableView<File>, TableRow<File>>() {  
            @Override  
            public TableRow<File> call(TableView<File> tableView2) {  
            final TableRow<File> row = new TableRow<>();  
            row.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {  
                @Override  
                public void handle(MouseEvent event) {  
                    final int index = row.getIndex();  
                    if (index >= 0 && index < tvLocal.getItems().size() && tvLocal.getSelectionModel().isSelected(index)  ) {
                        tvLocal.getSelectionModel().clearSelection();
                        event.consume();  
                    }  
                }  
            });  
            return row;  
            }  
        });  
    }    
    
    @FXML
    private void handleManageAccountsAction() {
        this.mainApp.showLoginView();
    }
    @FXML
    private void handleLocalBrowse() {
        DirectoryChooser directoryChooser = new DirectoryChooser(); 
        directoryChooser.setTitle("Please choose a folder");
        
        File folder = directoryChooser.showDialog(null);
        //we will do nothing if user does not select a folder
        if (folder == null) {
            return;
        }
        this.tfLocalPath.setText(folder.getAbsolutePath());
        this.getLocalFileCache().clear();
        Iterator<File> filesinFolder = FileUtils.iterateFiles(folder, null, false);
        while (filesinFolder.hasNext()) {
            this.getLocalFileCache().add(filesinFolder.next());
        }
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
    @FXML
    private void handleUploadAction() {
        ObservableList<File> selectedFiles = this.tvLocal.getSelectionModel().getSelectedItems();
        if (selectedFiles.size() == 0) {
            return;
        }
        for (File f : selectedFiles) {
            Random rng = new Random();
            this.tvTransferLog.getItems().add(
                new DataTransferTask(f.getName(), f.getPath(), rng.nextInt(3000) + 2000, rng.nextInt(30) + 20));
        }

        
        TableColumn filenameCol = (TableColumn) this.tvTransferLog.getColumns().get(0);
        filenameCol.setCellValueFactory(new PropertyValueFactory<DataTransferTask, String>("fileName"));
        TableColumn fromCol = (TableColumn) this.tvTransferLog.getColumns().get(1);
        fromCol.setCellValueFactory(new PropertyValueFactory<DataTransferTask, String>("fromPath"));
        TableColumn progressCol = (TableColumn) this.tvTransferLog.getColumns().get(4);
        progressCol.setCellValueFactory(new PropertyValueFactory<DataTransferTask, Double>("progress"));
        progressCol.setCellFactory(ProgressBarTableCell.<DataTransferTask> forTableColumn());
        TableColumn statusCol = (TableColumn) this.tvTransferLog.getColumns().get(5);
        statusCol.setCellValueFactory(new PropertyValueFactory<DataTransferTask, String>("message"));
        
        ExecutorService executor = Executors.newFixedThreadPool(this.tvTransferLog.getItems().size(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
        for (Iterator it = this.tvTransferLog.getItems().iterator(); it.hasNext();) {
            DataTransferTask task = (DataTransferTask) it.next();
            executor.execute(task);
        }
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.exception.IllegalCredentialsException;
import capercloud.model.DataTransferTask;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.DefaultFileComparator;
import org.apache.commons.io.comparator.DirectoryFileComparator;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class JobOverviewController implements Initializable {
    
    private CaperCloud mainApp;
    private ObservableList<String> nickList;
    private final ObservableList jobTypes = FXCollections.observableArrayList(
            "Novel Protein", 
            "SAP", 
            "AS",
            "Custom Protein Database"
    );
    
//data for local TableView
    private ObservableList<File> localFileCache;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private File homeDirectory;
    
    
//data for remote TableView
    private ObservableList<S3Bucket> remoteBucketCache;
    private ObservableList<S3Object> remoteObjectCache;
    
//main tab
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
    @FXML private TableColumn tcRemoteFilename;
    @FXML private TableColumn tcRemoteFilesize;
    @FXML private TableColumn tcRemoteUploadTime;
    @FXML private TextField tfLocalPath;
    @FXML private TextArea fileLog;
    
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
    
    public JobOverviewController() {
        System.out.println("in JobOverviewController constructor");
        this.nickList = FXCollections.observableArrayList();
    }

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

    public Button getBtnLogout() {
        return btnLogout;
    }

    public ObservableList<S3Bucket> getRemoteBucketCache() {
        if (this.remoteBucketCache == null) {
            this.remoteBucketCache = FXCollections.observableArrayList();
        }
        return remoteBucketCache;
    }
    
    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;   
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("in JobOverviewController initialize");
    
//File tab init
        this.cbSwitchAccount.setItems(this.nickList);
        this.tvLocal.setPlaceholder(new Text(""));
        this.tvLocal.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
          
//init local table data
        this.homeDirectory = new File(System.getProperty("user.home"));
        this.updateLocalFileCache();
        this.tcLocalFilename.setCellValueFactory(new Callback<CellDataFeatures<File, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<File, String> p) {
                return new SimpleStringProperty(p.getValue().getName());
            }
        });
        this.tcLocalFilename.setCellFactory(new Callback<TableColumn<File, String>, TableCell<File, String>>() {
            @Override
            public TableCell<File, String> call(TableColumn<File, String> p) {
                TableCell<File, String> cell = new TableCell<File, String>() {
                    @Override
                    protected void updateItem(String t, boolean empty) {
                        super.updateItem(t, empty);
                        if (t != null) {
                            HBox box= new HBox();
                            box.setSpacing(10);
                            
                            Label fileName = new Label(t);
                            ImageView imageview = new ImageView();
                            imageview.setFitHeight(15);
                            imageview.setFitWidth(12);
                            File f = (File) this.getTableRow().getItem();
                            if (f.isDirectory()) {
                                imageview.setImage(new Image(CaperCloud.class.getResource("res/images/folder.png").toString()));
                            } else {
                                imageview.setImage(new Image(CaperCloud.class.getResource("res/images/file.png").toString()));
                            }
                            box.getChildren().addAll(imageview,fileName); 
                            setGraphic(box);
                        }
                    }
                };
                return cell;
            }
            
        });
        this.tcLocalFilesize.setCellValueFactory(new Callback<CellDataFeatures<File, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(CellDataFeatures<File, String> p) {
                long fSize = p.getValue().length();         
                return new SimpleStringProperty(FileUtils.byteCountToDisplaySize(fSize));
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
        
//local table double click
        tvLocal.setRowFactory(new Callback<TableView<File>, TableRow<File>>() {  
            @Override  
            public TableRow<File> call(TableView<File> tableView2) {  
            final TableRow<File> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() > 1) {
                        System.out.println("double clicked!");
                        TableRow tr = (TableRow) event.getSource();
                        File f = (File) tr.getItem();
                        if (f.isDirectory()) {
                            JobOverviewController.this.homeDirectory = f;
                            JobOverviewController.this.updateLocalFileCache();
                        }
                    }      
                }
            });
            return row;  
            }
        });
        this.cbSwitchAccount.setItems(nickList); 
//list S3bucket object
        this.tvRemote.setItems(this.getRemoteBucketCache());
    }
    
//update localfilecache when homeDirectory is changed
    private void updateLocalFileCache() {
        getLocalFileCache().clear();
        File[] tmp = this.homeDirectory.listFiles();
        if (tmp == null) {
            System.out.println("can not access directory" + this.homeDirectory.getAbsolutePath());
            tfLocalPath.setText(homeDirectory.getAbsolutePath());
            return;
        }
        Arrays.sort(tmp, DirectoryFileComparator.DIRECTORY_COMPARATOR);
        getLocalFileCache().addAll(tmp);
        tfLocalPath.setText(homeDirectory.getAbsolutePath());
    }
    
//get selected file in local table
    private Iterator<File> getSelectedFiles() {
        return tvLocal.getSelectionModel().getSelectedItems().iterator();
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
        this.homeDirectory = folder;
        this.updateLocalFileCache();
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
    
    @FXML
    private void handleTransferPreferenceAction() {
        this.mainApp.showTransferPreferenceView();
    }
    
    @FXML
    private void handleLogoutAction() {
        String currentLogin = (String) cbSwitchAccount.getValue();
        try {
            this.mainApp.getCloudManager().logoutCloud(currentLogin);
        } catch (IllegalCredentialsException ex) {
            ex.printStackTrace();
            return;
        }
        if (currentLogin != null) {
            this.nickList.remove(currentLogin);
        }
        cbSwitchAccount.setValue(null);
    }
    
    @FXML
    private void handelLocalRefreshActon() {
        System.out.println("refresh button clicked");
        updateLocalFileCache();
    }
    
    @FXML
    private void handleLocalUpAction() {
        System.out.println("up button clicked");
        File parentFile = homeDirectory.getParentFile();
        if (parentFile == null) {
            System.out.println("root directory, can not up");
            return;
        }
        this.homeDirectory = parentFile;
        updateLocalFileCache();
    }
    @FXML
    private void handelLocalDeleteAction() {
        Iterator fileIterator = getSelectedFiles();
        if (!fileIterator.hasNext()) {
            System.out.println("no files are selected");
        }
        while(fileIterator.hasNext()) {
            File f = (File) fileIterator.next();
            FileUtils.deleteQuietly(f);
            System.out.println(f.getName() + " has been deleted");
        }
        updateLocalFileCache();
    }
}
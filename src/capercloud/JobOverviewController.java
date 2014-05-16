/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.exception.IllegalCredentialsException;
import capercloud.model.DataTransferTask;
import capercloud.model.DownloadTask;
import capercloud.model.UploadTask;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
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
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.DirectoryFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class JobOverviewController implements Initializable {
    private Log log = LogFactory.getLog(getClass());
    private CaperCloud mainApp;
    private ObservableList<String> nickList;
    private final ObservableList jobTypes = FXCollections.observableArrayList(
            "Novel Protein", 
            "SAP", 
            "AS",
            "Custom Protein Database"
    );
//File Tab    
//data for local TableView
    private ObservableList<File> localFileCache;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private File homeDirectory;
    
//data for remote TableView
    private S3Bucket currentBucket;
    private HashMap<String, S3Object> remoteCacheMap;
    private ObservableList<S3Bucket> remoteBucketCache;
    private ObservableList<S3Object> remoteObjectCache;
    
//data for transfer TableView
    private ObservableList<DataTransferTask> dataTransferTasks;
    
//JobTab
    private ObservableList<S3Object> inputCloudCache;
    
    

    @FXML private TabPane mainTab;  
//File Tab
    @FXML private Button btnManageAccounts;
    @FXML private Button btnTransferPreferences;
    @FXML private Button btnLogout;
    @FXML private Button btnLocalBrowse;
    @FXML private Button btnRemoteUp;
    @FXML private Button btnRemoteNew;
    @FXML private Button btnRemoteRefresh;
    @FXML private Button btnRemoteDelete;
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
    @FXML private TextField tfRemotePath;
    @FXML private TextArea fileLog;
    
//Job Tab
    @FXML private ComboBox cbJobType;
    @FXML private BorderPane bpJobType;
    @FXML private TableView tvInput;
    @FXML private TableView tvFixedModifications;
    @FXML private TableView tvVariableModifications;
    @FXML private TableView tvModifications;
    @FXML private ComboBox cbBucketSelection;
    
//Status Tab
    @FXML private TableView tvJobMonitor;
    @FXML private TableView tvInstanceMonitor;
        
//Result Tab
    @FXML private TableView tvResults;
    
    public JobOverviewController() {
        this.nickList = FXCollections.observableArrayList();
    }

    public ObservableList<File> getLocalFileCache() {  
//lazy init
        if (this.localFileCache == null) {
            this.localFileCache = FXCollections.observableArrayList();
        }
        return this.localFileCache;
    }
    
    public ObservableList<S3Object> getInputCloudCache() {
        if (this.inputCloudCache == null) {
            this.inputCloudCache = FXCollections.observableArrayList();
        }
        return this.inputCloudCache;
    }

    public ObservableList<DataTransferTask> getDataTransferTasks() {
        if (this.dataTransferTasks == null) {
            this.dataTransferTasks = FXCollections.observableArrayList();
        }
        return this.dataTransferTasks;
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
        return this.remoteBucketCache;
    }
    
    public ObservableList<S3Object> getRemoteObjectCache() {
        if (this.remoteObjectCache == null) {
            this.remoteObjectCache = FXCollections.observableArrayList();
        }
        return this.remoteObjectCache;
    }
    
    public Iterator getSelectedObjects() {
// return S3Bucket iterator
        if (this.currentBucket == null) {
            List<S3Bucket> selected = this.tvRemote.getSelectionModel().getSelectedItems();
            if (selected == null) {
                return null;
            }
            return selected.iterator();
        } else {
// return S3Object
            List<S3Object> selected = this.tvRemote.getSelectionModel().getSelectedItems();
            if (selected == null) {
                return null;
            }
            return selected.iterator();
        }
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
//local table double click
        tvLocal.setRowFactory(new Callback<TableView<File>, TableRow<File>>() {  
            @Override  
            public TableRow<File> call(TableView<File> tableView2) {  
            final TableRow<File> row = new TableRow<>();
            row.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() > 1) {
                        TableRow tr = (TableRow) event.getSource();
                        File f = (File) tr.getItem();
                        log.debug("Double click on file " + f.getName());
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
        this.tvLocal.setItems(this.getLocalFileCache());
//remote table init
        this.tfRemotePath.setText("/");
        this.tcRemoteFilename.setCellValueFactory(new Callback<CellDataFeatures, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures p) {
                if (p.getValue() instanceof S3Bucket) {
                    S3Bucket bucket = (S3Bucket) p.getValue();
                    return new SimpleStringProperty(bucket.getName());
                }
                if (p.getValue() instanceof S3Object) {
                    S3Object obj = (S3Object) p.getValue();
                    return new SimpleStringProperty(obj.getName());
                }
                return new SimpleStringProperty("unsupported type");
            }
        });
        this.tcRemoteFilesize.setCellValueFactory(new Callback<CellDataFeatures, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures p) {
                if (p.getValue() instanceof S3Bucket) {
//We will set all S3Bucket size = 0
                    return new SimpleStringProperty("0");
                }
                if (p.getValue() instanceof S3Object) {
                    S3Object obj = (S3Object) p.getValue();
                    return new SimpleStringProperty(FileUtils.byteCountToDisplaySize(obj.getContentLength()));
                }
                return new SimpleStringProperty("unsupported type");
            }
        });
        this.tcRemoteUploadTime.setCellValueFactory(new Callback<CellDataFeatures, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures p) {
                if (p.getValue() instanceof S3Bucket) {
                    S3Bucket bucket = (S3Bucket) p.getValue();
                    Date d = bucket.getCreationDate();
                    return new SimpleStringProperty(sdf.format(d));
                }
                if (p.getValue() instanceof S3Object) {
                    S3Object obj = (S3Object) p.getValue();
                    Date d = obj.getLastModifiedDate();
                    return new SimpleStringProperty(sdf.format(d));
                }
                return new SimpleStringProperty("unsupported type");
            }     
        });
//remote table double click
        tvRemote.setRowFactory(new Callback<TableView, TableRow>() {  
            @Override  
            public TableRow call(TableView tv) {          
                final TableRow row = new TableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getClickCount() > 1) {
                        TableRow tr = (TableRow) event.getSource();
                        if (tr.getItem() instanceof S3Bucket) {
                            S3Bucket bucket = (S3Bucket) tr.getItem();
                            log.debug("double click on bucket " + bucket.getName());
                            JobOverviewController.this.currentBucket = bucket;
//pop up a window and wait until success or canceled
                            JobOverviewController.this.updateRemoteObjectCache(bucket);
                            return;
                        }
                        if (tr.getItem() instanceof S3Object) {
                            S3Object obj = (S3Object) tr.getItem();
                            log.debug("double click on object " + obj.getName());
                            if (obj.isDirectoryPlaceholder()) {
                                return;
                            } else {
                                return;
                            }
                        }
                    }      
                }
            });
            return row;  
            }
        });
        this.currentBucket = null;
        this.tvRemote.setItems(this.getRemoteBucketCache());
        
//init data transfer table
        ((TableColumn) this.tvTransferLog.getColumns().get(0))
                .setCellValueFactory(new Callback<CellDataFeatures<DataTransferTask, String>, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures<DataTransferTask, String> p) {
                return new SimpleStringProperty(p.getValue().getFilename());
            }    
                });
        ((TableColumn) this.tvTransferLog.getColumns().get(1))
                .setCellValueFactory(new Callback<CellDataFeatures<DataTransferTask, String>, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures<DataTransferTask, String> p) {
                return new SimpleStringProperty(p.getValue().getFrom());
            }
                });
        ((TableColumn) this.tvTransferLog.getColumns().get(2))
                .setCellValueFactory(new Callback<CellDataFeatures<DataTransferTask, String>, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures<DataTransferTask, String> p) {
                return new SimpleStringProperty(p.getValue().getTo());
            }
                });
        ((TableColumn) this.tvTransferLog.getColumns().get(3))
                .setCellValueFactory(new Callback<CellDataFeatures<DataTransferTask, String>, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures<DataTransferTask, String> p) {
                return new SimpleStringProperty(p.getValue().getTransferType());
            }
                });
        TableColumn progressCol = (TableColumn) this.tvTransferLog.getColumns().get(4);
        progressCol.setCellValueFactory(new PropertyValueFactory<DataTransferTask, Double>("progress"));
        progressCol.setCellFactory(ProgressBarTableCell.<DataTransferTask> forTableColumn());
        ((TableColumn) this.tvTransferLog.getColumns().get(5))
                .setCellValueFactory(new PropertyValueFactory<DataTransferTask, String>("message"));
        this.tvTransferLog.setItems(this.getDataTransferTasks());
        
//nickname combobox       
        this.cbSwitchAccount.setItems(nickList); 
                
//bucket selection combobox
        this.cbBucketSelection.setCellFactory(new Callback<ListView<S3Bucket>, ListCell<S3Bucket>>() {
            @Override
            public ListCell<S3Bucket> call(ListView<S3Bucket> p) {
                return new ListCell<S3Bucket>() {
                    @Override
                    protected void updateItem(S3Bucket t, boolean bln) {
                        super.updateItem(t, bln); 
                        if (t != null) {
                            setText(t.getName());
                        } else {
                            setText(null);
                        }
                    }
                };
            }
        });
        this.cbBucketSelection.setConverter(new StringConverter<S3Bucket>() {
            @Override
            public String toString(S3Bucket bucket) {
                if (bucket == null) {
                    return null;
                } else {
                    return bucket.getName();
                }
            }

            @Override
            public S3Bucket fromString(String bucketName) {
                if (bucketName == null) {
                    return null;
                } else {
                    return new S3Bucket(bucketName);
                }
            }
        });
        this.cbBucketSelection.valueProperty().addListener(new ChangeListener<S3Bucket>() {
            @Override
            public void changed(ObservableValue<? extends S3Bucket> ov, S3Bucket t, S3Bucket t1) {
                JobOverviewController.this.updateInputCloudCache(t1);
            }
        });
        this.cbBucketSelection.setItems(this.getRemoteBucketCache());
        
//init input TableView
        TableColumn checkOnInputCol = (TableColumn) this.tvInput.getColumns().get(0);
        checkOnInputCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkOnInputCol));
        
        ((TableColumn) this.tvInput.getColumns().get(1))
                .setCellValueFactory(new Callback<CellDataFeatures<S3Object, String>, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures<S3Object, String> p) {
                return new SimpleStringProperty(p.getValue().getName());
            }
                });
        this.tvInput.setItems(getInputCloudCache());
        
    }
    
    public void enableButton() {
        this.btnRemoteUp.setDisable(false);
        this.btnRemoteNew.setDisable(false);
        this.btnRemoteRefresh.setDisable(false);
        this.btnRemoteDelete.setDisable(false);
    }
    
//update localfilecache when homeDirectory is changed
    private void updateLocalFileCache() {
        getLocalFileCache().clear();
        File[] tmp = this.homeDirectory.listFiles();
        if (tmp == null) {
            log.info("can not access directory" + this.homeDirectory.getAbsolutePath());
            tfLocalPath.setText(homeDirectory.getAbsolutePath());
            return;
        }
        Arrays.sort(tmp, DirectoryFileComparator.DIRECTORY_COMPARATOR);
        getLocalFileCache().addAll(tmp);
        tfLocalPath.setText(homeDirectory.getAbsolutePath());
    }
//update remote objects cache
    private void updateRemoteObjectCache(S3Bucket bucket) {
        String message = "Listing Objects";
        Stage dialog = this.mainApp.createProgressDialog(bucket.getName(), message, this.mainApp.getPrimaryStage());
        Service<S3Object[]> s = this.mainApp.getCloudManager().createListObjectsService(bucket, dialog);
        s.start();
        
        dialog.showAndWait();
//stuck until user click cancel button
        if (State.SUCCEEDED == s.getState()) {
            S3Object[] res = s.getValue();
            getRemoteObjectCache().clear();
            getRemoteObjectCache().addAll(res);
            tfRemotePath.setText("/" + currentBucket.getName());
            tvRemote.setItems(getRemoteObjectCache());
        } else {
            if (State.CANCELLED != s.getState()) {
                log.debug(s.getState());
                s.cancel();
                log.debug(s.getState());
            }
        }
    }
    
    private void updateInputCloudCache(S3Bucket bucket) {
        String message = "Listing Objects";
        Stage dialog = this.mainApp.createProgressDialog(bucket.getName(), message, this.mainApp.getPrimaryStage());
        Service<S3Object[]> s = this.mainApp.getCloudManager().createListObjectsService(bucket, dialog);
        s.start();
        
        dialog.showAndWait();
//stuck until user click cancel button
        if (State.SUCCEEDED == s.getState()) {
            S3Object[] res = s.getValue();
            getInputCloudCache().clear();
            getInputCloudCache().addAll(res);
        } else {
            if (State.CANCELLED != s.getState()) {
                log.debug(s.getState());
                s.cancel();
                log.debug(s.getState());
            }
        }
    }
    
    private void updateRemoteBucketCache() {
        String message = "Listing Buckets";
        Stage dialog = this.mainApp.createProgressDialog("Updating", message, this.mainApp.getPrimaryStage());
        Service<S3Bucket[]> s = this.mainApp.getCloudManager().createListBucketsService(dialog);  
        s.start();
        
        dialog.showAndWait();
        
        if (State.SUCCEEDED == s.getState()) {
            this.getRemoteBucketCache().clear();
            this.getRemoteBucketCache().addAll(s.getValue());
            this.tvRemote.setItems(this.getRemoteBucketCache());
        } else {
            if (State.CANCELLED != s.getState()) {
                log.debug(s.getState());
                s.cancel();
                log.debug(s.getState());
            }
        }
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
// do nothing if no files are selected
        if (selectedFiles.isEmpty()) {
            log.info("No files are selected");
            return;
        }
        
        if (currentBucket == null) {
            log.info("No bucket is selected");
            return;
        }
        
        List<DataTransferTask> newTasks = new ArrayList<>();
//create transfer task
        for (File f : selectedFiles) {
            newTasks.add(
                new UploadTask(f, currentBucket, this.mainApp.getCloudManager().getCurrentCredentials()));
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(newTasks.size(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        for (Iterator it = newTasks.iterator(); it.hasNext();) {
            executor.execute((DataTransferTask) it.next());
        }
        this.getDataTransferTasks().addAll(newTasks);
        //this.updateRemoteObjectCache();
    }
    @FXML
    private void handleDownloadAction() {
        ObservableList<S3Object> selectedObjects = this.tvRemote.getSelectionModel().getSelectedItems();
        log.debug("Selected " + selectedObjects.get(0).getName());
// do nothing if no files are selected
        if (selectedObjects.isEmpty()) {
            log.info("No s3objects are selected");
            return;
        }
        if (homeDirectory == null) {
            log.info("No download directory is selected");
            return;
        }
        
        List<DataTransferTask> newTasks = new ArrayList<>();
//create transfer task
        for (S3Object obj : selectedObjects) {
            newTasks.add(
                new DownloadTask(obj, homeDirectory, this.mainApp.getCloudManager().getCurrentCredentials()));
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(newTasks.size(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        for (Iterator it = newTasks.iterator(); it.hasNext();) {
            executor.execute((DataTransferTask) it.next());
        }
        this.getDataTransferTasks().addAll(newTasks);
        //this.updateRemoteObjectCache();
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
//clear 
        cbSwitchAccount.setValue(null);
        this.currentBucket = null;
        getRemoteBucketCache().clear();
        getRemoteObjectCache().clear();
    }
    
    @FXML
    private void handelLocalRefreshActon() {
        updateLocalFileCache();
    }
    
    @FXML
    private void handleLocalUpAction() {
        File parentFile = homeDirectory.getParentFile();
        if (parentFile == null) {
            log.info("Root Directory");
            return;
        }
        this.homeDirectory = parentFile;
        updateLocalFileCache();
    }
    @FXML
    private void handelLocalDeleteAction() {
        Iterator fileIterator = getSelectedFiles();
        if (!fileIterator.hasNext()) {
            log.error("no files are selected");
        }
        while(fileIterator.hasNext()) {
            File f = (File) fileIterator.next();
            FileUtils.deleteQuietly(f);
            log.info(f.getName() + " has been deleted");
        }
        updateLocalFileCache();
    }
    @FXML 
    private void handleRemoteRefreshAction() {
        if (this.currentBucket != null) {
            this.updateRemoteObjectCache(this.currentBucket);
            return;
        }
        if (this.currentBucket == null) {
            this.updateRemoteBucketCache();
            return;
        }
    }
    
//simply up to root, display cached bucket
    @FXML
    private void handleRemoteUpAction() {
        if (this.currentBucket == null) {
            return;
        }
        this.currentBucket = null;
        this.tfRemotePath.setText("/");
        this.tvRemote.setItems(getRemoteBucketCache());
    }
//only support create bucket    
    @FXML
    private void handleRemoteCreateAction() {
        this.mainApp.showTextFieldDialog();
    }
    
    @FXML
    private void handleRemoteDeleteAction() {
        Object obj = this.tvRemote.getSelectionModel().getSelectedItem();
        if (obj instanceof S3Bucket) {
            S3Bucket tmp = (S3Bucket) obj;
            String msg = "Delete Bucket " + tmp.getName();
            Stage stage = this.mainApp.createProgressDialog("Delete", msg, this.mainApp.getPrimaryStage());
            Service<Void> s = this.mainApp.getCloudManager().createDeleteBucketService((S3Bucket) obj, stage);
            log.debug(s.getState());
            s.start();
            
            stage.showAndWait();
            
            if (State.SUCCEEDED == s.getState()) {
                log.debug(this.getRemoteBucketCache().size());
                this.getRemoteBucketCache().remove(tmp);
                log.debug(this.getRemoteBucketCache().size());
            } else {
                log.debug(s.getState());
            }
            return;
        }
        
        if (obj instanceof S3Object) {
            S3Object tmp = (S3Object) obj;
            String msg = "Delete Object " + tmp.getName();
            Stage stage = this.mainApp.createProgressDialog("Delete", msg, this.mainApp.getPrimaryStage());
            Service<Void> s = this.mainApp.getCloudManager().createDeleteObjectService((S3Object) obj, stage);
            log.debug(s.getState());
            s.start();
            
            stage.showAndWait();
            
            if (State.SUCCEEDED == s.getState()) {
                log.debug(this.getRemoteObjectCache().size());
                this.getRemoteObjectCache().remove(tmp);
                log.debug(this.getRemoteObjectCache().size());
            } else {
                log.debug(s.getState());
            }
            return;
        }
    }
}
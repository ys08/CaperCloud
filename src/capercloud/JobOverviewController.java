/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.log.TextAreaAppender;
import capercloud.model.CloudJob;
import capercloud.model.ClusterConfigs;
import capercloud.model.DataTransferTask;
import capercloud.model.DownloadTask;
import capercloud.model.FileModel;
import capercloud.model.InputObjectModel;
import capercloud.model.InstanceModel;
import capercloud.model.JobModel;
import capercloud.model.ResultModel;
import capercloud.model.StatusModel;
import capercloud.model.UploadTask;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SearchParameters.MassAccuracyType;
import com.compomics.util.experiment.identification.identification_parameters.XtandemParameters;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FilePart;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.DirectoryFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import uk.ac.ebi.pride.tools.jmzreader.JMzReaderException;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class JobOverviewController implements Initializable {
    private Log log = LogFactory.getLog(getClass());
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private CaperCloud mainApp;
    private TypeOneController t1c;
    private TypeTwoController t2c;
    private TypeThreeController t3c;
    private TypeFourController t4c;
    
    private FileModel fm;
    private JobModel jm;
    private StatusModel sm;
    private ResultModel rm;

//File Tab
    @FXML private Button btnLogout;
    @FXML private Button btnLocalBrowse;
    @FXML private Button btnRemoteUp;
    @FXML private Button btnRemoteNew;
    @FXML private Button btnRemoteRefresh;
    @FXML private Button btnRemoteDelete;
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
    @FXML private Label username;
    
//Job Tab
    @FXML private ComboBox cbInstanceType;
    @FXML private ComboBox cbJobType;
    @FXML private BorderPane bpJobType;
    @FXML private TableView tvInput;
    @FXML private ComboBox cbBucketSelection;
    @FXML private TextArea taLog;
    @FXML private ComboBox cbCleavageSites;
    @FXML private CheckBox cbSemiCleavage;
    @FXML private TextField tfFragmentMassError;
    @FXML private ComboBox cbFragmentMassType;
    @FXML private ComboBox cbRefinementExpect;
    @FXML private TextField tfNumOfInstances;
    @FXML public static TextField tfSelectedNumOfInputSpectra;
    
//Status Tab
    @FXML private TableView tvJobMonitor;
    @FXML private TableView tvInstanceMonitor;
        
//Result Tab
    @FXML private TableView tvResults;
    
    public JobOverviewController() {
        this.jm = new JobModel();
        this.fm = new FileModel();
        this.sm = new StatusModel();
// testingggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg
        ResultModel rm = new ResultModel(new File("example_files/55merge_omssa.mzid"),new File("example_files/55merge.mgf"));
        rm.drawSpectrum();
    }

    public void setUsername(String username) {
        this.username.setText(username);
    }

    public Button getBtnLogout() {
        return btnLogout;
    }
    
    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;   
    }

    public FileModel getFm() {
        return fm;
    }

    public JobModel getJm() {
        return jm;
    }

    public StatusModel getSm() {
        return sm;
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.cbJobType.setItems(jm.getJobTypes());
          
//init local table data
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
                        if (f.isDirectory()) {
                            JobOverviewController.this.updateLocalFileCache(f);
                        }
                    }      
                }
            });
            return row;  
            }
        });
        this.tvLocal.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        this.updateLocalFileCache(new File(System.getProperty("user.home")));
        this.tvLocal.setItems(this.fm.getLocalCachedFileList());
        
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
//pop up a window and wait until success or canceled
                            JobOverviewController.this.runListingObjectsService(bucket);
                            return;
                        }
                        if (tr.getItem() instanceof S3Object) {
                            S3Object obj = (S3Object) tr.getItem();
                            log.debug("double click on object " + obj.getName());
                            if (obj.isDirectoryPlaceholder()) {
                                //TO DO
                            } else {
                                //TO DO
                            }
                        }
                    }      
                }
            });
            return row;  
            }
        });
        this.tvRemote.setItems(this.fm.getRemoteCachedBucketList());
        
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
        this.tvTransferLog.setItems(this.fm.getDataTransferTaskList());
                
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
                JobOverviewController.this.runListingInputObjectModelsService(t1);
                JobOverviewController.this.jm.setCurrentBucket(t1);
            }
        });
        this.cbBucketSelection.setItems(this.fm.getRemoteCachedBucketList());
        
//init cloud input TableView
        TableColumn<InputObjectModel, Boolean> checkOnInputCol = (TableColumn) this.tvInput.getColumns().get(0);
        checkOnInputCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkOnInputCol));
        checkOnInputCol.setCellValueFactory(new PropertyValueFactory<InputObjectModel, Boolean>("selected"));

        ((TableColumn) this.tvInput.getColumns().get(1))
                .setCellValueFactory(new Callback<CellDataFeatures<InputObjectModel, String>, ObservableValue>() {
            @Override
            public ObservableValue call(CellDataFeatures<InputObjectModel, String> p) {
                return new SimpleStringProperty(p.getValue().getName());
            }
                });
        this.tvInput.setItems(this.jm.getCachedInputModels());  
        
//init instance monitor TableView
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(0))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("instanceId"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(1))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("imageId"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(2))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("stateName"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(3))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("instanceType"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(4))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("platform"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(5))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("architecture"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(6))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("rootDevice"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(7))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("keyName"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(8))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("launchTime"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(9))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("availabilityZone"));
        ((TableColumn) this.tvInstanceMonitor.getColumns().get(10))
                .setCellValueFactory(new PropertyValueFactory<InstanceModel, String>("blockDevice"));
        this.tvInstanceMonitor.setItems(this.sm.getInstancesCache());
        
//init instance types ComboBox
        this.cbInstanceType.setItems(this.jm.getInstanceTypes());
//        
        TextAreaAppender.setTextArea(taLog);
        
//
        this.cbCleavageSites.setItems(this.jm.getCleavageSites());
//
        this.cbFragmentMassType.setItems(this.jm.getMassAccuracyTypes());
        this.cbFragmentMassType.getSelectionModel().selectFirst();
        
//
        this.cbRefinementExpect.setItems(this.jm.getRefinementExpects());
        this.cbRefinementExpect.getSelectionModel().select(2);
    }
    
    public void enableButton() {
        this.btnRemoteUp.setDisable(false);
        this.btnRemoteNew.setDisable(false);
        this.btnRemoteRefresh.setDisable(false);
        this.btnRemoteDelete.setDisable(false);
    }
    
    //update localfilecache when homeDirectory is changed
    private void updateLocalFileCache(File inDirectory) {
        File[] tmp = inDirectory.listFiles();
        if (tmp == null) {
            log.info("can not access directory");
            return;
        }
        Arrays.sort(tmp, DirectoryFileComparator.DIRECTORY_COMPARATOR);
        this.fm.setLocalCachedFileList(tmp);
        this.fm.setFolderPath(inDirectory);
        tfLocalPath.setText(inDirectory.getAbsolutePath());
    }
    
//update remote objects cache
    private void runListingObjectsService(S3Bucket bucket) {
        String message = "Listing " + bucket.getName();
        Stage dialog = this.mainApp.createStripedProgressDialog(message, this.mainApp.getPrimaryStage());
        Service<S3Object[]> s = this.mainApp.getCloudManager().createListObjectsService(bucket, dialog);
        s.start();
        
        dialog.showAndWait();
//stuck until user click cancel button
        if (State.SUCCEEDED == s.getState()) {
            S3Object[] res = s.getValue();
            this.fm.setRemoteCachedObjectList(res);
            this.fm.setBucketPath(bucket);
            tfRemotePath.setText("/" + bucket.getName());
            tvRemote.setItems(this.fm.getRemoteCachedObjectList());
        } else {
            if (State.CANCELLED != s.getState()) {
                log.debug(s.getState());
                s.cancel();
                log.debug(s.getState());
            }
        }
    }
    
    private void runListingInputObjectModelsService(S3Bucket bucket) {
        String message = "Listing Bucket " + bucket.getName();
        Stage dialog = this.mainApp.createStripedProgressDialog(message, this.mainApp.getPrimaryStage());
        Service<S3Object[]> s = this.mainApp.getCloudManager().createListObjectsService(bucket, dialog);
        s.start();
        
        dialog.showAndWait();
//stuck until user click cancel button
        if (State.SUCCEEDED == s.getState()) {
            S3Object[] res = s.getValue();
            this.jm.setCachedInputModels(res);
        } else {
            if (State.CANCELLED != s.getState()) {
                log.debug(s.getState());
                s.cancel();
                log.debug(s.getState());
            }
        }
    }
    
    private void runListingBucketsService() {
        String message = "Listing Buckets";
        Stage dialog = this.mainApp.createStripedProgressDialog(message, this.mainApp.getPrimaryStage());
        Service<S3Bucket[]> s = this.mainApp.getCloudManager().createListBucketsService(dialog);  
        s.start();
        
        dialog.showAndWait();
        
        if (State.SUCCEEDED == s.getState()) {
            this.fm.setRemoteCachedBucketList(s.getValue());
            this.tvRemote.setItems(this.fm.getRemoteCachedBucketList());
        } else {
            if (State.CANCELLED != s.getState()) {
                log.debug(s.getState());
                s.cancel();
                log.debug(s.getState());
            }
        }
    }

////instance
//    public void updateInstancesCache() {
//        final AmazonEC2AsyncClient ec2m = this.mainApp.getCloudManager().getEc2Manager();
//        ec2m.describeInstancesAsync(new DescribeInstancesRequest(), new AsyncHandler<DescribeInstancesRequest,DescribeInstancesResult>() {
//            @Override
//            public void onError(Exception excptn) {
//                log.debug(excptn.getMessage());
//            }
//
//            @Override
//            public void onSuccess(DescribeInstancesRequest rqst, DescribeInstancesResult result) {
//                JobOverviewController.this.getInstancesCache().clear();
//                for (Reservation r : result.getReservations()) {
//                    for (Instance i : r.getInstances()) {
//                        InstanceModel im = new InstanceModel(i);
//                        JobOverviewController.this.getInstancesCache().add(im);
//                        JobOverviewController.this.getInstancesMap().put(i.getInstanceId(), im);
//                    }
//                }
//            }
//        });
//    }
    
//get selected file in local table
    private Iterator<File> getSelectedFiles() {
        return tvLocal.getSelectionModel().getSelectedItems().iterator();
    }
    
    public S3Object getSelectedObject() {
        InputObjectModel ictm = (InputObjectModel) this.tvInput.getSelectionModel().getSelectedItem();
        if (ictm == null) {
            return null;
        }
        return ictm.getObj();
    }
    
    private List<S3Object> getSelectedSpectra() {
        List<S3Object> objs = new ArrayList<>();
        for (InputObjectModel i : this.jm.getCachedInputModels()) {
            if (i.selectedProperty().getValue()) {
                objs.add(i.getObj());
            }
        }
        return objs;
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
        this.updateLocalFileCache(folder);
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
                this.t4c = loader.getController();
                this.t4c.setMainApp(mainApp);
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
            log.warn("No files are selected");
            return;
        }
        
        if (this.fm.getBucketPath() == null) {
            log.warn("No bucket is selected");
            return;
        }
        
//create transfer task
        for (File f : selectedFiles) {
            final DataTransferTask task = new UploadTask(f, this.fm.getBucketPath(), this.mainApp.getCloudManager().getCurrentCredentials());
            task.setOnCancelled(new EventHandler() {
                @Override
                public void handle(Event t) {
                    task.updateMessage("Canceled");
                    task.updateProgress(0, 1);
                }
            });
            this.fm.addDataTransferTask(task);
        }

        ExecutorService executor = Executors.newFixedThreadPool(this.fm.getDataTransferTaskList().size(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
        for (Iterator it = this.fm.getDataTransferTaskList().iterator(); it.hasNext();) {
            executor.execute((DataTransferTask) it.next());
        }
    }
    @FXML
    private void handleDownloadAction() {
        ObservableList<S3Object> selectedObjects = this.tvRemote.getSelectionModel().getSelectedItems();
        log.debug("Selected " + selectedObjects.get(0).getName());
// do nothing if no files are selected
        if (selectedObjects.isEmpty()) {
            log.warn("No s3objects are selected");
            return;
        }
        if (this.fm.getFolderPath() == null) {
            log.warn("No download directory is selected");
            return;
        }
        
//create transfer task
        for (S3Object obj : selectedObjects) {
            this.fm.addDataTransferTask(
                new DownloadTask(obj, this.fm.getFolderPath(), this.mainApp.getCloudManager().getCurrentCredentials()));
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(this.fm.getDataTransferTaskList().size(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });

        for (Iterator it = this.fm.getDataTransferTaskList().iterator(); it.hasNext();) {
            executor.execute((DataTransferTask) it.next());
        }
    }
    
    @FXML
    private void handleTransferPreferenceAction() {
        this.mainApp.showTransferPreferenceView();
    }
    
    @FXML
    private void handleLogoutAction() {
        this.mainApp.getCloudManager().logoutCloud();
//clear 
        this.fm.clear();
    }
    
    @FXML
    private void handelLocalRefreshActon() {
        this.updateLocalFileCache(this.fm.getFolderPath());
    }
    
    @FXML
    private void handleLocalUpAction() {
        File parentFile = this.fm.getFolderPath().getParentFile();
        if (parentFile == null) {
            log.info("Root Directory");
            return;
        }
        this.fm.setFolderPath(parentFile);
        this.updateLocalFileCache(parentFile);
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
        this.updateLocalFileCache(this.fm.getFolderPath());
    }
    
    @FXML 
    private void handleRemoteRefreshAction() {
        if (this.fm.getBucketPath() != null) {
            this.runListingObjectsService(this.fm.getBucketPath());
            return;
        }
        if (this.fm.getBucketPath() == null) {
            this.runListingBucketsService();
        }
    }
    
//simply up to root, display cached bucket
    @FXML
    private void handleRemoteUpAction() {
        if (this.fm.getBucketPath() == null) {
            return;
        }
        this.fm.setBucketPath(null);
        this.tfRemotePath.setText("/");
        this.tvRemote.setItems(this.fm.getRemoteCachedBucketList());
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

            Stage confirmDialog = this.mainApp.createModalConfirmDialog(msg + "?", this.mainApp.getPrimaryStage());
            confirmDialog.showAndWait();
            if (this.mainApp.isCanceled()) {
                return;
            }
            this.mainApp.resetCanceled();            
            Stage progressDialog = this.mainApp.createStripedProgressDialog(msg, this.mainApp.getPrimaryStage());
            Service<Void> s = this.mainApp.getCloudManager().createDeleteBucketService((S3Bucket) obj, progressDialog);

            s.start();
            progressDialog.showAndWait();
            
            if (State.SUCCEEDED == s.getState()) {
                log.debug(this.fm.getRemoteCachedBucketList().size());
                this.fm.deleteBucket(tmp);
                log.debug(this.fm.getRemoteCachedBucketList().size());
            } else {
                log.debug(s.getState());
                if (State.CANCELLED != s.getState()) {
                    s.cancel();
                }
            }
            return;
        }
        
        if (obj instanceof S3Object) {
            S3Object tmp = (S3Object) obj;
            String msg = "Delete Object " + tmp.getName();
            Stage confirmDialog = this.mainApp.createModalConfirmDialog(msg + "?", this.mainApp.getPrimaryStage());
            confirmDialog.showAndWait();
            if (this.mainApp.isCanceled()) {
                return;
            }
            this.mainApp.resetCanceled();
            
            Stage progressDialog = this.mainApp.createStripedProgressDialog(msg, this.mainApp.getPrimaryStage());
            Service<Void> s = this.mainApp.getCloudManager().createDeleteObjectService((S3Object) obj, progressDialog);
            s.start();
            progressDialog.showAndWait();
            
            if (State.SUCCEEDED == s.getState()) {
                log.debug(this.fm.getRemoteCachedObjectList().size());
                this.fm.deleteObject(tmp);
                log.debug(this.fm.getRemoteCachedObjectList().size());
            } else {
                log.debug(s.getState());
                if (State.CANCELLED != s.getState()) {
                    s.cancel();
                }
            }
        }
    }
    
//Job tab event
    @FXML
    private void handleRunJobAction() {
//        for (InputObjectModel i : this.getInputObjectCache()) {
//            log.debug(i.selectedProperty().getValue());
//        }
    }
    
    @FXML
    private void handelLaunchInstances() {
//for free test
        CreateKeyPairRequest ckprqt = new CreateKeyPairRequest("capercloud");
        CreateKeyPairResult ckpres = this.mainApp.getCloudManager().getEc2Manager().createKeyPair(ckprqt);
        
        RunInstancesRequest rirqt = new RunInstancesRequest("ami-018c9568", 1, 1);
        rirqt.setInstanceType(InstanceType.T1Micro);
        rirqt.setKeyName(ckpres.getKeyPair().getKeyName());
        
        this.mainApp.getCloudManager().getEc2Manager().runInstancesAsync(rirqt, new AsyncHandler<RunInstancesRequest,RunInstancesResult>() {
            @Override
            public void onError(Exception excptn) {
                log.error(excptn.getMessage());
            }
            @Override
            public void onSuccess(RunInstancesRequest rqst, RunInstancesResult result) {
                for (Instance i : result.getReservation().getInstances()) {
                    log.debug(i);
                    InstanceModel im = new InstanceModel(i);
                    JobOverviewController.this.sm.addInstance(im);
                }
            }
        });
    }
    
    @FXML
    private void handleInstanceMonitorRefreshAction() {
        //TO DO
    }
//    
    @FXML
    private void handleInstanceMonitorStopAction() {
//        List<String> instanceIds = new ArrayList<>();
//        InstanceModel selectedInstance = (InstanceModel) this.tvInstanceMonitor.getSelectionModel().getSelectedItem();
//        if (selectedInstance == null) {
//            return;
//        }
//        instanceIds.add(selectedInstance.instanceIdProperty().getValue());
//        
//        final AmazonEC2AsyncClient ec2m = this.mainApp.getCloudManager().getEc2Manager();
//        ec2m.stopInstancesAsync(new StopInstancesRequest(instanceIds), new  AsyncHandler<StopInstancesRequest,StopInstancesResult>() {
//            @Override
//            public void onError(Exception excptn) {
//                log.debug(excptn.getMessage());
//            }
//
//            @Override
//            public void onSuccess(StopInstancesRequest rqst, StopInstancesResult result) {
//                List<InstanceStateChange> res = result.getStoppingInstances();
//                for (InstanceStateChange isc : res) {
//                    InstanceModel im = JobOverviewController.this.getInstancesMap().get(isc.getInstanceId());
//                    im.setState(isc.getCurrentState().getName());
//                }
//            }
//        });
    }
    
    @FXML
    private void handleInstanceMonitorTerminateAction() {
//        List<String> instanceIds = new ArrayList<>();
//        InstanceModel selectedInstance = (InstanceModel) this.tvInstanceMonitor.getSelectionModel().getSelectedItem();
//        if (selectedInstance == null) {
//            return;
//        }
//        instanceIds.add(selectedInstance.instanceIdProperty().getValue());
//        final AmazonEC2AsyncClient ec2m = this.mainApp.getCloudManager().getEc2Manager();
//        ec2m.terminateInstancesAsync(new TerminateInstancesRequest(instanceIds), new AsyncHandler<TerminateInstancesRequest,TerminateInstancesResult>() {
//            @Override
//            public void onError(Exception excptn) {
//                log.error(excptn.getMessage());
//            }
//
//            @Override
//            public void onSuccess(TerminateInstancesRequest rqst, TerminateInstancesResult result) {
//                List<InstanceStateChange> res = result.getTerminatingInstances();
//                for (InstanceStateChange isc : res) {
//                    InstanceModel im = JobOverviewController.this.getInstancesMap().get(isc.getInstanceId());
//                    im.setState(isc.getCurrentState().getName());
//                }
//            }
//        });
    }
    
    @FXML
    private void handleInstanceMonitorRebootAction() {
        
    }
    
    @FXML
    private void handleDataTransferCancelAction() {
        DataTransferTask task = (DataTransferTask) this.tvTransferLog.getSelectionModel().getSelectedItem();
        if (task == null) {
            return;
        }
        task.cancel();
    }
    
    @FXML
    private void handleInputCloudRefreshAction() {
        S3Bucket bucket = (S3Bucket) this.cbBucketSelection.getValue();
        this.runListingInputObjectModelsService(bucket);
    }
    
    @FXML
    private void handleJobSaveAction() {
        int jobType = this.cbJobType.getSelectionModel().getSelectedIndex() + 1;
        //S3Bucket saveToBucket = (S3Bucket) this.cbBucketSelection.getValue();
        String sep = IOUtils.LINE_SEPARATOR;
        if (jobType == 0) {
            return;
        }

        List<S3Object> selectedSpectra = this.getSelectedSpectra();
        if (selectedSpectra.isEmpty()) {
            log.warn("Please select one or more spectra");
            return;
        }

        SearchParameters sp = new SearchParameters();
        XtandemParameters xp = new XtandemParameters();
        sp.setIdentificationAlgorithmParameter(1, xp);
        String enzymeName = (String) this.cbCleavageSites.getValue();
        Enzyme e = this.jm.getEnzymeFactory().getEnzyme(enzymeName);
        e.setSemiSpecific(this.cbSemiCleavage.isSelected());
        sp.setEnzyme(e);
        String fragmentError = this.tfFragmentMassError.getText();
        sp.setFragmentIonAccuracy(Double.parseDouble(fragmentError));
        sp.setFragmentAccuracyType((MassAccuracyType) this.cbFragmentMassType.getValue());
        String refinementExpect = (String) this.cbRefinementExpect.getValue();
        xp.setMaximumExpectationValueRefinement(Double.parseDouble(refinementExpect));
        
        int num = Integer.parseInt(this.tfNumOfInstances.getText());
        InstanceType it = (InstanceType) this.cbInstanceType.getSelectionModel().getSelectedItem();
        ClusterConfigs cc = new ClusterConfigs("ami-b08b6cd8", num, num, it);
        CloudJob cj = new CloudJob(this.mainApp, selectedSpectra, sp, cc, jobType);
        if (jobType == CaperCloud.CUSTOM_DB) {
            cj.setT4c(this.t4c);
        }
        try {
            cj.saveToLocal();
        } catch (NoSuchAlgorithmException ex) {
            log.error(ex.getMessage());
            return;
        } catch (IOException ex) {
            Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.sm.addJob(cj);
        log.info("add job to job list: " + cj.getCloudJobId());
    }
    
    @FXML
    private void handleRunCloudJobAction() throws UnsupportedEncodingException, IOException, InterruptedException {
//        cj.launchMasterNode();
        if (this.sm.getJobs().isEmpty()) {
            log.info("Please save parameters before submit job");
            return;
        }
        
        CloudJob job = this.sm.getJobs().get(this.sm.getJobs().size() - 1);
        try {
            sendFiles(job).get();
        } catch (ExecutionException ex) {
            log.error("sth went wrong");
            return;
        }
        postJob(job);
    }
    
    private Future sendFiles(CloudJob job) throws IOException {
        String url = "http://127.0.0.1:5000/";
        final AsyncHttpClient client = new AsyncHttpClient();
        List<File> toBeUploaded = job.getInputFiles();
        toBeUploaded.add(job.getTaxonomyFile());
        RequestBuilder rb = new RequestBuilder("POST")
                .setUrl(url)
                .setHeader("Content-Type", "multipart/form-data");

        for (File f : toBeUploaded) {
            rb.addBodyPart(new FilePart(f.getName(), f, "text/plain", "UTF-8"));
        }
        
        return client.prepareRequest(rb.build()).execute(new AsyncCompletionHandler<Response>() {

            @Override
            public Response onCompleted(Response r) throws Exception {
                client.close();
                return r;
            }
            
        });
    }
    
    private void postJob(CloudJob job) throws IOException {
        List<File> input_xmls = job.getInputFiles();
        String url = "http://127.0.0.1:5000/job4";
        final AsyncHttpClient client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setIdleConnectionTimeoutInMs(10000000).setConnectionTimeoutInMs(10000000).setRequestTimeoutInMs(10000000).build());
        RequestBuilder rb = new RequestBuilder("POST")
            .setUrl(url)
            .addParameter("input_xml", input_xmls.get(0).getName())
            .addParameter("access_key", this.mainApp.getCloudManager().getCurrentCredentials().getAccessKey())
            .addParameter("secret_key", this.mainApp.getCloudManager().getCurrentCredentials().getSecretKey())
            .addParameter("bucket_name", this.jm.getCurrentBucket().getName());
        
        StringBuilder sb = new StringBuilder();
        List<S3Object> objs = job.getSpectrumObjs();
        objs.add(job.getDatabaseObj());
        Iterator it = objs.iterator();
        sb.append(((S3Object) it.next()).getName());
        while(it.hasNext()) {
            sb.append("," + ((S3Object) it.next()).getName());
        }
        
        rb.addParameter("key_names", sb.toString());
        
        AsyncCompletionHandler<Response> handler = new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response r) throws Exception {
                client.close();
                return r;
            }
            @Override
            public void onThrowable(Throwable t) {
                log.debug("---------");
            }
        };
        Future f = null;
        f = client.prepareRequest(rb.build()).execute(handler);
        
//        try {
//            f.get();
//        } catch (InterruptedException ex) {
//            log.error(ex.getMessage());
//        } catch (ExecutionException ex) {
//            log.error(ex.getMessage());
//        }
        
//some error happened, retry
//        while(!client.isClosed()) {
//            try {
//                Thread.sleep(3000);
//                f = client.prepareRequest(rb.build()).execute(handler);
//                f.get();
//            } catch (InterruptedException ex) {
//                log.error(ex.getMessage());
//            } catch (ExecutionException ex) {
//                log.error(ex.getMessage());
//            }
//        }
    }
}

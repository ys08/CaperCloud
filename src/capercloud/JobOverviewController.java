/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.log.TextAreaAppender;
import capercloud.model.CloudJob;
import capercloud.model.DataTransferTask;
import capercloud.model.DownloadTask;
import capercloud.model.FileModel;
import capercloud.model.InputObjectModel;
import capercloud.model.InstanceModel;
import capercloud.model.JobModel;
import capercloud.model.ModificationTableModel;
import capercloud.model.PeptideModel;
import capercloud.model.ResultModel;
import capercloud.model.SpectrumModel;
import capercloud.model.StatusModel;
import capercloud.model.UploadTask;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SearchParameters.MassAccuracyType;
import com.compomics.util.experiment.identification.identification_parameters.XtandemParameters;
import com.compomics.util.preferences.ModificationProfile;
import impl.org.controlsfx.i18n.Localization;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
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
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.DirectoryFileComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
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
    @FXML private TextField tfSelectedNumOfInputSpectra;
    @FXML private TableView tvModification;
    @FXML private TextField tfKeyName;
    @FXML private TextField tfImageId;
    @FXML private TextField tfSecurityGroup;
    @FXML private TextField tfOutputBucketName;
    
//Status Tab
    @FXML private TableView tvJobMonitor;
    @FXML private TableView tvInstanceMonitor;
        
//Result Tab
    @FXML private TableView tvResults;
    @FXML private AnchorPane apSpectrum;
    @FXML private WebView wvBrowser;
    @FXML private TableView tvPSMs;
    @FXML private TextField tfSpectraFile;
    @FXML private TextField tfBedUrl;
    
    public JobOverviewController() {
        //set dialog locale
        Localization.setLocale(new Locale("en", "US"));
        this.jm = new JobModel();

        this.fm = new FileModel();
        this.sm = new StatusModel();
        this.rm = new ResultModel();
//        log.debug("mainApp: " + this.mainApp);
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
    
    public TextField getTfSelectedNumOfInputSpectra() {
        return this.tfSelectedNumOfInputSpectra;
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
                            box.getChildren().addAll(imageview, fileName); 
                            setGraphic(box);
                        } else {
                            setGraphic(null);
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
                        if (f == null) {
                            return;
                        }
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
                    if (d != null) {
                        return new SimpleStringProperty(sdf.format(d));
                    }
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
        
//init modification tableview
        ((TableColumn) this.tvModification.getColumns().get(0))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("name"));
        ((TableColumn) this.tvModification.getColumns().get(1))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, Double>("mass"));
        TableColumn<ModificationTableModel, Boolean> checkVariant = (TableColumn) this.tvModification.getColumns().get(2);
        checkVariant.setCellFactory(CheckBoxTableCell.forTableColumn(checkVariant));
        checkVariant.setCellValueFactory(new PropertyValueFactory<ModificationTableModel, Boolean>("isVariant"));
        TableColumn<ModificationTableModel, Boolean> checkFixed = (TableColumn) this.tvModification.getColumns().get(3);
        checkFixed.setCellFactory(CheckBoxTableCell.forTableColumn(checkFixed));
        checkFixed.setCellValueFactory(new PropertyValueFactory<ModificationTableModel, Boolean>("isFixed"));   
        
        this.tvModification.setItems(this.jm.getDefaultModifications());
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
        this.cbFragmentMassType.getSelectionModel().select(1);
        
//
        this.cbRefinementExpect.setItems(this.jm.getRefinementExpects());
        this.cbRefinementExpect.getSelectionModel().selectFirst();
        
//
        this.rm.init(this.apSpectrum);
//init result table
        ((TableColumn) this.tvResults.getColumns().get(0))
                .setCellValueFactory(new PropertyValueFactory<PeptideModel, String>("chrom"));
        ((TableColumn) this.tvResults.getColumns().get(1))
                .setCellValueFactory(new PropertyValueFactory<PeptideModel, String>("peptideSeq"));
        ((TableColumn) this.tvResults.getColumns().get(2))
                .setCellValueFactory(new PropertyValueFactory<PeptideModel, String>("proteinStart"));
        ((TableColumn) this.tvResults.getColumns().get(3))
                .setCellValueFactory(new PropertyValueFactory<PeptideModel, String>("proteinEnd"));
        ((TableColumn) this.tvResults.getColumns().get(4))
                .setCellValueFactory(new PropertyValueFactory<PeptideModel, String>("peptideStart"));
        ((TableColumn) this.tvResults.getColumns().get(5))
                .setCellValueFactory(new PropertyValueFactory<PeptideModel, String>("peptideEnd"));
        ((TableColumn) this.tvResults.getColumns().get(6))
                .setCellValueFactory(new PropertyValueFactory<PeptideModel, String>("modifications"));

        //init web engine
        WebEngine webEngine = JobOverviewController.this.wvBrowser.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> ov, State t, State t1) {
                log.debug(webEngine.getLocation());
            }
                    });
        
        this.tvResults.setRowFactory(new Callback<TableView<PeptideModel>, TableRow<PeptideModel>>() {
            @Override
            public TableRow<PeptideModel> call(TableView<PeptideModel> param) {
                final TableRow<PeptideModel> row = new TableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                            TableRow tr = (TableRow) event.getSource();
                            PeptideModel item = (PeptideModel) tr.getItem();
                            //display PSMs
                            String peptideId = item.getId();
                            JobOverviewController.this.tvPSMs.setItems(JobOverviewController.this.rm.getSpectrumList(peptideId));
                            
                            String chrom = item.chromProperty().get();

                            String proteinStart = item.proteinStartProperty().get();
                            String proteinEnd = item.proteinEndProperty().get();


                            String url = "http://61.50.130.100/ucsc/cgi-bin/hgTracks?org=human&position=chr" 
                                    + chrom + ":" + proteinStart + "-" + proteinEnd 
                                    + "&hgt.customText=" + JobOverviewController.this.tfBedUrl.getText();
                            log.debug(url);
                            webEngine.load(url);
                        }      
                    });
                return row;
            } 
        });
//init tvPSMs        
        ((TableColumn) this.tvPSMs.getColumns().get(0))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("spectrumId"));
        ((TableColumn) this.tvPSMs.getColumns().get(1))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("calculatedMassToCharge"));
        ((TableColumn) this.tvPSMs.getColumns().get(2))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("experimentalMassToCharge"));
        ((TableColumn) this.tvPSMs.getColumns().get(3))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("xtandemExpect"));
        ((TableColumn) this.tvPSMs.getColumns().get(4))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("xtandemHyperscore"));
        ((TableColumn) this.tvPSMs.getColumns().get(5))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("percolatorScore"));
        ((TableColumn) this.tvPSMs.getColumns().get(6))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("percolatorQvalue"));
        ((TableColumn) this.tvPSMs.getColumns().get(7))
                .setCellValueFactory(new PropertyValueFactory<SpectrumModel, String>("percolatorPEP"));
        
        this.tvPSMs.setRowFactory(new Callback<TableView<SpectrumModel>, TableRow<SpectrumModel>>() {
            @Override
            public TableRow<SpectrumModel> call(TableView<SpectrumModel> param) {
                final TableRow<SpectrumModel> row = new TableRow<>();
                row.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                            TableRow tr = (TableRow) event.getSource();
                            SpectrumModel item = (SpectrumModel) tr.getItem();
                            //display PSMs
                            try {
                                String index = item.spectrumIdProperty().get();
                                int i = Integer.parseInt(index) + 1;
                                JobOverviewController.this.rm.drawSpectrum(i);
                            } catch (JMzReaderException ex) {
                                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }      
                    });
                return row;
            } 
        });
        
        ((TableColumn) this.tvJobMonitor.getColumns().get(0))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("jobId"));
        ((TableColumn) this.tvJobMonitor.getColumns().get(1))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("spectra"));
        ((TableColumn) this.tvJobMonitor.getColumns().get(2))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("startTime"));
        ((TableColumn) this.tvJobMonitor.getColumns().get(3))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("passedTime"));
        ((TableColumn) this.tvJobMonitor.getColumns().get(4))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("clusterSize"));
        ((TableColumn) this.tvJobMonitor.getColumns().get(5))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("instanceId"));
        ((TableColumn) this.tvJobMonitor.getColumns().get(6))
                .setCellValueFactory(new PropertyValueFactory<ModificationTableModel, String>("status"));
        this.tvJobMonitor.setItems(this.sm.getJobs());
        //----------------------------init end-------------------------
        
//        log.debug("mainApp: " + this.mainApp);
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
    private void runListingObjectsService(final S3Bucket bucket) {
//        String message = "Listing " + bucket.getName();
//        Stage dialog = this.mainApp.createStripedProgressDialog(message, this.mainApp.getPrimaryStage());
        final Service<S3Object[]> s = this.mainApp.getCloudManager().createListObjectsService(bucket);
        s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                S3Object[] res = s.getValue();
                JobOverviewController.this.fm.setRemoteCachedObjectList(res);
                JobOverviewController.this.fm.setBucketPath(bucket);
                tfRemotePath.setText("/" + bucket.getName());
                tvRemote.setItems(JobOverviewController.this.fm.getRemoteCachedObjectList());
            }
        });
        Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Listing Objects")
                .message("Listing objects in bucket " + bucket.getName())
                .showWorkerProgress(s);
        s.start();
        
//        dialog.showAndWait();
//stuck until user click cancel button
//        if (State.SUCCEEDED == s.getState()) {
//            S3Object[] res = s.getValue();
//            this.fm.setRemoteCachedObjectList(res);
//            this.fm.setBucketPath(bucket);
//            tfRemotePath.setText("/" + bucket.getName());
//            tvRemote.setItems(this.fm.getRemoteCachedObjectList());
//        } else {
//            if (State.CANCELLED != s.getState()) {
//                log.debug(s.getState());
//                s.cancel();
//                log.debug(s.getState());
//            }
//        }
    }
    
    private void runListingInputObjectModelsService(S3Bucket bucket) {
//        String message = "Listing Bucket " + bucket.getName();
//        Stage dialog = this.mainApp.createStripedProgressDialog(message, this.mainApp.getPrimaryStage());
        final Service<S3Object[]> s = this.mainApp.getCloudManager().createListObjectsService(bucket);
        s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                S3Object[] res = s.getValue();
                JobOverviewController.this.jm.setCachedInputModels(res);
            }
        });
        Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Listing Objects")
                .message("Listing objects in bucket " + bucket.getName())
                .showWorkerProgress(s);
        s.start();
        
//        dialog.showAndWait();
//stuck until user click cancel button
//        if (State.SUCCEEDED == s.getState()) {
//            S3Object[] res = s.getValue();
//            this.jm.setCachedInputModels(res);
//        } else {
//            if (State.CANCELLED != s.getState()) {
//                log.debug(s.getState());
//                s.cancel();
//                log.debug(s.getState());
//            }
//        }
    }
    
    private void runListingBucketsService() {
        String message = "Listing Buckets";
        Stage dialog = this.mainApp.createStripedProgressDialog(message, this.mainApp.getPrimaryStage());
        final Service<S3Bucket[]> s = this.mainApp.getCloudManager().createListBucketsService(dialog);  
        s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                JobOverviewController.this.fm.setRemoteCachedBucketList(s.getValue());
                JobOverviewController.this.tvRemote.setItems(JobOverviewController.this.fm.getRemoteCachedBucketList());
            }
        });
        Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Listing Buckets")
                .message("Listing all of your buckets")
                .showWorkerProgress(s);
        s.start();
        
//        dialog.showAndWait();
//        
//        if (State.SUCCEEDED == s.getState()) {
//            this.fm.setRemoteCachedBucketList(s.getValue());
//            this.tvRemote.setItems(this.fm.getRemoteCachedBucketList());
//        } else {
//            if (State.CANCELLED != s.getState()) {
//                log.debug(s.getState());
//                s.cancel();
//                log.debug(s.getState());
//            }
//        }
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
        this.jm.setMainApp(mainApp);
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
        if(cbJobType.getSelectionModel().getSelectedIndex() == 0) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TypeOne.fxml"));
                AnchorPane ap = (AnchorPane) loader.load();
                this.t1c = loader.getController();
                bpJobType.setCenter(ap);
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if(cbJobType.getSelectionModel().getSelectedIndex() == 1) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TypeTwo.fxml"));
                AnchorPane ap = (AnchorPane) loader.load();
                this.t2c = loader.getController();
                bpJobType.setCenter(ap);
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
        if(cbJobType.getSelectionModel().getSelectedIndex() == 2) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TypeThree.fxml"));
                AnchorPane ap = (AnchorPane) loader.load();
                this.t3c = loader.getController();
                bpJobType.setCenter(ap);
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
       
        if(cbJobType.getSelectionModel().getSelectedIndex() == 3) {
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
    private void handleLocalRefreshActon() {
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
    private void handleLocalDeleteAction() {
        Iterator fileIterator = getSelectedFiles();
        if (!fileIterator.hasNext()) {
            log.error("no files are selected");
        }
        
        Action response = Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Do you want to delete?")
                .message("Selected files will be deleted.")
                .actions(Dialog.Actions.OK, Dialog.Actions.CANCEL)
                .showConfirm();
        
        if (response == Dialog.Actions.OK) {
            while(fileIterator.hasNext()) {
                File f = (File) fileIterator.next();
                FileUtils.deleteQuietly(f);
                log.info(f.getName() + " has been deleted");
            }
            this.updateLocalFileCache(this.fm.getFolderPath());
        } else {
            return;
        }
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
        Optional<String> response = Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Create a Bucket")
                .masthead("Amazon S3 bucket names are globally unique, "
                        + "so once a bucket name has been taken by any user, "
                        + "you can't create another bucket with that same name.")
                .message("Please enter an unique bucket name:")
                .showTextInput("");
        if (response.isPresent()) {
            String bucketName = response.get();
            final Service<S3Bucket> s = this.mainApp.getCloudManager().createCreateBucketService(bucketName);
            s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    JobOverviewController.this.fm.addBucket(s.getValue());
                }
            });
            Dialogs.create()
                        .owner(this.mainApp.getPrimaryStage())
                        .title("Creating")
                        .message("Creating bucket " + bucketName)
                        .showWorkerProgress(s);
            s.start();
        }
        //this.mainApp.showTextFieldDialog();
    }
    
    @FXML
    private void handleRemoteDeleteAction() {
        Object obj = this.tvRemote.getSelectionModel().getSelectedItem();
        if (obj == null) {
            return;
        }
        
        if (obj instanceof S3Bucket) {
            final S3Bucket tmp = (S3Bucket) obj;
            String msg = "Bucket " + tmp.getName() + " will be deleted";
            Action response = Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Delete")
                .message(msg)
                .actions(Dialog.Actions.OK, Dialog.Actions.CANCEL)
                .showConfirm();
            if (response == Dialog.Actions.OK) {
                Service<Void> s = this.mainApp.getCloudManager().createDeleteBucketService((S3Bucket) obj);
                s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent event) {
                        JobOverviewController.this.fm.deleteBucket(tmp);                }
                });
                Dialogs.create()
                        .owner(this.mainApp.getPrimaryStage())
                        .title("Deleting")
                        .message("Deleting bucket " + tmp.getName())
                        .showWorkerProgress(s);
                s.start();
            }
            if (response == Dialog.Actions.CANCEL) {
                return;
            }
        }
        
        if (obj instanceof S3Object) {
            final S3Object tmp = (S3Object) obj;
            String msg = "Object " + tmp.getName() + " will be deleted";
            Action response = Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Delete")
                .message(msg)
                .actions(Dialog.Actions.OK, Dialog.Actions.CANCEL)
                .showConfirm();
            
            if (response == Dialog.Actions.OK) {
                Service<Void> s = this.mainApp.getCloudManager().createDeleteObjectService((S3Object) obj);
                s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent event) {
                        JobOverviewController.this.fm.deleteObject(tmp);
                    }
                });
                
                Dialogs.create()
                        .owner(this.mainApp.getPrimaryStage())
                        .title("Deleting")
                        .message("Deleting object " + tmp.getName())
                        .showWorkerProgress(s);
                s.start();
            }
            if (response == Dialog.Actions.CANCEL) {
                return;
            }
        }
    }
    
    @FXML
    private void handleInstanceMonitorRefreshAction() {
        Service<DescribeInstancesResult> s = this.mainApp.getCloudManager().createDescribeInstancesService(new DescribeInstancesRequest());
        s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                ObservableList<InstanceModel> res = FXCollections.observableArrayList();
                Iterator<Reservation> it = s.getValue().getReservations().iterator();
                while (it.hasNext()) {
                    Reservation r = it.next();
                    for (Instance i : r.getInstances()) {
                        res.add(new InstanceModel(i));
                    }
                }
                JobOverviewController.this.tvInstanceMonitor.setItems(res);
                JobOverviewController.this.sm.setInstancesCache(res);
            }
        });
        s.start();
    }
//    
    @FXML
    private void handleInstanceMonitorStopAction() {
        List<String> instanceIds = new ArrayList<>();
        InstanceModel selectedInstance = (InstanceModel) this.tvInstanceMonitor.getSelectionModel().getSelectedItem();
        if (selectedInstance == null) {
            Dialogs.create()
                    .owner(this.mainApp.getPrimaryStage())
                    .title("Error")
                    .masthead(null)
                    .message("No instance is selected!")
                    .showError();
            return;
        }
        
        instanceIds.add(selectedInstance.instanceIdProperty().getValue());   
        
        Service<StopInstancesResult> s = this.mainApp.getCloudManager().createStopInstancesService(new StopInstancesRequest(instanceIds));
        s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                StopInstancesResult r = s.getValue();
                for (InstanceStateChange isc : r.getStoppingInstances()) {
                    InstanceModel im = JobOverviewController.this.sm.getInstance(isc.getInstanceId());
                    im.setState(isc.getCurrentState().getName());
                }
            }
        });
        s.start();
    }
    
    @FXML
    private void handleInstanceMonitorTerminateAction() {
        List<String> instanceIds = new ArrayList<>();
        InstanceModel selectedInstance = (InstanceModel) this.tvInstanceMonitor.getSelectionModel().getSelectedItem();
        if (selectedInstance == null) {
            Dialogs.create()
                    .owner(this.mainApp.getPrimaryStage())
                    .title("Error")
                    .masthead(null)
                    .message("No instance is selected!")
                    .showError();
            return;
        }
        instanceIds.add(selectedInstance.instanceIdProperty().getValue());
        Service<TerminateInstancesResult> s = this.mainApp.getCloudManager().createTerminateInstancesService(new TerminateInstancesRequest(instanceIds));
        s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                TerminateInstancesResult r = s.getValue();
                List<InstanceStateChange> res = r.getTerminatingInstances();
                for (InstanceStateChange isc : res) {
                    InstanceModel im = JobOverviewController.this.sm.getInstance(isc.getInstanceId());
                    im.setState(isc.getCurrentState().getName());
                }
            }
            
        });
        s.start();
    }
    
    @FXML
    private void handleInstanceMonitorRebootAction() {
        List<String> instanceIds = new ArrayList<>();
        InstanceModel selectedInstance = (InstanceModel) this.tvInstanceMonitor.getSelectionModel().getSelectedItem();
        if (selectedInstance == null) {
            Dialogs.create()
                    .owner(this.mainApp.getPrimaryStage())
                    .title("Error")
                    .masthead(null)
                    .message("No instance is selected!")
                    .showError();
            return;
        }
        Service<Void> s = this.mainApp.getCloudManager().createRebootInstancesService(new RebootInstancesRequest(instanceIds));
        s.start();
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
    
    //generate x!tandem parameter files: input.xml, taxonomy.xml
    @FXML
    private void handleJobSaveAction() {
        int jobType = this.cbJobType.getSelectionModel().getSelectedIndex() + 1;
        //S3Bucket saveToBucket = (S3Bucket) this.cbBucketSelection.getValue();
        String sep = IOUtils.LINE_SEPARATOR;
        List<S3Object> selectedSpectra = this.getSelectedSpectra();
        
        //cluster parameters
        String imageId = this.tfImageId.getText();
        String keyName = this.tfKeyName.getText();
        String securityGroup = this.tfSecurityGroup.getText();
        Integer clusterSize = Integer.parseInt(this.tfNumOfInstances.getText());
        InstanceType instanceType = (InstanceType) this.cbInstanceType.getSelectionModel().getSelectedItem();
        String outputBucketName = this.tfOutputBucketName.getText();
           
        if (jobType == 0 || selectedSpectra.isEmpty() || imageId == null || keyName == null || securityGroup == null || clusterSize.intValue() <= 0 || instanceType == null) {
            Dialogs.create()
                    .owner(this.mainApp.getPrimaryStage())
                    .title("Error")
                    .masthead(null)
                    .message("Your parameters are incomplete or invalid, please check again!")
                    .showError();
            return;
        }

        //x!tandem parameters
        SearchParameters sp = new SearchParameters();
        XtandemParameters xp = new XtandemParameters();
        sp.setIdentificationAlgorithmParameter(1, xp);
        //set enzyme
        String enzymeName = (String) this.cbCleavageSites.getValue();
        Enzyme e = this.jm.getEnzymeFactory().getEnzyme(enzymeName);
        e.setSemiSpecific(this.cbSemiCleavage.isSelected());
        sp.setEnzyme(e);
        //set fragment error
        String fragmentError = this.tfFragmentMassError.getText();
        sp.setFragmentIonAccuracy(Double.parseDouble(fragmentError));
        sp.setFragmentAccuracyType((MassAccuracyType) this.cbFragmentMassType.getValue());
        //set refinement expect
        String refinementExpect = (String) this.cbRefinementExpect.getValue();
        xp.setMaximumExpectationValueRefinement(Double.parseDouble(refinementExpect));
        //set modification
        ModificationProfile mp = new ModificationProfile();
        for (ModificationTableModel mtm : this.jm.getDefaultModifications()) {
            if (mtm.isFixedProperty().get()) {
                mp.addFixedModification(mtm.getPtm());
            }
            if (mtm.isVariantProperty().get()) {
                mp.addVariableModification(mtm.getPtm());
            }
        }
        sp.setModificationProfile(mp);
        
        CloudJob cj = new CloudJob(this.mainApp, selectedSpectra, sp, jobType);
        cj.setImageId(imageId);
        cj.setKeyName(keyName);
        cj.setClusterSize(clusterSize);
        cj.setSecurityGroup(securityGroup);
        cj.setInstanceType(instanceType);
        cj.setOutputBucketName(outputBucketName);

        //monitor status
        cj.setStartTime("0");
        cj.setPassedTime("0");
        cj.setInstanceId("not available");
        cj.setStatus("saved");
        
        if (cj.getJobType() == 1) {
            try {
                String chromNum = this.t1c.getSelectedChromosomeNumber();
                if (chromNum == null) {
                    Dialogs.create()
                            .owner(this.mainApp.getPrimaryStage())
                            .title("Error")
                            .masthead(null)
                            .message("Please select one chromosome!")
                            .showError();
                    return;
                }
                cj.createTaxonomyFile("chr" + chromNum + "_six_7.fa");
                cj.createInputFiles();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        if (cj.getJobType() == 2) {
            try {
                cj.createTaxonomyFile("missense_snv_protein_40.fa");
                cj.createInputFiles();
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (cj.getJobType() == 3) {
            try {
                cj.createTaxonomyFile("EEJ_peptide.fa");
                cj.createInputFiles();
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (cj.getJobType() == 4) {
            try {
                cj.createTaxonomyFile("custom_snv_peptide_40.fa");
                cj.createInputFiles();
            } catch (IOException ex) {
                Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        this.sm.addJob(cj);
        Dialogs.create()
                .owner(this.mainApp.getPrimaryStage())
                .title("Information")
                .masthead(null)
                .message("Job configurations have been saved, you can check it in job monitor panel!")
                .showInformation();
    }
    
    @FXML
    private void handleRunCloudJobAction() throws UnsupportedEncodingException, IOException, InterruptedException {
        if (this.sm.getJobs().isEmpty()) {
            Dialogs.create()
                    .owner(this.mainApp.getPrimaryStage())
                    .title("Error")
                    .masthead(null)
                    .message("please save job configuration first!")
                    .showError();
            return;
        }
        
        Action response = Dialogs.create()
                    .owner(this.mainApp.getPrimaryStage())
                    .title("Confirm")
                    .masthead("You will be charged for AWS fees!")
                    .message("Do you want to continue?")
                    .showConfirm();
        if (response == Dialog.Actions.CANCEL || response == Dialog.Actions.NO) {
            return;
        }  
        
        CloudJob cj = this.sm.getJobs().get(this.sm.getJobs().size()-1);
        String imageId = cj.getImageId();
        String keyName = cj.getKeyName();
        String securityGroup = cj.getSecurityGroup();
        InstanceType instanceType = cj.getInstanceType();
        Integer clusterSize = cj.getClusterSize();
        
        CloudManager cm = JobOverviewController.this.mainApp.getCloudManager(); 
        Date startTime = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
        cj.setStartTime(sdf.format(startTime));
        cj.setStatus("lauching instances");  
        
        Service<List<String>> msSearchService = new Service<List<String>>() {
            @Override
            protected Task<List<String>> createTask() {
                return new Task<List<String>>() {
                    @Override
                    protected List<String> call() throws Exception {    

                        //delete and create private key
                        File privateKey = cm.createKeyPair(keyName, FileUtils.getUserDirectory());
                        //delete and create security group
                        cm.createSecurityGroup(securityGroup, "capercloud group");
                        //create on-demand instances
                        List<String> instances = cm.createOnDemandInstances(imageId, instanceType, clusterSize, keyName, securityGroup);
                        cj.setInstanceId(instances.toString());
                        //copy file and run it
                        cj.setStatus("copying config file to cluster");
                        String masterId = instances.get(0);
                        Instance masterInstance = cm.getEc2Client().describeInstances(new DescribeInstancesRequest().withInstanceIds(masterId)).getReservations().get(0).getInstances().get(0);
                        String masterPrivateIp = masterInstance.getPrivateIpAddress();
                        String masterPublicIp = masterInstance.getPublicIpAddress();
                        
                        //make hosts file
                        DescribeInstancesResult r = cm.getEc2Client().describeInstances(new DescribeInstancesRequest().withInstanceIds(instances));
                        Iterator i = r.getReservations().iterator();
                        StringBuilder hosts = new StringBuilder();
                        while (i.hasNext()) {
                            Reservation rr = (Reservation) i.next();
                            for (Instance ii : rr.getInstances()) {
                                String hostIp = ii.getPrivateIpAddress();
                                String hostName = "ip." + hostIp;
                                hostName = hostName.replaceAll("\\.", "-");
                                log.debug("hostname: " + hostName);
                                hosts.append(hostIp).append("    ").append(hostName).append("\n");
                            }
                        }
                        log.debug(hosts.toString());
                        
                        r = cm.getEc2Client().describeInstances(new DescribeInstancesRequest().withInstanceIds(instances));
                        i = r.getReservations().iterator();
                        while (i.hasNext()) {
                            // on every node
                            Reservation rr = (Reservation) i.next();
                            for (Instance ii : rr.getInstances()) {
                                log.debug(ii.getPublicDnsName());
                                //correct time on eucalyptus
                                String cmdCorrectNtpTime = "sudo chmod 777 /mnt;mkdir /mnt/hadoop;sudo service ntpd stop;sudo ntpdate 192.168.99.111;sudo service ntpd start;echo \"export PATH=$PATH:/usr/local/hadoop-1.2.1/bin\" >> /home/ec2-user/.bashrc;source /home/ec2-user/.bashrc;echo '" + hosts.toString() + "' | sudo tee -a /etc/hosts";
                                log.info("remote execute: " + cmdCorrectNtpTime);
                                cm.remoteCallByShh("ec2-user", ii.getPublicIpAddress(), cmdCorrectNtpTime, privateKey);
                                //launching hadoop cluster
                                log.info("uploading hadoop-remote-init.sh");
                                cm.sftp("ec2-user", ii.getPublicIpAddress(), "/Users/shuai/Developer/CaperCloud/backend/hadoop-remote-init.sh", "/home/ec2-user/hadoop-remote-init.sh", privateKey);
                                String cmdRemoteInit = "chmod 755 hadoop-remote-init.sh;./hadoop-remote-init.sh " + masterPrivateIp;
                                cm.remoteCallByShh("ec2-user", ii.getPublicIpAddress(), cmdRemoteInit, privateKey);
                            }
                        }                
                        // on master    


                        //download data from s3 and upload to hdfs
                        log.info("uploading download_data.py");
                        cm.sftp("ec2-user", masterPublicIp, "/Users/shuai/Developer/CaperCloud/backend/download_data.py", "/home/ec2-user/download_data.py", privateKey);
                        log.info("uploading upload_data.py");
                        cm.sftp("ec2-user", masterPublicIp, "/Users/shuai/Developer/CaperCloud/backend/upload_data.py", "/home/ec2-user/upload_data.py", privateKey);
                        log.info("uploading taxonomy file: " + cj.getTaxonomyFile().getAbsolutePath());
                        String taxonomyFileName =  cj.getTaxonomyFile().getName();
                        cm.sftp("ec2-user", masterPublicIp, cj.getTaxonomyFile().getAbsolutePath(), "/mnt/" + taxonomyFileName, privateKey);
                        log.info("uploading input file: " + cj.getInputFiles().get(0).getAbsolutePath());
                        String inputXmlFileName = cj.getInputFiles().get(0).getName();
                        cm.sftp("ec2-user", masterPublicIp, cj.getInputFiles().get(0).getAbsolutePath(), "/mnt/" + inputXmlFileName , privateKey);
                        log.info("uploading mrtandem binary file");
                        cm.sftp("ec2-user", masterPublicIp, "/Users/shuai/Bio/tandem-bin/mrtandem-centos", "/mnt/mrtandem", privateKey);
                        cm.remoteCallByShh("ec2-user", masterPublicIp, "chmod 755 /mnt/mrtandem", privateKey); 
                        log.info("uploading x!tandem default xml");
                        cm.sftp("ec2-user", masterPublicIp, "/Users/shuai/Bio/tandem-bin/default_input.xml", "/mnt/default_input.xml", privateKey);
                        
                        //wait hadoop cluster start up
                        cm.sftp("ec2-user", masterPublicIp, "/Users/shuai/Developer/CaperCloud/backend/wait_hadoop.sh", "/home/ec2-user/wait_hadoop.sh", privateKey);
                        String cmdWaitHadoopCluster = "chmod 755 wait_hadoop.sh;./wait_hadoop.sh " + cj.getClusterSize().toString() + ";hadoop dfs -mkdir shared";
                        log.debug(cmdWaitHadoopCluster);
                        cm.remoteCallByShh("ec2-user", masterPublicIp, cmdWaitHadoopCluster, privateKey);
                        
                        int jobType = cj.getJobType();
                        if (jobType == 1) {
                            String fdr = JobOverviewController.this.t1c.getFdr();
                            String chrNum = JobOverviewController.this.t1c.getSelectedChromosomeNumber();
                            String refDatabase = "chr_" + chrNum + "_six_20.fasta";
                            //download reference database from s3
                            String cmdDownloadRef = "python download_data.py " + cm.getCurrentCredentials().getAccessKey() 
                                    + " " + cm.getCurrentCredentials().getSecretKey()
                                    + " " + "capercloud-ref"
                                    + " " + refDatabase
                                    + " " + "/mnt";
                            log.debug(cmdDownloadRef);
                            cm.remoteCallByShh("ec2-user", masterPublicIp, cmdDownloadRef, privateKey); 
                            //download spectra file from s3
                            String spectraName = cj.getSpectrumObjs().get(0).getName();
                            String cmdDownloadSpectra = "python download_data.py " + cm.getCurrentCredentials().getAccessKey()
                                    + " " + cm.getCurrentCredentials().getSecretKey()
                                    + " " + cj.getSpectrumObjs().get(0).getBucketName()
                                    + " " + spectraName
                                    + " " + "/mnt";
                            log.debug(cmdDownloadSpectra);
                            cm.remoteCallByShh("ec2-user", masterPublicIp, cmdDownloadSpectra, privateKey);
                            
                            String cmdUploadFilesToHDFS = "hadoop dfs -put /mnt/" + taxonomyFileName + " shared/" + taxonomyFileName + ";"
                                    + "hadoop dfs -put /mnt/" + inputXmlFileName + " shared/" + inputXmlFileName + ";"
                                    + "hadoop dfs -put /mnt/mrtandem shared/mrtandem;"
                                    + "hadoop dfs -put /mnt/default_input.xml shared/default_input.xml;"
                                    + "hadoop dfs -put /mnt/" + refDatabase + " shared/" + refDatabase + ";"
                                    + "hadoop dfs -put /mnt/" + spectraName + " shared/" + spectraName;
                            log.debug(cmdUploadFilesToHDFS);
                            cm.remoteCallByShh("ec2-user", masterPublicIp, cmdUploadFilesToHDFS, privateKey);
                            
                            String sharedFiles = " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/shared/" + taxonomyFileName + "#" + taxonomyFileName 
                                    + " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/shared/" + inputXmlFileName + "#" + inputXmlFileName
                                    + " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/shared/mrtandem#mrtandem"
                                    + " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/shared/default_input.xml#default_input.xml"
                                    + " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/shared/" + refDatabase + "#" + refDatabase
                                    + " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/shared/" + spectraName + "#" + spectraName;
                        
                            cj.setStatus("x!tandem searching");
                            StringBuilder step1InputLines = new StringBuilder();
                            int multi = 2;
                            int numOfMappers = Integer.parseInt(cj.clusterSizeProperty().get()) * multi;
                            for (int j=1; j<numOfMappers; j++) {
                                step1InputLines.append(j).append("    ").append(numOfMappers).append("\n");
                            }
                            step1InputLines.append(numOfMappers).append("    ").append(numOfMappers);
                            File step1InputFile = new File("step1input");
                            FileUtils.writeStringToFile(step1InputFile, step1InputLines.toString());
                            cm.sftp("ec2-user", masterPublicIp, step1InputFile.getAbsolutePath(), "/home/ec2-user/step1input", privateKey);
                            //upload step1input to hdfs
                            log.info("******************"+Calendar.getInstance().getTime());
                            cm.remoteCallByShh("ec2-user", masterPublicIp, "hadoop dfs -put /home/ec2-user/step1input step1input", privateKey);

                            String stepArgs = " -jobconf mapred.task.timeout=36000000 -jobconf mapred.reduce.tasks=1 -jobconf mapred.map.tasks=" + cj.clusterSizeProperty().get() + " -jobconf mapred.reduce.tasks.speculative.execution=false -jobconf mapred.map.tasks.speculative.execution=false";
                            //be careful, it's a mess
                            cj.setStatus("first stage of mapping and reducing");
                            String cmdStepOne = "hadoop jar /usr/local/hadoop-1.2.1/contrib/streaming/hadoop-streaming-1.2.1.jar -input step1input -output step1output" + sharedFiles + " -mapper \"mrtandem -mapper1_1 hdfs://" + masterPrivateIp + ":9000/user/ec2-user/ " + cj.getInputFiles().get(0).getName() + "\" -reducer \"mrtandem -reducer1_1 hdfs://" + masterPrivateIp + ":9000/user/ec2-user/ " + cj.getInputFiles().get(0).getName() + "\"" + stepArgs;
                            log.debug(cmdStepOne);
                            cm.remoteCallByShh("ec2-user", masterPublicIp, cmdStepOne, privateKey);

                            cj.setStatus("second stage of mapping and reducing");
                            String cmdStepTwo = "hadoop jar /usr/local/hadoop-1.2.1/contrib/streaming/hadoop-streaming-1.2.1.jar -input step1output -output step2output" + sharedFiles + " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/reducer1_1#reducer1_1 -mapper \"mrtandem -mapper2_1 hdfs://" + masterPrivateIp + ":9000/user/ec2-user/ " + cj.getInputFiles().get(0).getName() + "\" -reducer \"mrtandem -reducer2_1 hdfs://" + masterPrivateIp + ":9000/user/ec2-user/ " + cj.getInputFiles().get(0).getName() + "\"" + stepArgs;
                            log.debug(cmdStepTwo);
                            cm.remoteCallByShh("ec2-user", masterPublicIp, cmdStepTwo, privateKey);

                            cj.setStatus("final stage of mapping and reducing");
                            String cmdStepThree = "hadoop jar /usr/local/hadoop-1.2.1/contrib/streaming/hadoop-streaming-1.2.1.jar -input step2output -output step3output" + sharedFiles + " -cacheFile hdfs://" + masterPrivateIp + ":9000/user/ec2-user/reducer2_1#reducer2_1 -mapper \"mrtandem -mapper3_1 hdfs://" + masterPrivateIp + ":9000/user/ec2-user/ " + cj.getInputFiles().get(0).getName() + "\" -reducer \"mrtandem -reducer3_1 hdfs://" + masterPrivateIp + ":9000/user/ec2-user/ " + cj.getInputFiles().get(0).getName() + " -reportURL hdfs://" + masterPrivateIp + ":9000/user/ec2-user/\"" + stepArgs;
                            log.debug(cmdStepThree);
                            cm.remoteCallByShh("ec2-user", masterPublicIp, cmdStepThree, privateKey);

                            log.info("******************"+Calendar.getInstance().getTime());
                        
                        }
                     
                        //download output(in hdfs) to local
                        String cmdDownloadOutput = "hadoop dfs -copyToLocal output output";
                        cm.remoteCallByShh("ec2-user", masterPublicIp, cmdDownloadOutput, privateKey);
                        // upload result to s3
                        cj.setStatus("uploading result to s3");
                        String bucketName = JobOverviewController.this.tfOutputBucketName.getText();
                        String cmd5 = "python upload_data.py " + cm.getCurrentCredentials().getAccessKey()
                                + " " + cm.getCurrentCredentials().getSecretKey()
                                + " " + bucketName
                                + " " + "/home/ec2-user/output";
                        log.debug(cmd5);
                        cm.remoteCallByShh("ec2-user", masterPublicIp, cmd5, privateKey);
                        
                        return instances;
                    }
                };
            };
        };
        
        msSearchService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                List<String> instances = msSearchService.getValue();
                log.info("shutting down instances");
                cj.setStatus("shutting down instances");
                cm.getEc2Client().terminateInstances(new TerminateInstancesRequest().withInstanceIds(instances));
                cj.setStatus("downloading result");
                AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(cm.getCurrentCredentials().getAccessKey(), cm.getCurrentCredentials().getSecretKey()));
                s3Client.setEndpoint("http://192.168.99.111:8773/services/Walrus/");
                writeInputStreamToFile(s3Client.getObject(new GetObjectRequest(cj.getOutputBucketName(), "output")).getObjectContent(), new File("backend/IPeak_release/output.xml"));
                 
                Service<Void> postProcessService = new Service<Void>() {
                    @Override
                    protected Task<Void> createTask() {
                        return new Task<Void>() {
                            @Override
                            protected Void call() throws Exception {
                                updateProgress(-1, 0);
                                updateMessage("Parsing result...please wait");
                                if (cj.getJobType() == 1) {
                                    String postProcessCMD = "backend/IPeak_release/post_process.py backend/IPeak_release/output.xml " + JobOverviewController.this.t1c.getFdr();
                                    log.debug(postProcessCMD);
                                    Runtime.getRuntime().exec(postProcessCMD);
                                }
                                JobOverviewController.this.rm.parse(new File("result.mzid"));
                                return null;
                            }
                        };
                    }     
                };
                
                postProcessService.start();
                postProcessService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent t) {
                        cj.setStatus("job completed");
                        Date stopTime = Calendar.getInstance().getTime();       
                        cj.setPassedTime(sdf.format(stopTime));
                        JobOverviewController.this.tvResults.setItems(JobOverviewController.this.rm.getPeptideList()); 
                    }
                });
                
                Dialogs.create()
                        .owner(JobOverviewController.this.mainApp.getPrimaryStage())
                        .title("Parsing result")
                        .showWorkerProgress(postProcessService);
                }
        });
        msSearchService.start();
    }
    private void writeInputStreamToFile(InputStream inputStream, File outFile) {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outFile);
            int read = 0;
            byte[] bytes = new byte[1024];
            
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
//    private Future sendFiles(CloudJob job) throws IOException {
//        String url = "http://127.0.0.1:5000/";
//        final AsyncHttpClient client = new AsyncHttpClient();
//        List<File> toBeUploaded = job.getInputFiles();
//        toBeUploaded.add(job.getTaxonomyFile());
//        RequestBuilder rb = new RequestBuilder("POST")
//                .setUrl(url)
//                .setHeader("Content-Type", "multipart/form-data");
//
//        for (File f : toBeUploaded) {
//            rb.addBodyPart(new FilePart(f.getName(), f, "text/plain", "UTF-8"));
//        }
//        
//        return client.prepareRequest(rb.build()).execute(new AsyncCompletionHandler<Response>() {
//
//            @Override
//            public Response onCompleted(Response r) throws Exception {
//                client.close();
//                return r;
//            }
//            
//        });
//    }
    
//    private void postJob(CloudJob job) throws IOException {
//        List<File> input_xmls = job.getInputFiles();
//        String url = "http://127.0.0.1:5000/job4";
//        final AsyncHttpClient client = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setIdleConnectionTimeoutInMs(10000000).setConnectionTimeoutInMs(10000000).setRequestTimeoutInMs(10000000).build());
//        RequestBuilder rb = new RequestBuilder("POST")
//            .setUrl(url)
//            .addParameter("input_xml", input_xmls.get(0).getName())
//            .addParameter("access_key", this.mainApp.getCloudManager().getCurrentCredentials().getAccessKey())
//            .addParameter("secret_key", this.mainApp.getCloudManager().getCurrentCredentials().getSecretKey())
//            .addParameter("bucket_name", this.jm.getCurrentBucket().getName());
//        
//        StringBuilder sb = new StringBuilder();
//        List<S3Object> objs = job.getSpectrumObjs();
//        objs.add(job.getDatabaseObj());
//        Iterator it = objs.iterator();
//        sb.append(((S3Object) it.next()).getName());
//        while(it.hasNext()) {
//            sb.append("," + ((S3Object) it.next()).getName());
//        }
//        
//        rb.addParameter("key_names", sb.toString());
//        
//        AsyncCompletionHandler<Response> handler = new AsyncCompletionHandler<Response>() {
//            @Override
//            public Response onCompleted(Response r) throws Exception {
//                client.close();
//                return r;
//            }
//            @Override
//            public void onThrowable(Throwable t) {
//                log.debug("---------");
//            }
//        };
//        Future f = null;
//        f = client.prepareRequest(rb.build()).execute(handler);
        
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
//    }
    
    @FXML
    private void handleDownloadResultAction() {
        //TO DO
    }
    @FXML
    private void handleAdvancedSearchAction() {
        //TO DO
    }
    @FXML
    private void handleSelectSpectraFileAction() {
        FileChooser fileChooser = new FileChooser();
        File spectraFile = fileChooser.showOpenDialog(null);
        if (spectraFile == null) {
            return;
        }
        this.tfSpectraFile.setText(spectraFile.getAbsolutePath());
        this.rm.setSpectraFile(spectraFile);
    }
    
    @FXML
    private void handleVisualizeAction() {
        String bedUrl = this.tfBedUrl.getText();
        this.rm.setBedUrl(bedUrl);
    }
}

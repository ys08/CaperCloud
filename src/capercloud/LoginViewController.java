/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.Dialogs;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class LoginViewController implements Initializable {
    private Log log = LogFactory.getLog(getClass());
    private CaperCloud mainApp;
    
    private File selectedFolder;
    private ObservableList<File> credentialsFiles;
    private AWSCredentials currentCredentials;
    private AWSCredentials newCredentials;
    
    private static final int LOGIN_MODE_LOCAL_FOLDER = 0;
    private static final int LOGIN_MODE_DIRECT = 1;
    private int loginMode = LOGIN_MODE_LOCAL_FOLDER;
     
//general components
    @FXML TabPane tpLogin;
    @FXML Button btnLogin;
    @FXML Button btnSave;
    @FXML Tab tab0;
    @FXML Tab tab1; 
//Local folder tab
    @FXML TextField tfSelectedFolder;
    @FXML ListView lvCredentialsFile;
    @FXML PasswordField pfPassword;
//Direct Login tab
    @FXML TextField tfAccessKey;
    @FXML TextField tfSecretKey;
    
    public LoginViewController() {
        credentialsFiles = FXCollections.observableArrayList();
        this.selectedFolder = new File(System.getProperty("user.home"));
    }

    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }
    
    public Stage getNewAccountStage() throws IOException {
        return this.mainApp.getNewAccountStage();
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
//init TabPane
        tpLogin.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Tab>() {
                    @Override
                    public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab t1) {
                         if (t.equals(tab0)) {
                             btnSave.setDisable(true);
                             btnLogin.setDisable(false);
                         } else {
                             btnSave.setDisable(false);
                         }
                    }
                });
//init TextField
        tfSelectedFolder.setText(selectedFolder.getAbsolutePath());
        updateCredentialsFiles();   
//init ListView
        lvCredentialsFile.focusedProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue ov, Object t, Object t1) {
                if (lvCredentialsFile.isFocused()) {
                    btnLogin.setDisable(false);
                } 
            }
            
        });
        lvCredentialsFile.setItems(credentialsFiles);
    }   
    
//find files endwith ".enc", call it when selectedFolder is changed
    public void updateCredentialsFiles() {
        credentialsFiles.clear();
        FilenameFilter fileFilter = new SuffixFileFilter(".enc");  
        credentialsFiles.addAll(selectedFolder.listFiles(fileFilter));
    }

// retrieve credentials to currentCredentials
    private void retrieveCredentials(String password) throws ServiceException {
        File credentialsFile = this.getSelectedCredentialsFile();
        if (credentialsFile == null) {
            this.currentCredentials = null;
        }
        this.currentCredentials = (AWSCredentials) ProviderCredentials.load(password, credentialsFile);
    }
    
    /**
     * @return
     * the credentials encrypted file chosen by the user.
     */
    public File getSelectedCredentialsFile() {
        File selectedFile = (File) lvCredentialsFile.getSelectionModel().getSelectedItem();
        if (selectedFile == null) {
            return null;
        }
        return selectedFile;
    }
    
    /**
     * Where credentials is saved
     * @return 
     */
    public File getHomeFolder() {
        return this.selectedFolder;
    }
    
    /**
     * @return
     * the password the user provided to unlock their encrypted credentials file.
     */
    public String getPassword() {
        return pfPassword.getText();
    }
    
    /**
     * @return
     * the Access Key provided by the user.
     */
    public String getAccessKey() {
        return tfAccessKey.getText();
    }
    
    /**
     * @return
     * the Secret Key provided by the user.
     */
    public String getSecretKey() {
        return tfSecretKey.getText();
    }
    
    /**
     * Clears the user-provided password field.
     */
    public void clearUserInput() {
        pfPassword.setText("");
        tfAccessKey.setText("");
        tfSecretKey.setText("");
        this.currentCredentials = null;
    }
    
    @FXML private void handleSaveAction() {
        this.mainApp.showAccountManagerView();   
        this.clearUserInput();
    }
    
    @FXML private void handleLoginAction() {
        loginMode = tpLogin.getSelectionModel().getSelectedIndex();
        if (loginMode == LOGIN_MODE_LOCAL_FOLDER) {
            try {         
                retrieveCredentials(this.getPassword());
            } catch (ServiceException ex) { 
                log.error(ex.getErrorMessage());
                return;
            }
        } else if (loginMode == LOGIN_MODE_DIRECT) {
//direct login
            if (this.getAccessKey().trim() == "" || this.getSecretKey().trim() == "") {
                log.error("All fields are needed");
                return;
            } else {
                this.currentCredentials = new AWSCredentials(
                        this.getAccessKey(),
                        this.getSecretKey(),
                        "capercloud"
                );
            }
        }
//make sure we have a credentials
        if (this.currentCredentials == null) {
            log.error("Please provide AWS credentials!!!");
            return;
        }
//login to cloud
        try {
            this.mainApp.getCloudManager().loginCloud(this.currentCredentials);
//            log.info("checking your account information");
//            Stage checkingDialog = this.mainApp.createStripedProgressDialog("Checking Your Account", this.mainApp.getLoginStage());
//            Service<S3Bucket[]> s = this.mainApp.getCloudManager().createListBucketsService(checkingDialog);
//            s.start();
////wait for cancel or success event
//            checkingDialog.showAndWait();
            final Service<S3Bucket[]> s = this.mainApp.getCloudManager().createCheckingAccountService("Connecting...");
            
            s.setOnFailed(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    Dialogs.create()
                            .title("Error")
                            .message("Failed, please retry later or check network connection.")
                            .showError();
                }
            });
            
            s.setOnSucceeded(new EventHandler<WorkerStateEvent>() {    
                @Override
                public void handle(WorkerStateEvent event) {
                    mainApp.getMainController().getFm().setRemoteCachedBucketList(s.getValue());
                    mainApp.getMainController().enableButton();
                    mainApp.getLoginStage().close();

                    mainApp.getMainController().setUsername(currentCredentials.getFriendlyName());
                    mainApp.getLoginController().clearUserInput();
                    mainApp.getLoginStage().close();
    //logout button enabled  
                    mainApp.getMainController().getBtnLogout().setDisable(false);     
                }
            });
            
            Dialogs.create()
                    .owner(this.mainApp.getLoginStage())
                    .title("Connecting to the cloud")
                    .showWorkerProgress(s);
            s.start();
            
//            if (State.SUCCEEDED == s.getState()) {
//                log.info("login success!");
//                this.mainApp.getMainController().getFm().setRemoteCachedBucketList(s.getValue());
//                this.mainApp.getMainController().enableButton();
//                this.mainApp.getLoginStage().close();
//
//                this.mainApp.getMainController().setUsername(this.currentCredentials.getFriendlyName());
//                clearUserInput();
//                this.mainApp.getLoginStage().close();
////logout button enabled  
//                this.mainApp.getMainController().getBtnLogout().setDisable(false); 
//            } else {
////something went wrong when listing bucket
//                log.debug(s.getState());
//                if (State.CANCELLED != s.getState()) {
//                    s.cancel();
//                    log.debug(s.getState());
//                }
//                this.mainApp.getCloudManager().logoutCloud();
//            }        
        } catch (ServiceException ex) {
            log.error(ex.getErrorMessage());
            clearUserInput();
        }
    }
    
    @FXML private void handleChooseFolderAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser(); 
        directoryChooser.setTitle("Please choose a folder");
        
        File file = directoryChooser.showDialog(null);
        if(file != null) {
            this.selectedFolder = file;
            tfSelectedFolder.setText(file.getAbsolutePath());
            updateCredentialsFiles();
        }
    }
    
    @FXML private void handleCancelAction() {
        clearUserInput();
        this.mainApp.getLoginStage().close();
    }
}

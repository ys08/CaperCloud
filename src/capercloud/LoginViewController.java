/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.exception.IllegalCredentialsException;
import capercloud.s3.S3Manager;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
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
    private ObservableList<File> storedCredentialsList;
    private AWSCredentials selectedCredentials;
    private AWSCredentials newCredentials;
    
    public static final String EMPTY_PASSWORD_SURROGATE = "NONE";    
    private static final int LOGIN_MODE_LOCAL_FOLDER = 0;
    private static final int LOGIN_MODE_DIRECT = 1;
    private int loginMode = LOGIN_MODE_LOCAL_FOLDER;
     
//general components
    @FXML TabPane tpLogin;
    @FXML Button btnSave;
    @FXML Button btnLogin;
    @FXML Tab tab0;
    @FXML Tab tab1; 
//Local folder tab
    @FXML TextField tfHomeFolder;
    @FXML ListView lvCredentialsFile;
    @FXML PasswordField pfPassword;
//Direct Login tab
    @FXML TextField tfAccessKey;
    @FXML TextField tfSecretKey;
    
    public LoginViewController() {
        storedCredentialsList = FXCollections.observableArrayList();
        this.selectedFolder = new File(System.getProperty("user.home"));
    }

    public void setMainApp(CaperCloud mainApp) {
        System.out.println("in LoginViewController setMainApp Method");
        this.mainApp = mainApp;
    }

    public void setNewCredentials(AWSCredentials newCredentials) {
        this.newCredentials = newCredentials;
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
//switch btnSave disable mode
        tpLogin.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Tab>() {
                    @Override
                    public void changed(ObservableValue<? extends Tab> ov, Tab t, Tab t1) {
                         if (t.equals(tab0)) {
                             btnSave.setDisable(true);
                         } else {
                             btnSave.setDisable(false);
                         }
                    }
                });
//display default homePath
        tfHomeFolder.setText(selectedFolder.getAbsolutePath());
        refreshStoredCredentials();   
//credentials file for display
        lvCredentialsFile.setItems(storedCredentialsList);
    }   
    
    /**
     * Refreshes the table of stored credentials by finding <tt>*.enc</tt> files in the
     * directory specified as the Cockpit home folder.
     *
     */
    public void refreshStoredCredentials() {
        storedCredentialsList.clear();
        FilenameFilter fileFilter = new SuffixFileFilter(".enc");  
        storedCredentialsList.addAll(selectedFolder.listFiles(fileFilter));
    }
    /**
     * set selectedCredentials
     * @param directory
     * @param credentialsFile
     * @param password 
     */
    private void retrieveCredentialsFromDirectory(File directory, File credentialsFile, String password) throws ServiceException {
        if (!validFolderInputs(false, directory, credentialsFile, password, true)) {
            return;
        }
        this.selectedCredentials = (AWSCredentials) ProviderCredentials.load(password, credentialsFile);
    }
    
    private boolean validFolderInputs(boolean isStoreAction, File directory, 
            File credentialsFile, String password, boolean allowLegacyPassword) 
    {
        if (password.length() < 6) {
            if (allowLegacyPassword) {             
// Legacy password allowed for login, an error will be displayed later if it's incorrect.
            } else if (EMPTY_PASSWORD_SURROGATE.equals(password)) {             
// Surrogate empty password was used, not an error.
            } else {
                System.out.println("Password must be at least 6 characters. " +
                    "If you do not wish to set a password, use the password " +
                    EMPTY_PASSWORD_SURROGATE + ".");
                return false;
            }
        }
        if (!directory.exists() || !directory.canWrite()) {
            String invalidInputsMessage = "Directory '" + directory.getAbsolutePath()
            + "' does not exist or cannot be written to.";
            System.out.println(invalidInputsMessage);
            return false;
        }
        if (credentialsFile == null && !isStoreAction) {
            String invalidInputsMessage = "You must choose which stored login to use";
            System.out.println(invalidInputsMessage);
            return false;
        }
        return true;
    }
    
    private void storeCredentialsInDirectory(File directory, String password) {
        if (!validFolderInputs(true, directory, null, password, false)) {
            return;
        }
        if (EMPTY_PASSWORD_SURROGATE.equals(password.trim())) {
            password = "";
        }
        this.mainApp.showAccountManagerView();   
//AccountManagerView will set credentials value
        if (newCredentials == null) {
            System.out.println("user canceled");
            return;
        }
              
//save the credentials to file
        File credentialsFile = new File(directory, newCredentials.getFriendlyName() + ".enc");

        try {
            String algorithm = S3Manager.jets3tProperties.getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
            newCredentials.save(password, credentialsFile, algorithm);
            clearUserInput();
            refreshStoredCredentials();
            System.out.println("Your credentials have been stored in the file:\n" +
                credentialsFile.getAbsolutePath());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String message = "Unable to encrypt your credentials to a folder";
            System.out.println(message);
        }
    }
    
    /**
     * @return
     * the credentials encrypted file chosen by the user.
     */
    public File getSelectedCredentialsFile() {
        File selectedFile = (File)lvCredentialsFile.getSelectionModel().getSelectedItem();
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
     * Clears the user-provided password field.
     */
    public void clearUserInput() {
        pfPassword.setText("");
        tfAccessKey.setText("");
        tfSecretKey.setText("");
        this.selectedCredentials = null;
        this.newCredentials = null;
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
     * Verifies that the user has provided the correct inputs, and returns a list
     * of error messages if not.
     *
     * @return
     * an empty array if there a no input errors, otherwise the array will contain
     * a list of error messages.
     */
    public String[] checkForInputErrors() {
        ArrayList errors = new ArrayList();

        if (getAccessKey().trim().length() == 0) {
            errors.add("Access Key must be provided");
        }

        if (getSecretKey().trim().length() == 0) {
            errors.add("Secret Key must be provided");
        }   
        return (String[]) errors.toArray(new String[errors.size()]);
    }
    
    @FXML private void handleSaveAction() {
        storeCredentialsInDirectory(this.getHomeFolder(), this.getPassword());
    }
    
    @FXML private void handleLoginAction() {
        loginMode = tpLogin.getSelectionModel().getSelectedIndex();
        if (loginMode == LOGIN_MODE_LOCAL_FOLDER) {
            try {         
//credentials is stored in selectedCredentials
                retrieveCredentialsFromDirectory(this.getHomeFolder(), this.getSelectedCredentialsFile(), this.getPassword());
            } catch (ServiceException ex) { 
                log.error(ex.getErrorMessage());
                return;
            }
        } else if (loginMode == LOGIN_MODE_DIRECT) {
//direct login
            String[] inputErrors = this.checkForInputErrors();
            if (inputErrors.length > 0) {
                String errorMessages = "Please correct the following errors:";
                for (int i = 0; i < inputErrors.length; i++) {
                    errorMessages += inputErrors[i];
                }
                log.error(errorMessages);
                return;
            } else {
                this.selectedCredentials = new AWSCredentials(
                        this.getAccessKey(),
                        this.getSecretKey()
                );
            }
        } 
//login to cloud
        try {
            this.mainApp.getCloudManager().loginCloud(this.selectedCredentials);
            log.info("checking your account information");
            Stage checkingDialog = this.mainApp.createProgressDialog("Checking", "Checking your account...", this.mainApp.getLoginStage());
            Service<S3Bucket[]> listService = this.mainApp.getCloudManager().createListBucketsService(checkingDialog);
            listService.start();
//wait for cancel or success event
            checkingDialog.showAndWait();
            
            if (State.SUCCEEDED == listService.getState()) {
                log.info("login success!");
                this.mainApp.getMainController().getRemoteBucketCache().addAll(listService.getValue());
                this.mainApp.getMainController().enableButton();
                this.mainApp.getLoginStage().close();
//let combobox knows it
                if (this.selectedCredentials.getFriendlyName() == null) {
                    log.info("Direct Login");
                } else {
                    this.mainApp.getMainController().getCbSwitchAccount().setValue(this.selectedCredentials.getFriendlyName());
                }
                clearUserInput();
                this.mainApp.getLoginStage().close();
//logout button enabled  
                this.mainApp.getMainController().getBtnLogout().setDisable(false); 
            } else {
                log.debug(listService.getState());
                if (State.CANCELLED != listService.getState()) {
                    listService.cancel();
                    log.debug(listService.getState());
                }
                this.mainApp.getCloudManager().logoutOfCredentials(this.selectedCredentials);
            }        
        } catch (IllegalCredentialsException ex) {
            log.error(ex.getMessage());
            clearUserInput();
            return;
        } catch (ServiceException ex) {
            log.error(ex.getErrorMessage());
            clearUserInput();
            return;
        }
    }
    
    @FXML private void handleChooseFolderAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser(); 
        directoryChooser.setTitle("Please choose a folder");
        
        File file = directoryChooser.showDialog(null);
        if(file != null) {
            this.selectedFolder = file;
            tfHomeFolder.setText(file.getAbsolutePath());
            refreshStoredCredentials();
        }
    }
    
    @FXML private void handleCancelAction() {
        clearUserInput();
        this.mainApp.getLoginStage().close();
    }
}

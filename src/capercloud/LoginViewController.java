/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
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
    private CaperCloud mainApp;
    Jets3tProperties jets3tProperties;
    
    //data for ListView
    private ObservableList<File> fileList;
    
    //data for login
    private ProviderCredentials currentCredentials = null;
    
    //credentials to be saved
    private AWSCredentials credentials;
    
    public static final String EMPTY_PASSWORD_SURROGATE = "NONE";    
    private static final int LOGIN_MODE_LOCAL_FOLDER = 0;
    private static final int LOGIN_MODE_DIRECT = 1;
    private int loginMode = LOGIN_MODE_LOCAL_FOLDER;
    
    private File homeFolder;
    
    
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
        
        fileList = FXCollections.observableArrayList();
        
        if (Constants.DEFAULT_PREFERENCES_DIRECTORY.exists()) {
            this.homeFolder = Constants.DEFAULT_PREFERENCES_DIRECTORY;
        } else {
            this.homeFolder = new File(System.getProperty("user.home"));
        }
    }
    
    //getters and setters
    public AWSCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    public void setMainApp(CaperCloud mainApp) {
        System.out.println("in LoginViewController setMainApp Method");
        this.mainApp = mainApp;
        this.jets3tProperties = mainApp.getJets3tProperties();
    }

    public ProviderCredentials getcurrentCredentials() {
        return currentCredentials;
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        //when switch tab, storeCredentials button will be disabled
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
        tfHomeFolder.setText(homeFolder.getAbsolutePath());
        refreshStoredCredentials();
        //credentials file for display
        lvCredentialsFile.setItems(fileList);
    }   
    
    /**
     * Refreshes the table of stored credentials by finding <tt>*.enc</tt> files in the
     * directory specified as the Cockpit home folder.
     *
     */
    public void refreshStoredCredentials() {
        fileList.clear();
        try {
            File[] files = homeFolder.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                File candidateFile = files[i];
                if (candidateFile.getName().endsWith(".enc")) {
                    System.out.println("Candidate File: " + candidateFile.getName());
                    fileList.add(candidateFile);
                }
            }
        } catch (Exception e) {
            String message = "Unable to find credential files in the folder "
                + homeFolder.getAbsolutePath();
            System.out.println(message);
        }
    }
    /**
     * set currentCredentials
     * @param directory
     * @param credentialsFile
     * @param password 
     */
    private void retrieveCredentialsFromDirectory(File directory, File credentialsFile, String password) throws ServiceException {
        if (!validFolderInputs(false, directory, credentialsFile, password, true)) {
            return;
        }
        this.currentCredentials = ProviderCredentials.load(password, credentialsFile);
            System.out.println(currentCredentials);
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
        if (credentials == null) {
            System.out.println("storeCredentialsInDirectory returns because no credentials are set");
            return;
        }
        
        //save the credentials to file
        File credentialsFile = new File(directory, credentials.getFriendlyName() + ".enc");

        try {
            String algorithm = this.mainApp.getJets3tProperties().getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
            credentials.save(password, credentialsFile, algorithm);
            this.clearPassword();
            this.refreshStoredCredentials();
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
    public File getCredentialsFile() {
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
        return this.homeFolder;
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
    public void clearPassword() {
        pfPassword.setText("");
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
    
    @FXML private void handleLoginAction() throws ServiceException {
        loginMode = tpLogin.getSelectionModel().getSelectedIndex();
        if (loginMode == LOGIN_MODE_LOCAL_FOLDER) {
            try {
                retrieveCredentialsFromDirectory(this.getHomeFolder(), this.getCredentialsFile(), this.getPassword());
            } catch (ServiceException ex) { 
                System.out.println("Unable to load your credentials from the file:" + 
                        this.getCredentialsFile().getAbsolutePath());
                System.out.println("Please check your password.");
                return;
            }
            if (this.currentCredentials == null) {
                System.out.println("currentCredentials is empty");
                return;
            }
            //do some login initiating stuff here
            System.out.println("listing bucket local folder, please wait");
            
        } else if (loginMode == LOGIN_MODE_DIRECT) {
            //direct login
            String[] inputErrors = this.checkForInputErrors();
            if (inputErrors.length > 0) {
                String errorMessages = "Please correct the following errors:";
                for (int i = 0; i < inputErrors.length; i++) {
                    errorMessages += inputErrors[i];
                }
                System.out.println(errorMessages);
                return;
            } else {
                this.currentCredentials = new AWSCredentials(
                        this.getAccessKey(),
                        this.getSecretKey(),
                        "default nick");
            }
            if (this.currentCredentials == null) {
                System.out.println("currentCredentials is empty");
                return;
            }
            //do some login initiating stuff here
            
            System.out.println("listing bucket direct login, please wait");
        }
        
        //close the window
        try {
            clearPassword();
            //let our app know it
            this.mainApp.getLoginAwsCredentialsMap().put(currentCredentials.getFriendlyName(), (AWSCredentials) currentCredentials);
            //let combobox knows it
            this.mainApp.getMainController().getNickLists().add(currentCredentials.getFriendlyName());
            this.mainApp.getMainController().getCbSwitchAccount().setValue(currentCredentials.getFriendlyName());
            //logout button enabled
            this.mainApp.getMainController().getBtnLogout().setDisable(false);
            
            this.mainApp.getS3m().setAwsCredentials((AWSCredentials) currentCredentials);
            System.out.println("Listing Buckets");
            S3Bucket[] buckets = this.mainApp.getS3m().listBuckets();

            for (S3Bucket b : buckets) {
                System.out.println(b.getName());
            }
            this.mainApp.getLoginStage().close();
        } catch (S3ServiceException ex) {
            System.out.println("Unable to connect to S3");
        } catch (IOException ex) {
            Logger.getLogger(LoginViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML private void handleChooseFolderAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser(); 
        directoryChooser.setTitle("Please choose a folder");
        
        File file = directoryChooser.showDialog(null);
        if(file != null) {
            this.homeFolder = file;
            tfHomeFolder.setText(file.getAbsolutePath());
            refreshStoredCredentials();
        }
    }
    
    @FXML private void handleCancelAction() {
        try {
            this.mainApp.getLoginStage().close();
        } catch (IOException ex) {
            Logger.getLogger(LoginViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

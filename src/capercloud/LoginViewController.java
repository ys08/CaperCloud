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
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.StorageService;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class LoginViewController implements Initializable {
    private CaperCloud mainApp;
    Jets3tProperties myProperties = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
    
    private ObservableList<File> fileList = FXCollections.observableArrayList();
    
    //store access key and private key
    private ProviderCredentials credentials = null;
    
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

    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }

    public ProviderCredentials getCredentials() {
        return credentials;
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
       //initiate default home folder
       if (Constants.DEFAULT_PREFERENCES_DIRECTORY.exists()) {
            this.homeFolder = Constants.DEFAULT_PREFERENCES_DIRECTORY;
        } else {
            this.homeFolder = new File(System.getProperty("user.home"));
        }
       tfHomeFolder.setText(homeFolder.getAbsolutePath());
       refreshStoredCredentialsTable();
    }    
    
    protected StorageService getStorageService() throws S3ServiceException
    {
        return new RestS3Service(credentials);
    }
    
    private void retrieveCredentialsFromDirectory(File directory, File credentialsFile, String password) {
        if (!validFolderInputs(false, directory, credentialsFile, password, true)) {
            return;
        }
        try {
            this.credentials = ProviderCredentials.load(password, credentialsFile);
            //When user click login, we should close the window if success
            System.out.println(credentials);
            mainApp.getLoginStage().close();
        } catch (Exception e) {
            String message = "Unable to load your credentials from the file: "
                + credentialsFile + "Please check your password.";
            System.out.println(message);
        }
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
    
    /**
     * Refreshes the table of stored credentials by finding <tt>*.enc</tt> files in the
     * directory specified as the Cockpit home folder.
     *
     */
    public void refreshStoredCredentialsTable() {
        fileList.clear();
        try {
            File[] files = homeFolder.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                File candidateFile = files[i];
                if (candidateFile.getName().endsWith(".enc")) {
                    // Load partial details from credentials file.
                    ProviderCredentials credentials = ProviderCredentials.load(null, candidateFile);
                    System.out.println(credentials);
                    fileList.add(candidateFile);
                }
            }
            lvCredentialsFile.setItems(fileList);
        } catch (Exception e) {
            String message = "Unable to find credential files in the folder "
                + homeFolder.getAbsolutePath();
            System.out.println(message);
        }
    }
    
    private void storeCredentialsInDirectory(File directory, String password) {
        if (!validFolderInputs(true, directory, null, password, false)) {
            return;
        }
        if (EMPTY_PASSWORD_SURROGATE.equals(password.trim())) {
            password = "";
        }
        this.mainApp.showAccountManagerView();
        //get credentials from AccountManagerView
        ProviderCredentials myCredentials = this.mainApp.getAccountController().getCredentials();
        if (myCredentials == null) {
            return;
        }
        if (myCredentials.getFriendlyName() == null || myCredentials.getFriendlyName().length() == 0) {
            String message = "You must enter a nickname when storing your credentials";
            System.out.println(message);
            return;
        }

        File credentialsFile = new File(directory, myCredentials.getFriendlyName() + ".enc");

        try {
            String algorithm = myProperties.getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
            myCredentials.save(password, credentialsFile, algorithm);
            this.clearPassword();
            this.refreshStoredCredentialsTable();

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
     * @return
     * the folder chosen by the user as their Cockpit home.
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
    
    //connect to S3
    protected RestS3Service getRestS3Service(ProviderCredentials credentials) throws S3ServiceException {
        return new RestS3Service(credentials, CaperCloud.APPLICATION_DESCRIPTION,
            new BasicCredentialsProvider(), mainApp.getCaperCloudProperties());
    }
    
    @FXML private void handleSaveAction() {
        storeCredentialsInDirectory(this.getHomeFolder(), this.getPassword());
    }
    
    @FXML private void handleLoginAction() {
        loginMode = tpLogin.getSelectionModel().getSelectedIndex();
        if (loginMode == LOGIN_MODE_LOCAL_FOLDER) {
            retrieveCredentialsFromDirectory(this.getHomeFolder(), this.getCredentialsFile(), this.getPassword());
            if (this.credentials != null) {
                //do login here
                this.mainApp.getMainController().getNickLists().add(this.credentials.getFriendlyName());
                this.mainApp.getMainController().getCbSwitchAccount().setValue(this.credentials.getFriendlyName());
                System.out.println("listing bucket local folder, please wait");
            }
        } else if (loginMode == LOGIN_MODE_DIRECT) {
            //direct login
            String[] inputErrors = this.checkForInputErrors();
            if (inputErrors.length > 0) {
                String errorMessages = "Please correct the following errors:";
                for (int i = 0; i < inputErrors.length; i++) {
                    errorMessages += inputErrors[i];
                }
                System.out.println(errorMessages);
            } else {
                this.credentials = new AWSCredentials(
                        this.getAccessKey(),
                        this.getSecretKey(),
                        "default_nick");
                    }
            try {
                this.mainApp.getMainController().getNickLists().add("haha");
                //combobox value
                this.mainApp.getLoginStage().close();
                
                if (this.credentials != null) {
                    //do some login here
                    System.out.println("listing bucket direct login, please wait");
                }
            } catch (IOException ex) {
                Logger.getLogger(LoginViewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @FXML private void handleChooseFolderAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser(); 
        directoryChooser.setTitle("Please choose a folder");
        
        File file = directoryChooser.showDialog(null);
        if(file != null) {
            this.homeFolder = file;
            tfHomeFolder.setText(file.getAbsolutePath());
            refreshStoredCredentialsTable();
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

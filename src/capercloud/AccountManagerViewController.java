/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.s3.S3Manager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.security.AWSCredentials;

/**
 * store credentials to mainApp.loginAwsCredentialsMap
 *
 * @author shuai
 */
public class AccountManagerViewController implements Initializable {
    private Log log = LogFactory.getLog(getClass());
    LoginViewController parentController;
    private Stage me;
    
    @FXML TextField tfNickname;
    @FXML TextField tfAccessKey;
    @FXML TextField tfSecretKey;
    
    public void setParentController(LoginViewController parentController) {
        this.parentController = parentController;
        try {
            this.me = this.parentController.getNewAccountStage();
        } catch (IOException ex) {
            Logger.getLogger(AccountManagerViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }
    
    private void clear() {
        tfAccessKey.setText("");
        tfSecretKey.setText("");
        tfNickname.setText("");
    }
    /**
     * check if nickname(friendly name) already exists
     */
    @FXML private void handleOkAction() {
        if (tfAccessKey.getText().equals("") || tfSecretKey.getText().equals("") || tfNickname.getText().equals("")) {
            log.warn("All fields are needed");
            clear();
            return;
        }
        AWSCredentials credentials = new AWSCredentials(
                tfAccessKey.getText(),
                tfSecretKey.getText(),
                tfNickname.getText()
        );
        
//save the credentials to file
        File credentialsFile = new File(this.parentController.getHomeFolder(), credentials.getFriendlyName() + ".enc");
        
//        String algorithm = S3Manager.jets3tProperties.getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
        String algorithm = "PBEWithMD5AndDES";
        try {
            credentials.save(this.parentController.getPassword(), credentialsFile, algorithm);
        } catch (Exception e) {
            log.error(e.getMessage());
            clear();
            return;
        }
        this.parentController.updateCredentialsFiles();   
            
        clear();
        log.info("File " + credentialsFile.getName() + " is created in " + credentialsFile.getPath() + ", it is encrypted by " + algorithm);
        this.me.close();
    }
    
    @FXML private void handleCancelAction() {
        clear();
        this.me.close();
    }
}

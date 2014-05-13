/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import org.jets3t.service.security.AWSCredentials;

/**
 * store credentials to mainApp.loginAwsCredentialsMap
 *
 * @author shuai
 */
public class AccountManagerViewController implements Initializable {
    private CaperCloud mainApp;
    
    @FXML TextField tfNickname;
    @FXML TextField tfAccessKey;
    @FXML TextField tfSecretKey;
    

    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }
    
    private void clear() {
        tfAccessKey.setText("");
        tfSecretKey.setText("");
        tfNickname.setText("");

    }
    
    @FXML private void handleOkAction() {
        if (tfAccessKey.getText().equals("") || tfSecretKey.getText().equals("") || tfNickname.getText().equals("")) {
            System.out.println("All fields are needed");
            clear();
            return;
        }
        //nickname must unique
        if (this.mainApp.getLoginAwsCredentialsMap().containsKey(tfNickname.getText())) {
            System.out.println("already have nickname:" + tfNickname.getText());
            clear();
            return;
        }
        
        AWSCredentials credentials = new AWSCredentials(
                tfAccessKey.getText(),
                tfSecretKey.getText(),
                tfNickname.getText()
        );
        
        this.mainApp.getLoginController().setCredentials(credentials);
        
        try {
            clear();
            this.mainApp.getNewAccountStage().close();
        } catch (IOException ex) {
            Logger.getLogger(AccountManagerViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML private void handleCancelAction() {
        this.mainApp.getLoginController().setCredentials(null);
        clear();
        try {
            this.mainApp.getNewAccountStage().close();
        } catch (IOException ex) {
            Logger.getLogger(AccountManagerViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

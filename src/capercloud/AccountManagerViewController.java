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
import org.jets3t.service.security.ProviderCredentials;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class AccountManagerViewController implements Initializable {
    private CaperCloud mainApp;
    private ProviderCredentials credentials;
    
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
        // TODO
    }   

    public ProviderCredentials getCredentials() {
        return credentials;
    }
    
    private void clear() {
        tfAccessKey.setText("");
        tfSecretKey.setText("");
        tfNickname.setText("");
    }
    
    @FXML private void handleOkAction() {
        this.credentials = new AWSCredentials(
                tfAccessKey.getText(),
                tfSecretKey.getText(),
                tfNickname.getText()
        );
        try {
            clear();
            this.mainApp.getNewAccountStage().close();
        } catch (IOException ex) {
            Logger.getLogger(AccountManagerViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @FXML private void handleCancelAction() {
        clear();
        try {
            this.mainApp.getNewAccountStage().close();
        } catch (IOException ex) {
            Logger.getLogger(AccountManagerViewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.concurrent.Service;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Bucket;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class TextFieldViewController implements Initializable {
    private Log log = LogFactory.getLog(getClass());
    private CaperCloud mainApp;
    private Stage me;
    
    @FXML TextField tfInput;
    @FXML Button btnOK;
    @FXML Button btnCancel;
    
    public void setStage(Stage me) {
        this.me = me;
    }
    
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
        this.tfInput.setText("");
    }
    
    @FXML private void handleOkAction() {
        if (tfInput.getText().equals("")) {
            return;
        }
        String message = "Creating Bucket " + tfInput.getText() + " ...";
        Stage dialog = this.mainApp.createProgressDialog("Creating Bucket", message, me);
        Service<S3Bucket> s = this.mainApp.getCloudManager().createCreateBucketService(tfInput.getText(), dialog);
        s.start();
        
        dialog.showAndWait();
        
        if (State.SUCCEEDED == s.getState()) {
            S3Bucket res = s.getValue();
            this.mainApp.getMainController().getRemoteBucketCache().add(res);
        } else {
            if (State.CANCELLED != s.getState()) {
                log.debug(s.getState());
                s.cancel();
                log.debug(s.getState());
            }
        }
        clear();
        this.me.close();
    }
    @FXML private void handleCancelAction() {
        clear();
        this.me.close();
    }
    
}

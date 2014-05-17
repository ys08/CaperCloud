/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class ProgressViewController implements Initializable {
    private Log log = LogFactory.getLog(getClass());
    Stage me;
    
    @FXML private Label title;
    @FXML private ProgressBar pbProgress;
    @FXML private Button btnCancel;
    
    public void setStage(Stage me) {
        this.me = me;
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pbProgress.setProgress(-1);
    } 
    
    public void displayMessage(String message) {
        this.title.setText(message);
    }
    
    @FXML
    private void handleCancelAction() {
        log.debug("Canceled by user");
        this.me.close();
    }
}

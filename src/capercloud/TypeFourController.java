/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import org.jets3t.service.model.S3Object;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class TypeFourController implements Initializable {
    private CaperCloud mainApp;
    
    private S3Object selectedObj;
    
    @FXML private TextField tfVcf;
    @FXML private TextField tfFdr;

    public TypeFourController() {
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
    
    @FXML
    private void handelImportAction() {
        this.selectedObj = this.mainApp.getMainController().getSelectedObject();
        if (this.selectedObj == null) {
            return;
        }
        this.tfVcf.setText(this.selectedObj.getName());
    }
    
    public String getFdr() {
        return this.tfFdr.getText();
    }
}

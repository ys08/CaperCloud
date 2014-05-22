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
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import org.jets3t.service.model.S3Object;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class TypeFourController implements Initializable {
    private CaperCloud mainApp;
    private ObservableList ol;
    
    private S3Object selectedObj;
    
    @FXML private ListView lvPath;

    public TypeFourController() {
        this.ol = FXCollections.observableArrayList();
    }
    
    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        lvPath.setItems(ol);
    }    
    
    @FXML
    private void handelImportAction() {
        this.selectedObj = this.mainApp.getMainController().getSelectedObject();
        if (this.selectedObj == null) {
            return;
        }
        this.ol.clear();
        String uri = "s3n://" + this.selectedObj.getBucketName() + "/" + this.selectedObj.getName();
        this.ol.add(uri);
    }
    
    public String getDatabaseURI() {
        if (this.ol.isEmpty()) {
            return null;
        }
        return (String) this.ol.get(0);
    }
    
    public String getDatabaseName() {
        if (this.selectedObj == null) {
            return null;
        }
        return this.selectedObj.getName();
    }
}

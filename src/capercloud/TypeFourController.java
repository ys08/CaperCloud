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
import javafx.util.Callback;
import org.jets3t.service.model.S3Object;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class TypeFourController implements Initializable {
    private CaperCloud mainApp;
    private ObservableList<S3Object> ol;
    
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
        lvPath.setCellFactory(new Callback<ListView<S3Object>, ListCell<S3Object>>() {
            @Override
            public ListCell<S3Object> call(ListView<S3Object> p) {
                return new ListCell<S3Object>() {
                    @Override
                    protected void updateItem(S3Object t, boolean bln) {
                        super.updateItem(t, bln); //To change body of generated methods, choose Tools | Templates.
                        if (t != null) {
                            setText(t.getBucketName() + ":" + t.getName());
                        }
                    }
                    
                };
            }
        });
        lvPath.setItems(ol);
    }    
    
    public S3Object getDatabaseObj() {
        if (this.ol.isEmpty()) {
            return null;
        }
        return this.ol.get(0);
    }
    
    @FXML
    private void handelImportAction() {
        this.selectedObj = this.mainApp.getMainController().getSelectedObject();
        if (this.selectedObj == null) {
            return;
        }
        this.ol.clear();
        this.ol.add(this.selectedObj);
    }
}

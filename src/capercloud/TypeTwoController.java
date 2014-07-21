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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author shuai
 */
public class TypeTwoController implements Initializable {
    
    private ObservableList<String> items = FXCollections.observableArrayList("Ensembl Missense SNV");
    
    @FXML ComboBox cbDatabase;
    @FXML TextField tfFdr;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbDatabase.setItems(items);
        cbDatabase.getSelectionModel().selectFirst();
    }    
    
    public String getFdr() {
        return tfFdr.getText();
    }
}

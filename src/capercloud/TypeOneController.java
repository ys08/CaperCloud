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
public class TypeOneController implements Initializable {

    /**
     * Initializes the controller class.
     */
    private ObservableList<String> chromNumList;
    
    @FXML ComboBox cbChromNum;
    @FXML TextField tfFdr;
    
    public TypeOneController() {
        this.chromNumList = FXCollections.observableArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y");
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        this.cbChromNum.setItems(chromNumList);
    }    
    
    public String getSelectedChromosomeNumber() {
        return (String) this.cbChromNum.getSelectionModel().getSelectedItem();
    }
    
    public String getFdr() {
        return this.tfFdr.getText();
    }
}

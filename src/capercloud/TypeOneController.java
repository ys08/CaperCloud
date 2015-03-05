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
import javafx.scene.control.CheckBox;
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
    @FXML CheckBox cbFilter;
    
    public TypeOneController() {
        this.chromNumList = FXCollections.observableArrayList("Chr 1 (6-frame translation)", "Chr 2 (6-frame translation)", "Chr 3 (6-frame translation)", "Chr 4 (6-frame translation)", "Chr 5 (6-frame translation)", "Chr 6 (6-frame translation)", "Chr 7 (6-frame translation)", "Chr 8 (6-frame translation)", "Chr 9 (6-frame translation)", "Chr 10 (6-frame translation)", "Chr 11 (6-frame translation)", "Chr 12 (6-frame translation)", "Chr 13 (6-frame translation)", "Chr 14 (6-frame translation)", "Chr 15 (6-frame translation)", "Chr 16 (6-frame translation)", "Chr 17 (6-frame translation)", "Chr 18 (6-frame translation)", "Chr 19 (6-frame translation)", "Chr 20 (6-frame translation)", "Chr 21 (6-frame translation)", "Chr 22 (6-frame translation)", "Chr X (6-frame translation)", "Chr Y (6-frame translation)");
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        this.cbChromNum.setItems(chromNumList);
    }    
    
    public String getSelectedChromosomeNumber() {
        int i = this.cbChromNum.getSelectionModel().getSelectedIndex();
        String chrom = null;
        if (i < 22) {
            chrom = Integer.toString(i+1);   
        } else if (i == 22) {
            chrom = "X";
        } else {
            chrom = "Y";
        }
        System.out.println(chrom);
        return chrom;
    }
    
    public String getFdr() {
        return this.tfFdr.getText();
    }
    
    public boolean isFilterSelected() {
        return this.cbFilter.isSelected();
    }
}

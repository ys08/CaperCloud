/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

/**
 *
 * @author shuai
 */
public class PeptideModel {
    private StringProperty peptideId;
    private StringProperty peptideSeq;
    private StringProperty genomicLocation;
    private StringProperty modification;
    private ObservableList<SpectrumModel> psmList;
    
    public PeptideModel(String peptideId, String peptideSeq, String genomicLocation, String modification) {
        this.peptideId = new SimpleStringProperty(peptideId);
        this.peptideSeq = new SimpleStringProperty(peptideSeq);
        this.genomicLocation = new SimpleStringProperty(genomicLocation);
        this.modification = new SimpleStringProperty(modification);
    }
    
    public StringProperty peptideIdProperty() {
        return this.peptideId;
    }
    
    public StringProperty peptideSeqProperty() {
        return this.peptideSeq;
    }
    
    public StringProperty genomicLocationProperty() {
        return this.genomicLocation;
    }
    
    public StringProperty modificationProperty() {
        return this.modification;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author shuai
 */
public class PeptideModel {
    private String id;
    
    private StringProperty chrom;
    private StringProperty peptideSeq;
    private StringProperty proteinStart;
    private StringProperty proteinEnd;
    private StringProperty peptideStart;
    private StringProperty peptideEnd;
    private StringProperty modifications;
    
    public PeptideModel(String id, String chrom, String peptideSeq, String proteinStart, String proteinEnd, String peptideStart, String peptideEnd, String modifications) {
        this.id = id;
        this.chrom = new SimpleStringProperty(chrom);
        this.peptideSeq = new SimpleStringProperty(peptideSeq);
        this.proteinStart = new SimpleStringProperty(proteinStart);
        this.proteinEnd = new SimpleStringProperty(proteinEnd);
        this.peptideStart = new SimpleStringProperty(peptideStart);
        this.peptideEnd = new SimpleStringProperty(peptideEnd);
        this.modifications = new SimpleStringProperty(modifications);
    }

    public String getId() {
        return id;
    }

    public StringProperty chromProperty() {
        return chrom;
    }

    public StringProperty peptideSeqProperty() {
        return peptideSeq;
    }

    public StringProperty proteinStartProperty() {
        return proteinStart;
    }

    public StringProperty proteinEndProperty() {
        return proteinEnd;
    }

    public StringProperty peptideStartProperty() {
        return peptideStart;
    }

    public StringProperty peptideEndProperty() {
        return peptideEnd;
    }

    public StringProperty modificationsProperty() {
        return modifications;
    }
    
}

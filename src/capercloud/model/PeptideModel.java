/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author shuai
 */
public class PeptideModel {
    private String peptideRef;
    
    private StringProperty id;
    private StringProperty chrom;
    private StringProperty strand;
    private StringProperty peptideSeq;
    private StringProperty modifications;
    private StringProperty description;
    
    private ArrayList<Range> regions;
    
    public PeptideModel(String peptideRef, String id, String chrom, String strand, String modifications, String peptideSeq, String description) {
        this.peptideRef = peptideRef;
        this.id = new SimpleStringProperty(id);
        this.chrom = new SimpleStringProperty(chrom);
        this.strand = new SimpleStringProperty(strand);
        this.peptideSeq = new SimpleStringProperty(peptideSeq);
        this.modifications = new SimpleStringProperty(modifications);
        this.description = new SimpleStringProperty(description);
        
        this.regions = new ArrayList<>();
    }

    public StringProperty idProperty() {
        return id;
    }

    public StringProperty chromProperty() {
        return chrom;
    }
    
    public StringProperty strandProperty() {
        return this.strand;
    }

    public StringProperty peptideSeqProperty() {
        return peptideSeq;
    }

    public StringProperty modificationsProperty() {
        return modifications;
    }
        
    public StringProperty descriptionProperty() {
        return this.description;
    }
    
    public void addRegions(Range r) {
        this.regions.add(r);
    }

    public ArrayList<Range> getRegions() {
        return regions;
    }

    public String getPeptideRef() {
        return peptideRef;
    }
    
    public Range getUcscRange() {
        if (this.regions.size() == 1) {
            return regions.get(0);
        }
        
        Range leftRange = this.regions.get(0);
        Range rightRange = this.regions.get(this.regions.size()-1);
        int leftStart = leftRange.getStartPos();
        int leftEnd = leftRange.getEndPos();
        int rightStart = rightRange.getStartPos();
        int rightEnd = rightRange.getEndPos();
        
        if (leftStart < rightStart) {
            return new Range(leftStart, rightEnd);
        } else {
            return new Range(rightStart, leftEnd);
        }
    }
    
    public Range getParentRange() {
        int size = this.regions.size();
        if (size == 1) {
            return this.regions.get(0);
        } else {
            Range rf = this.regions.get(0);
            Range rl = this.regions.get(size - 1);
            if ("1".equals(strand.get())) {
                return new Range(rf.getStartPos(), rl.getEndPos());
            } else {
                return new Range(rl.getStartPos(), rf.getEndPos());
            }
        }
    }
}

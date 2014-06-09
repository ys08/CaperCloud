/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import com.compomics.util.experiment.biology.PTM;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author shuai
 */
public class ModificationTableModel {
    private PTM ptm;
    private StringProperty name;
    private DoubleProperty mass;
    private BooleanProperty isFixed;
    private BooleanProperty isVariant;
    
    public ModificationTableModel(PTM ptm) {
        this.name = new SimpleStringProperty(ptm.getName());
        this.mass = new SimpleDoubleProperty(ptm.getMass());
        this.isFixed = new SimpleBooleanProperty(false);
        this.isVariant = new SimpleBooleanProperty(false);
    }

    public PTM getPtm() {
        return ptm;
    }
    
    public StringProperty nameProperty() {
        return this.name;
    }
    public DoubleProperty massProperty() {
        return this.mass;
    }
    public BooleanProperty isFixedProperty() {
        return this.isFixed;
    }
    public BooleanProperty isVariantProperty() {
        return this.isVariant;
    }
}

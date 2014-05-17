/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.jets3t.service.model.S3Object;

/**
 *
 * @author shuai
 */
public class InputCloudTableModel {
    private BooleanProperty selected;
    private S3Object obj;
    
    public InputCloudTableModel(S3Object obj) {
        this.obj = obj;
        this.selected = new SimpleBooleanProperty(true);
    }

    public void setSelected(SimpleBooleanProperty selected) {
        this.selected = selected;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public S3Object getObj() {
        return obj;
    }

    public void setObj(S3Object obj) {
        this.obj = obj;
    }
    
    public String getName() {
        return this.obj.getName();
    }
}

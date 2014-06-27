/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.JobOverviewController;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;
import org.jets3t.service.model.S3Object;

/**
 *
 * @author shuai
 */
public class InputObjectModel {
    private BooleanProperty selected;
    private S3Object obj;
    
    public InputObjectModel(S3Object obj) {
        this.obj = obj;
        this.selected = new SimpleBooleanProperty(false);

    }
    
    public void addListener(TextField tfNum) {
        this.selected.addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                if (t1) {
                    String str = tfNum.getText();
                    int num = Integer.parseInt(str);
                    num = num + 1;
                    tfNum.setText(Integer.toString(num));
                }
                if (t) {
                    String str = tfNum.getText();
                    int num = Integer.parseInt(str);
                    num = num - 1;
                    tfNum.setText(Integer.toString(num));
                }
            }
        });
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

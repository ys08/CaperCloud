/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceBlockDeviceMapping;
import java.text.SimpleDateFormat;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author shuai
 */
public class InstanceModel {
    private Instance instance;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
    
    private StringProperty instanceId;
    private StringProperty imageId;
    private StringProperty stateName;
    private StringProperty instanceType;
    private StringProperty platform;
    private StringProperty architecture;
    private StringProperty rootDevice;
    private StringProperty keyName;
    private StringProperty launchTime;
    private StringProperty availabilityZone;
    private StringProperty blockDevice;
    
    public InstanceModel(Instance instance) {
        this.instance = instance;
    }
    
    public StringProperty instanceIdProperty() {
        return new SimpleStringProperty(instance.getInstanceId());
    }
    
    public StringProperty imageIdProperty() {
        return new SimpleStringProperty(instance.getImageId());
    }
    
    public StringProperty stateNameProperty() {
        return new SimpleStringProperty(instance.getState().getName());
    }
    
    public StringProperty instanceTypeProperty() {
        return new SimpleStringProperty(instance.getInstanceType());
    }
    
    public StringProperty platformProperty() {
        return new SimpleStringProperty(instance.getHypervisor());
    }
    
    public StringProperty architectureProperty() {
        return new SimpleStringProperty(instance.getArchitecture());
    }
    
    public StringProperty rootDeviceProperty() {
        return new SimpleStringProperty(instance.getRootDeviceName() + " " + instance.getRootDeviceType());
    }
    
    public StringProperty keyNameProperty() {
        return new SimpleStringProperty(instance.getKeyName());
    }
    
    public StringProperty launchTimeProperty() {
        return new SimpleStringProperty(df.format(instance.getLaunchTime().getTime()));
    }
    
    public StringProperty availabilityZoneProperty() {
        return new SimpleStringProperty(instance.getPlacement().getAvailabilityZone());
    }
    
    public StringProperty blockDeviceProperty() {
        StringBuilder sb = new StringBuilder();
        for (InstanceBlockDeviceMapping i : instance.getBlockDeviceMappings()) {
            sb.append("|").append(i.getEbs().getVolumeId());
        }
        return new SimpleStringProperty(sb.toString());
    }
}

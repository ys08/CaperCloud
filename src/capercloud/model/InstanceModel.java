/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import java.text.SimpleDateFormat;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author shuai
 */
public class InstanceModel {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
    
    private StringProperty instanceId;
    private StringProperty imageId;
    private StringProperty stateName;
    private StringProperty instanceType;
    private StringProperty publicIp;
    private StringProperty architecture;
    private StringProperty rootDevice;
    private StringProperty keyName;
    private StringProperty launchTime;
    private StringProperty availabilityZone;
    private StringProperty securityGroup;
    
    public InstanceModel(Instance instance) {
        this.instanceId = new SimpleStringProperty(instance.getInstanceId());
        this.imageId = new SimpleStringProperty(instance.getImageId());
        this.stateName = new SimpleStringProperty(instance.getState().getName());
        this.instanceType = new SimpleStringProperty(instance.getInstanceType());
        this.publicIp = new SimpleStringProperty(instance.getPublicIpAddress());
        this.architecture = new SimpleStringProperty(instance.getArchitecture());
        this.rootDevice = new SimpleStringProperty(instance.getRootDeviceType() + ":" + instance.getRootDeviceName());
        this.keyName = new SimpleStringProperty(instance.getKeyName());
        this.launchTime = new SimpleStringProperty(df.format(instance.getLaunchTime().getTime()));
        this.availabilityZone = new SimpleStringProperty(instance.getPlacement().getAvailabilityZone());
        StringBuilder sb = new StringBuilder();
        List<GroupIdentifier> groupIds = instance.getSecurityGroups();
        for (GroupIdentifier gi : groupIds) {
            sb.append(gi.getGroupName()).append(",");
        }
        this.securityGroup = new SimpleStringProperty(sb.toString());
    }
    
    public StringProperty instanceIdProperty() {
        return this.instanceId;
    }
    
    public StringProperty imageIdProperty() {
        return this.imageId;
    }
    
    public StringProperty stateNameProperty() {
        return this.stateName;
    }
    
    public StringProperty instanceTypeProperty() {
        return this.instanceType;
    }
    
    public StringProperty platformProperty() {
        return this.publicIp;
    }
    
    public StringProperty architectureProperty() {
        return this.architecture;
    }
    
    public StringProperty rootDeviceProperty() {
        return this.rootDevice;
    }
    
    public StringProperty keyNameProperty() {
        return this.keyName;
    }
    
    public StringProperty launchTimeProperty() {
        return this.launchTime;
    }
    
    public StringProperty availabilityZoneProperty() {
        return this.availabilityZone;
    }
    
    public StringProperty blockDeviceProperty() {
        return this.securityGroup;
    }
    
    public void setState(String stateName) {
        this.stateName.setValue(stateName);
    }
}

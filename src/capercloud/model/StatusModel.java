/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author shuai
 */
public class StatusModel {
    private ObservableList<CloudJob> jobs;
    private ObservableList<InstanceModel> instancesCache;
    
    public StatusModel() {
        this.jobs = FXCollections.observableArrayList();
        this.instancesCache = FXCollections.observableArrayList();
    }

    public ObservableList<InstanceModel> getInstancesCache() {
        return instancesCache;
    }

    public void addJob(CloudJob job) {
        this.jobs.add(job);
    }
    
    public void addInstance(InstanceModel im) {
        this.instancesCache.add(im);
    }

    public ObservableList<CloudJob> getJobs() {
        return jobs;
    }
    
    public void refreshInstanceList(List<Instance> res) {
        this.instancesCache.clear();
        for (Instance i : res) {
            System.out.println(i.getInstanceId());
            this.instancesCache.add(new InstanceModel(i));
        }
    }
    
}

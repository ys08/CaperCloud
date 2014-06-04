/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

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
    
    
}

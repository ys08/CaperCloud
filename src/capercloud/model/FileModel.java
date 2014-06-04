/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.s3.S3Manager;
import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 *
 * @author shuai
 */
public class FileModel {
    private final S3Manager s3m = null;
    
//current local folder and remote bucket
    private File folderPath;
    private S3Bucket bucketPath;
    
    private ObservableList<File> localCachedFileList;
    private ObservableList<S3Bucket> remoteCachedBucketList;
    private ObservableList<S3Object> remoteCachedObjectList;
    private ObservableList<DataTransferTask> dataTransferTaskList;

    public FileModel() {
        this.folderPath = new File(System.getProperty("user.home"));
        this.localCachedFileList = FXCollections.observableArrayList();
        this.remoteCachedBucketList = FXCollections.observableArrayList();
        this.remoteCachedObjectList = FXCollections.observableArrayList();
        this.dataTransferTaskList = FXCollections.observableArrayList();
    }
    
    //getters and setters
    public ObservableList<File> getLocalCachedFileList() {
        return localCachedFileList;
    }

    public void setLocalCachedFileList(File[] files) {
        this.localCachedFileList.clear();
        this.localCachedFileList.addAll(files);
    }

    public void setRemoteCachedBucketList(S3Bucket[] buckets) {
        this.remoteCachedBucketList.clear();
        this.remoteCachedBucketList.addAll(buckets);
    }

    public void setRemoteCachedObjectList(S3Object[] objs) {
        this.remoteCachedObjectList.clear();
        this.remoteCachedObjectList.addAll(objs);
    }

    public ObservableList<S3Bucket> getRemoteCachedBucketList() {
        return remoteCachedBucketList;
    }

    public ObservableList<S3Object> getRemoteCachedObjectList() {
        return remoteCachedObjectList;
    }

    public ObservableList<DataTransferTask> getDataTransferTaskList() {
        return dataTransferTaskList;
    }

    public File getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(File folderPath) {
        this.folderPath = folderPath;
    }

    public S3Bucket getBucketPath() {
        return bucketPath;
    }

    public void setBucketPath(S3Bucket bucketPath) {
        this.bucketPath = bucketPath;
    }

    public void addDataTransferTask(DataTransferTask task) {
        this.dataTransferTaskList.add(task);
    }
    
    public void clearDataTransferTask() {
        this.dataTransferTaskList.clear();
    }
    
    public void clear() {
        this.bucketPath = null;
        this.remoteCachedBucketList.clear();
        this.remoteCachedObjectList.clear();
    }
    
    public void deleteBucket(S3Bucket bucket) {
        this.remoteCachedBucketList.remove(bucket);
    }
    
    public void addBucket(S3Bucket bucket) {
        this.remoteCachedBucketList.add(bucket);
    }
    public void deleteObject(S3Object obj) {
        this.remoteCachedObjectList.remove(obj);
    }
}

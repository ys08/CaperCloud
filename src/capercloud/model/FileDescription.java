/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.util.Date;

/**
 *
 * @author shuai
 */
public class FileDescription {
    private String filename;
    private long filesize;
    private Date modifiedTime;
    private Date uploadTime;
    
    public FileDescription() {
        
    }
    public FileDescription(String filename, long filesize, Date modifiedTime, Date uploadTime) {

        this.filename = filename;
        this.filesize = filesize;
        this.modifiedTime = modifiedTime;
        this.uploadTime = uploadTime;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }
    
}

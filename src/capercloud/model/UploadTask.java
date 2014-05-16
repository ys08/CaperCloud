/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.s3.S3Manager;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.security.AWSCredentials;

/**
 *
 * @author shuai
 */
public class UploadTask extends DataTransferTask {
    private File uploadFile;
    private S3Bucket toBucket;
    private String transferType = "Upload";
    private AWSCredentials currentCredentials;
    private S3Manager s3m;

    public UploadTask(File uploadFile, S3Bucket toBucket, AWSCredentials currentCredentials) {
        this.uploadFile = uploadFile;
        this.toBucket = toBucket;
        this.currentCredentials = currentCredentials;
    }
    
    private void runUploadTask() throws ServiceException, IOException, NoSuchAlgorithmException {
        this.s3m = new S3Manager(this.currentCredentials, this);
        s3m.uploadFile(uploadFile, toBucket);
    }

    @Override
    public String getTransferType() {
        return this.transferType;
    }

    @Override
    public String getFilename() {
        return this.uploadFile.getName();
    }

    @Override
    public String getFrom() {
        return this.uploadFile.getPath();
    }

    @Override
    public String getTo() {
        return this.toBucket.getName();
    }

    @Override
    protected Void call() throws Exception {
        this.runUploadTask();
        return null;
    }
    
}

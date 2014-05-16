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
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;


/**
 *
 * @author shuai
 */
public class DownloadTask extends DataTransferTask{
    private S3Object downloadS3Object;
    private File toFile;
    private String transferType = "Download";
    private AWSCredentials currentCredentials;
    private S3Manager s3m;
    
    public DownloadTask(S3Object downloadS3Object, File toFile, AWSCredentials currentCredentials) {
        this.downloadS3Object = downloadS3Object;
        this.toFile = toFile;
        this.currentCredentials = currentCredentials;
    }

    private void runDownloadTask() throws ServiceException, IOException, NoSuchAlgorithmException {
        this.s3m = new S3Manager(this.currentCredentials, this);
        s3m.downloadObject(downloadS3Object, toFile);
    }

    @Override
    protected Void call() throws Exception {
        this.runDownloadTask();
        return null;
    }

    @Override
    public String getTransferType() {
        return this.transferType;
    }

    @Override
    public String getFilename() {
        return this.downloadS3Object.getName();
    }

    @Override
    public String getFrom() {
        return this.downloadS3Object.getBucketName();
    }

    @Override
    public String getTo() {
        return this.toFile.getPath();
    }
}

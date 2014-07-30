/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.CaperCloud;
import capercloud.s3.S3Manager;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;


/**
 *
 * @author shuai
 */
public class DownloadTask extends DataTransferTask{
    private CaperCloud mainApp;
    private S3Object downloadS3Object;
    private File toFile;
    private String transferType = "Download";
    private AWSCredentials currentCredentials;
    private S3Manager s3m;
    
    public DownloadTask(S3Object downloadS3Object, File toFile, AWSCredentials currentCredentials, CaperCloud mainApp) {
        this.downloadS3Object = downloadS3Object;
        this.toFile = toFile;
        this.currentCredentials = currentCredentials;
        try {
            this.s3m = new S3Manager(this.currentCredentials, this, mainApp);
        } catch (ServiceException ex) {
            Logger.getLogger(DownloadTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void runDownloadTask() throws ServiceException {
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

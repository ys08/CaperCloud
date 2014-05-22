/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.CaperCloud;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 *
 * @author shuai
 */
public class JobConfigs {
    private Log log = LogFactory.getLog(getClass());
    private CaperCloud mainApp; 
    private File jobType;
    private File searchOptions;
    
    private S3Bucket bucket;
    private S3Object jtObj;
    private S3Object soObj;
    
    public JobConfigs(CaperCloud mainApp, File jobType, File searchOptions) {
        this.mainApp = mainApp;
        this.jobType = jobType;
        this.searchOptions = searchOptions;
    }
    
    public void saveToS3(final S3Bucket bucket) throws NoSuchAlgorithmException, IOException, S3ServiceException {
        this.bucket = bucket;
        this.jtObj = new S3Object(this.jobType);
        this.soObj = new S3Object(this.searchOptions);
        final S3Service s3Service = new RestS3Service(this.mainApp.getCloudManager().getCurrentCredentials());
        
        Service<Void> s = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        s3Service.putObject(bucket, jtObj);
                        s3Service.putObject(bucket, soObj);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        super.succeeded(); //To change body of generated methods, choose Tools | Templates.
                        log.info("save done");
                    }
                };
            } 
        };
        s.start();
        log.info("saving please wait...");
    }
    
    public String getJobOptionsLocationInS3() {
        return this.bucket.getLocation() + this.jtObj.getName();
    }
    
    public String getSearchOptionsLocationInS3() {
        return this.bucket.getLocation() + this.soObj.getName();
    }
}

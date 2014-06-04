/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.s3.S3Manager;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

/**
 *
 * @author shuai
 */
public class CloudManager {
    private Log log = LogFactory.getLog(getClass());
    private static CloudManager singleton = null;
    private AWSCredentials currentCredentials; 
    private S3Manager s3m;
    private AmazonEC2AsyncClient ec2m;
    
    private CloudManager() {
    }
    /**
     * dan li
     * @return 
     */
    public static CloudManager getInstance() {
        if (singleton == null) {
            singleton = new CloudManager();
        }
        return singleton;
    }
    
    /**
     * get credentials that are currently used by s3 and ec2
     * @return 
     */
    public AWSCredentials getCurrentCredentials() {
        return currentCredentials;
    }
    
    public AmazonEC2AsyncClient getEc2Manager() {
        return this.ec2m;
    }
    
    /**
     * create S3Manager and EC2Manager instance
     * add credentials to hashmap
     * set it to current credentials
     * @param credentials 
     * @throws org.jets3t.service.ServiceException 
     */
    public void loginCloud(AWSCredentials credentials) throws ServiceException {
        this.currentCredentials = credentials;
        this.s3m = new S3Manager(currentCredentials);
        this.ec2m = new AmazonEC2AsyncClient(new BasicAWSCredentials(currentCredentials.getAccessKey(), currentCredentials.getSecretKey()));  
    }
    
    public void logoutCloud() {
        if (this.currentCredentials == null) {
            log.debug("Something goes wrong");
            return;
        }
        this.s3m = null;
        this.ec2m = null;
        this.currentCredentials = null;
    }

//S3
    public S3Bucket[] listBuckets() throws S3ServiceException {
        return s3m.listBuckets();
    }
    
    public S3Object[] listObjects(S3Bucket bucket) throws S3ServiceException {
        return this.s3m.getObjects(bucket.getName());
    }
    
    public S3Bucket createBucket(String bucketName) throws S3ServiceException {
        return this.s3m.createBucket(bucketName);
    }
    
    public void deleteObject(S3Object obj) throws ServiceException {
        this.s3m.deleteObject(obj);
    }
    
    public void deleteBucket(S3Bucket bucket) throws ServiceException {
        this.s3m.deleteBucket(bucket);
    }
    
    public Service<S3Bucket[]> createListBucketsService(final Stage progressDialog) {
        return new Service<S3Bucket[]>() {
            @Override
            protected Task<S3Bucket[]> createTask() {
                return new Task<S3Bucket[]>() {
                    @Override
                    protected S3Bucket[] call() throws Exception {
                        return CloudManager.this.listBuckets();
                    }
                };
            }
            
            @Override
            protected void succeeded() {
                super.succeeded();
                log.debug("Listing Buckets Success!");
                progressDialog.close();
                
            }
            
            @Override
            protected void cancelled() {
                super.cancelled(); 
                log.debug("Lising Buckets Cancelled!");
                progressDialog.close();
            }
            
            @Override
            protected void failed() {
                super.failed(); 
                log.debug("Lising Buckets Failed!");
                progressDialog.close();
            }
        };
    }
    
    public Service<S3Object[]> createListObjectsService(final S3Bucket bucket, final Stage progressDialog) {
        return new Service<S3Object[]>() {
            @Override
            protected Task<S3Object[]> createTask() {
                return new Task<S3Object[]>() {
                    @Override
                    protected S3Object[] call() throws Exception {
                        return CloudManager.this.listObjects(bucket);
                    }
                };
            }
            @Override
            protected void succeeded() {
                super.succeeded();
                log.debug("Listing Objects Success!");
                progressDialog.close();
            }
            @Override
            protected void cancelled() {
                super.cancelled(); 
                log.debug("Listing Objects Cancelled!");
                progressDialog.close();
            }
            @Override
            protected void failed() {
                super.failed(); 
                log.debug("Lising Objects Failed!");
                progressDialog.close();
            }
        };
    }
    
    public Service<S3Bucket> createCreateBucketService(final String bucketName, final Stage progressDialog) {
        return new Service<S3Bucket>() {
            @Override
            protected Task<S3Bucket> createTask() {
                return new Task<S3Bucket>() {
                    @Override
                    protected S3Bucket call() throws Exception {
                        return CloudManager.this.createBucket(bucketName);
                    }
                };
            }
            @Override
            protected void succeeded() {
                super.succeeded();
                log.debug("Create Bucket " + bucketName + " Succeeded!");
                progressDialog.close();
            }
            @Override
            protected void cancelled() {
                super.cancelled(); 
                log.debug("Create Bucket " + bucketName + " Cancelled!");
                progressDialog.close();
            }
            @Override
            protected void failed() {
                super.failed(); 
                log.debug("Create Bucket " + bucketName + " Failed!");
                progressDialog.close();
            }
        };
    }
    
    public Service<Void> createDeleteObjectService(final S3Object obj, final Stage progressDialog) {
        return new Service<Void>() {
            
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        log.debug(obj.getName());
                        CloudManager.this.deleteObject(obj);
                        return null;
                    }
                };
            }
            @Override
            protected void succeeded() {
                super.succeeded();
                log.debug(this.getState());
                progressDialog.close();
            }
            @Override
            protected void cancelled() {
                super.cancelled(); 
                log.debug(this.getState());
                progressDialog.close();
            }
            @Override
            protected void failed() {
                super.failed(); 
                log.debug(this.getState());
                progressDialog.close();
            }
        };
    }
    
    public Service<Void> createDeleteBucketService(final S3Bucket bucket, final Stage progressDialog) {
        return new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        log.debug(bucket.getName());
                        CloudManager.this.deleteBucket(bucket);
                        return null;
                    }
                };
            }
            @Override
            protected void succeeded() {
                super.succeeded();
                log.debug(this.getState());
                progressDialog.close();
            }
            @Override
            protected void cancelled() {
                super.cancelled(); 
                log.debug(this.getState());
                progressDialog.close();
            }
            @Override
            protected void failed() {
                super.failed(); 
                log.debug(this.getState());
                progressDialog.close();
            }
        };
    }
}

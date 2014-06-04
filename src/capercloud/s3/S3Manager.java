package capercloud.s3;

import capercloud.CaperCloud;
import capercloud.model.DataTransferTask;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.multi.DownloadPackage;
import org.jets3t.service.multi.StorageServiceEventListener;
import org.jets3t.service.multi.ThreadWatcher;
import org.jets3t.service.multi.event.CopyObjectsEvent;
import org.jets3t.service.multi.event.CreateBucketsEvent;
import org.jets3t.service.multi.event.CreateObjectsEvent;
import org.jets3t.service.multi.event.DeleteObjectsEvent;
import org.jets3t.service.multi.event.DownloadObjectsEvent;
import org.jets3t.service.multi.event.GetObjectHeadsEvent;
import org.jets3t.service.multi.event.GetObjectsEvent;
import org.jets3t.service.multi.event.ListObjectsEvent;
import org.jets3t.service.multi.event.LookupACLEvent;
import org.jets3t.service.multi.event.ServiceEvent;
import org.jets3t.service.multi.event.UpdateACLEvent;
import org.jets3t.service.multi.s3.ThreadedS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.utils.ByteFormatter;
import org.jets3t.service.utils.Mimetypes;

/**
 * List buckets, multiple files uploading and downloading 
 * @author Yang Shuai
 *
 */
public class S3Manager implements StorageServiceEventListener, CredentialsProvider{

    private Log log = LogFactory.getLog(getClass());
    public static Jets3tProperties jets3tProperties = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);;
    private CredentialsProvider mCredentialProvider;
    private RestS3Service s3Service;
    private ThreadedS3Service storageService;
    private final ByteFormatter byteFormatter = new ByteFormatter();
    
    private DataTransferTask transferTask;
    /**
     * 
     * @param currentCredentials
     * @throws ServiceException 
     */
    public S3Manager(AWSCredentials currentCredentials) throws ServiceException {
//constructor for listing action
        this.mCredentialProvider = new BasicCredentialsProvider();
        this.s3Service = new RestS3Service(currentCredentials, CaperCloud.APPLICATION_DESCRIPTION, this, this.jets3tProperties);
        this.storageService = new ThreadedS3Service(this.s3Service, this);
        
        this.transferTask = null;
    }
    
    public S3Manager(AWSCredentials currentCredentials, DataTransferTask transferTask) throws ServiceException {
//constructor for data transfers task
        this.mCredentialProvider = new BasicCredentialsProvider();
        this.s3Service = new RestS3Service(currentCredentials, CaperCloud.APPLICATION_DESCRIPTION, this, this.jets3tProperties);
        this.storageService = new ThreadedS3Service(this.s3Service, this);
        
        this.transferTask = transferTask;
    }

    public Jets3tProperties getJets3tProperties() {
        return jets3tProperties;
    }

    public S3Bucket[] listBuckets() throws S3ServiceException {
        return this.s3Service.listAllBuckets();
    }

    public S3Object[] getObjects(String inBucket) throws S3ServiceException {
        return this.s3Service.listObjects(inBucket);
    }
    
    public S3Bucket createBucket(String bucketName) throws S3ServiceException {
        return this.s3Service.createBucket(bucketName);
    }
    
    public void deleteObject(S3Object obj) throws ServiceException {
        this.s3Service.deleteObject(obj.getBucketName(), obj.getKey());
    }
    
    public void deleteBucket(S3Bucket bucket) throws ServiceException {
        this.s3Service.deleteBucket(bucket.getName());
    }
    
    public void uploadFile(File file, S3Bucket bucket) throws NoSuchAlgorithmException, IOException {
        S3Object obj = new S3Object(bucket, file);
        
        obj.setKey(file.getName());
        obj.setContentType(Mimetypes.getInstance().getMimetype(file));
        
        S3Object[] objs = new S3Object[1];
        objs[0] = obj;
        log.debug(file.getName() + " " + bucket.getName());
        this.storageService.putObjects(bucket.getName(), objs);
    } 
    
    public void downloadObject(S3Object obj, File folder) throws ServiceException {
        if (!folder.isDirectory()) {
            log.error(folder.getAbsolutePath() + " is not a directory!");
            return;
        }
        log.debug(obj.getName());
        DownloadPackage[] packages = new DownloadPackage[1];
        log.debug(folder.getAbsolutePath());
        File outFile = new File(folder, obj.getName());
        log.debug(outFile.getAbsolutePath());
        packages[0] = new DownloadPackage(obj, outFile);
        this.storageService.downloadObjects(obj.getBucketName(), packages);
    }
    
    @Override
    public void setCredentials(AuthScope as, Credentials c) {
        mCredentialProvider.setCredentials(as, c);
    }
    
    /**
     * Implementation method for the CredentialsProvider interface.
     * <p>
     * Based on sample code:
     * <a href="http://svn.apache.org/viewvc/jakarta/commons/proper/httpclient/trunk/src/examples/InteractiveAuthenticationExample.java?view=markup">InteractiveAuthenticationExample</a>
     *
     */
    @Override
    public Credentials getCredentials(AuthScope scope) {
        
        BufferedReader brin = new BufferedReader(new InputStreamReader(System.in));
        if (scope == null || scope.getScheme() == null) {
            return null;
        }
        Credentials credentials = mCredentialProvider.getCredentials(scope);
        if (credentials!=null){
            return credentials;
        }
        try {
            if (scope.getScheme().equals("ntlm")) {
                System.out.println("Anthentication Required");
                System.out.println("Host " + scope.getHost() + ":" + scope.getPort() +
                        " requires Windows authentication");
                System.out.print("Enter domain: ");
                String domain = brin.readLine();
                System.out.print("Enter username: ");
                String user = brin.readLine();
                System.out.print("Enter password: ");
                String password = brin.readLine();
                credentials = new NTCredentials(user, password, scope.getHost(), domain);
            } else if (scope.getScheme().equals("basic")
                    || scope.getScheme().equals("digest")) {
                //if (authscheme instanceof RFC2617Scheme) {
                System.out.println("Authentication Required");
                System.out.println("Host " + scope.getHost() + scope.getPort() +
                        " requires authentication for the realm:" + scope.getRealm());
                System.out.print("Enter username: ");
                String user = brin.readLine();
                System.out.print("Enter password: ");
                String password = brin.readLine();
                credentials = new UsernamePasswordCredentials(user, password);
            } else {
                throw new IllegalArgumentException("Unsupported authentication scheme: "
                        + scope.getScheme());
            }
            if (credentials != null){
                mCredentialProvider.setCredentials(scope, credentials);
            }
            return credentials;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        mCredentialProvider.clear();
    }

    @Override
    public void event(ListObjectsEvent loe) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void event(CreateObjectsEvent event) {
        if(ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            this.transferTask.updateMessage("starting");
            log.debug("CreateObjectsEvent Start");
        }else if(event.getEventCode() == ServiceEvent.EVENT_ERROR) {
            this.transferTask.updateMessage("Failed");
            log.debug("CreateObjectsEvent Error");
        }else if(event.getEventCode() == ServiceEvent.EVENT_IN_PROGRESS) {
            log.debug("CreateObjectsEvent In Progress");
            ThreadWatcher watcher = event.getThreadWatcher();
            if (watcher.getBytesTransferred() >= watcher.getBytesTotal()) {
                this.transferTask.updateMessage("Verifying");
            } else {
                double percentage = ((double)watcher.getBytesTransferred()) / watcher.getBytesTotal();
                this.transferTask.updateProgress(percentage, 1);
                long bytesPerSecond = watcher.getBytesPerSecond();
                StringBuilder transferDetailsText=new StringBuilder("Uploading : ");
                transferDetailsText.append(byteFormatter.formatByteSize(bytesPerSecond) + "/s");
                this.transferTask.updateMessage(transferDetailsText.toString());
            }
        }else if(ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            this.transferTask.updateMessage("completed");
            this.transferTask.updateProgress(1, 1);
            log.debug("CreateObjectsEvent Completed");
        }
    }

    @Override
    public void event(CopyObjectsEvent coe) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void event(CreateBucketsEvent cbe) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void event(DeleteObjectsEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
//            log.debug("DeleteObjectsEvent Start");
//        } else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
//            log.debug("DeleteObjectsEvent In Progress");
//        } else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
//            log.debug("DeleteObjectsEvent Completed");
//        } else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
//            log.debug("DeleteObjectsEvent Error");
//        }
    }

    @Override
    public void event(GetObjectsEvent goe) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void event(GetObjectHeadsEvent gohe) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void event(LookupACLEvent lacle) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void event(UpdateACLEvent uacle) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void event(DownloadObjectsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            this.transferTask.updateMessage("starting");
            log.debug("DownloadObjectsEvent Start");
        } else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            log.debug("DownloadObjectsEvent In Progress");
            ThreadWatcher watcher = event.getThreadWatcher();
            if (watcher.getBytesTransferred() >= watcher.getBytesTotal()) {
                this.transferTask.updateMessage("Verifying");
            } else {
                double percentage = ((double)watcher.getBytesTransferred()) / watcher.getBytesTotal();
                this.transferTask.updateProgress(percentage, 1);
                long bytesPerSecond = watcher.getBytesPerSecond();
                StringBuilder transferDetailsText=new StringBuilder("Downloading : ");
                transferDetailsText.append(byteFormatter.formatByteSize(bytesPerSecond) + "/s");
                this.transferTask.updateMessage(transferDetailsText.toString());
            }
        } else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            log.debug("DownloadObjectsEvent Completed");
            this.transferTask.updateMessage("completed");
            this.transferTask.updateProgress(1, 1);
        } else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            log.debug("DownloadObjectsEvent Error");
            this.transferTask.updateMessage("Failed");
        }
    }
}

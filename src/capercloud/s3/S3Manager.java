package capercloud.s3;

import capercloud.CaperCloud;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.jets3t.service.model.StorageObject;
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
import org.jets3t.service.utils.TimeFormatter;

/**
 * List buckets, multiple files uploading and downloading 
 * @author Yang Shuai
 *
 */
public class S3Manager implements StorageServiceEventListener, CredentialsProvider{

    public static Jets3tProperties jets3tProperties = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);;
    private CredentialsProvider mCredentialProvider;
    
    private RestS3Service s3Service;
    //for monitor progress
    private ThreadedS3Service storageService;
    
    //data
    private List<S3Object> uploadingObjects = new ArrayList<>();
    private Set<String> uploadedObjects = new HashSet<>();
    private boolean isUploadingErrorOccured = true;
    private final ByteFormatter byteFormatter = new ByteFormatter();
    private final TimeFormatter timeFormatter = new TimeFormatter();
    
    private List<S3Object> downloadingObjects = new ArrayList<>();
    private Set<String> downloadedObjects = new HashSet<>();
    private List<DownloadPackage> downloadPackages = new ArrayList<>();
    private boolean isDownloadingErrorOccured = true;

    /**
     * 
     * @param currentCredentials
     * @throws ServiceException 
     */
    public S3Manager(AWSCredentials currentCredentials) throws ServiceException {
        this.mCredentialProvider = new BasicCredentialsProvider();
        this.s3Service = new RestS3Service(currentCredentials, CaperCloud.APPLICATION_DESCRIPTION, this, this.jets3tProperties);
        this.storageService = new ThreadedS3Service(this.s3Service, this);
    }

    public Jets3tProperties getJets3tProperties() {
        return jets3tProperties;
    }

    public S3Bucket[] listBuckets() throws S3ServiceException {
        return this.s3Service.listAllBuckets();
    }

    public S3Object[] listObjects(String bucket) throws S3ServiceException {
        return this.s3Service.listObjects(bucket);
    }

    public S3Bucket getBucket(String name) throws S3ServiceException{
        return s3Service.getBucket(name);
    }
    
    public void uploadFiles(ArrayList<File> files, S3Bucket bucket) throws NoSuchAlgorithmException, IOException {
        System.out.println("Uploading" + files.size() + "files");
        uploadingObjects.clear();
        isUploadingErrorOccured = false;
        uploadedObjects.clear();
        //building uploadingObjects ArrayList
        for(File f : files) {
            String key = f.getName();
            S3Object s3Obj = new S3Object(bucket, f);
            s3Obj.setKey(key);
            s3Obj.setContentType(Mimetypes.getInstance().getMimetype(s3Obj.getKey()));
            uploadingObjects.add(s3Obj);
        }
        //upload objects in uploadingObjects ArrayList
        storageService.putObjects(bucket.getName(), uploadingObjects.toArray(new S3Object[uploadingObjects.size()]));
        
        //yibu
        if(isUploadingErrorOccured || uploadingObjects.size() != uploadedObjects.size()) {
            System.out.println("Have to try uploading a few objects again for folder " + 
                    " - Completed = " + uploadedObjects.size() + " and Total =" + uploadingObjects.size());
        }        
    }    
    
    //bucketName - name of the bucket containing the objects
    public void downloadObjects(String bucketName, ArrayList<S3Object> s3Objs, File downloadDirectory) throws ServiceException {
        System.out.println("Downloading" + s3Objs.size() + "Objects");
        downloadingObjects.clear();
        isDownloadingErrorOccured = false;
        downloadedObjects.clear();
        downloadPackages.clear();
        //building downloadPackages
        for (S3Object o : s3Objs) {  
            downloadPackages.add(new DownloadPackage(o, new File(downloadDirectory, o.getKey())));
        }
        this.storageService.downloadObjects(bucketName, (DownloadPackage[]) downloadPackages.toArray());
        
        //yibu
        if(isDownloadingErrorOccured || downloadingObjects.size() != downloadedObjects.size()) {
            System.out.println("Have to try uploading a few objects again for folder " + 
                    " - Completed = " + downloadedObjects.size() + " and Total =" + downloadingObjects.size());
        }    
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
        if (ServiceEvent.EVENT_IGNORED_ERRORS == event.getEventCode()) {
            Throwable[] throwables = event.getIgnoredErrors();
            for (int i = 0; i < throwables.length; i++) {
                System.out.println("Ignoring error: " + throwables[i].getMessage());
            }
        }else if(ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            System.out.println("**********************************Upload Event Started***********************************");
        }else if(event.getEventCode() == ServiceEvent.EVENT_ERROR) {
            isUploadingErrorOccured = true;
        }else if(event.getEventCode() == ServiceEvent.EVENT_IN_PROGRESS) {
            StorageObject[] storeObjs = event.getCreatedObjects();
            for(StorageObject storeObj : storeObjs) {
                uploadedObjects.add(storeObj.getKey());
            }
            ThreadWatcher watcher = event.getThreadWatcher();
            if (watcher.getBytesTransferred() >= watcher.getBytesTotal()) {
                System.out.println("Upload Completed.. Verifying");
            }else {
                int percentage = (int) (((double) watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);

                long bytesPerSecond = watcher.getBytesPerSecond();
                StringBuilder transferDetailsText=new StringBuilder("Uploading.... ");
                transferDetailsText.append("Speed: " + byteFormatter.formatByteSize(bytesPerSecond) + "/s");

                if (watcher.isTimeRemainingAvailable()) {
                    long secondsRemaining = watcher.getTimeRemaining();
                    if (transferDetailsText.length() > 0) {
                        transferDetailsText.append(" - ");
                    }
                    transferDetailsText.append("Time remaining: " + timeFormatter.formatTime(secondsRemaining));
                }
                System.out.println(transferDetailsText.toString()+" "+percentage);
            }
        }else if(ServiceEvent.EVENT_COMPLETED==event.getEventCode()) {
            System.out.println("**********************************Upload Event Completed***********************************");
            if(isUploadingErrorOccured) {
                System.out.println("**********************But with errors, have to retry failed uploads**************************");
            }
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
    public void event(DeleteObjectsEvent doe) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
            ThreadWatcher watcher = event.getThreadWatcher();
            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                System.out.println("Downloaded " +
                    watcher.getCompletedThreads() + "/" + watcher.getThreadCount() + " - " +
                    byteFormatter.formatByteSize(watcher.getBytesTransferred())
                    + " of " + byteFormatter.formatByteSize(watcher.getBytesTotal()));
            // ... otherwise just show the number of completed threads.
            } else {
                System.out.println("Downloaded " + watcher.getCompletedThreads()
                    + " of " + watcher.getThreadCount() + " objects");
            }
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            StorageObject[] storeObjs = event.getDownloadedObjects();
            for(StorageObject storeObj : storeObjs) {
                uploadedObjects.add(storeObj.getKey());
            }
            ThreadWatcher watcher = event.getThreadWatcher();
            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                String bytesCompletedStr = byteFormatter.formatByteSize(watcher.getBytesTransferred());
                String bytesTotalStr = byteFormatter.formatByteSize(watcher.getBytesTotal());
                String statusText = "Downloaded " +
                    watcher.getCompletedThreads() + "/" + watcher.getThreadCount() + " - " +
                    bytesCompletedStr + " of " + bytesTotalStr;

                int percentage = (int)
                    (((double)watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);
                System.out.println("statusText: " + statusText);
                System.out.println("percentage: " + percentage);
            }
            // ... otherwise just show the number of completed threads.
            else {
                ThreadWatcher progressStatus = event.getThreadWatcher();
                String statusText = "Downloaded " + progressStatus.getCompletedThreads()
                    + " of " + progressStatus.getThreadCount() + " objects";
                System.out.println(statusText);
            }
        } else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            System.out.println("Download complete");
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            System.out.println("Download canceled");
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            System.out.print("Download error");

            String message = "Unable to download objects";
            System.out.println(message);
        }
    }
}

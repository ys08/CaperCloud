/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.s3.S3Manager;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.RebootInstancesRequest;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
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
    private CaperCloud mainApp;
    private static CloudManager singleton = null;
    private AWSCredentials currentCredentials; 
    private S3Manager s3m;
    private AmazonEC2Client ec2Client;
    
    private CloudManager() {
    }
    
    
    public static CloudManager getInstance() {
        if (singleton == null) {
            singleton = new CloudManager();
        }
        return singleton;
    }

    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }
    /**
     * get credentials that are currently used by s3 and ec2
     * @return 
     */
    public AWSCredentials getCurrentCredentials() {
        return currentCredentials;
    }

    public AmazonEC2Client getEc2Client() {
        return ec2Client;
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
        this.ec2Client = new AmazonEC2Client(new BasicAWSCredentials(currentCredentials.getAccessKey(), currentCredentials.getSecretKey()));  
        //test in eucalyptus
        this.ec2Client.setEndpoint("http://192.168.99.111:8773/services/Eucalyptus");
    }
    
    public void logoutCloud() {
        if (this.currentCredentials == null) {
            log.debug("Something goes wrong");
            return;
        }
        this.s3m = null;
        this.ec2Client = null;
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
    
    public Service<S3Bucket[]> createCheckingAccountService(final String msg) {
        return new Service<S3Bucket[]>() {
            @Override
            protected Task<S3Bucket[]> createTask() {
                return new Task<S3Bucket[]>() {
                    @Override
                    protected S3Bucket[] call() throws Exception {
                        return CloudManager.this.listBuckets();
                    }

                    @Override
                    protected void running() {
                        super.running(); //To change body of generated methods, choose Tools | Templates.
                        updateMessage(msg);
                        updateProgress(-1, 1);
                    }
                    
                    @Override
                    protected void succeeded() {
                        super.succeeded();
                        updateMessage("Success");
                        updateProgress(1, 1);            
                    }
                };
            }
        };
    }
    
    public Service<S3Bucket[]> createListBucketsService() {
        return new Service<S3Bucket[]>() {
            @Override
            protected Task<S3Bucket[]> createTask() {
                return new Task<S3Bucket[]>() {
                    @Override
                    protected S3Bucket[] call() throws Exception {
                        return CloudManager.this.listBuckets();
                    }

                    @Override
                    protected void running() {
                        super.running(); //To change body of generated methods, choose Tools | Templates.
                        updateProgress(-1, 1);
                    }
                    
                };
            }
        };
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
    
    public Service<S3Object[]> createListObjectsService(final S3Bucket bucket) {
        return new Service<S3Object[]>() {
            @Override
            protected Task<S3Object[]> createTask() {
                return new Task<S3Object[]>() {
                    @Override
                    protected S3Object[] call() throws Exception {
                        return CloudManager.this.listObjects(bucket);
                    }

                    @Override
                    protected void running() {
                        super.running(); //To change body of generated methods, choose Tools | Templates.
                        updateProgress(-1, 1);
                    }
                };
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
    
    public Service<S3Bucket> createCreateBucketService(final String bucketName) {
        return new Service<S3Bucket>() {
            @Override
            protected Task<S3Bucket> createTask() {
                return new Task<S3Bucket>() {
                    @Override
                    protected S3Bucket call() throws Exception {
                        return CloudManager.this.createBucket(bucketName);
                    }

                    @Override
                    protected void running() {
                        super.running(); //To change body of generated methods, choose Tools | Templates.
                        updateProgress(-1, 1);
                    }
                };
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
    
    public Service<Void> createDeleteObjectService(final S3Object obj) {
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

                    @Override
                    protected void running() {
                        super.running(); //To change body of generated methods, choose Tools | Templates.
                        updateProgress(-1, 1);
                    }
                };
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
    
    public Service<Void> createDeleteBucketService(final S3Bucket bucket) {
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
                    
                    @Override
                    protected void running() {
                        super.running(); //To change body of generated methods, choose Tools | Templates.
                        updateProgress(-1, 1);
                    }
                };
            }
        };
    }
    
    public Service<DescribeInstancesResult> createDescribeInstancesService(DescribeInstancesRequest request) {
        return new Service<DescribeInstancesResult>() {
            @Override
            protected Task<DescribeInstancesResult> createTask() {
                return new Task<DescribeInstancesResult>() {
                    @Override
                    protected DescribeInstancesResult call() throws Exception {
                        return CloudManager.this.ec2Client.describeInstances(request);
                    }
                };
            }
        };
    }
    
    public Service<StopInstancesResult> createStopInstancesService(StopInstancesRequest request) {
        return new Service<StopInstancesResult>() {
            @Override
            protected Task<StopInstancesResult> createTask() {
                return new Task<StopInstancesResult>() {
                    @Override
                    protected StopInstancesResult call() throws Exception {
                        return CloudManager.this.ec2Client.stopInstances(request);
                    }
                };
            }
        };
    }
    
    public Service<TerminateInstancesResult> createTerminateInstancesService(TerminateInstancesRequest request) {
        return new Service<TerminateInstancesResult>() {
            @Override
            protected Task<TerminateInstancesResult> createTask() {
                return new Task<TerminateInstancesResult>() {
                    @Override
                    protected TerminateInstancesResult call() throws Exception {
                        return CloudManager.this.ec2Client.terminateInstances(request);
                    }
                };
            }
        };
    }
    
    public Service<Void> createRebootInstancesService(RebootInstancesRequest request) {
        return new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        CloudManager.this.ec2Client.rebootInstances(request);
                        return null;
                    }
                };
            }
        };
    }
    
    public List<String> createOnDemandInstances(String imageId, InstanceType instanceType, Integer size, String keyName, String securityGroup) {   
        List<String> instanceIds = new ArrayList<>();
        
        RunInstancesRequest rir = new RunInstancesRequest();
        rir.withImageId(imageId);
        rir.withInstanceType(instanceType);
        rir.withMinCount(1);
        rir.withMaxCount(1);
        rir.withKeyName(keyName);
        rir.withSecurityGroups(securityGroup);
        
        for (int i=0; i<size.intValue(); i++) {
            RunInstancesResult r = this.ec2Client.runInstances(rir);
            Reservation rr = r.getReservation();
            for (Instance ii : rr.getInstances()) {
                instanceIds.add(ii.getInstanceId());
            }
        }
        
        boolean isWaiting = true;
        Set<String> states = new HashSet<>();
        while (isWaiting) {
            try {
                log.info("waiting for instances start up...");
                //10 secs
                Thread.sleep(10000);
                DescribeInstancesResult r = this.ec2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));
                Iterator<Reservation> ir= r.getReservations().iterator();
                while(ir.hasNext()){
                    Reservation rr = ir.next();
                    List<Instance> instances = rr.getInstances();
                    for(Instance ii : instances){
                        log.info(ii.getImageId() + "\t" + ii.getInstanceId()+ "\t" + ii.getState().getName() + "\t"+ ii.getPrivateDnsName() + "\t" + ii.getPublicIpAddress());
                        states.add(ii.getState().getName());
                    }
                }
                
                if (states.contains("pending")) {
                    states.clear();
                } else {
                    isWaiting = false;
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
        return instanceIds;
    }

    
    public void remoteCallByShh(String userName, String ipAddress, String command, File privateKey) throws JSchException, IOException, InterruptedException {
        JSch jsch = new JSch();
        jsch.addIdentity(privateKey.getAbsolutePath());
        JSch.setConfig("StrictHostKeyChecking", "no");
        Session session=jsch.getSession(userName, ipAddress, 22);
        for(int i = 0; i < 10; i++) {
            try {
                session.connect();
                break;
            } catch(JSchException ex) {
                if(i == 10 - 1) {
                    throw ex;
                }
                log.info("retry " + (i+1) + " times");
                Thread.sleep(20000);
            }
        }
        //session.connect();
        
        //run stuff
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        channel.setPty(true);
        channel.connect();
                
        InputStream input = channel.getInputStream();
//start reading the input from the executed commands on the shell
        byte[] tmp = new byte[1024];
        while (true) {
            while (input.available() > 0) {
                int i = input.read(tmp, 0, 1024);
                if (i < 0) break;
                log.info(new String(tmp, 0, i));
            }
            if (channel.isClosed()){
                log.info("exit-status: " + channel.getExitStatus());
                if (channel.getExitStatus() != 0) {
                    System.exit(1);
                }
                break;
            }
            Thread.sleep(1000);
        }
        channel.disconnect();
        session.disconnect();
    }
    
    public void sftp(String userName, String ipAddress, String lfile, String rfile, File privateKey) {
        Session session = null;
        ChannelSftp sftpChannel = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(privateKey.getAbsolutePath());
            
            session = jsch.getSession(userName, ipAddress, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            for(int i = 0; i < 10; i++) {
                try {
                    session.connect();
                    break;
                } catch(JSchException ex) {
                    if(i == 10 - 1) {
                        throw ex;
                    }
                    log.info("retry " + (i+1) + " times");
                    Thread.sleep(20000);
                }
            }
            
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            sftpChannel.put(lfile, rfile);
            
        } catch (JSchException ex) {
            ex.printStackTrace();
        } catch (SftpException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }  finally {
            sftpChannel.disconnect();
            session.disconnect();
        }
    }
    
    public List<String> getInstanceList() {
        List<String> instanceIds = new ArrayList<>();
        DescribeInstancesResult result = this.ec2Client.describeInstances();
        Iterator<Reservation> i = result.getReservations().iterator();
        while (i.hasNext()) {
            Reservation r = i.next();
            List<Instance> instances = r.getInstances();
            for (Instance ii : instances) {
                instanceIds.add(ii.getInstanceId());
            }
        }
        return instanceIds;
    }
    
    public File createKeyPair(String keyName, File directory) {
        DescribeKeyPairsResult r = this.ec2Client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyName));
        if (!r.getKeyPairs().isEmpty()) {
            log.info("delete existing key: " + keyName);
            try {
                this.ec2Client.deleteKeyPair(new DeleteKeyPairRequest().withKeyName(keyName));
            } catch (AmazonServiceException ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
        log.info("create new key: " + keyName);
        CreateKeyPairResult rr = this.ec2Client.createKeyPair(new CreateKeyPairRequest().withKeyName(keyName));
        String fileName = keyName + ".pem";
        File outFile = new File(FileUtils.getUserDirectory(), fileName);
        String privateKey = rr.getKeyPair().getKeyMaterial();
        try {
            FileUtils.writeStringToFile(outFile, privateKey, false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return outFile;
    }
    
    public String createSecurityGroup(String groupName, String groupDescription) {
        DescribeSecurityGroupsResult r = this.ec2Client.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(groupName));
        if (!r.getSecurityGroups().isEmpty()) {
            log.info("delete existing security group: " + groupName);
            this.ec2Client.deleteSecurityGroup(new DeleteSecurityGroupRequest().withGroupName(groupName));
        }
        log.info("create new security group: " + groupName);
        CreateSecurityGroupRequest createSecurityGroupRequest =  new CreateSecurityGroupRequest();
        createSecurityGroupRequest.withGroupName(groupName)
                .withDescription(groupDescription);
        createSecurityGroupRequest.setRequestCredentials(new BasicAWSCredentials(currentCredentials.getAccessKey(), currentCredentials.getSecretKey()));
        CreateSecurityGroupResult csgr = this.ec2Client.createSecurityGroup(createSecurityGroupRequest);

        String groupid = csgr.getGroupId();
        log.info("Security Group Id : " + groupid);
        log.info("Create Security Group Permission");

        Collection<IpPermission> ips = new ArrayList<IpPermission>();
// Permission for SSH only to your ip
        IpPermission ipssh = new IpPermission();
        ipssh.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
                .withFromPort(22).withToPort(22);
        ips.add(ipssh);

// Permission for HTTP, any one can access
        IpPermission iphttp = new IpPermission();
        iphttp.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
                .withFromPort(80).withToPort(80);
        ips.add(iphttp);
//Permission for HTTPS, any one can accesss
        IpPermission iphttps = new IpPermission();
        iphttps.withIpRanges("0.0.0.0/0").withIpProtocol("tcp")
                .withFromPort(443).withToPort(443);
        ips.add(iphttps);
           
        log.info("Attach Owner to security group");
// Register this security group with owner
        AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
        authorizeSecurityGroupIngressRequest
                .withGroupName(groupName).withIpPermissions(ips);
        this.ec2Client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
        return groupName;
    }
    
}

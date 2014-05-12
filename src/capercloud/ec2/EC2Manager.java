package capercloud.ec2;

import capercloud.CaperCloud;
import com.xerox.amazonws.ec2.AddressInfo;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.InstanceType;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jets3t.service.security.AWSCredentials;

/**
 * The EC2Manager handles all the functions which can be performed using the EC2 QUERY API.
 * @author Yang Shuai
 *
 */
public class EC2Manager {
    
    private CaperCloud mainApp;
    
    private Jec2 ec2;
    private AWSCredentials awsCredentials;

    public EC2Manager(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }

    public Jec2 getEc2() {
        return ec2;
    }

    public AWSCredentials getAwsCredentials() {
        return awsCredentials;
    }

    public void setAwsCredentials(AWSCredentials awsCredentials) {
        this.awsCredentials = awsCredentials;
    }

    
    /**
     * This method launches the instances.
     * @param imageName The name of the image
     * @param keyPair The keypair
     * @param group The security group
     * @param zone The availability zone
     * @param type The instance type
     * @return idList the instance IDs of the launched instances
     * @throws EC2Exception 
     */
    public void launchInstances(String imageName, int minCount, int maxCount, String keyPair, String group, String zone, InstanceType type) throws EC2Exception{
    // Start up the launch configuration
    LaunchConfiguration launchCfg = new LaunchConfiguration(imageName, minCount, maxCount);
    launchCfg.setKeyName(keyPair); 
    launchCfg.setSecurityGroup(Collections.singletonList(group));
    launchCfg.setAvailabilityZone(zone);               
    //launchCfg.setMonitoring(false); 
    launchCfg.setInstanceType(type);

    ec2.runInstances(launchCfg);
    }

    /**
     * Returns the internal IP for an assigned elastic IP.
     * @return
     * @throws EC2Exception 
     */
    public String getInternalIpFromAdress(String elasticIP) throws EC2Exception {		
            String instanceID = "";

            // Get the InstanceID associated with the Elastic IP
            List<AddressInfo> adresses = ec2.describeAddresses(Arrays.asList(elasticIP));

            for (AddressInfo addressInfo : adresses) {
                    instanceID = addressInfo.getInstanceId();
            }
            String intIP = "";

            // Get the internal IP from the instanceID
            List<ReservationDescription> instances = ec2.describeInstances(Arrays.asList(instanceID));
    for (ReservationDescription res : instances) {            	
            if (res.getInstances() != null) {
                    for (Instance inst : res.getInstances()) {  
                                            String temp = inst.getPrivateDnsName();                        			
                                            intIP = temp.substring(3, temp.indexOf(".")).replaceAll("-", ".");
                    }
            }
    }
    return intIP;
    }

    /**
     * This method terminates the running instances.
     * @throws EC2Exception 
     */
    public void terminateInstances(String[] instanceIds) throws EC2Exception{
            ec2.terminateInstances(instanceIds);
    }

    /**
     * Returns the instance state: Pending, running, shutting down or terminated.
     * @param instanceID
     * @throws EC2Exception 
     */
    public String getInstanceState(String instanceID) throws EC2Exception{
            String instanceState = "";
    List<ReservationDescription> instances = ec2.describeInstances(Arrays.asList(instanceID));

    for (ReservationDescription res : instances) {            	
            if (res.getInstances() != null) {
                    for (Instance inst : res.getInstances()) {                        		
                                            instanceState = inst.getState();                        		
                    }
            }
    }
    return instanceState;
    }

    /**
     * Returns a list of all instances.
     * @return instanceList The instances as List.
     * @throws EC2Exception 
     */	
    public List<Instance> getAllInstances() throws EC2Exception{
            // Describe the instances
            List<Instance> instanceList = new ArrayList<Instance>();
    List<ReservationDescription> instances = ec2.describeInstances(new ArrayList<String>());
    for (ReservationDescription res : instances) {        	
           if (res.getInstances() != null) {
                    for (Instance inst : res.getInstances()) {                        		
                            instanceList.add(inst);
                    }
            }
    }
    return instanceList;
    }

    /**
     * Returns a list of the instances with a certain state (for example: "pending")
     * @return instanceList The instances as List.
     * @throws EC2Exception 
     */
    public List<Instance> getInstancesWithState(String state) throws EC2Exception{
            // Describe the instances
            List<Instance> instanceList = new ArrayList<Instance>();
    List<ReservationDescription> instances = ec2.describeInstances(new ArrayList<String>());
    for (ReservationDescription res : instances) {        	
            if (res.getInstances() != null) {
                    for (Instance inst : res.getInstances()) {
                                    if(inst.getState().equals(state)){
                                            instanceList.add(inst);
                                    }
                    }
            }
    }
    return instanceList;
    }
}

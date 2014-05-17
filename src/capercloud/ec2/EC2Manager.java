package capercloud.ec2;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The EC2Manager handles all the functions which can be performed using the EC2 QUERY API.
 * @author Yang Shuai
 *
 */
public class EC2Manager {
    private Log log = LogFactory.getLog(getClass());
    private AmazonEC2AsyncClient ec2;
    private List<Reservation> reservations;

    public EC2Manager(BasicAWSCredentials currentCredentials) {
        ec2 = new AmazonEC2AsyncClient(currentCredentials);
    }
    
    public Future<DescribeInstancesResult> describeAllInstances() {
        return this.ec2.describeInstancesAsync(new DescribeInstancesRequest(), new AsyncHandler<DescribeInstancesRequest,DescribeInstancesResult>() {
            @Override
            public void onError(Exception excptn) {
                log.error(excptn.getMessage());
            }

            @Override
            public void onSuccess(DescribeInstancesRequest rqst, DescribeInstancesResult result) {
                log.info("Success");
                EC2Manager.this.reservations.clear();
                EC2Manager.this.reservations.addAll(result.getReservations());
            }
        });
    }
}

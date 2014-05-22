/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.model.ClusterConfigs;
import capercloud.model.JobConfigs;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author shuai
 */
public class CloudJob {
    private Log log = LogFactory.getLog(getClass());
    CaperCloud mainApp;
    RunInstancesRequest rir;
    JobConfigs jc;
    
    public CloudJob(CaperCloud mainApp, JobConfigs jc, ClusterConfigs cc) {
        this.mainApp = mainApp;
        this.jc = jc;
        this.rir = new RunInstancesRequest();
        this.rir.setImageId(cc.getImageId());
        this.rir.setMinCount(cc.getMinCount());
        this.rir.setMaxCount(cc.getMaxCount());
        this.rir.setInstanceType(cc.getInstanceType());
        //TO DO
        this.rir.setKeyName("caper");
    }
    
    public void runInCloud() {
        this.rir.setUserData(this.userdata());
        this.mainApp.getCloudManager().getEc2Manager().runInstancesAsync(rir, new AsyncHandler<RunInstancesRequest,RunInstancesResult>() {
            @Override
            public void onError(Exception excptn) {
                log.error(excptn.getMessage());
            }

            @Override
            public void onSuccess(RunInstancesRequest rqst, RunInstancesResult result) {
                log.info("launch success");
            }
        });
    }
    
    private String userdata() {
        this.jc.getJobOptionsLocationInS3();
        this.jc.getSearchOptionsLocationInS3();
        return null;
    }
}

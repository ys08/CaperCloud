/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

/**
 *
 * @author shuai
 */
public class Result {
    private String jobName;
    private String resultPath;

    public Result() {
    }
    public Result(String jobName, String resultPath) {
        this.jobName = jobName;
        this.resultPath = resultPath;
    }
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getResultPath() {
        return resultPath;
    }

    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }
    
    
}

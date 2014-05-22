/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import com.amazonaws.services.ec2.model.InstanceType;

/**
 *
 * @author shuai
 */
public class ClusterConfigs {
    private String imageId;
    private Integer minCount;
    private Integer maxCount;
    private InstanceType instanceType;

    public ClusterConfigs(String imageId, Integer minCount, Integer maxCount, InstanceType instanceType) {
        this.imageId = imageId;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.instanceType = instanceType;
    }

    public String getImageId() {
        return imageId;
    }

    public Integer getMinCount() {
        return minCount;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public InstanceType getInstanceType() {
        return instanceType;
    }
}

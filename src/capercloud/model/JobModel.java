/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.JobOverviewController;
import com.amazonaws.services.ec2.model.InstanceType;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.identification.SearchParameters.MassAccuracyType;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.xmlpull.v1.XmlPullParserException;

/**
 *
 * @author shuai
 */
public class JobModel {
    private ObservableList<InputObjectModel> cachedInputModels;
    private Properties jobConfigs;
    private File searchOptions;
    
    private S3Bucket currentBucket;
    
    private EnzymeFactory enzymeFactory;
    private ObservableList<String> cleavageSites;
    private ObservableList<MassAccuracyType> massAccuracyTypes;
    private ObservableList<InstanceType> instanceTypes;
    
    private final ObservableList jobTypes = FXCollections.observableArrayList(
        "Novel Protein", 
        "SAP", 
        "AS",
        "Custom Protein Database"
    );
    private final ObservableList refinementExpects = FXCollections.observableArrayList(
            "0",
            "-1",
            "-2",
            "-3",
            "-4",
            "-5",
            "-6",
            "-7",
            "-8",
            "-9"
    );

    public JobModel() {
        this.instanceTypes = FXCollections.observableArrayList();
        this.instanceTypes.addAll(Arrays.asList(InstanceType.values()));
        this.cleavageSites = FXCollections.observableArrayList();
        this.massAccuracyTypes = FXCollections.observableArrayList(MassAccuracyType.values());
        try {
            this.enzymeFactory = EnzymeFactory.getInstance();
            File enzymesFile = new File("enzymes.xml");
            this.enzymeFactory.importEnzymes(enzymesFile);
            List<Enzyme> enzymes = this.enzymeFactory.getEnzymes();
            for (Enzyme e : enzymes) {
                this.cleavageSites.add(e.getName());
            }
        } catch (XmlPullParserException | IOException ex) {
            Logger.getLogger(JobOverviewController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //getters and setters
    public ObservableList getJobTypes() {
        return jobTypes;
    }

    public S3Bucket getCurrentBucket() {
        return currentBucket;
    }

    public void setCurrentBucket(S3Bucket currentBucket) {
        this.currentBucket = currentBucket;
    }

    public ObservableList<InstanceType> getInstanceTypes() {
        return instanceTypes;
    }

    public ObservableList<String> getCleavageSites() {
        return cleavageSites;
    }

    public ObservableList<MassAccuracyType> getMassAccuracyTypes() {
        return massAccuracyTypes;
    }

    public ObservableList getRefinementExpects() {
        return refinementExpects;
    }

    public ObservableList<InputObjectModel> getCachedInputModels() {
        if (this.cachedInputModels == null) {
            this.cachedInputModels = FXCollections.observableArrayList();
        }
        return this.cachedInputModels;
    }

    public void setCachedInputModels(S3Object[] objs) {
        this.cachedInputModels.clear();
        for (S3Object obj : objs) {
            this.cachedInputModels.add(new InputObjectModel(obj));
        }
    }

    public EnzymeFactory getEnzymeFactory() {
        return enzymeFactory;
    }
}

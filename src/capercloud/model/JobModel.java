/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.CaperCloud;
import capercloud.JobOverviewController;
import com.amazonaws.services.ec2.model.InstanceType;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.PTMFactory;
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
    private CaperCloud mainApp;
    private ObservableList<InputObjectModel> cachedInputModels;
    private Properties jobConfigs;
    private File searchOptions;
    
    private S3Bucket currentBucket;
    
    private EnzymeFactory enzymeFactory;
    private PTMFactory ptmFactory;
    
    private ObservableList<String> cleavageSites;
    private ObservableList<MassAccuracyType> massAccuracyTypes;
    private ObservableList<InstanceType> instanceTypes;
    private ObservableList<ModificationTableModel> defaultModifications;
    
    private final ObservableList jobTypes = FXCollections.observableArrayList(
        "Novel Protein", 
        "Missense SNV", 
        "Exon-Exon Junction",
        "Sample-specific SNV"
    );
    
    private final ObservableList refinementExpects = FXCollections.observableArrayList(
            "0.01",
            "0.02",
            "0.03",
            "0.04",
            "0.05",
            "0.06",
            "0.07",
            "0.08",
            "0.09",
            "0.1"
    );

    public JobModel() {
        this.instanceTypes = FXCollections.observableArrayList();
        this.instanceTypes.addAll(Arrays.asList(InstanceType.values()));
        this.cleavageSites = FXCollections.observableArrayList();
        this.massAccuracyTypes = FXCollections.observableArrayList(MassAccuracyType.values());
        this.defaultModifications = FXCollections.observableArrayList();
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
        this.ptmFactory = PTMFactory.getInstance();
        try {
            this.ptmFactory.importModifications(new File("mods.xml"), false);
            for (String modName: this.ptmFactory.getDefaultModificationsOrdered()) {
                this.defaultModifications.add(new ModificationTableModel(this.ptmFactory.getPTM(modName)));
            }
        } catch (XmlPullParserException ex) {
            Logger.getLogger(JobModel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JobModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //getters and setters
    
    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }
    
    public ObservableList getJobTypes() {
        return jobTypes;
    }

    public S3Bucket getCurrentBucket() {
        return currentBucket;
    }

    public ObservableList<ModificationTableModel> getDefaultModifications() {
        return defaultModifications;
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
            InputObjectModel iom = new InputObjectModel(obj);
            System.out.println(mainApp);
            System.out.println(mainApp.getMainController());
            System.out.println(mainApp.getMainController().getTfSelectedNumOfInputSpectra());
            iom.addListener(this.mainApp.getMainController().getTfSelectedNumOfInputSpectra());
            this.cachedInputModels.add(iom);
        }
    }

    public EnzymeFactory getEnzymeFactory() {
        return enzymeFactory;
    }
}

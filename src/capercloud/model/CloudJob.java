/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.CaperCloud;
import com.amazonaws.services.ec2.model.InstanceType;
import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.XtandemParameters;
import com.compomics.util.preferences.ModificationProfile;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Object;

/**
 *
 * @author shuai
 */
public class CloudJob {
    private Log log = LogFactory.getLog(CloudJob.class);
    
    private CaperCloud mainApp;
    private SearchParameters sp;
    private int jobType;
    private List<S3Object> spectrumObjs;
    
    //cloud computing parameters
    private String imageId;
    private String keyName;
    private String securityGroup;
    private Integer clusterSize;
    private InstanceType instanceType;
    private String outputBucketName;
    
    private File taxonomyFile;
    private List<File> inputFiles;
    
    private String timestamp;
    String tmpPath;
    
    private StringProperty startTime;
    private StringProperty passedTime;
    private StringProperty instanceId;
    private StringProperty status;
    
    private List<InstanceModel> instanceModelList;
    
    public CloudJob(CaperCloud mainApp, List<S3Object> spectrumObjs, SearchParameters sp, int jobType) {
        this.mainApp = mainApp;
        this.spectrumObjs = spectrumObjs;
        this.sp = sp;
        this.jobType = jobType;
        
        this.inputFiles = new ArrayList<>();
        this.tmpPath = FileUtils.getTempDirectoryPath() + "capercloud";

        this.timestamp = Long.toString(System.currentTimeMillis());
        this.instanceModelList = new ArrayList<>();
    }
    
    public String getOutputBucketName() {
        return outputBucketName;
    }

    public void setOutputBucketName(String outputBucketName) {
        this.outputBucketName = outputBucketName;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public void setClusterSize(Integer clusterSize) {
        this.clusterSize = clusterSize;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public void setInstanceType(InstanceType instanceType) {
        this.instanceType = instanceType;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public InstanceType getInstanceType() {
        return instanceType;
    }

    public String getImageId() {
        return imageId;
    }

    public String getKeyName() {
        return keyName;
    }

    public Integer getClusterSize() {
        return clusterSize;
    }

    public StringProperty startTimeProperty() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = new SimpleStringProperty(startTime);
    }

    public StringProperty passedTimeProperty() {
        return passedTime;
    }
    
    public StringProperty clusterSizeProperty() {
        return new SimpleStringProperty(this.clusterSize.toString());
    }
    
    public StringProperty instanceIdProperty() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = new SimpleStringProperty(instanceId);
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void setStatus(String status) {
        this.status = new SimpleStringProperty(status);
    }

    public void setPassedTime(String passedTime) {
        this.passedTime = new SimpleStringProperty(passedTime);
    }
    
    public StringProperty spectraProperty() {
        S3Object obj = this.spectrumObjs.get(0);
        return new SimpleStringProperty(obj.getName());
    }

    public File getTaxonomyFile() {
        return taxonomyFile;
    }

    public List<File> getInputFiles() {
        return inputFiles;
    }

    public List<S3Object> getSpectrumObjs() {
        return spectrumObjs;
    }
    
    public StringProperty jobIdProperty() {
        return new SimpleStringProperty(timestamp);
    }
    
//    public String userdata() {
//        if (jobType == CaperCloud.CUSTOM_DB) {
//            String userData = "#!/bin/bash\n"
//                    + "echo \"Hello World.  The time is now $(date -R)!\" | tee ~/output.txt\n";
//            return Base64.encodeAsString(userData.getBytes());
//        }
//        return null;
//    }
    
    private void createTaxonomyFile(String databasePath) throws IOException {
        String sep = IOUtils.LINE_SEPARATOR;
        String content = 
                "<?xml version=\"1.0\"?>" + sep
                + "<bioml label=\"x! taxon-to-file matching list\">" + sep
                + "\t<taxon label=\"all\">" + sep
                + "\t\t<file format=\"peptide\" URL=\"" + databasePath + "\" />" + sep
                + "\t</taxon>" + sep
                + "</bioml>";
        
        String taxonomyFilename = timestamp + "taxonomy.xml";
        this.taxonomyFile = new File(this.tmpPath, taxonomyFilename);
        FileUtils.writeStringToFile(this.taxonomyFile, content);
    }
    
    private void createInputFile(S3Object spectrumObj) throws IOException {
        String fragmentUnit = "ppm";
        String enzymeIsSemiSpecific = "no";
        String sep = IOUtils.LINE_SEPARATOR;
        XtandemParameters xp = (XtandemParameters) sp.getIdentificationAlgorithmParameter(1);
        ModificationProfile mp = sp.getModificationProfile();
        String modDescription = getSearchedModList(mp);
        
        if (sp.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.PPM) {
            fragmentUnit = "ppm";
        } else if (sp.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.DA) {
            fragmentUnit = "Daltons";
        }
        if (sp.getEnzyme().isSemiSpecific()) {
            enzymeIsSemiSpecific = "yes";
        } else {
            enzymeIsSemiSpecific = "no";
        }
        
        String content = 
                "<?xml version=\"1.0\"?>" + sep
                + "<bioml>" + sep
                + "\t<note type=\"input\" label=\"list path, default parameters\">default_input.xml</note>" + sep
                + "\t<note type=\"input\" label=\"list path, taxonomy information\">" + this.taxonomyFile.getName() + "</note>" + sep
                + "\t<note type=\"input\" label=\"protein, taxon\">all</note>" + sep
                + "\t<note type=\"input\" label=\"spectrum, path\">" + spectrumObj.getName() + "</note>" + sep
                + "\t<note type=\"input\" label=\"output, path\">output</note>" + sep
                + "\t<note type=\"input\" label=\"protein, cleavage site\">" + sp.getEnzyme().getXTandemFormat() + "</note>" + sep
                + "\t<note type=\"input\" label=\"protein, cleavage semi\">" + enzymeIsSemiSpecific + "</note>" + sep
                + modDescription
                + "\t<note type=\"input\" label=\"spectrum, fragment monoisotopic mass error\">" + sp.getFragmentIonAccuracy() + "</note>" + sep
                + "\t<note type=\"input\" label=\"spectrum, fragment monoisotopic mass error units\">" + fragmentUnit + "</note>" + sep
                + "\t<note type=\"input\" label=\"refine, maximum valid expectation value\">" + xp.getMaximumExpectationValueRefinement() + "</note>" + sep
                + "</bioml>" + sep;
        
        String inputFilename = timestamp + spectrumObj.getName() + ".xml";
        File f = new File(this.tmpPath, inputFilename);
        FileUtils.writeStringToFile(f, content);
        this.inputFiles.add(f);
    }
    
    private String getSearchedModList(ModificationProfile mp) throws IllegalArgumentException {
        String completeModificationString = "";
        String variableModsString = "\t<note type=\"input\" label=\"residue, potential modification mass\">";
        String variableModsDescriptionString = "\t\t<note>";
        // get the sorted list of ptms, the keys in the maps are the target, and the values the ptms with that target
        HashMap<String, ArrayList<PTM>> allVariableMods = sortModifications(mp.getVariableModifications(), mp);
        HashMap<String, ArrayList<PTM>> allFixedMods = sortModifications(mp.getFixedModifications(), mp);
        // list of ptms that were set as variable, but have to be set as "variable fixed"
        HashMap<String, ArrayList<PTM>> variableFixedPtms = new HashMap<String, ArrayList<PTM>>();
        // iterate the variable ptms, and find the ones with unique targets
        Iterator<String> targets = allVariableMods.keySet().iterator();
        while (targets.hasNext()) {
            String target = targets.next();
            if (allVariableMods.get(target).size() == 1) {
                // unique target across all the variable ptms
                variableModsString += allVariableMods.get(target).get(0).getMass() + "@" + target + ",";
                variableModsDescriptionString += allVariableMods.get(target).get(0).getName() + ",";
            } else {
                // none-unique target, add to "variable fixed" ptms
                variableFixedPtms.put(target, allVariableMods.get(target));
            }
        }

        // remove the ending commas
        if (variableModsString.endsWith(",")) {
            variableModsString = variableModsString.substring(0, variableModsString.length() - 1);
            variableModsDescriptionString = variableModsDescriptionString.substring(0, variableModsDescriptionString.length() - 1);
        }

        // set the variable ptms
        variableModsString += "</note>" + System.getProperty("line.separator");
        variableModsDescriptionString += "</note>" + System.getProperty("line.separator");

        // fixed mods strings
        String fixedModsStringTemplate = "\t<note type=\"input\" label=\"residue, modification mass";
        String fixedModsStringDescriptionTemplate = "\t\t<note>";

        String fixedModsString = fixedModsStringTemplate + "\">";
        String fixedModsDescriptionString = fixedModsStringDescriptionTemplate;
        String defaultFixedModsString = "";
        String defaultFixedModsDescription = "";

        // iterate the fixed ptms, and find the ones with unique targets
        targets = allFixedMods.keySet().iterator();

        while (targets.hasNext()) {

            String target = targets.next();

            if (allFixedMods.get(target).size() == 1) {
                // unique target across all the fixed ptms
                fixedModsString += allFixedMods.get(target).get(0).getMass() + "@" + target + ",";
                fixedModsDescriptionString += allFixedMods.get(target).get(0).getName() + ",";

                defaultFixedModsString += allFixedMods.get(target).get(0).getMass() + "@" + target + ",";
                defaultFixedModsDescription += allFixedMods.get(target).get(0).getName() + ",";
            } else {
                // non-unique targets for fixed ptms detected, this is not supported!!
                throw new IllegalArgumentException("More than one fixed modification with the same target was detected! Target: " + target + ". "
                        + "X!Tandem does not support this. Please replace by a single modification and try again.");
            }
        }

        // remove the ending commas
        if (fixedModsString.endsWith(",")) {
            fixedModsString = fixedModsString.substring(0, fixedModsString.length() - 1);
            fixedModsDescriptionString = fixedModsDescriptionString.substring(0, fixedModsDescriptionString.length() - 1);
        }

        // close the default fixed mods tag
        fixedModsString += "</note>" + System.getProperty("line.separator");
        fixedModsDescriptionString += "</note>" + System.getProperty("line.separator");

        // add the default fixed mods
        completeModificationString += fixedModsString + fixedModsDescriptionString;

        // iterate the "variable fixed" mods
        targets = variableFixedPtms.keySet().iterator();

        ArrayList<String> fixedSecondaryLines = new ArrayList<String>();
        ArrayList<String> fixedSecondaryLinesDescription = new ArrayList<String>();

        while (targets.hasNext()) {

            ArrayList<String> newLines = new ArrayList<String>();
            ArrayList<String> newDescriptions = new ArrayList<String>();

            String target = targets.next();

            for (PTM tempPtm : variableFixedPtms.get(target)) {

                PTM currentPtm = tempPtm;
                String tempModsString = currentPtm.getMass() + "@" + target;
                String tempModsStringModsDescriptionString = currentPtm.getName();

                newLines.add(defaultFixedModsString + tempModsString);
                newDescriptions.add(defaultFixedModsDescription + tempModsStringModsDescriptionString);

                for (String previousLines : fixedSecondaryLines) {
                    newLines.add(previousLines + "," + tempModsString);
                }

                for (String previousLines : fixedSecondaryLinesDescription) {
                    newDescriptions.add(previousLines + "," + tempModsStringModsDescriptionString);
                }
            }

            fixedSecondaryLines.addAll(newLines);
            fixedSecondaryLinesDescription.addAll(newDescriptions);
        }

        // add the fixed variable mods
        for (int i = 0; i < fixedSecondaryLines.size(); i++) {

            // add the mods
            fixedModsString = fixedModsStringTemplate + " " + (i + 1) + "\">" + fixedSecondaryLines.get(i);
            fixedModsDescriptionString = fixedModsStringDescriptionTemplate + fixedSecondaryLinesDescription.get(i);

            // close the tags
            fixedModsString += "</note>" + System.getProperty("line.separator");
            fixedModsDescriptionString += "</note>" + System.getProperty("line.separator");

            // add to mods string
            completeModificationString += fixedModsString + fixedModsDescriptionString;
        }

        // add the variable mods to the mods string
        completeModificationString += variableModsString + variableModsDescriptionString;

        return completeModificationString;
    }

    private HashMap<String, ArrayList<PTM>> sortModifications(ArrayList<String> modifications, ModificationProfile mp) {

        HashMap<String, ArrayList<PTM>> sortedMods = new HashMap<String, ArrayList<PTM>>();
        for (String name : modifications) {
            PTM ptm = mp.getPtm(name);
            if (ptm.getType() == PTM.MODN
                    || ptm.getType() == PTM.MODNAA
                    || ptm.getType() == PTM.MODNP
                    || ptm.getType() == PTM.MODNPAA) {
                ArrayList<PTM> ptms;
                if (sortedMods.containsKey("[")) {
                    ptms = sortedMods.get("[");
                } else {
                    ptms = new ArrayList<PTM>();
                }
                ptms.add(ptm);
                sortedMods.put("[", ptms);
            }
            for (AminoAcid aa : ptm.getPattern().getAminoAcidsAtTarget()) {
                ArrayList<PTM> ptms;
                if (sortedMods.containsKey(aa.singleLetterCode)) {
                    ptms = sortedMods.get(aa.singleLetterCode);
                } else {
                    ptms = new ArrayList<PTM>();
                }
                ptms.add(ptm);
                sortedMods.put(aa.singleLetterCode, ptms);
            }
            if (ptm.getType() == PTM.MODC
                    || ptm.getType() == PTM.MODCAA
                    || ptm.getType() == PTM.MODCP
                    || ptm.getType() == PTM.MODCPAA) {
                ArrayList<PTM> ptms;
                if (sortedMods.containsKey("]")) {
                    ptms = sortedMods.get("]");
                } else {
                    ptms = new ArrayList<PTM>();
                }
                ptms.add(ptm);
                sortedMods.put("]", ptms);
            }
        }
        return sortedMods;
    }
}

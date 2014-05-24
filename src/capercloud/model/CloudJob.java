/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import capercloud.CaperCloud;
import capercloud.TypeFourController;
import capercloud.TypeOneController;
import capercloud.TypeThreeController;
import capercloud.TypeTwoController;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.util.Base64;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.XtandemParameters;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 *
 * @author shuai
 */
public class CloudJob {
    private Log log = LogFactory.getLog(getClass());
    private CaperCloud mainApp;
    private RunInstancesRequest rir;
    private SearchParameters sp;
    private int jobType;
    private S3Bucket bucket;
    
    private File taxonomyFile;
    private List<File> inputFiles;
    private List<S3Object> spectrumObjs;
    private S3Object databaseObj;
// TO UPLOAD
    private S3Object taxonomyObj;
    private List<S3Object> inputObjs;

    private TypeOneController t1c;
    private TypeTwoController t2c;
    private TypeThreeController t3c;
    private TypeFourController t4c;
    
    String timestamp;
    
    public CloudJob(CaperCloud mainApp, List<S3Object> spectrumObjs, SearchParameters sp, ClusterConfigs cc, int jobType) {
        this.mainApp = mainApp;
        this.spectrumObjs = spectrumObjs;
        this.sp = sp;
        this.jobType = jobType;
        
        this.rir = new RunInstancesRequest();
        this.rir.setImageId(cc.getImageId());
        this.rir.setMinCount(cc.getMinCount());
        this.rir.setMaxCount(cc.getMaxCount());
        this.rir.setInstanceType(cc.getInstanceType());
        //TO DO
        this.rir.setKeyName("caper");
        
        this.inputObjs = new ArrayList<>();
        this.inputFiles = new ArrayList<>();

        this.timestamp = Long.toString(System.currentTimeMillis());
    }

    public void setT1c(TypeOneController t1c) {
        this.t1c = t1c;
    }

    public void setT2c(TypeTwoController t2c) {
        this.t2c = t2c;
    }

    public void setT3c(TypeThreeController t3c) {
        this.t3c = t3c;
    }
    
    public void setT4c(TypeFourController t4c) {
            this.t4c = t4c;
    }

    
//master node will launch slave node    
    public void launchMasterNode() {
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
    
    private void createTaxonomyFile() throws NoSuchAlgorithmException {
        String sep = IOUtils.LINE_SEPARATOR;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.taxonomyFile));
            bw.write(
                    "<?xml version=\"1.0\"?>" + sep
                    + "<bioml label=\"x! taxon-to-file matching list\">" + sep
                    + "\t<taxon label=\"all\">" + sep
                    + "\t\t<file format=\"peptide\" URL=\"" + databaseObj.getName() + "\" />" + sep
                    + "\t</taxon>" + sep
                    + "</bioml>");
            bw.flush();
            bw.close();
            this.taxonomyObj = new S3Object(this.taxonomyFile);
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        }
    }
    
    private void createInputFile(File inputFile, S3Object spectrumObj) throws NoSuchAlgorithmException {
        String fragmentUnit = "ppm";
        String enzymeIsSemiSpecific = "no";
        String sep = IOUtils.LINE_SEPARATOR;
        XtandemParameters xp = (XtandemParameters) sp.getIdentificationAlgorithmParameter(1);
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
        
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(inputFile));
            bw.write("<?xml version=\"1.0\"?>" + sep
                    + "<bioml>" + sep
                    + "\t<note type=\"input\" label=\"list path, default parameters\">default_input.xml</note>" + sep
                    + "\t<note type=\"input\" label=\"list path, taxonomy information\">" + this.taxonomyFile.getName() + "</note>" + sep
                    + "\t<note type=\"input\" label=\"protein, taxon\">all</note>" + sep
                    + "\t<note type=\"input\" label=\"spectrum, path\">" + spectrumObj.getName() + "</note>" + sep
                    + "\t<note type=\"input\" label=\"output, path\">output</note>" + sep
                    + "\t<note type=\"input\" label=\"protein, cleavage site\">" + sp.getEnzyme().getXTandemFormat() + "</note>" + sep
                    + "\t<note type=\"input\" label=\"protein, cleavage semi\">" + enzymeIsSemiSpecific + "</note>" + sep
                    + "\t<note type=\"input\" label=\"spectrum, fragment monoisotopic mass error\">" + sp.getFragmentIonAccuracy() + "</note>" + sep
                    + "\t<note type=\"input\" label=\"spectrum, fragment monoisotopic mass error units\">" + fragmentUnit + "</note>" + sep
                    + "\t<note type=\"input\" label=\"refine, maximum valid expectation value\">" + xp.getMaximumExpectationValueRefinement() + "</note>" + sep
                    + "</bioml>" + sep);
            bw.flush();
            bw.close();
            this.inputFiles.add(inputFile);
            this.inputObjs.add(new S3Object(inputFile));
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        }
    }
    
    public void saveToLocal() throws NoSuchAlgorithmException {
        if (this.jobType == CaperCloud.CUSTOM_DB) {
            this.databaseObj = this.t4c.getDatabaseObj();
        }
        
        String tmpPath = FileUtils.getTempDirectoryPath() + "capercloud";
        String taxonomyFilename = timestamp + "taxonomy.xml";
        
        this.taxonomyFile = new File(tmpPath, taxonomyFilename);
        this.createTaxonomyFile();
        
        for (S3Object obj : spectrumObjs) {
                String inputFilename = obj.getName() + timestamp + ".xml";
                File inputFile = new File(tmpPath, inputFilename);
                this.createInputFile(inputFile, obj);
        }
        
    }
    
    public void saveToS3(final S3Bucket bucket) throws NoSuchAlgorithmException, IOException, S3ServiceException {
        this.bucket = bucket;
        this.saveToLocal();
        final S3Service s3Service = new RestS3Service(this.mainApp.getCloudManager().getCurrentCredentials());
        
        Service<Void> s = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        s3Service.putObject(bucket, taxonomyObj);
                        for (S3Object i : inputObjs) {
                            s3Service.putObject(bucket, i);
                        }
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        super.succeeded(); //To change body of generated methods, choose Tools | Templates.
                        log.info("saved");
                    }
                };
            } 
        };
        s.start();
        log.info("saving...");
    }
    
    public String getCloudJobId() {
        return this.timestamp;
    }
    
    public String userdata() {
        if (jobType == CaperCloud.CUSTOM_DB) {
            String userData = "#!/bin/bash\n"
                    + "echo \"Hello World.  The time is now $(date -R)!\" | tee ~/output.txt\n";
            return Base64.encodeAsString(userData.getBytes());
        }
        return null;
    }
}

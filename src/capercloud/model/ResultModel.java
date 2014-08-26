/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.AnchorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.sf.jfasta.FASTAElement;
import net.sf.jfasta.FASTAFileReader;
import net.sf.jfasta.impl.FASTAElementIterator;
import net.sf.jfasta.impl.FASTAFileReaderImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.XQuery;
import uk.ac.ebi.pride.mzgraph.chart.graph.SpectrumPanel;
import uk.ac.ebi.pride.tools.jmzreader.JMzReader;
import uk.ac.ebi.pride.tools.jmzreader.JMzReaderException;
import uk.ac.ebi.pride.tools.jmzreader.model.Spectrum;
import uk.ac.ebi.pride.tools.mgf_parser.MgfFile;

/**
 *
 * @author shuai
 */
public class ResultModel {
    
    private String bedUrl;
    private File spectraFile;
    
    private JMzReader jmzReader;
    private ObservableList<PeptideModel> peptideList;
    private HashMap<String, List<String>> knownPeptides;
    private Map<String, ObservableList<SpectrumModel>> peptideToSpectrumMap;
    private Context context;
    
    private SpectrumPanel spectrumPanel;
    
    public ResultModel() {
        this.peptideList = FXCollections.observableArrayList();
        this.peptideToSpectrumMap = new HashMap<>();
        this.context = new Context();
    }
    
    //getters and setters

    public ObservableList<PeptideModel> getPeptideList() {
        return peptideList;
    }
    
    public ObservableList<SpectrumModel> getSpectrumList(String peptideId) {
        return this.peptideToSpectrumMap.get(peptideId);
    }

    public void setBedUrl(String bedUrl) {
        this.bedUrl = bedUrl;
    }

    public void setSpectraFile(File spectraFile) {
        this.spectraFile = spectraFile;
        try {
            this.jmzReader = new MgfFile(this.spectraFile);
        } catch (JMzReaderException ex) {
            Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getBedUrl() {
        return bedUrl;
    }

    public File getSpectraFile() {
        return spectraFile;
    }
    
    public void parse(File resultFile, int jobType) throws BaseXException {
        this.peptideList.clear();
        this.peptideToSpectrumMap.clear();
        
        String querySpectrumIdentificationResult =
                "declare default element namespace \"http://psidev.info/psi/pi/mzIdentML/1.1\";"
                + "for $sir in doc('" + resultFile.getAbsolutePath() + "')//SpectrumIdentificationResult "
                + "for $sii in $sir//SpectrumIdentificationItem "
                + "where $sii/@rank=1 and count($sii/PeptideEvidenceRef)=1 "//rank 1 and unique genome location
                + "let $spectrumId:=data($sir/@spectrumID) "
                + "let $pr:=data($sii/@peptide_ref) "
                + "let $calMZ:=data($sii/@calculatedMassToCharge) "
                + "let $expMZ:=data($sii/@experimentalMassToCharge) "
                + "let $chargeState:=data($sii/@chargeState) "
                + "let $score:=string-join(data($sii//cvParam/@value), \",\") "
                + "return string-join(($spectrumId, $pr, $calMZ, $expMZ, $chargeState, $score),',')";
        String[] sirs = query(querySpectrumIdentificationResult).split(" ");
        for (String sir : sirs) {
            String[] tokens = sir.split(",");
            String spectrumId = tokens[0].substring(6);
            String peptideRef = tokens[1];
            String calculatedMassToCharge = tokens[2];
            String experimentalMassToCharge = tokens[3];
            String chargeState = tokens[4];
            String xExpect = tokens[5];
            String xScore = tokens[6];
            String localFdr = tokens[7];
            String qValue = tokens[8];
            String fdrScore = tokens[9];
            if (this.peptideToSpectrumMap.containsKey(peptideRef)) {
                ObservableList ol = this.peptideToSpectrumMap.get(peptideRef);
                ol.add(new SpectrumModel(spectrumId, calculatedMassToCharge, experimentalMassToCharge, chargeState, xExpect, xScore, localFdr, qValue, fdrScore));
            } else {
                ObservableList ol = FXCollections.observableArrayList();
                ol.add(new SpectrumModel(spectrumId, calculatedMassToCharge, experimentalMassToCharge, chargeState, xExpect, xScore, localFdr, qValue, fdrScore));
                this.peptideToSpectrumMap.put(peptideRef, ol);
            }
        }
        
        for (String peptideRef: this.peptideToSpectrumMap.keySet()) {
            String queryPeptideEvidence = "declare default element namespace \"http://psidev.info/psi/pi/mzIdentML/1.1\";"
                    + "for $pe in doc('" + resultFile.getAbsolutePath() + "')//PeptideEvidence "
                    + "where $pe/@peptide_ref=\"" + peptideRef + "\" "
                    + "let $isDecoy:=data($pe/@isDecoy) "
                    + "let $end:=data($pe/@end) "
                    + "let $start:=data($pe/@start) "
                    + "let $dBSequenceRef:=data($pe/@dBSequence_ref) "
                    + "return ($isDecoy,$start,$end,$dBSequenceRef)";
            String pe = query(queryPeptideEvidence);
            String[] tokens = pe.split(" ");
            String isDecoy = tokens[0];
            int relStartPos = Integer.parseInt(tokens[1]);
            int relEndPos = Integer.parseInt(tokens[2]);
            String seqDescription = tokens[3];
            
            String[] peptide = peptideRef.split("_");
            StringBuilder mod = new StringBuilder();
            String seq = peptide[0];
            if (peptide.length == 2) {
                mod.append(peptide[1]);
            }
            if (peptide.length == 3) {
                mod.append(peptide[1]).append(peptide[2]);
            }
            
            // real peptide
            if (isDecoy.equals("false")) {     
                if (jobType == 1) {
                    Pattern descPattern = Pattern.compile("dbseq_(.*)\\|SIX-FRAME\\|(.*):(-?\\d)\\|orf:(.*)"); 
                    Matcher descMatcher = descPattern.matcher(seqDescription);
                    if (descMatcher.find()) {
//                        System.out.println(seqDescription);
                        String id = descMatcher.group(1) + "_" + descMatcher.group(4);
                        String chrom = descMatcher.group(2);
                        String strand = descMatcher.group(3);
                        String description = "ORF:" + descMatcher.group(4);
                        String genomicRegions = descMatcher.group(4);
                        
                        Range region = reconstructRanges(genomicRegions).get(0);
                        int nnStartPos = region.getStartPos() + relStartPos * 3;
                        int nnEndPos = region.getStartPos() + relEndPos * 3 + 2;
                        
//                        System.out.println(nnStartPos + " " + nnEndPos);
                        boolean isKnown = false;        
                        if (this.knownPeptides != null) {
                            String tmp_id = chrom + "_" + strand;
                            List<String> rl = this.knownPeptides.get(tmp_id);
                            for (String i: rl) {
                                if (i.contains(seq)) {
                                    isKnown = true;
                                    break;
                                }
                            }
                        }
                        
                        if (isKnown) {
                            continue;
                        }
                        
                        PeptideModel pm = new PeptideModel(peptideRef, id, chrom, strand, mod.toString(), seq, description);
                        pm.addRegions(new Range(nnStartPos, nnEndPos));
                        this.peptideList.add(pm);
                    }
                }
                
                if (jobType == 2 || jobType == 4) {
                    Pattern descPattern = Pattern.compile("dbseq_(.*)\\|(VAR|VCF)\\|(.*):(-?\\d)\\|(.*:\\w/\\w)/(\\d+)\\|cds:(.*)"); 
                    Matcher descMatcher = descPattern.matcher(seqDescription);
                    if (descMatcher.find()) {
//                        System.out.println(seqDescription);
//                        System.out.println(descMatcher.group(1) + " " + descMatcher.group(2) + " " + descMatcher.group(3) + " " + descMatcher.group(4) + " " + descMatcher.group(5));
                        String id = descMatcher.group(1);
                        String chrom = descMatcher.group(3);
                        String strand = descMatcher.group(4);
                        String description = descMatcher.group(5);
                        int varPos = Integer.parseInt(descMatcher.group(6));
                        String genomicRegions = descMatcher.group(7);
                        
                        PeptideModel pm = new PeptideModel(peptideRef, id, chrom, strand, mod.toString(), seq, description);
                        
//                        System.out.println(relStartPos + " " + varPos + " " + relEndPos);
                        //variant point in peptide
                        if (relStartPos<=varPos && varPos<=relEndPos) {
//                            System.out.println(seqDescription);
                            ArrayList<Range> regions = this.reconstructRanges(genomicRegions);
                            //0-based
                            int nnStartPos = relStartPos * 3 - 3;
                            int nnEndPos = relEndPos * 3 - 3;
                            
                            int window = 0;
                            int leftCrIndex = -1;
                            int leftInOffset = 0;
                            for (int i=0; i<regions.size(); i++) {
                                window = window + regions.get(i).getLength();
                                if (nnStartPos <= window) {
                                    leftCrIndex = i;
                                    leftInOffset = nnStartPos - window + regions.get(i).getLength();
                                    break;
                                }
                            }
                            
                            window = 0;
                            int rightCrIndex = -2;
                            int rightInOffset = 0;
                            for (int i=0; i<regions.size(); i++) {
                                window = window + regions.get(i).getLength();
                                if (nnEndPos <= window) {
                                    rightCrIndex = i;
                                    rightInOffset = nnEndPos - window + regions.get(i).getLength();
                                    break;
                                }
                            }
                            
                            if (leftCrIndex == rightCrIndex) {
                                System.out.println(leftCrIndex+" "+rightCrIndex+" "+nnStartPos+" "+nnEndPos+" "+genomicRegions+" "+window);
                                int startPos = leftInOffset + regions.get(leftCrIndex).getStartPos();
                                int endPos = rightInOffset + regions.get(rightCrIndex).getStartPos() + 2;
                                Range r = new Range(startPos, endPos);
                                pm.addRegions(r);
                            } else {
                                System.out.println(leftCrIndex+" "+rightCrIndex+" "+nnStartPos+" "+nnEndPos+" "+genomicRegions+" "+window);
                                int leftStartPos = leftInOffset + regions.get(leftCrIndex).getStartPos();
                                int leftEndPos = regions.get(leftCrIndex).getEndPos();
                                Range newLeftCr = new Range(leftStartPos, leftEndPos);
                                pm.addRegions(newLeftCr);
                                
                                for (int i=leftCrIndex+1; i<rightCrIndex; i++) {
                                    pm.addRegions(regions.get(i));
                                }
                                
                                int rightStartPos = regions.get(rightCrIndex).getStartPos();
                                int rightEndPos = rightInOffset + regions.get(rightCrIndex).getStartPos() + 2;
                                Range newRightCr = new Range(rightStartPos, rightEndPos);
                                pm.addRegions(newRightCr);
                            }
                            this.peptideList.add(pm);
                        } else {
                            continue;
                        }  
                    }
                }
                
                if (jobType == 3) {
                    Pattern descPattern = Pattern.compile("dbseq_(.*)\\|EEJ\\|(.*):(-?\\d)\\|(.*/[012])/(\\d+)\\|cds:(.*)"); 
                    Matcher descMatcher = descPattern.matcher(seqDescription);
                    if (descMatcher.find()) {
                        String id = descMatcher.group(1);
                        String chrom = descMatcher.group(2);
                        String strand = descMatcher.group(3);
                        String description = descMatcher.group(4);
                        int jPos = Integer.parseInt(descMatcher.group(5));
                        String genomicRegions = descMatcher.group(6);
                        
                        //junction site in peptide
                        if (relStartPos<=jPos && jPos<=relEndPos) {
//                            System.out.println(seqDescription);
                            ArrayList<Range> regions = reconstructRanges(genomicRegions);
                            int firstStartPos = regions.get(0).getStartPos() + (jPos - relStartPos) * 3;
                            int secondEndPos = regions.get(1).getStartPos() + (relEndPos - jPos) * 3 + 2;
                            
                            int firstEndPos = regions.get(0).getEndPos();
                            int secondStartPos = regions.get(1).getStartPos();
                            
                            if (firstStartPos > firstEndPos) {
                                firstStartPos = firstEndPos - 3;
                            }
                            
                            Range firstRegion = new Range(firstStartPos, firstEndPos);
                            Range secondRegion = new Range(secondStartPos, secondEndPos);
                            
                            PeptideModel pm = new PeptideModel(peptideRef, id, chrom, strand, mod.toString(), seq, description);
                            pm.addRegions(firstRegion);
                            pm.addRegions(secondRegion);
                            this.peptideList.add(pm);
                        }
                    }
                }
            }
        }
        generateBed();
    }
    
    private ArrayList<Range> reconstructRanges(String regions) {
        ArrayList<Range> res = new ArrayList<>();
        Pattern p = Pattern.compile("(\\d+)\\.\\.(\\d*)");
        Matcher m = p.matcher(regions);
        while (m.find()) {
            int startPos = Integer.parseInt(m.group(1));
            int endPos = Integer.parseInt(m.group(2));
            res.add(new Range(startPos, endPos));
        }
        return res;
    }
    
    private String query(String query) throws BaseXException {
        return new XQuery(query).execute(context);
    }
    
    public void init(AnchorPane pane) {
        this.spectrumPanel = new SpectrumPanel();
        SwingNode swingNode = new SwingNode();
        this.createSwingContent(swingNode, spectrumPanel);
        pane.getChildren().add(swingNode);
    }
    
    public void generateBed() {
        File bedFile = new File("result.bed");
        if (bedFile.exists()) {
            FileUtils.deleteQuietly(bedFile);
        }
        
        StringBuilder line = new StringBuilder();
        line.append("track name=\"capercloud\" description=\"capercloud-generated track\" visibility=1 itemRgb=\"On\"")
                .append(IOUtils.LINE_SEPARATOR);
        
        try {
            FileUtils.writeStringToFile(bedFile , line.toString(), true);
            } catch (IOException ex) {
                Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        
        for (PeptideModel pm : this.peptideList) {
//random generate color
            int R = (int)(Math.random()*256);
            int G = (int)(Math.random()*256);
            int B= (int)(Math.random()*256);
            String strand = null;
            if (pm.strandProperty().get().equals("1")) {
                strand = "+";
            } else {
                strand = "-";
            }
        
            line = new StringBuilder();
            ArrayList<Range> regions = pm.getRegions();
            for (Range r : regions) {
                line.append("chr")
                    .append(pm.chromProperty().get())
                    .append("\t")
                    .append(r.getStartPos())
                    .append("\t")
                    .append(r.getEndPos())
                    .append("\t")
                    .append(pm.idProperty().get())
                    .append("\t")
                    .append("0")
                    .append("\t")
                    .append(strand)
                    .append("\t")
                    .append(r.getStartPos())
                    .append("\t")
                    .append(r.getEndPos())
                    .append("\t")
                    .append(R).append(",").append(G).append(",").append(B)
                    .append(IOUtils.LINE_SEPARATOR);
            }
            try {
                FileUtils.writeStringToFile(bedFile , line.toString(), true);
            } catch (IOException ex) {
                Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void drawSpectrum(int index) throws JMzReaderException {     
        if (this.jmzReader == null) {
            return;
        }
        Spectrum spectrum = this.jmzReader.getSpectrumByIndex(index);
        List<Double> mzArrList = new ArrayList<>();
        List<Double> intentArrList = new ArrayList<>();
        
        Iterator it = spectrum.getPeakList().entrySet().iterator();
        while(it.hasNext()){
            Map.Entry e = (Map.Entry)it.next();
            mzArrList.add((Double) e.getKey());
            intentArrList.add((Double) e.getValue());
        }
        
        double[] mzArr = new double[mzArrList.size()];
        for (int i = 0; i < mzArr.length; i++) {
           mzArr[i] = mzArrList.get(i);
        }
        
        double[] intentArr = new double[intentArrList.size()];
        for (int i = 0; i < intentArr.length; i++) {
           intentArr[i] = intentArrList.get(i);
        }
        spectrumPanel.setPeaks(mzArr, intentArr);
        spectrumPanel.paintGraph();
    }
    
    private void createSwingContent(final SwingNode swingNode, final JPanel container) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                swingNode.setContent(container);
            }
        });
    }
    
    public int getJobTypeFromResult(File resultFile) {
        
        String accession = "none";
        String queryDbSequence =
                "declare default element namespace \"http://psidev.info/psi/pi/mzIdentML/1.1\";"
                + "let $ds:=doc('" + resultFile.getAbsolutePath() + "')//DBSequence "
                + "let $accession:=data($ds/@accession) "
                + "return $accession";
        try {
            accession = query(queryDbSequence);
        } catch (BaseXException ex) {
            return 0;
        }
        if (accession.contains("SIX-FRAME")) {
            return 1;
        }
        if (accession.contains("VAR")) {
            return 2;
        }
        if (accession.contains("EEJ")) {
            return 3;
        }
        if (accession.contains("VCF")) {
            return 4;
        }
        return 0;
    }
    
    public String getSpectraFilenameFromResult(File resultFile) {
        String querySpectraData =
                "declare default element namespace \"http://psidev.info/psi/pi/mzIdentML/1.1\";"
                + "let $ds:=doc('" + resultFile.getAbsolutePath() + "')//SpectraData "
                + "let $location:=data($ds/@location) "
                + "return $location";
        String fileName = null;
        try {
            fileName = query(querySpectraData);
        } catch (BaseXException ex) {
            Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fileName;
    }
    
    public void loadKnownPeptide(File knownPeptideFile) {
        try {
            this.knownPeptides = new HashMap<>();
            FASTAFileReader reader = new FASTAFileReaderImpl(knownPeptideFile);
            FASTAElementIterator it = reader.getIterator();
            while (it.hasNext()) {
                FASTAElement el = it.next();
                String header = el.getHeader();
                String seq = el.getSequence();
                if (seq.contains("*")) {
                    continue;
                }
                
                Pattern p = Pattern.compile("GRCh37:([1-9XY][1-9]?):\\d+:\\d+:(-?1)");
                Matcher m = p.matcher(header);
                if (m.find()) {
                    String chrom = m.group(1);
                    String strand = m.group(2);
                    String id = chrom + "_" + strand;
                    
                    if (this.knownPeptides.containsKey(id)) {
                        List l = this.knownPeptides.get(id);
                        l.add(seq);
                    } else {
                        List l = new ArrayList<Range>();
                        l.add(seq);
                        this.knownPeptides.put(id, l);
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}

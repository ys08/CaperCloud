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
        if (jobType == 1) {
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
                if (isDecoy.equals("false")) { 
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
                        int nnStartPos = region.getStartPos() + relStartPos * 3 - 4;
                        int nnEndPos = region.getStartPos() + relEndPos * 3 - 1;

                        if ("-1".equals(strand)) {
                            nnStartPos = region.getEndPos() - relEndPos * 3 + 2;
                            nnEndPos = region.getEndPos() - relStartPos * 3 + 5;
                        }
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
            }
        }

        if (jobType == 2) {
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
            
                if (isDecoy.equals("false")) { 
                    Pattern descPattern = Pattern.compile("dbseq_(.*)\\|(VAR)\\|(.*):(-?\\d)\\|(.*:\\w/\\w)/(\\d+)\\|cds:(.*)"); 
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
                        
                        //variant point in peptide
                        if (relStartPos<varPos && varPos<relEndPos) {
                            CodingRegion cr = this.reconstructCodingRegion(genomicRegions);
                            //0-based
                            int nnStartPos = relStartPos * 3 - 3;
                            int nnEndPos = relEndPos * 3 - 1;
                            if (strand.equals("1")) {
                                String codingRegion = cr.intervalsOf(nnStartPos, nnEndPos);
//                                System.out.println("1");
//                                System.out.println("chr"+chrom+":"+codingRegion);
//                                System.out.println(pm.getPeptideRef());
                                String[] manyRegions = codingRegion.split(",");
                                for (String r : manyRegions) {
                                    String[] tmpR = r.split("-");
                                    pm.addRegions(new Range(Integer.parseInt(tmpR[0]), Integer.parseInt(tmpR[1])));
                                }
                            } else {
                                String codingRegion = cr.negtiveIntervalsOf(nnStartPos, nnEndPos);
//                                System.out.println("-1");
//                                System.out.println("chr"+chrom+":"+codingRegion);
//                                System.out.println(pm.getPeptideRef());
                                String[] manyRegions = codingRegion.split(",");
                                for (String r : manyRegions) {
                                    String[] tmpR = r.split("-");
                                    pm.addRegions(new Range(Integer.parseInt(tmpR[0]), Integer.parseInt(tmpR[1])));
                                }
                            }
                            this.peptideList.add(pm);
                        } else {
                            continue;
                        }
                    }
                }
            }
        }

        if (jobType == 3) {
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
                if (isDecoy.equals("false")) {
                    Pattern descPattern = Pattern.compile("dbseq_(.*)\\|EEJ\\|(.*):(\\d)\\|(.*/)([012])/(\\d+)\\|cds:(.*)"); 
                    Matcher descMatcher = descPattern.matcher(seqDescription);
                    if (descMatcher.find()) {
                        String id = descMatcher.group(1);
                        String chrom = descMatcher.group(2);
                        String strand = descMatcher.group(3);

                        String description = descMatcher.group(4);
                        int phase = Integer.parseInt(descMatcher.group(5));
                        description = description + "/" + phase;
                        int jPos = Integer.parseInt(descMatcher.group(6));
                        String genomicRegions = descMatcher.group(7);

                        //junction site in peptide
                        if (relStartPos<jPos && jPos<relEndPos) {
                            PeptideModel pm = new PeptideModel(peptideRef, id, chrom, strand, mod.toString(), seq, description);
                            String[] twoRegions = genomicRegions.split(",");
                            String[] first = twoRegions[0].split("-");
                            String[] second = twoRegions[1].split("-");
                            int firstRight = Integer.parseInt(first[1]);
                            int secondLeft = Integer.parseInt(second[0]);
                            
                            int firstLeft = 0;
                            int secondRight = 0;
                            switch (phase) {
                                case 0: {
                                    firstLeft = firstRight - 3 * (jPos - relStartPos) + 1;
                                    secondRight = secondLeft + 3 * (relEndPos - jPos + 1) - 1;
                                    break;
                                }
                                case 1: {
                                    firstLeft = firstRight - 3 * (jPos - relStartPos);
                                    secondRight = secondLeft + 3 * (relEndPos - jPos) + 1;
                                    break;
                                }
                                case 2: {
                                    firstLeft = firstRight - 3 * (jPos - relStartPos) + 2;
                                    secondRight = secondLeft + 3 * (relEndPos - jPos + 1);
                                    break;
                                }
                                default: break;
                            }
//                            System.out.println(phase);
//                            System.out.println("chr"+chrom+":"+firstLeft+"-"+firstRight+","+secondLeft+"-"+secondRight);
//                            System.out.println(pm.getPeptideRef());
                            pm.addRegions(new Range(firstLeft, firstRight));
                            pm.addRegions(new Range(secondLeft, secondRight));
                            this.peptideList.add(pm);
                        }
                    }
                }
            }
        }
        if (jobType == 4) {
            String querySpectrumIdentificationResult =
                    "declare default element namespace \"http://psidev.info/psi/pi/mzIdentML/1.1\";"
                    + "for $sir in doc('" + resultFile.getAbsolutePath() + "')//SpectrumIdentificationResult "
                + "for $sii in $sir//SpectrumIdentificationItem "
                + "where $sii/@rank=1 "//only rank1
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
                //query the first protein that generate the reference peptide
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
                if (isDecoy.equals("false")) {
                    Pattern descPattern = Pattern.compile("dbseq_(.*)\\|(VCF)\\|(.*):(-?\\d)\\|(.*):(\\d+)\\|cds:(.*)"); 
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

                        //variant point in peptide
                        if (relStartPos<varPos && varPos<relEndPos) {
                            CodingRegion cr = this.reconstructCodingRegion(genomicRegions);
                            //0-based
                            int nnStartPos = relStartPos * 3 - 3;
                            int nnEndPos = relEndPos * 3 - 1;
                            if (strand.equals("1")) {
                                String codingRegion = cr.intervalsOf(nnStartPos, nnEndPos);
//                                System.out.println("1");
//                                System.out.println("chr"+chrom+":"+codingRegion);
//                                System.out.println(pm.getPeptideRef());
                                String[] manyRegions = codingRegion.split(",");
                                for (String r : manyRegions) {
                                    String[] tmpR = r.split("-");
                                    pm.addRegions(new Range(Integer.parseInt(tmpR[0]), Integer.parseInt(tmpR[1])));
                                }
                            } else {
                                String codingRegion = cr.negtiveIntervalsOf(nnStartPos, nnEndPos);
//                                System.out.println("-1");
//                                System.out.println("chr"+chrom+":"+codingRegion);
//                                System.out.println(pm.getPeptideRef());
                                String[] manyRegions = codingRegion.split(",");
                                for (String r : manyRegions) {
                                    String[] tmpR = r.split("-");
                                    pm.addRegions(new Range(Integer.parseInt(tmpR[0]), Integer.parseInt(tmpR[1])));
                                }
                            }
                            this.peptideList.add(pm);
                        } else {
                            continue;
                        }  
                    }
                } 
            }
        }
        generateGff();
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
    
    private CodingRegion reconstructCodingRegion(String regions) {
        CodingRegion cr = new CodingRegion();
        Pattern p = Pattern.compile("(\\d+)-(\\d*)");
        Matcher m = p.matcher(regions);
        while (m.find()) {
            int startPos = Integer.parseInt(m.group(1));
            int endPos = Integer.parseInt(m.group(2));
            cr.addInterval(startPos, endPos);
        }
        return cr;
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
    
    public void generateGff() {
        File gffFile = new File("result.gff");
        if (gffFile.exists()) {
            FileUtils.deleteQuietly(gffFile);
        }
        
        for (PeptideModel pm : this.peptideList) {
            String strand = null;
            if (pm.strandProperty().get().equals("1")) {
                strand = "+";
            } else {
                strand = "-";
            }
            StringBuilder parentLine = new StringBuilder();
            Range parentRange = pm.getParentRange();
            parentLine.append("chr")
                    .append(pm.chromProperty().get())
                    .append("\t")
                    .append("capercloud")
                    .append("\t")
                    .append("mRNA")
                    .append("\t")
                    .append(parentRange.getStartPos())
                    .append("\t")
                    .append(parentRange.getEndPos())
                    .append("\t")
                    .append(".")
                    .append("\t")
                    .append(strand)
                    .append("\t")
                    .append(".")
                    .append("\t")
                    .append("ID=").append(pm.peptideSeqProperty().get())
                    .append(IOUtils.LINE_SEPARATOR);
            StringBuilder childLine = new StringBuilder();
            ArrayList<Range> regions = pm.getRegions();
            for (Range childRange : regions) {
                childLine.append("chr")
                    .append(pm.chromProperty().get())
                    .append("\t")
                    .append("capercloud")
                    .append("\t")
                    .append("CDS")
                    .append("\t")
                    .append(childRange.getStartPos())
                    .append("\t")
                    .append(childRange.getEndPos())
                    .append("\t")
                    .append(".")
                    .append("\t")
                    .append(strand)
                    .append("\t")
                    .append(".")
                    .append("\t")
                    .append("Parent=").append(pm.peptideSeqProperty().get())
                    .append(IOUtils.LINE_SEPARATOR);
            }
            try {
                FileUtils.writeStringToFile(gffFile , parentLine.toString(), true);
                FileUtils.writeStringToFile(gffFile , childLine.toString(), true);
            } catch (IOException ex) {
                Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void generateBed() {
        File bedFile = new File("result.bed");
        if (bedFile.exists()) {
            FileUtils.deleteQuietly(bedFile);
        }
        
        StringBuilder line = new StringBuilder();
        line.append("browser hide CCPDPeptides CCPDProteins EnsemblmRNA ensembleProtein").append(IOUtils.LINE_SEPARATOR);
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

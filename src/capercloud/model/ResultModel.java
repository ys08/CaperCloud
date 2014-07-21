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
    private File resultFile;
    
    private String bedUrl;
    private File spectraFile;
    
    private JMzReader jmzReader;
    private ObservableList<PeptideModel> peptideList;
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
        this.resultFile = resultFile;
        this.peptideList.clear();
        this.peptideToSpectrumMap.clear();
        
        String querySpectrumIdentificationResult =
                "declare default element namespace \"http://psidev.info/psi/pi/mzIdentML/1.1\";"
                + "for $sir in doc('" + this.resultFile.getAbsolutePath() + "')//SpectrumIdentificationResult "
                + "let $id:=data($sir/@spectrumID) "
                + "for $sii in $sir//SpectrumIdentificationItem "
                + "where $sii/@rank=\"1\" "
                + "let $pr:=data($sii/@peptide_ref) "
                + "let $cal:=data($sii/@calculatedMassToCharge) "
                + "let $exp:=data($sii/@experimentalMassToCharge) "
                + "let $score:=string-join(data($sii//cvParam/@value), \",\") "
                + "return string-join(($id,$pr,$cal,$exp,$score), \",\")";
        String[] sirs = query(querySpectrumIdentificationResult).split(" ");
        for (String sir : sirs) {
            String[] attributes = sir.split(",");
            if (this.peptideToSpectrumMap.containsKey(attributes[1])) {
                ObservableList ol = this.peptideToSpectrumMap.get(attributes[1]);
                ol.add(new SpectrumModel(attributes[0].substring(6), attributes[2], attributes[3], attributes[4], attributes[5], attributes[6], attributes[7], attributes[8]));
            } else {
                ObservableList ol = FXCollections.observableArrayList();
                ol.add(new SpectrumModel(attributes[0].substring(6), attributes[2], attributes[3], attributes[4], attributes[5], attributes[6], attributes[7], attributes[8]));
                this.peptideToSpectrumMap.put(attributes[1], ol);

                String queryPeptideEvidence = 
                        "declare default element namespace \"http://psidev.info/psi/pi/mzIdentML/1.1\";"
                        + "for $pe in doc('" + this.resultFile.getAbsolutePath() + "')//PeptideEvidence "
                        + "where $pe/@peptide_ref=\"" + attributes[1] + "\" "
                        + "let $id:=data($pe/@id) "
                        + "let $end:=data($pe/@end) "
                        + "let $start:=data($pe/@start) "
                        + "let $dBSequence_ref:=data($pe/@dBSequence_ref) "
                        + "return string-join(($id,$start,$end,$dBSequence_ref), \",\")";
                String pe = query(queryPeptideEvidence);
                //filter peptides that have multiple peptide evidence(that is, the peptide may map to multiple genomic location
                if (pe.split("PE").length == 2) {
                    String[] pe_attrs = pe.split(",");
                    String description = pe_attrs[3];
                    Pattern p = Pattern.compile("dbseq_(\\d+)\\s(\\d):(\\d+)-(\\d+)");
                    Matcher m = p.matcher(description);
                    if (m.find()) {
                        //test on chrom 1
                        String chrom = m.group(1);
                        String frame = m.group(2);
                        String proteinStart = m.group(3);
                        String proteinStop = m.group(4);
                        String[] peptide = attributes[1].split("_");
                        StringBuilder mod = new StringBuilder();
                        String seq = peptide[0];
                        if (peptide.length == 2) {
                            mod.append(peptide[1]);
                        }
                        if (peptide.length == 3) {
                            mod.append(peptide[1]).append(peptide[2]);
                        }
                        this.peptideList.add(new PeptideModel(attributes[1], chrom, seq, proteinStart, proteinStop, pe_attrs[1], pe_attrs[2], mod.toString()));
                    }
                }
            }
        }
    generateBed();
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
        line.append("track name= \"capercloud\" description=\"capercloud-generated track\" visibility=1 itemRgb=\"On\"")
                .append(IOUtils.LINE_SEPARATOR);
        
        try {
            FileUtils.writeStringToFile(bedFile , line.toString(), true);
            } catch (IOException ex) {
                Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
            }
        for (PeptideModel pm : this.peptideList) {
            line = new StringBuilder();
            line.append("chr")
                    .append(pm.chromProperty().get())
                    .append("\t")
                    .append(pm.proteinStartProperty().get())
                    .append("\t")
                    .append(pm.proteinEndProperty().get())
                    .append("\t")
                    .append(pm.getId())
                    .append("\t")
                    .append("0")
                    .append("\t")
                    .append("+")
                    .append("\t")
                    .append(pm.proteinStartProperty().get())
                    .append("\t")
                    .append(pm.proteinEndProperty().get())
                    .append("\t")
                    .append("255,128,128")
                    .append(IOUtils.LINE_SEPARATOR);
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
}

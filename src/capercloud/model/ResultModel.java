/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.AnchorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.basex.core.BaseXException;
import org.basex.core.Context;
import org.basex.core.cmd.XQuery;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLUnmarshaller;
import uk.ac.ebi.pride.mzgraph.chart.graph.SpectrumPanel;
import uk.ac.ebi.pride.tools.jmzreader.JMzReader;
import uk.ac.ebi.pride.tools.jmzreader.JMzReaderException;
import uk.ac.ebi.pride.tools.jmzreader.model.Spectrum;

/**
 *
 * @author shuai
 */
public class ResultModel {
    private File resultFile;
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

    public void load(File resultFile, File spectraFile) throws JMzReaderException {
        this.resultFile = resultFile;
        this.spectraFile = spectraFile;
        
        String query =
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
        try {
            query(query);
        } catch (BaseXException ex) {
            Logger.getLogger(ResultModel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void query(String query) throws BaseXException {
        System.out.println(new XQuery(query).execute(context));
    }
    
    public void init(AnchorPane pane) {
        this.spectrumPanel = new SpectrumPanel();
        SwingNode swingNode = new SwingNode();
        this.createSwingContent(swingNode, spectrumPanel);
        pane.getChildren().add(swingNode);
    }
    
    public void drawSpectrum(int index) throws JMzReaderException {     
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

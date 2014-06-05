/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.AnchorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import uk.ac.ebi.jmzidml.model.mzidml.AnalysisData;
import uk.ac.ebi.jmzidml.model.mzidml.DataCollection;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationList;
import uk.ac.ebi.jmzidml.model.mzidml.SpectrumIdentificationResult;
import uk.ac.ebi.jmzidml.xml.io.MzIdentMLUnmarshaller;
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
    private File spectraFile;
    
    private JMzReader jmzReader;
    private MzIdentMLUnmarshaller unmarshaller;
    private ObservableList<SpectrumIdentificationResult> spectrumIdentificationResult;
    
    SpectrumPanel spectrumPanel;
    
    public ResultModel() {
        this.spectrumIdentificationResult = FXCollections.observableArrayList();
    }
    
    //getters and setters
    
    public ObservableList<SpectrumIdentificationResult> getSpectrumIdentificationResult() {
        return spectrumIdentificationResult;
    
    }

    public void load(File resultFile, File spectraFile) throws JMzReaderException {
        this.resultFile = resultFile;
        this.spectraFile = spectraFile;
        
        this.jmzReader = new MgfFile(this.spectraFile);
        this.unmarshaller = new MzIdentMLUnmarshaller(this.resultFile);
        
        DataCollection dc =  unmarshaller.unmarshal(DataCollection.class);
        AnalysisData ad = dc.getAnalysisData();
        List<SpectrumIdentificationList> sil = ad.getSpectrumIdentificationList();
        
        for (SpectrumIdentificationList sIdentList : sil) {
            for (SpectrumIdentificationResult spectrumIdentResult 
                    : sIdentList.getSpectrumIdentificationResult()) {
                this.spectrumIdentificationResult.add(spectrumIdentResult);
//            // as MGF files are index based the "index=" portion of the spectrumId needs to be removed
//            String spectrumIndex = spectrumID.substring(6);
//
//            //  and since the index in mzid files is 0 based and the index in jmzReader is 1 based, this has to be addressed as well
//            int index = Integer.parseInt(spectrumIndex);
//            index = index + 1;
//            System.out.println(index);
//            // using the index the spectrum can now be retrieved from the
//            // MGF file.
//            Spectrum spectrum = mgfParser.getSpectrumByIndex(index);
            }
        }
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.awt.BorderLayout;
import java.io.File;
import java.util.List;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javax.swing.JPanel;
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
    
    public ResultModel(File resultFile, File spectraFile) {
        this.resultFile = resultFile;
        this.spectraFile = spectraFile;
    }
    
    public void test() throws JMzReaderException {
        JMzReader jmzreader = new MgfFile(this.spectraFile);
        MzIdentMLUnmarshaller unmarshaller = new MzIdentMLUnmarshaller(this.resultFile);
        
        DataCollection dc =  unmarshaller.unmarshal(DataCollection.class);
        AnalysisData ad = dc.getAnalysisData();

        List<SpectrumIdentificationList> sil = ad.getSpectrumIdentificationList();

        for (SpectrumIdentificationList sIdentList : sil) {
            for (SpectrumIdentificationResult spectrumIdentResult 
                    : sIdentList.getSpectrumIdentificationResult()) {
            String spectrumID =  spectrumIdentResult.getSpectrumID(); // this returns a value like "index=246"
            System.out.println(spectrumID);
            String spectrumFileRef = spectrumIdentResult.getSpectraDataRef();
            System.out.println(spectrumFileRef);

            // as MGF files are index based the "index=" portion of the spectrumId needs to be removed
            String spectrumIndex = spectrumID.substring(6);

            //  and since the index in mzid files is 0 based and the index in jmzReader is 1 based, this has to be addressed as well
            int index = Integer.parseInt(spectrumIndex);
            index = index + 1;
            System.out.println(index);
            // using the index the spectrum can now be retrieved from the
            // MGF file.
            Spectrum spectrum = jmzreader.getSpectrumByIndex(index);
        } // end spectrum identification results
        }
    }
    
    public void drawSpectrum() {
        // Create a m/z data array
        double[] mzArr = new double[]{1.0, 2.012312313, 3.0, 4.234, 6.0, 7.34342};
        // Create an intensity data array
        double[] intentArr = new double[]{2.0, 4.345345345, 6.0, 1.4545, 5.0, 8.23423};
        // Create a spectrum panel
        SpectrumPanel spectrum = new SpectrumPanel(mzArr, intentArr);
        // Paint the spectrum peaks
        spectrum.paintGraph();
        // Added the spectrum panel to your own JPanel
        JPanel container = new JPanel();
        container.add(spectrum, BorderLayout.CENTER);
    }
}

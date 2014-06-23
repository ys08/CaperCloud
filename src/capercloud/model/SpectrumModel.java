/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author shuai
 */
public class SpectrumModel {
    private StringProperty spectrumId;
    private StringProperty calculatedMassToCharge;
    private StringProperty experimentalMassToCharge;
    private StringProperty xtandemExpect;
    private StringProperty xtandemHyperscore;
    private StringProperty percolatorScore;
    private StringProperty percolatorQvalue;
    private StringProperty percolatorPEP;
    
    public SpectrumModel(String spectrumId, String calculatedMassToCharge, String experimentalMassToCharge,
            String xtandemExpect, String xtandemHyperscore, String percolatorScore, String percolatorQvalue, String percolatorPEP) {
        this.spectrumId = new SimpleStringProperty(spectrumId);
        this.calculatedMassToCharge = new SimpleStringProperty(calculatedMassToCharge);
        this.experimentalMassToCharge = new SimpleStringProperty(experimentalMassToCharge);
        this.xtandemExpect = new SimpleStringProperty(xtandemExpect);
        this.xtandemHyperscore = new SimpleStringProperty(xtandemHyperscore);
        this.percolatorScore = new SimpleStringProperty(percolatorScore);
        this.percolatorQvalue = new SimpleStringProperty(percolatorQvalue);
        this.percolatorPEP = new SimpleStringProperty(percolatorPEP);
    }

    public StringProperty spectrumIdProperty() {
        return spectrumId;
    }

    public StringProperty calculatedMassToChargeProperty() {
        return calculatedMassToCharge;
    }

    public StringProperty experimentalMassToChargeProperty() {
        return experimentalMassToCharge;
    }

    public StringProperty xtandemExpectProperty() {
        return xtandemExpect;
    }

    public StringProperty xtandemHyperscoreProperty() {
        return xtandemHyperscore;
    }

    public StringProperty percolatorScoreProperty() {
        return percolatorScore;
    }

    public StringProperty percolatorQvalueProperty() {
        return percolatorQvalue;
    }

    public StringProperty percolatorPEPProperty() {
        return percolatorPEP;
    }
    
}

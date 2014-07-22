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
    private StringProperty chargeState;
    private StringProperty xtandemExpect;
    private StringProperty xtandemHyperscore;
    private StringProperty localFdr;
    private StringProperty qValue;
    private StringProperty fdrScore;
    
    public SpectrumModel(String spectrumId, String calculatedMassToCharge, String experimentalMassToCharge, String chargeState,
            String xtandemExpect, String xtandemHyperscore, String localFdr, String qValue, String fdrScore) {
        this.spectrumId = new SimpleStringProperty(spectrumId);
        this.calculatedMassToCharge = new SimpleStringProperty(calculatedMassToCharge);
        this.experimentalMassToCharge = new SimpleStringProperty(experimentalMassToCharge);
        this.chargeState = new SimpleStringProperty(chargeState);
        this.xtandemExpect = new SimpleStringProperty(xtandemExpect);
        this.xtandemHyperscore = new SimpleStringProperty(xtandemHyperscore);
        this.localFdr = new SimpleStringProperty(localFdr);
        this.qValue = new SimpleStringProperty(qValue);
        this.fdrScore = new SimpleStringProperty(fdrScore);
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
    
    public StringProperty chargeStateProperty() {
        return this.chargeState;
    }

    public StringProperty xtandemExpectProperty() {
        return xtandemExpect;
    }

    public StringProperty xtandemHyperscoreProperty() {
        return xtandemHyperscore;
    }

    public StringProperty localFdrProperty() {
        return localFdr;
    }

    public StringProperty qValueProperty() {
        return qValue;
    }

    public StringProperty fdrScoreProperty() {
        return fdrScore;
    }
}

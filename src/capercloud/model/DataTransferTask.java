/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import javafx.concurrent.Task;

/**
 *
 * @author shuai
 */
public abstract class DataTransferTask extends Task<Void>{
    public abstract String getTransferType();
    public abstract String getFilename();
    public abstract String getFrom();
    public abstract String getTo();

    @Override
    public void updateMessage(String string) {
        super.updateMessage(string); 
    }

    @Override
    public void updateProgress(double d, double d1) {
        super.updateProgress(d, d1); 
    }
    
}

    

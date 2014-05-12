/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;

/**
 *
 * @author shuai
 */
public class DataTransferTask extends Task<Void> {
    private int waitTime; // milliseconds
    private int pauseTime; // milliseconds
    private String fileName;
    private String fromPath;


    public static final int NUM_ITERATIONS = 100;

    public DataTransferTask(String fileName, String fromPath, int waitTime, int pauseTime) {
        this.fileName = fileName;
        this.fromPath = fromPath;
        this.waitTime = waitTime;
        this.pauseTime = pauseTime;
    }
    
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFromPath() {
        return fromPath;
    }

    public void setFromPath(String fromPath) {
        this.fromPath = fromPath;
    }
    
    @Override
    protected Void call() throws Exception {
        this.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
        this.updateMessage("Waiting...");
        Thread.sleep(waitTime);
        this.updateMessage("Running...");
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            updateProgress((1.0 * i) / NUM_ITERATIONS, 1);
            Thread.sleep(pauseTime);
        }
        this.updateMessage("Done");
        this.updateProgress(1, 1);
        return null;
    }
}

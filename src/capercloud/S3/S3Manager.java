/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.S3;

import capercloud.CaperCloud;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.multithread.S3ServiceMulti;
import org.jets3t.service.utils.ByteFormatter;
import org.jets3t.service.utils.TimeFormatter;

/**
 *
 * @author shuai
 */
public class S3Manager {
    private CaperCloud mainApp;
    
    private final ByteFormatter byteFormatter = new ByteFormatter();
    private final TimeFormatter timeFormatter = new TimeFormatter();
    private final SimpleDateFormat yearAndTimeSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat timeSDF = new SimpleDateFormat("HH:mm:ss");
    
    private final HashMap cachedBuckets = new HashMap();
    
    private S3ServiceMulti s3ServiceMulti = null;
    
    public S3Manager(CaperCloud mainApp) {
        this.mainApp = mainApp;
    }
    
    /**
     * Lists the buckets in the user's S3 account and refreshes the GUI to display
     * these buckets. Any buckets or objects already listed in the GUI are cleared first.
     */
    private void listAllBuckets() {
        // Remove current bucket and object data from models.
        cachedBuckets.clear();

        // This is all very convoluted.
        runInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final S3Bucket[] buckets = s3ServiceMulti.getS3Service().listAllBuckets();
                } catch (S3ServiceException ex) {
                    Logger.getLogger(S3Manager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    
    /**
     * Run the provided Runnable object in a background thread. This method will
     * return as soon as the background thread is started, it does not wait for
     * the thread to complete.
     */
    private synchronized void runInBackgroundThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.start();
    }
}

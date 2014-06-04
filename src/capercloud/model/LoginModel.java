/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud.model;

import java.io.File;
import java.io.FilenameFilter;
import javafx.collections.ObservableList;
import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 *
 * @author shuai
 */
public class LoginModel {
    private ObservableList<File> savedCredentialsList;

    
    public ObservableList<File> getSavedCredentialsList() {
        return savedCredentialsList;
    }
    
    public void updateSavedCredentialsList(File inDirectory) {
        FilenameFilter fileFilter = new SuffixFileFilter(".enc");  
        this.savedCredentialsList.clear();
        this.savedCredentialsList.addAll(inDirectory.listFiles(fileFilter));
    }
    
    
}

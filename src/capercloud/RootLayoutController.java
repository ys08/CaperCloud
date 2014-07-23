/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.Dialogs;


/**
 * FXML Controller class
 *
 * @author shuai
 */
public class RootLayoutController implements Initializable {
    
    private CaperCloud mainApp;
    private Log log = LogFactory.getLog(getClass());
    
    @FXML MenuItem miAbout;
    @FXML MenuItem miPrivateCloudMode;
    @FXML CheckMenuItem cmiEucalyptusEnabled;
    
    public void setMainApp(CaperCloud mainApp) {
        this.mainApp = mainApp;
        this.cmiEucalyptusEnabled.selectedProperty().unbind();
        this.cmiEucalyptusEnabled.selectedProperty().bindBidirectional(this.mainApp.getEucalyptusEnabled());
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        this.miAbout.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Dialogs.create()
                        .owner(RootLayoutController.this.mainApp.getPrimaryStage())
                        .title("CAPER version 3.0")
                        .masthead(null)
                        .message("Copyright 2014")
                        .showInformation();
            }
        });
       
        this.miPrivateCloudMode.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                String info = "Your current eucalyputus cloud controller ip address is: " + RootLayoutController.this.mainApp.getEucalyptusClcIpAddress();
                Optional<String> response = Dialogs.create()
                .owner(RootLayoutController.this.mainApp.getPrimaryStage())
                .title("Text Input Dialog")
                .masthead(info)
                .message("Please enter eucalyptus CLC Ip address:")
                .showTextInput("192.168.99.111");

                // One way to get the response value.
                if (response.isPresent()) {
                    RootLayoutController.this.mainApp.setEucalyptusClcIpAddress(response.get());
                }
                
                System.out.println(RootLayoutController.this.mainApp.getEucalyptusEnabled().get());
            }
        });
    }
}

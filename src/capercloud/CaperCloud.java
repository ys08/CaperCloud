/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 *
 * @author shuai
 */
public class CaperCloud extends Application {
    
    private Stage primaryStage;
    
    private BorderPane rootLayout;
    private RootLayoutController rootController;
    private AnchorPane mainView;
    private JobOverviewController mainController;

    
    /**
     * CaperCloud is a Cloud-based Proteogenomics pipeline.
     * Main features:
     * 1. Rubost interact with/monitor Amazon S3 and EC2 Service.
     * 2. Aim to aid four scientific goals - new gene, SAP, AS and custom database.
     * 3. Results can be sent to CAPER server or download to local storage.
     * @author Yang Shuai
     */
    public CaperCloud() {
    }
        public BorderPane getRootLayout() {
        return rootLayout;
    }

    public void setRootLayout(BorderPane rootLayout) {
        this.rootLayout = rootLayout;
    }

    public RootLayoutController getRootController() {
        return rootController;
    }

    public void setRootController(RootLayoutController rootController) {
        this.rootController = rootController;
    }

    public AnchorPane getMainView() {
        return mainView;
    }

    public void setMainView(AnchorPane mainView) {
        this.mainView = mainView;
    }

    public JobOverviewController getMainController() {
        return mainController;
    }

    public void setMainController(JobOverviewController mainController) {
        this.mainController = mainController;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        this.setPrimaryStage(stage);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("view/RootLayout.fxml"));
        
        BorderPane rootLayout = (BorderPane) loader.load();
        this.setRootLayout(rootLayout);
        
        Scene scene = new Scene(rootLayout);
        
        stage.setScene(scene);
        stage.setTitle("CaperCloud");
        stage.show();
        
        this.setRootController((RootLayoutController) loader.getController());
        this.getRootController().setMainApp(this);
        
        //show the tabpane
        this.showJobOverview();
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    public void showJobOverview() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("view/JobOverview.fxml"));
        //the root node is AnchorPane type
        this.setMainView((AnchorPane) loader.load());
        
        this.getRootLayout().setCenter(this.getMainView());
        
        this.setMainController((JobOverviewController) loader.getController());
        //let JobOverviewController know us
        this.getMainController().setMainApp(this);
    }
    
    public void showAboutView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/AboutView.fxml"));
            AnchorPane apAboutView = (AnchorPane) loader.load();
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            
            Scene scene = new Scene(apAboutView);
            dialogStage.setScene(scene);
            
            dialogStage.showAndWait();
            
            //so we can get data in CaperCloud
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void showPreferenceView(int index) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/PreferenceView.fxml"));
            AnchorPane ap = (AnchorPane) loader.load();
            Stage preferenceStage = new Stage();
            preferenceStage.setTitle("Preferences");
            preferenceStage.initModality(Modality.WINDOW_MODAL);
            preferenceStage.initOwner(primaryStage);
            
            Scene scene = new Scene(ap);
            preferenceStage.setScene(scene);
            
            //will not raise nullpointer error here
            PreferenceViewController controller = loader.getController();
            
            controller.selectTabAtIndex(index);
            preferenceStage.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
}

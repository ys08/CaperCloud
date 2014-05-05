/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.model.Result;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    private ObservableList<Result> results;
    private BorderPane rootLayout;
    
    public CaperCloud() {
        if (results == null) {
            results = FXCollections.observableArrayList();
        }
    }
    
    public ObservableList<Result> getResults() {
        return results;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("view/RootLayout.fxml"));
        
        BorderPane rootLayout = (BorderPane) loader.load();
        this.rootLayout = rootLayout;
        
        Scene scene = new Scene(rootLayout);
        
        stage.setScene(scene);
        stage.setTitle("CaperCloud");
        stage.show();
        
        RootLayoutController controller = loader.getController();
        controller.setMainApp(this);
        
        showJobOverview();
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
        AnchorPane apJobView = (AnchorPane) loader.load();
        
        rootLayout.setCenter(apJobView);
        
        JobOverviewController controller = loader.getController();
        //so we can get data in CaperCloud
        controller.setMainApp(this);
        
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

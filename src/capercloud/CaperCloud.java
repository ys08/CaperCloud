/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import capercloud.s3.S3Manager;
import capercloud.ec2.EC2Manager;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.ServiceException;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;

/**
 *
 * @author shuai
 */
public class CaperCloud extends Application {
    
    public static final String APPLICATION_DESCRIPTION = "CaperCloud";
    
    private Stage primaryStage;
    private Stage loginStage;
    private Stage newAccountStage;
    
    private BorderPane rootLayout;
    private RootLayoutController rootController;
    private AnchorPane mainView;
    private JobOverviewController mainController;
    private AnchorPane loginView;
    private LoginViewController loginController;
    private AnchorPane accountView;
    private AccountManagerViewController accountController;
    
    //for cloud management
    private final HashMap<String, AWSCredentials> loginAwsCredentialsMap = new HashMap<>();
    private AWSCredentials currentCredentials; 
    private S3Manager s3m;
    private EC2Manager ec2m;
    
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
       
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getLoginStage() throws IOException {
        if (this.loginStage == null) {
            this.loginStage = new Stage();
        }
        return loginStage;
    }

    public Stage getNewAccountStage() throws IOException {
        if (this.newAccountStage == null) {
            this.newAccountStage = new Stage();
        }
        return newAccountStage;
    }
    
    public BorderPane getRootLayout() throws IOException {
        if (this.rootLayout == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/RootLayout.fxml"));
            this.rootLayout = (BorderPane) loader.load();
            //we should load fxml only one time, so set controller here
            this.rootController = loader.getController();
        }
        return rootLayout;
    }

    public RootLayoutController getRootController() {
        return rootController;
    }

    public AnchorPane getMainView() throws IOException {
        if (this.mainView == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/JobOverview.fxml"));
            this.mainView = (AnchorPane) loader.load();
            this.mainController = loader.getController();
        }
        return mainView;
    }

    public JobOverviewController getMainController() {
        return mainController;
    }

    public AnchorPane getLoginView() throws IOException {
        if (this.loginView == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/LoginView.fxml"));
            this.loginView = (AnchorPane) loader.load();
            this.loginController = loader.getController();
        }
        return loginView;
    }

    public LoginViewController getLoginController() {
        return loginController;
    }

    public AnchorPane getAccountView() throws IOException {
        if (this.accountView == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/AccountManagerView.fxml"));
            this.accountView = (AnchorPane) loader.load();
            this.accountController = loader.getController();
        }
        return accountView;
    }

    public AccountManagerViewController getAccountController() {
        return accountController;
    }

    public S3Manager getS3m() throws ServiceException {
        if (this.s3m == null) {
            //no credentials provided, so let controller set it
            if (this.currentCredentials == null) {
                this.s3m = new S3Manager(this);
            } else {
                //we will do some init
                this.s3m = new S3Manager(this);
                this.s3m.setAwsCredentials(currentCredentials);
            }
        }
        return s3m;
    }

    public EC2Manager getEc2m() {
        if (this.ec2m == null) {
            if (this.currentCredentials == null) {
                this.ec2m = new EC2Manager(this);
            } else {
                this.ec2m = new EC2Manager(this);
                this.ec2m.setAwsCredentials(currentCredentials);
            }
        }
        return ec2m;
    }

    public AWSCredentials getCurrentCredentials() {
        return currentCredentials;
    }

    public void setCurrentCredentials(AWSCredentials currentCredentials) throws ServiceException {
        this.currentCredentials = currentCredentials;
        //set ec2m and s3m at the same time
        this.getEc2m().setAwsCredentials(currentCredentials);
        this.getS3m().setAwsCredentials(currentCredentials);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        this.setPrimaryStage(stage);
        
        Scene scene = new Scene(this.getRootLayout());
        
        stage.setScene(scene);
        stage.setTitle("CaperCloud");
        stage.show();

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
        
        this.getRootLayout().setCenter(this.getMainView());
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
            
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void showLoginView() {
        try {       
            if (this.getLoginStage().getScene() == null) {
                this.getLoginStage().setTitle("Manage Accounts");
                this.getLoginStage().initModality(Modality.WINDOW_MODAL);
                this.getLoginStage().initOwner(this.getPrimaryStage());

                Scene scene = new Scene(this.getLoginView());
                this.getLoginStage().setScene(scene);
                this.getLoginController().setMainApp(this);

                this.getLoginStage().showAndWait();                
            }
            else {
                this.getLoginStage().showAndWait();  
            }
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void showAccountManagerView() {
        try {
            if (this.getNewAccountStage().getScene() == null) {
                this.getNewAccountStage().setTitle("New Account");
                this.getNewAccountStage().initModality(Modality.WINDOW_MODAL);
                this.getNewAccountStage().initOwner(this.getLoginStage());

                Scene scene = new Scene(this.getAccountView());
                this.getNewAccountStage().setScene(scene);
                //will not raise nullpointer error here 
                this.getAccountController().setMainApp(this);

                this.getNewAccountStage().showAndWait();   
            } else {
                this.getNewAccountStage().showAndWait();  
            }
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

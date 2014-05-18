/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package capercloud;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBuilder;
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author shuai
 */
public class CaperCloud extends Application {
    
    public static final String APPLICATION_DESCRIPTION = "CaperCloud/1.0";
    private Log log = LogFactory.getLog(getClass());
    private CloudManager cloudManager;
//Modal Confirm Dialog will set it
    private boolean canceled = false;
    
    private Stage primaryStage;
    private Stage loginStage;
    private Stage newAccountStage;
    private Stage transferPreferenceStage;
    private Stage progressViewStage;
    
    private BorderPane rootLayout;
    private RootLayoutController rootController;
    private AnchorPane mainView;
    private JobOverviewController mainController;
    private AnchorPane loginView;
    private LoginViewController loginController;
    private AnchorPane accountView;
    private AccountManagerViewController accountController;
    private AnchorPane preferenceView;
    private TransferPreferenceViewController preferenceController;
    
    /**
     * CaperCloud is a Cloud-based Proteogenomics pipeline.
     * Main features:
     * 1. Rubost interact with/monitor Amazon S3 and EC2 Service.
     * 2. Aim to aid four scientific goals - new gene, SAP, AS and custom database.
     * 3. Results can be sent to CAPER server or download to local storage.
     * @author Yang Shuai
     */
    public CaperCloud() {
        this.cloudManager = CloudManager.getInstance();
    }
    
    //geters and setters
    public CloudManager getCloudManager() {
        return this.cloudManager;
    }
    
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getLoginStage() {
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

    public Stage getTransferPreferenceStage() {
        if (this.transferPreferenceStage == null) {
            this.transferPreferenceStage = new Stage();
        }
        return transferPreferenceStage;
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

    public AnchorPane getPreferenceView() throws IOException {
        if (this.preferenceView == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TransferPreferenceView.fxml"));
            this.preferenceView = (AnchorPane) loader.load();
            this.preferenceController = loader.getController();
        }
        return preferenceView;
    }

    public TransferPreferenceViewController getPreferenceController() {
        return preferenceController;
    }
    
    public boolean isCanceled() {
        return this.canceled;
    }
    
    public void resetCanceled() {
        this.canceled = false;
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        this.setPrimaryStage(stage);
        this.getPrimaryStage().setOnHidden(new EventHandler() {
            @Override
            public void handle(Event t) {
                if (CaperCloud.this.getCloudManager().getEc2Manager() != null) {
                    CaperCloud.this.getCloudManager().getEc2Manager().shutdown();
                }
            }
        });
        
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
                this.getAccountController().setParentController(this.getLoginController());

                this.getNewAccountStage().showAndWait();   
            } else {
                this.getNewAccountStage().showAndWait();  
            }
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void showTransferPreferenceView() {
        try{
            if (this.getTransferPreferenceStage().getScene() == null) {
                this.getTransferPreferenceStage().setTitle("Transfer Preferences");
                this.getTransferPreferenceStage().initModality(Modality.WINDOW_MODAL);
                this.getTransferPreferenceStage().initOwner(this.getPrimaryStage());

                Scene scene = new Scene(this.getPreferenceView());
                this.getTransferPreferenceStage().setScene(scene);
                this.getPreferenceController().setMainApp(this);

                this.getTransferPreferenceStage().showAndWait();
            } else {
                this.getTransferPreferenceStage().showAndWait();
            }
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void showTextFieldDialog() {
        try {
            Stage textFieldDialog = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("view/TextFieldView.fxml"));
            AnchorPane textFieldView = (AnchorPane) loader.load();
            TextFieldViewController controller = loader.getController();
            
            textFieldDialog.setTitle("Create Bucket");
            textFieldDialog.initModality(Modality.WINDOW_MODAL);
            textFieldDialog.initOwner(this.getPrimaryStage());
            
            Scene scene = new Scene(textFieldView);
            textFieldDialog.setScene(scene);
            controller.setMainApp(this);
            controller.setStage(textFieldDialog);
            
            textFieldDialog.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(CaperCloud.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Stage createModalConfirmDialog(String msg, final Stage parentStage) {
        final Stage dialog = new Stage(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setScene(new Scene(
                HBoxBuilder.create().styleClass("modal-dialog").children(
                        LabelBuilder.create().text(msg).build(),
                        ButtonBuilder.create().text("Yes").defaultButton(true).onAction(new EventHandler<ActionEvent>() {
                            @Override public void handle(ActionEvent actionEvent) {
                                parentStage.getScene().getRoot().setEffect(null); 
                                dialog.close();
                            }
                        }).build(),
                        ButtonBuilder.create().text("No").cancelButton(true).onAction(new EventHandler<ActionEvent>() {
                            @Override public void handle(ActionEvent actionEvent) {
                                parentStage.getScene().getRoot().setEffect(null);
                                CaperCloud.this.canceled = true;
                                dialog.close();
                            }
                        }).build()
                ).build()
                , Color.TRANSPARENT)
        );
        dialog.getScene().getStylesheets().add(getClass().getResource("res/theme.css").toExternalForm());
// allow the dialog to be dragged around.
        final Node root = dialog.getScene().getRoot();
        final Delta dragDelta = new Delta();
        root.setOnMousePressed(new EventHandler<MouseEvent>() {
          @Override public void handle(MouseEvent mouseEvent) {
// record a delta distance for the drag and drop operation.
            dragDelta.x = dialog.getX() - mouseEvent.getScreenX();
            dragDelta.y = dialog.getY() - mouseEvent.getScreenY();
          }
        });
        root.setOnMouseDragged(new EventHandler<MouseEvent>() {
          @Override public void handle(MouseEvent mouseEvent) {
            dialog.setX(mouseEvent.getScreenX() + dragDelta.x);
            dialog.setY(mouseEvent.getScreenY() + dragDelta.y);
          }
        });
        return dialog;
    }
    
    public Stage createStripedProgressDialog(String title, Stage parentStage) {
        Stage dialog = new Stage();
        final ProgressBar bar = new ProgressBar(0);
        bar.setPrefSize(400, 24);
 
        final Timeline task = new Timeline(
            new KeyFrame(
                    Duration.ZERO,       
                    new KeyValue(bar.progressProperty(), 0)
            ),
            new KeyFrame(
                    Duration.seconds(2), 
                    new KeyValue(bar.progressProperty(), 1)
            )
        );
        
        task.setOnFinished(new EventHandler() {
            @Override
            public void handle(Event t) {
                bar.setProgress(-1);
            }
        });
        task.playFromStart();
        
        HBox layout = new HBox(10);
        layout.getChildren().setAll(bar);
        layout.setPadding(new Insets(10));
        layout.setAlignment(Pos.CENTER);

        layout.getStylesheets().add(
            getClass().getResource("res/theme.css").toExternalForm()
        );
        dialog.setTitle(title);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setScene(new Scene(layout));
        
        return dialog;
    }
// records relative x and y co-ordinates.
    class Delta { double x, y; }
}

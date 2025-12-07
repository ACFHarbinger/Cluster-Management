package personal.cluster_management.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);

        // 1. Create the controller. The controller will create its own view and service.
        Dash dashController = new Dash(this, primaryStage);

        // 2. Get the root UI node from the controller and place it in the scene.
        Scene s = new Scene(dashController.getView());

        primaryStage.setScene(s);
        primaryStage.getIcons().add(new Image(getClass().getResource("assets/icon.png").toExternalForm()));
        primaryStage.setResizable(false);
        primaryStage.setTitle("PCHWRM Client By github.com/dubbadhar <3");
        primaryStage.show();

        // 3. (REMOVED) No longer need to call d.startHardwareInitThread();
        // The Dash controller's constructor now handles starting its own background tasks.

        // 4. Update close request to call the controller's shutdown method.
        primaryStage.setOnCloseRequest(event -> {
            // Tell the controller to gracefully shut down (e.g., disconnect socket)
            dashController.shutdown();
            
            // Exit the application
            Platform.exit();
        });
    }

    public void openDonation() {
        getHostServices().showDocument("https.www.paypal.me/ladiesman6969");
    }

    public void openOpenHardwareMonitorDownloads() {
        getHostServices().showDocument("https://openhardwaremonitor.org/downloads/");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
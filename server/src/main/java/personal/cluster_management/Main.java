package personal.cluster_management;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * The main application entry point (Composition Root).
 * This class handles JavaFX setup and dependency injection for the Monitor application.
 */
public class Main extends Application {
    
    // Hold a reference to the monitor instance for shutdown logic
    private MonitorInterface monitorController;

    @Override
    public void start(Stage primaryStage) {
        
        // 1. Dependency Injection / Composition Root
        // Initialize concrete dependencies
        IOInterface io = new IO();
        
        // Initialize the Controller, injecting its dependencies
        monitorController = new Monitor(io);
        
        // 2. Setup JavaFX Stage
        Scene scene = new Scene(monitorController.getView());
        
        primaryStage.setScene(scene);
        
        // Apply standard look-and-feel
        try {
            // Assumes 'assets/icon.png' exists
            primaryStage.getIcons().add(new Image(getClass().getResource("assets/icon.png").toExternalForm()));
        } catch (Exception e) {
            io.pln("Could not load application icon.");
        }
        
        primaryStage.setTitle("PCHWRM Monitor By github.com/dubbadhar <3");
        primaryStage.setResizable(true); 
        primaryStage.show();

        // 3. Start Application Logic
        // Start the server thread after the UI is ready
        monitorController.startServerThread();

        // 4. Clean Shutdown Hook
        primaryStage.setOnCloseRequest(event -> {
            // Stop the server, which closes sockets and exits the thread loop
            monitorController.stopServer(); 
            // Allow JavaFX platform to exit
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        // You might want to enable logging for debugging issues
        // System.setProperty("javafx.sg.logger.level", "FINER");
        
        launch(args);
    }
}
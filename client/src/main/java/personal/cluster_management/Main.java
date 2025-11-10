package personal.cluster_management;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage)
    {
        Platform.setImplicitExit(false);
        
        /**
         * MODIFICATION:
         * 1. Create the real IO object
         * 2. Inject the real IO object into the Dash constructor
         */
        IO io = new IO();
        Dash d = new Dash(this, primaryStage, io);
        
        Scene s = new Scene(d);
        primaryStage.setScene(s);
        primaryStage.getIcons().add(new Image(getClass().getResource("assets/icon.png").toExternalForm()));
        primaryStage.setResizable(false);
        primaryStage.setTitle("PCHWRM Client By github.com/dubbadhar <3");
        primaryStage.show();

        /**
         * MODIFICATION:
         * 3. Start the hardware init thread *after* the UI is shown
         */
        d.startHardwareInitThread();

        primaryStage.setOnCloseRequest(event->{
            try
            {
                if(d.isConnected){
                    d.writeToOS("QUIT");
                    Thread.sleep(500);
                    d.isConnected=false;
                }
                Platform.exit();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    public void openDonation()
    {
        getHostServices().showDocument("https.www.paypal.me/ladiesman6969");
    }

    public void openOpenHardwareMonitorDownloads()
    {
        getHostServices().showDocument("https://openhardwaremonitor.org/downloads/");
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
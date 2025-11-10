package personal.cluster_management;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage)
    {
        IO io = new IO();
        Monitor m = new Monitor(io);
        Scene s = new Scene(m);
        primaryStage.setScene(s);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event-> m.isConnected=false);
    }
}

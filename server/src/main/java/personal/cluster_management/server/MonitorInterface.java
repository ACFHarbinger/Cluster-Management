package personal.cluster_management.server;

import javafx.scene.layout.StackPane;

/**
 * Interface for the Monitor Controller.
 * Defines the contract for the main logic of the server/monitor application.
 */
public interface MonitorInterface {

    /**
     * Gets the root JavaFX node for the view managed by this controller.
     * @return The root StackPane to be displayed in the scene.
     */
    StackPane getView();

    /**
     * Starts the main server thread in the background.
     */
    void startServerThread();

    /**
     * The main server loop. Listens for connections and processes data.
     * @throws Exception If socket errors occur.
     */
    void startServer() throws Exception;

    /**
     * Processes a raw data string received from the client.
     * @param s The data string.
     */
    void process(String s);

    /**
     * Reads the configuration file into the in-memory config map.
     */
    void readConfig();

    /**
     * Attempts to close the server socket and client socket connections gracefully.
     * This stops the server thread running in the background.
     */
    void stopServer();
}
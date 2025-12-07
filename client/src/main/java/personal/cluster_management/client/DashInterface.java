package personal.cluster_management.client;

import javafx.scene.Node;

/**
 * Interface for the Dash Controller.
 * Defines the public-facing contract for the main application logic.
 */
public interface DashInterface {

    /**
     * Gets the root JavaFX node for the view managed by this controller.
     * @return The root Node to be displayed in the scene.
     */
    Node getView();

    /**
     * Validates and saves the configuration from the UI fields.
     */
    void saveConfig();

    /**
     * Validates all configuration text fields.
     * @return "OK" if all fields are valid, otherwise an error message string.
     */
    String validateConfig();

    /**
     * Fetches WMI values from the service and populates the in-memory config map.
     * @throws Exception If service call fails.
     */
    void initGPUCPURAM() throws Exception;

    /**
     * Shuts down any background tasks and cleans up resources.
     */
    void shutdown();
}
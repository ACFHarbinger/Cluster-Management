package personal.cluster_management.client;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

/**
 * Interface for the Dash UI (View).
 * Defines a contract for the controller to interact with the UI,
 * allowing for different UI implementations (e.g., test mocks)
 * to be swapped in.
 */
public interface DashUIInterface {

    /**
     * Initializes and lays out all UI components.
     */
    void loadNodes();

    /**
     * Enables or disables all configuration text fields.
     * @param isDisable true to disable, false to enable.
     */
    void setTextFieldDisableStatus(boolean isDisable);

    /**
     * Gets the root JavaFX node for this view, to be placed in a Scene.
     * @return The root Node.
     */
    Node getRootNode();

    // --- Getters for UI Components ---
    // These allow the controller to read values and attach event listeners.

    TextField getServerIPAddressTextField();
    TextField getDataRefreshIntervalTextField();
    TextField getServerPortTextField();
    TextField getCPULoadNameTextField();
    TextField getGPULoadNameTextField();
    TextField getCPUTemperatureNameTextField();
    TextField getGPUTemperatureNameTextField();
    TextField getCPUFanNameTextField();
    TextField getGPUFanNameTextField();
    TextField getTotalVRAMNameTextField();
    TextField getUsedVRAMNameTextField();
    TextField getUsedRAMNameTextField();
    TextField getAvailableRAMNameTextField();

    Button getConnectDisconnectServerButton();
    Button getUpdateValuesButton();
    Button getDonateButton();
    Button getMinimizeToSystemTrayButton();
    Button getDownloadOpenHardwareMonitorButton();

    ToggleButton getRunOnStartupToggleButton();
}
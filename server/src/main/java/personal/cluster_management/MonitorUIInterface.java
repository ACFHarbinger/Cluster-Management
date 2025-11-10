package personal.cluster_management;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.tilesfx.Tile;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Interface for the Monitor UI (View).
 * Defines a contract for the controller to interact with the UI.
 */
public interface MonitorUIInterface {

    /**
     * Initializes and lays out all UI components.
     */
    void loadNodes();

    /**
     * Switches the visible pane.
     * @param p The MonitorPane to show.
     */
    void switchPane(MonitorPaneEnum p);

    /**
     * Resets all gauge and label values to default.
     */
    void resetAllNodes();

    /**
     * Gets the root JavaFX node for this view.
     * @return The root StackPane.
     */
    StackPane getRootNode();

    // --- Getters for UI Components ---

    Label getNotConnectedPaneHeadingLabel();
    Label getNotConnectedPaneSubHeadingLabel();
    Label getGPUModelNameLabel();
    Label getCPUModelNameLabel();
    Label getMemorySubHeadingLabel();
    Label getVideoMemorySubHeadingLabel();

    Tile getCPULoadGauge();
    Gauge getCPUTempGauge();
    Tile getGPULoadGauge();
    Gauge getGPUTempGauge();
    Gauge getMemoryGauge();
    Gauge getVideoMemoryGauge();
    Gauge getCPUFanSpeedGauge();
    Gauge getGPUFanSpeedGauge();
}
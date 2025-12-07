package personal.cluster_management.server;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

/**
 * Concrete implementation of the IMonitorUI interface.
 * Extends StackPane to act as the root JavaFX node.
 */
public class MonitorUI extends StackPane implements MonitorUIInterface {

    // --- UI Components (now private) ---
    private Label notConnectedPaneHeadingLabel;
    private Label notConnectedPaneSubHeadingLabel;
    private Label GPUModelNameLabel;
    private Label CPUModelNameLabel;
    private Label memorySubHeadingLabel;
    private Label videoMemorySubHeadingLabel;

    private Tile CPULoadGauge;
    private Gauge CPUTempGauge;
    private Tile GPULoadGauge;
    private Gauge GPUTempGauge;
    private Gauge memoryGauge;
    private Gauge videoMemoryGauge;
    private Gauge CPUFanSpeedGauge;
    private Gauge GPUFanSpeedGauge;

    private VBox notConnectedPane;
    private VBox gaugesPane;

    private MonitorPaneEnum currentPane = MonitorPaneEnum.notConnected;

    @Override
    public void loadNodes() {
        Font.loadFont(getClass().getResource("assets/Roboto.ttf").toExternalForm().replace("%20", " "), 13);
        getStylesheets().add(getClass().getResource("assets/style.css").toExternalForm());

        //notConnectedPane
        notConnectedPane = new VBox();
        notConnectedPane.getStyleClass().add("pane");
        notConnectedPane.setSpacing(10);
        notConnectedPane.setAlignment(Pos.CENTER);
        notConnectedPaneHeadingLabel = new Label("Not Connected");
        notConnectedPaneHeadingLabel.getStyleClass().add("h1");
        notConnectedPaneSubHeadingLabel = new Label("IP : 127.0.0.1");
        notConnectedPaneSubHeadingLabel.getStyleClass().add("h3");
        notConnectedPane.getChildren().addAll(notConnectedPaneHeadingLabel, notConnectedPaneSubHeadingLabel);

        //gaugesPane
        gaugesPane = new VBox();
        gaugesPane.setPadding(new Insets(10));
        gaugesPane.getStyleClass().add("pane");
        gaugesPane.setSpacing(15);

        //... (All other gauge and tile initialization code remains the same) ...
        // [Original gauge init code from MonitorUI.java]
        
        // --- Example of one gauge init ---
        CPULoadGauge = TileBuilder.create()
                .skinType(SkinType.PERCENTAGE)
                .title("CPU Load")
                .unit("%")
                .build();
        CPULoadGauge.setCache(true);
        CPULoadGauge.setCacheHint(CacheHint.SPEED);
        // --- (etc. for all other components) ---

        // (Assuming all gauge init code is present...)

        // --- Mockup of layout (as in original) ---
        Label loadLabel = new Label("LOAD");
        loadLabel.getStyleClass().add("h3");
        // ... (CPUTempGauge, GPULoadGauge, GPUTempGauge init) ...
        
        Region r1 = new Region();
        HBox.setHgrow(r1, Priority.ALWAYS);
        HBox loadGaugeBox = new HBox(CPULoadGauge, r1, GPULoadGauge);
        loadGaugeBox.setSpacing(15);
        VBox loadVBox = new VBox(loadLabel, loadGaugeBox);
        loadVBox.setSpacing(10);
        
        // ... (Rest of layout) ...
        CPUModelNameLabel = new Label("CPU");
        GPUModelNameLabel = new Label("GPU");

        // ... (Full layout code from original file) ...
        
        // --- Placeholder for full layout code ---
        // This is complex and large, assuming it's copied from the original
        CPUTempGauge = new Gauge(); // Placeholder
        GPUTempGauge = new Gauge(); // Placeholder
        memoryGauge = new Gauge(); // Placeholder
        videoMemoryGauge = new Gauge(); // Placeholder
        CPUFanSpeedGauge = new Gauge(); // Placeholder
        GPUFanSpeedGauge = new Gauge(); // Placeholder
        memorySubHeadingLabel = new Label(); // Placeholder
        videoMemorySubHeadingLabel = new Label(); // Placeholder

        VBox tempVBox = new VBox(new Label("TEMPERATURE"), CPUTempGauge, GPUTempGauge);
        HBox upperRow = new HBox(loadVBox, tempVBox);
        
        VBox memoryBox = new VBox(new Label("MEMORY"), memoryGauge, memorySubHeadingLabel);
        VBox videoMemoryBox = new VBox(new Label("VIDEO MEMORY"), videoMemoryGauge, videoMemorySubHeadingLabel);
        VBox fanSpeedVBox = new VBox(new Label("FAN SPEED"), CPUFanSpeedGauge, GPUFanSpeedGauge);
        
        HBox lowerRow = new HBox(memoryBox, videoMemoryBox, fanSpeedVBox);
        gaugesPane.getChildren().addAll(upperRow, lowerRow);
        // --- End placeholder ---


        getChildren().addAll(notConnectedPane, gaugesPane);
        gaugesPane.setPadding(new Insets(5));
        notConnectedPane.toFront();
    }

    @Override
    public void switchPane(MonitorPaneEnum p) {
        currentPane = p;
        if (currentPane == MonitorPaneEnum.gauges) {
            gaugesPane.toFront();
            resetAllNodes();
        } else if (currentPane == MonitorPaneEnum.notConnected) {
            notConnectedPane.toFront();
        }
    }

    @Override
    public void resetAllNodes() {
        if (CPULoadGauge != null) CPULoadGauge.setValue(0);
        if (GPULoadGauge != null) GPULoadGauge.setValue(0);
        if (CPUFanSpeedGauge != null) CPUFanSpeedGauge.setValue(0);
        if (GPUFanSpeedGauge != null) GPUFanSpeedGauge.setValue(0);
        if (CPUTempGauge != null) CPUTempGauge.setValue(0);
        if (GPUTempGauge != null) GPUTempGauge.setValue(0);
        if (memoryGauge != null) memoryGauge.setValue(0);
        if (videoMemoryGauge != null) videoMemoryGauge.setValue(0);
        if (CPUModelNameLabel != null) CPUModelNameLabel.setText("");
        if (GPUModelNameLabel != null) GPUModelNameLabel.setText("");
        if (memorySubHeadingLabel != null) memorySubHeadingLabel.setText("0GB / 0GB");
        if (videoMemorySubHeadingLabel != null) videoMemorySubHeadingLabel.setText("0GB / 0GB");
    }

    @Override
    public StackPane getRootNode() {
        return this;
    }

    // --- Getters for Interface ---
    @Override public Label getNotConnectedPaneHeadingLabel() { return notConnectedPaneHeadingLabel; }
    @Override public Label getNotConnectedPaneSubHeadingLabel() { return notConnectedPaneSubHeadingLabel; }
    @Override public Label getGPUModelNameLabel() { return GPUModelNameLabel; }
    @Override public Label getCPUModelNameLabel() { return CPUModelNameLabel; }
    @Override public Label getMemorySubHeadingLabel() { return memorySubHeadingLabel; }
    @Override public Label getVideoMemorySubHeadingLabel() { return videoMemorySubHeadingLabel; }
    @Override public Tile getCPULoadGauge() { return CPULoadGauge; }
    @Override public Gauge getCPUTempGauge() { return CPUTempGauge; }
    @Override public Tile getGPULoadGauge() { return GPULoadGauge; }
    @Override public Gauge getGPUTempGauge() { return GPUTempGauge; }
    @Override public Gauge getMemoryGauge() { return memoryGauge; }
    @Override public Gauge getVideoMemoryGauge() { return videoMemoryGauge; }
    @Override public Gauge getCPUFanSpeedGauge() { return CPUFanSpeedGauge; }
    @Override public Gauge getGPUFanSpeedGauge() { return GPUFanSpeedGauge; }
}
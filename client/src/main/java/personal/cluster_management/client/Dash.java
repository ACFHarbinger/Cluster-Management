package personal.cluster_management.client;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Controller class for the Dash.
 * This class manages the DashUI (View) and the DashService (Model/Service).
 * It handles UI events, validates input, and coordinates with the service layer.
 */
public class Dash implements DashInterface {

    // View and Service
    private final DashUI view;
    private final DashService service;

    // Application state
    public HashMap<String, String> config = new HashMap<>(); // Made public for test assertion
    private boolean isConnected = false;

    // References to other application parts
    private final Main mainApp;

    /**
     * Primary constructor for production use.
     * Creates its own UI and Service instances.
     * @param m The main application class.
     * @param ps The primary stage.
     */
    public Dash(Main m, Stage ps) {
        this.view = new DashUI();
        this.view.loadNodes();
        this.service = new DashService(new IO());
        this.mainApp = m;

        // Load config, apply to UI, and register all event handlers
        this.config = service.readConfig();
        loadNodes();
        applyConfigReadingsToFields();
        registerEventHandlers();

        // Start background tasks
        startWMIInitTask();
        startUpdateLoopTask();
    }

    /**
     * Constructor for testing.
     * Allows injecting a mock UI and mock Service.
     * @param m Mock Main app.
     * @param ps Mock Stage.
     * @param ui The UI view (can be real or mock).
     * @param service The mock service.
     */
    public Dash(Main m, Stage ps, DashUI ui, DashService service) {
        this.view = ui;
        this.service = service;
        this.mainApp = m;

        // Load config from the (mock) service
        this.config = service.readConfig();
        // In a test, we assume view.loadNodes() was already called.
        // We must register handlers to test button clicks.
        applyConfigReadingsToFields();
        registerEventHandlers();
    }

    /**
     * @return The root JavaFX node (VBox) for the view.
     */
    public VBox getView() {
        return view;
    }

    /**
     * Binds all UI component actions to controller methods.
     */
    private void registerEventHandlers() {
        view.donateButton.setOnAction(event -> mainApp.openDonation());
        view.downloadOpenHardwareMonitorButton.setOnAction(event -> mainApp.openOpenHardwareMonitorDownloads());

        view.updateValuesButton.setOnAction(event -> saveConfig());
        view.connectDisconnectServerButton.setOnAction(event -> {
            if (isConnected) disconnect();
            else connect();
        });

        view.runOnStartupToggleButton.setOnAction(event -> {
            if (view.runOnStartupToggleButton.isSelected()) {
                service.addToStartup();
            } else {
                service.removeFromStartup();
            }
        });

        // Other handlers like minimizeToSystemTrayButton can be added here
        // view.minimizeToSystemTrayButton.setOnAction(event -> ...);
    }

    /**
     * Populates UI text fields from the loaded config map.
     */
    private void applyConfigReadingsToFields() {
        view.serverIPAddressTextField.setText(config.get("SERVER_IP"));
        view.serverPortTextField.setText(config.get("SERVER_PORT"));
        view.CPULoadNameTextField.setText(config.get("CPU_LOAD_NAME"));
        view.GPULoadNameTextField.setText(config.get("GPU_LOAD_NAME"));
        view.CPUTemperatureNameTextField.setText(config.get("CPU_TEMP_NAME"));
        view.GPUTemperatureNameTextField.setText(config.get("GPU_TEMP_NAME"));
        view.CPUFanNameTextField.setText(config.get("CPU_FAN_NAME"));
        view.GPUFanNameTextField.setText(config.get("GPU_FAN_NAME"));
        view.TotalVRAMNameTextField.setText(config.get("TOTAL_VRAM_NAME"));
        view.UsedVRAMNameTextField.setText(config.get("USED_VRAM_NAME"));
        view.UsedRAMNameTextField.setText(config.get("USED_RAM_NAME"));
        view.AvailableRAMNameTextField.setText(config.get("AVAILABLE_RAM_NAME"));
        view.dataRefreshIntervalTextField.setText(config.get("REFRESH_INTERVAL"));
    }

    /**
     * Loads node-specific UI elements, like the startup toggle.
     */
    private void loadNodes() {
        if (OSEnum.getOS() == OSEnum.WINDOWS) {
            view.runOnStartupToggleButton.setSelected(service.checkIfRunningOnStartup());
        } else {
            view.runOnStartupToggleButton.setDisable(true);
        }
    }

    /**
     * Runs in a background thread to fetch WMI data and populate the config map.
     */
    private void startWMIInitTask() {
        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    initGPUCPURAM();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }).start();
    }

    /**
     * Runs the main update loop in a background thread.
     */
    private void startUpdateLoopTask() {
        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                while (true) {
                    try {
                        if (isConnected) {
                            String data = config.get("CPU_LOAD") + "," + config.get("CPU_TEMP") + "," + config.get("CPU_FAN") + "," + config.get("GPU_LOAD") + "," + config.get("GPU_TEMP") + "," + config.get("GPU_FAN") + "," + config.get("USED_VRAM") + "," + config.get("TOTAL_VRAM") + "," + config.get("USED_RAM") + "," + config.get("AVAILABLE_RAM");
                            service.sendData(data);
                        }
                        initGPUCPURAM();
                        Thread.sleep(Integer.parseInt(view.dataRefreshIntervalTextField.getText()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (e instanceof IOException) {
                            disconnect();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * Connects to the server using values from the UI.
     * Updates UI state based on connection success or failure.
     */
    private void connect() {
        view.setTextFieldDisableStatus(true);
        try {
            service.connectSocket(view.serverIPAddressTextField.getText(), Integer.parseInt(view.serverPortTextField.getText()));
            service.sendData("connect");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Platform.runLater(() -> {
                Socket s = service.getSocket();
                if (s != null && s.isConnected()) {
                    view.connectDisconnectServerButton.setText("Disconnect");
                    isConnected = true;
                } else {
                    view.connectDisconnectServerButton.setText("Connect");
                    view.setTextFieldDisableStatus(false);
                    isConnected = false;
                    showErrorAlert("Connection Failed", "Could not connect to server at " + view.serverIPAddressTextField.getText() + ":" + view.serverPortTextField.getText());
                }
            });
        }
    }

    /**
     * Disconnects from the server and updates UI state.
     */
    private void disconnect() {
        try {
            service.disconnectSocket();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Platform.runLater(() -> {
                view.connectDisconnectServerButton.setText("Connect");
                view.setTextFieldDisableStatus(false);
                isConnected = false;
            });
        }
    }

    /**
     * Validates and saves the configuration from the UI fields.
     */
    public void saveConfig() {
        String validation = validateConfig();
        if (validation.equals("OK")) {
            String[] data = {
                    view.serverIPAddressTextField.getText(),
                    view.serverPortTextField.getText(),
                    view.CPULoadNameTextField.getText(),
                    view.GPULoadNameTextField.getText(),
                    view.CPUTemperatureNameTextField.getText(),
                    view.GPUTemperatureNameTextField.getText(),
                    view.CPUFanNameTextField.getText(),
                    view.GPUFanNameTextField.getText(),
                    view.TotalVRAMNameTextField.getText(),
                    view.UsedVRAMNameTextField.getText(),
                    view.UsedRAMNameTextField.getText(),
                    view.AvailableRAMNameTextField.getText(),
                    view.dataRefreshIntervalTextField.getText()
            };

            // Save to file via service
            service.saveConfig(data);

            // Update in-memory config map
            config.put("SERVER_IP", data[0]);
            config.put("SERVER_PORT", data[1]);
            config.put("CPU_LOAD_NAME", data[2]);
            config.put("GPU_LOAD_NAME", data[3]);
            config.put("CPU_TEMP_NAME", data[4]);
            config.put("GPU_TEMP_NAME", data[5]);
            config.put("CPU_FAN_NAME", data[6]);
            config.put("GPU_FAN_NAME", data[7]);
            config.put("TOTAL_VRAM_NAME", data[8]);
            config.put("USED_VRAM_NAME", data[9]);
            config.put("USED_RAM_NAME", data[10]);
            config.put("AVAILABLE_RAM_NAME", data[11]);
            config.put("REFRESH_INTERVAL", data[12]);

            showInfoAlert("Config Saved", "Configuration saved successfully.");
        } else {
            showErrorAlert("Validation Error", validation);
        }
    }

    /**
     * Validates all configuration text fields.
     * @return "OK" if all fields are valid, otherwise an error message string.
     */
    public String validateConfig() {
        String toReturn = "";
        boolean error = false;

        if (view.dataRefreshIntervalTextField.getText().length() == 0 || !view.dataRefreshIntervalTextField.getText().matches("\\d+")) {
            error = true;
            toReturn += "Refresh Data Interval must be a number and cannot be empty!\n";
        }
        // ... (All other validation checks from original Dash.java) ...
        if (view.serverIPAddressTextField.getText().length() == 0) {
            error = true;
            toReturn += "Server IP cannot be empty!\n";
        }
        if (view.serverPortTextField.getText().length() == 0 || !view.serverPortTextField.getText().matches("\\d+")) {
            error = true;
            toReturn += "Server Port must be a number and cannot be empty!\n";
        }
        if (view.CPULoadNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "CPU Load Name cannot be empty!\n";
        }
        if (view.GPULoadNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "GPU Load Name cannot be empty!\n";
        }
        if (view.CPUTemperatureNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "CPU Temp Name cannot be empty!\n";
        }
        if (view.GPUTemperatureNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "GPU Temp Name cannot be empty!\n";
        }
        if (view.CPUFanNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "CPU Fan Name cannot be empty!\n";
        }
        if (view.GPUFanNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "GPU Fan Name cannot be empty!\n";
        }
        if (view.TotalVRAMNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "Total VRAM Name cannot be empty!\n";
        }
        if (view.UsedVRAMNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "Used VRAM Name cannot be empty!\n";
        }
        if (view.UsedRAMNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "Used RAM Name cannot be empty!\n";
        }
        if (view.AvailableRAMNameTextField.getText().length() == 0) {
            error = true;
            toReturn += "Available RAM Name cannot be empty!\n";
        }

        if (error) return toReturn;
        else return "OK";
    }

    /**
     * Fetches WMI values from the service and populates the in-memory config map.
     * @throws Exception If service call fails.
     */
    public void initGPUCPURAM() throws Exception {
        ArrayList<String[]> values = service.getValuesFromWMI();
        for (String[] s : values) {
            if (s[1].equals("Load")) {
                if (s[0].equals(config.get("CPU_LOAD_NAME"))) config.put("CPU_LOAD", s[2]);
                if (s[0].equals(config.get("GPU_LOAD_NAME"))) config.put("GPU_LOAD", s[2]);
            } else if (s[1].equals("Temperature")) {
                if (s[0].equals(config.get("CPU_TEMP_NAME"))) config.put("CPU_TEMP", s[2]);
                if (s[0].equals(config.get("GPU_TEMP_NAME"))) config.put("GPU_TEMP", s[2]);
            } else if (s[1].equals("Fan")) {
                if (s[0].equals(config.get("CPU_FAN_NAME"))) config.put("CPU_FAN", s[2]);
                if (s[0].equals(config.get("GPU_FAN_NAME"))) config.put("GPU_FAN", s[2]);
            } else if (s[1].equals("SmallData")) {
                if (s[0].equals(config.get("TOTAL_VRAM_NAME"))) config.put("TOTAL_VRAM", s[2]);
                if (s[0].equals(config.get("USED_VRAM_NAME"))) config.put("USED_VRAM", s[2]);
                if (s[0].equals(config.get("USED_RAM_NAME"))) config.put("USED_RAM", s[2]);
                if (s[0].equals(config.get("AVAILABLE_RAM_NAME"))) config.put("AVAILABLE_RAM", s[2]);
            }
        }
    }

    /**
     * Handles application shutdown logic.
     * Attempts to gracefully disconnect from the server.
     */
    public void shutdown() {
        if (isConnected) {
            // Run disconnection on a background thread.
            // This avoids blocking the JavaFX Application Thread.
            new Thread(() -> {
                try {
                    // Send a final "QUIT" message (based on original Main.java logic)
                    service.sendData("QUIT");
                    // Give a brief moment for the message to send
                    Thread.sleep(200);
                    service.disconnectSocket();
                } catch (Exception e) {
                    // Suppress errors during shutdown
                    // e.printStackTrace();
                }
            }).start();
        }
        // Immediately return. Platform.exit() will be called in Main.
        // The daemon update thread will be killed by Platform.exit().
    }

    // Helper methods for showing dialogs
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
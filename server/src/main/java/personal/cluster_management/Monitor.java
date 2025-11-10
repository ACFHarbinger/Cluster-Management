package personal.cluster_management;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.layout.StackPane;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * Concrete implementation of the IMonitor controller interface.
 * Manages the IMonitorUI (View) and IMonitorIO (Service/IO).
 */
public class Monitor implements MonitorInterface {
    
    // Depend on interfaces
    private final IOInterface io;
    private final MonitorUIInterface view;

    private final HashMap<String, String> config = new HashMap<>();
    private volatile boolean isConnected = false; // volatile for thread safety
    private volatile boolean isRunning = true; // Flag to control the server loop

    // Server components
    private Socket socket;
    private ServerSocket serverSocket;
    private BufferedReader is;

    // State for max gauge values
    private double cpuLoadMaxValue = 0, gpuLoadMaxValue = 0;
    
    public Monitor(IOInterface io) {
        this.io = io;
        this.view = new MonitorUI(); // Controller creates its view

        readConfig();

        // Apply config to the view
        int screenWidth = Integer.parseInt(config.getOrDefault("SCREEN_WIDTH", "800"));
        int screenHeight = Integer.parseInt(config.getOrDefault("SCREEN_HEIGHT", "600"));
        view.getRootNode().setPrefSize(screenWidth, screenHeight);
        
        view.loadNodes(); // Initialize UI components
    }

    @Override
    public StackPane getView() {
        return view.getRootNode();
    }

    @Override
    public void startServerThread() {
        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    io.pln("Starting server thread...");
                    startServer();
                } catch (Exception e) {
                    if (isRunning) { // Only log error if not a graceful shutdown
                        io.pln("Server Thread Error: " + e.getMessage());
                    }
                    Platform.runLater(() -> view.switchPane(MonitorPaneEnum.notConnected));
                }
                return null;
            }
        }).start();
    }

    @Override
    public void startServer() throws Exception {
        io.pln("Starting Server ...");
        
        // Ensure old socket is closed
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }

        // Initialize and bind server socket
        serverSocket = new ServerSocket(Integer.parseInt(config.get("SERVER_PORT")));
        io.pln("Server Started on Port " + config.get("SERVER_PORT"));

        String ip = InetAddress.getLocalHost().getHostAddress();
        Platform.runLater(() -> {
            view.getNotConnectedPaneHeadingLabel().setText("Listening on Port " + config.get("SERVER_PORT"));
            view.getNotConnectedPaneSubHeadingLabel().setText("IP : " + ip);
        });

        // Main server loop
        while (isRunning) {
            try {
                // Wait for client connection
                socket = serverSocket.accept();
                io.pln("Client Connected : " + socket.getInetAddress().getCanonicalHostName());
                isConnected = true;
                Platform.runLater(() -> view.switchPane(MonitorPaneEnum.gauges));

                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Client message loop
                while (isConnected && isRunning) {
                    String in = is.readLine();
                    if (in == null || in.equalsIgnoreCase("QUIT")) {
                        isConnected = false;
                    } else if (!in.equalsIgnoreCase("connect")) {
                        process(in);
                    }
                }
                
                // Cleanup after client disconnect
                io.pln("Client Disconnected!");
                if (socket != null && !socket.isClosed()) socket.close();
                if (is != null) is.close();

            } catch (IOException e) {
                if (isRunning) {
                    // Handle case where serverSocket.accept() is interrupted by close()
                    io.pln("Server accept loop interrupted: " + e.getMessage());
                }
            } finally {
                isConnected = false;
                Platform.runLater(() -> view.switchPane(MonitorPaneEnum.notConnected));
            }
        }
        
        // Final server socket close when isRunning is false
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            io.pln("Server Socket Closed.");
        }
    }

    double totalVRAM = -1, usedVRAM = -1, freeRAM = -1, usedRAM = -1;

    @Override
    public void process(String s) {
        String[] data = s.split(",");
        
        HashMap<String, String> dataMap = new HashMap<>();
        if (data.length >= 10) { 
            dataMap.put("CPU_LOAD", data[0]);
            dataMap.put("CPU_TEMP", data[1]);
            dataMap.put("CPU_FAN", data[2]);
            dataMap.put("GPU_LOAD", data[3]);
            dataMap.put("GPU_TEMP", data[4]);
            dataMap.put("GPU_FAN", data[5]);
            dataMap.put("USED_VRAM", data[6]);
            dataMap.put("TOTAL_VRAM", data[7]);
            dataMap.put("USED_RAM", data[8]);
            dataMap.put("AVAILABLE_RAM", data[9]);
        } else {
            io.pln("Received malformed data: " + s);
            return;
        }


        for (String d : dataMap.keySet()) {
            String val = dataMap.get(d);
            if (val == null || val.isEmpty() || val.equalsIgnoreCase("N/A")) continue;
            
            double parsedValue;
            try {
                parsedValue = Double.parseDouble(val);
            } catch (NumberFormatException e) {
                io.pln("Skipping non-numeric value for " + d + ": " + val);
                continue;
            }

            switch (d) {
                case "CPU_LOAD" -> {
                    Platform.runLater(() -> view.getCPULoadGauge().setValue(parsedValue));
                    if (parsedValue > cpuLoadMaxValue) {
                        cpuLoadMaxValue = parsedValue;
                    }
                }
                case "GPU_LOAD" -> {
                    Platform.runLater(() -> view.getGPULoadGauge().setValue(parsedValue));
                    if (parsedValue > gpuLoadMaxValue) {
                        gpuLoadMaxValue = parsedValue;
                    }
                }
                case "CPU_TEMP" -> Platform.runLater(() -> view.getCPUTempGauge().setValue(parsedValue));
                case "GPU_TEMP" -> Platform.runLater(() -> view.getGPUTempGauge().setValue(parsedValue));
                case "CPU_FAN" -> Platform.runLater(() -> view.getCPUFanSpeedGauge().setValue(parsedValue));
                case "GPU_FAN" -> Platform.runLater(() -> view.getGPUFanSpeedGauge().setValue(parsedValue));
                
                case "USED_VRAM" -> usedVRAM = parsedValue;
                case "TOTAL_VRAM" -> totalVRAM = parsedValue;
                case "USED_RAM" -> usedRAM = parsedValue;
                case "AVAILABLE_RAM" -> freeRAM = parsedValue;
            }
        }

        if (totalVRAM > -1 && usedVRAM > -1) {
            Platform.runLater(() -> {
                view.getVideoMemoryGauge().setValue((usedVRAM / totalVRAM) * 100);
                view.getVideoMemorySubHeadingLabel().setText(((int) (usedVRAM / 1024)) + "GB / " + ((int) (totalVRAM / 1024)) + "GB");
            });
        }

        if (freeRAM > -1 && usedRAM > -1) {
            Platform.runLater(() -> {
                view.getMemoryGauge().setValue((usedRAM / (usedRAM + freeRAM)) * 100);
                view.getMemorySubHeadingLabel().setText(((int) usedRAM) + "GB / " + ((int) (usedRAM + freeRAM)) + "GB");
            });
        }
    }

    @Override
    public void readConfig() {
        try {
            String[] configArray = io.readFileArranged("config", "::");
            config.clear();
            config.put("SCREEN_WIDTH", configArray[0]);
            config.put("SCREEN_HEIGHT", configArray[1]);
            config.put("SERVER_PORT", configArray[2]);
        } catch (Exception e) {
            io.pln("Config file not found or corrupt, creating default.");
            config.put("SCREEN_WIDTH", "1280");
            config.put("SCREEN_HEIGHT", "720");
            config.put("SERVER_PORT", "8080");
            io.writeToFile("1280::720::8080::", "config");
        }
    }

    @Override
    public void stopServer() {
        isRunning = false;
        try {
            // Close the client socket if connected
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            // Close the server socket to interrupt the accept() call
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            io.pln("Error while closing sockets: " + e.getMessage());
        }
    }
}
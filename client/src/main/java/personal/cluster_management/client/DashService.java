package personal.cluster_management.client;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Service layer for handling all backend logic, I/O, and OS interactions.
 * This class is designed to be injected into the Dash controller and easily mocked for testing.
 */
public class DashService implements DashServiceInterface {

    private final IO io;
    private final OSEnum currentOS;
    private final String currentDir = System.getProperty("user.dir");

    /**
     * Creates a new DashService.
     * @param io The I/O helper to use for file and network operations.
     */
    public DashService(IO io) {
        this.io = io;
        this.currentOS = OSEnum.getOS();
    }

    /**
     * Reads the configuration file and returns its contents as a HashMap.
     * @return A HashMap containing the configuration key-value pairs.
     */
    public HashMap<String, String> readConfig() {
        HashMap<String, String> config = new HashMap<>();
        try {
            String[] configContents = io.readFile(currentDir + File.separator + "config.cfg").split("\n");
            if (configContents.length >= 13) {
                config.put("SERVER_IP", configContents[0].trim());
                config.put("SERVER_PORT", configContents[1].trim());
                config.put("CPU_LOAD_NAME", configContents[2].trim());
                config.put("GPU_LOAD_NAME", configContents[3].trim());
                config.put("CPU_TEMP_NAME", configContents[4].trim());
                config.put("GPU_TEMP_NAME", configContents[5].trim());
                config.put("CPU_FAN_NAME", configContents[6].trim());
                config.put("GPU_FAN_NAME", configContents[7].trim());
                config.put("TOTAL_VRAM_NAME", configContents[8].trim());
                config.put("USED_VRAM_NAME", configContents[9].trim());
                config.put("USED_RAM_NAME", configContents[10].trim());
                config.put("AVAILABLE_RAM_NAME", configContents[11].trim());
                config.put("REFRESH_INTERVAL", configContents[12].trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    /**
     * Saves the provided configuration array to the config file.
     * @param data A String array containing the 13 configuration values.
     */
    public void saveConfig(String[] data) {
        try {
            String config = String.join("\n", data);
            io.saveFile(currentDir + File.separator + "config.cfg", config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Queries WMI via PowerShell to get sensor values from OpenHardwareMonitor.
     * @return A list of String arrays, where each array contains [Name, SensorType, Value].
     * @throws Exception If the shell command fails or parsing errors occur.
     */
    public ArrayList<String[]> getValuesFromWMI() throws Exception {
        String out = io.getShellOutput("powershell.exe get-wmiobject -namespace root\\\\OpenHardwareMonitor -query 'SELECT Value,Name,SensorType FROM Sensor'").replace("\r\n\r\n__GENUS          : 2\r\n__CLASS          : Sensor\r\n__SUPERCLASS     : \r\n__DYNASTY        : \r\n__RELPATH        : \r\n__PROPERTY_COUNT : 3\r\n__DERIVATION     : {}\\r\n__SERVER         : \r\n__NAMESPACE      : \r\n__PATH           : \r\n", "");
        ArrayList<String[]> returnable = new ArrayList<>();

        String[] x = out.split("PSComputerName {3}:");

        for (int i = 0; i < x.length - 1; i++) {
            String[] cd = x[i].split("\r\n");
            returnable.add(new String[]{
                    cd[0].substring(cd[0].indexOf("Name             : ")).replace("Name             : ", ""),
                    cd[1].substring(cd[1].indexOf("SensorType       : ")).replace("SensorType       : ", ""),
                    cd[2].substring(cd[2].indexOf("Value            : ")).replace("Value            : ", "")
            });
        }
        return returnable;
    }

    /**
     * Checks the Windows Registry to see if the application is set to run on startup.
     * @return true if the startup entry exists, false otherwise.
     */
    public boolean checkIfRunningOnStartup() {
        if (currentOS == OSEnum.WINDOWS) {
            return Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Run", "ClusterManagement");
        }
        return false;
    }

    /**
     * Adds the application to the Windows Registry to run on startup.
     */
    public void addToStartup() {
        if (currentOS == OSEnum.WINDOWS) {
            Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Run", "ClusterManagement", "\"" + currentDir + File.separator + "ClusterManagement.exe" + "\"");
        }
    }

    /**
     * Removes the application from the Windows Registry startup items.
     */
    public void removeFromStartup() {
        if (currentOS == OSEnum.WINDOWS) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Run", "ClusterManagement");
        }
    }

    /**
     * Attempts to create and connect a socket.
     * @param ip The IP address to connect to.
     * @param port The port to connect to.
     * @return The connected Socket.
     * @throws IOException If the connection fails (e.g., timeout).
     */
    public Socket connectSocket(String ip, int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(ip, port), 2500);
        io.setSocket(s);
        return s;
    }

    /**
     * Closes the current socket connection.
     * @throws IOException If closing the socket fails.
     */
    public void disconnectSocket() throws IOException {
        if (io.getSocket() != null) {
            io.getSocket().close();
            io.setSocket(null);
        }
    }

    /**
     * Sends data over the currently connected socket.
     * @param data The string data to send.
     */
    public void sendData(String data) {
        io.sendData(data);
    }

    /**
     * @return The current socket, or null if not connected.
     */
    public Socket getSocket() {
        return io.getSocket();
    }
}
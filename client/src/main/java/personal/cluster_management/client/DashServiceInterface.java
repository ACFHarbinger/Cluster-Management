package personal.cluster_management.client;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Interface for the DashService.
 * Defines the contract for all backend logic, I/O, and OS interactions
 * that the Dash controller can request.
 */
public interface DashServiceInterface {
    /**
     * Reads the configuration file and returns its contents as a HashMap.
     * @return A HashMap containing the configuration key-value pairs.
     */
    HashMap<String, String> readConfig();

    /**
     * Saves the provided configuration array to the config file.
     * @param data A String array containing the 13 configuration values.
     */
    void saveConfig(String[] data);

    /**
     * Queries WMI via PowerShell to get sensor values from OpenHardwareMonitor.
     * @return A list of String arrays, where each array contains [Name, SensorType, Value].
     * @throws Exception If the shell command fails or parsing errors occur.
     */
    ArrayList<String[]> getValuesFromWMI() throws Exception;

    /**
     * Checks the Windows Registry to see if the application is set to run on startup.
     * @return true if the startup entry exists, false otherwise.
     */
    boolean checkIfRunningOnStartup();

    /**
     * Adds the application to the Windows Registry to run on startup.
     */
    void addToStartup();

    /**
     * Removes the application from the Windows Registry startup items.
     */
    void removeFromStartup();

    /**
     * Attempts to create and connect a socket.
     * @param ip The IP address to connect to.
     * @param port The port to connect to.
     * @return The connected Socket.
     * @throws IOException If the connection fails (e.g., timeout).
     */
    Socket connectSocket(String ip, int port) throws IOException;

    /**
     * Closes the current socket connection.
     * @throws IOException If closing the socket fails.
     */
    void disconnectSocket() throws IOException;

    /**
     * Sends data over the currently connected socket.
     * @param data The string data to send.
     */
    void sendData(String data);

    /**
     * @return The current socket, or null if not connected.
     */
    Socket getSocket();
}
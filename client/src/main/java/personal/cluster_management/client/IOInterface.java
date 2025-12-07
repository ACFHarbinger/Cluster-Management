package personal.cluster_management.client;

import java.io.IOException;
import java.net.Socket;

/**
 * Interface for all Input/Output operations.
 * This abstracts file system access, shell command execution, and socket communication,
 * making the DashService fully testable without real I/O.
 */
public interface IOInterface {

    /**
     * Reads the entire content of a file into a single string.
     * @param path The path to the file.
     * @return The file's content.
     * @throws IOException If an I/O error occurs.
     */
    String readFile(String path) throws IOException;

    /**
     * Saves a string of content to a file, overwriting it.
     * @param path The path to the file.
     * @param content The content to write.
     * @throws IOException If an I/O error occurs.
     */
    void saveFile(String path, String content) throws IOException;

    /**
     * Executes a shell command and returns its standard output.
     * @param cmd The command to execute.
     * @return The output from the command.
     * @throws Exception If the command fails or an error occurs.
     */
    String getShellOutput(String cmd) throws Exception;

    /**
     * Stores the active socket connection.
     * @param s The socket to store.
     */
    void setSocket(Socket s);

    /**
     * Retrieves the currently stored socket.
     * @return The active Socket, or null if none is set.
     */
    Socket getSocket();

    /**
     * Sends data over the currently stored socket.
     * @param data The string data to send.
     */
    void sendData(String data);
}
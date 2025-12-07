package personal.cluster_management.client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Concrete implementation of the IOInterface.
 * Handles real file, shell, and socket operations.
 */
public class IO implements IOInterface {

    private Socket socket;
    private PrintWriter out;

    @Override
    public String readFile(String path) throws IOException {
        // Use modern Java NIO for simpler file reading
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    @Override
    public void saveFile(String path, String content) throws IOException {
        // Use modern Java NIO for simpler file writing
        Files.write(Paths.get(path), content.getBytes());
    }

    @Override
    public String getShellOutput(String cmd) throws Exception {
        // Determine the correct shell for the OS
        String[] shellCmd;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            shellCmd = new String[]{"powershell.exe", "-Command", cmd};
        } else {
            shellCmd = new String[]{"bash", "-c", cmd};
        }

        ProcessBuilder pb = new ProcessBuilder(shellCmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder sb = new StringBuilder();
        // Use try-with-resources for reliable stream handling
        try (InputStreamReader isr = new InputStreamReader(p.getInputStream())) {
            int x;
            while ((x = isr.read()) != -1) {
                sb.append((char) x);
            }
        }

        p.waitFor();
        p.destroy();

        return sb.toString();
    }

    @Override
    public void setSocket(Socket s) {
        this.socket = s;
        if (s != null) {
            try {
                // Initialize the PrintWriter for sendData
                this.out = new PrintWriter(s.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
                this.out = null;
            }
        } else {
            this.out = null;
        }
    }

    @Override
    public Socket getSocket() {
        return this.socket;
    }

    @Override
    public void sendData(String data) {
        if (this.out != null && this.socket != null && this.socket.isConnected()) {
            this.out.println(data);
        } else {
            System.err.println("Could not send data: Socket is not connected or output stream is null.");
        }
    }
}
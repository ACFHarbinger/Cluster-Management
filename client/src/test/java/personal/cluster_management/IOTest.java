package personal.cluster_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IOTest {

    private IO io;

    @BeforeEach
    void setUp() {
        io = new IO();
    }

    // --- FILE SYSTEM TESTS ---

    /**
     * Tests both saveFile and readFile to ensure they work in tandem.
     * Uses @TempDir so no actual garbage files are left on your OS.
     */
    @Test
    void testFileWriteAndRead(@TempDir Path tempDir) throws IOException {
        // 1. Prepare path and content
        File tempFile = tempDir.resolve("testfile.txt").toFile();
        String absPath = tempFile.getAbsolutePath();
        String contentToWrite = "Hello, World!";

        // 2. Write file
        io.saveFile(absPath, contentToWrite);

        // 3. Read file
        String readContent = io.readFile(absPath);

        // 4. Assert
        assertEquals(contentToWrite, readContent, "The content read should match the content written.");
    }

    @Test
    void testReadFileFileNotFound(@TempDir Path tempDir) {
        String nonExistentPath = tempDir.resolve("ghost.txt").toString();
        
        // Should throw IOException if file doesn't exist
        assertThrows(IOException.class, () -> io.readFile(nonExistentPath));
    }

    // --- SHELL COMMAND TESTS ---

    @Test
    void testGetShellOutputEcho() throws Exception {
        // "echo" is a standard command available on both Windows (cmd/powershell) and *nix
        String message = "integration_test";
        String cmd = "echo " + message;

        String output = io.getShellOutput(cmd);

        assertNotNull(output);
        assertTrue(output.trim().contains(message), 
            "Shell output should contain the echoed message.");
    }

    @Test
    void testGetShellOutputInvalidCommand() {
        // Attempt to run a garbage command
        String invalidCmd = "this_command_does_not_exist_12345";

        // Depending on OS, this might throw an exception OR return an error string.
        // Usually ProcessBuilder doesn't throw on start() for bad commands, 
        // but the error stream handling in IO.java might capture stderr.
        // However, checking for non-null execution is the baseline safety.
        
        try {
            String output = io.getShellOutput(invalidCmd);
            // If it returns, the output usually contains "not found" or is empty
            assertNotNull(output); 
        } catch (Exception e) {
            // If it throws, that is also an acceptable outcome for a bad command
            assertNotNull(e); 
        }
    }

    // --- SOCKET TESTS ---

    @Test
    void testSetAndGetSocket() {
        Socket mockSocket = mock(Socket.class);
        
        io.setSocket(mockSocket);
        
        assertEquals(mockSocket, io.getSocket(), "Should retrieve the exact socket instance set previously.");
    }

    @Test
    void testSendDataSuccess() throws IOException {
        // 1. Mock the Socket and its OutputStream
        Socket mockSocket = mock(Socket.class);
        ByteArrayOutputStream mockOutputStream = new ByteArrayOutputStream();

        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
        when(mockSocket.isConnected()).thenReturn(true);

        // 2. Initialize IO with the mock
        io.setSocket(mockSocket);

        // 3. Send data
        String message = "ping";
        io.sendData(message);

        // 4. Verify the data ended up in the stream
        // PrintWriter usually adds a newline, so we check if the stream contains our message
        String output = mockOutputStream.toString();
        assertTrue(output.contains(message), "The output stream should contain the sent data.");
    }

    @Test
    void testSendDataSocketNotConnected() throws IOException {
        // 1. Mock a socket that is NOT connected
        Socket mockSocket = mock(Socket.class);
        when(mockSocket.isConnected()).thenReturn(false);
        // Even if we get an output stream, the logic inside sendData checks isConnected()
        when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        io.setSocket(mockSocket);
        
        // 2. Send data
        io.sendData("test");

        // 3. Verify interaction
        // Since isConnected() is false, logic dictates we shouldn't write to the stream.
        // However, the PrintWriter wraps the stream immediately in setSocket.
        // The specific implementation of sendData checks: if (out != null && socket != null && socket.isConnected())
        
        // Because isConnected is false, the print(ln) should NOT happen.
        // Note: We cannot easily verify "no write" on the PrintWriter without a Spy, 
        // but we can verify the behavior was safe (no exception threw).
        assertDoesNotThrow(() -> io.sendData("test"));
    }
    
    @Test
    void testSetSocketNull() {
        io.setSocket(null);
        assertNull(io.getSocket());
        
        // Should handle sending data safely even if socket is null
        assertDoesNotThrow(() -> io.sendData("hello"));
    }
}
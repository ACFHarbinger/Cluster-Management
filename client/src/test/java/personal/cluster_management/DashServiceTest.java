package personal.cluster_management;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for the DashService.
 * This test uses Mockito to create a fake IO object,
 * which allows us to test file I/O, shell parsing, and socket logic
 * without real external dependencies.
 */
class DashServiceTest {

    @Mock
    private IO mockIo;

    @Mock
    private Socket mockSocket;

    private DashService service;
    private final String currentDir = System.getProperty("user.dir");

    @BeforeEach
    void setUp() {
        // Initialize mocks for each test
        MockitoAnnotations.openMocks(this);
        // Create the service, injecting the mock IO
        service = new DashService(mockIo);
    }

    @Test
    @DisplayName("readConfig() should correctly parse a valid config file")
    void testReadConfigHappyPath() throws Exception {
        // Arrange
        String fakeConfig = "127.0.0.1\n" +
                "8080\n" +
                "CPU Load\n" +
                "GPU Load\n" +
                "CPU Temp\n" +
                "GPU Temp\n" +
                "CPU Fan\n" +
                "GPU Fan\n" +
                "VRAM Total\n" +
                "VRAM Used\n" +
                "RAM Used\n" +
                "RAM Avail\n" +
                "1500";
        String configPath = currentDir + File.separator + "config.cfg";
        when(mockIo.readFile(configPath)).thenReturn(fakeConfig);

        // Act
        HashMap<String, String> config = service.readConfig();

        // Assert
        assertNotNull(config);
        assertEquals(13, config.size());
        assertEquals("127.0.0.1", config.get("SERVER_IP"));
        assertEquals("8080", config.get("SERVER_PORT"));
        assertEquals("CPU Load", config.get("CPU_LOAD_NAME"));
        assertEquals("1500", config.get("REFRESH_INTERVAL"));
    }

    @Test
    @DisplayName("readConfig() should return empty map if file is too short")
    void testReadConfigShortFile() throws Exception {
        // Arrange
        String fakeConfig = "127.0.0.1\n8080"; // Only 2 lines
        String configPath = currentDir + File.separator + "config.cfg";
        when(mockIo.readFile(configPath)).thenReturn(fakeConfig);

        // Act
        HashMap<String, String> config = service.readConfig();

        // Assert
        assertNotNull(config);
        assertTrue(config.isEmpty());
    }

    @Test
    @DisplayName("readConfig() should return empty map on file read error")
    void testReadConfigReadError() throws Exception {
        // Arrange
        String configPath = currentDir + File.separator + "config.cfg";
        when(mockIo.readFile(configPath)).thenThrow(new IOException("File not found"));

        // Act
        HashMap<String, String> config = service.readConfig();

        // Assert
        assertNotNull(config);
        assertTrue(config.isEmpty());
    }

    @Test
    @DisplayName("saveConfig() should call io.saveFile with correct path and content")
    void testSaveConfig() throws Exception {
        // Arrange
        String[] data = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"};
        String expectedContent = String.join("\n", data);
        String configPath = currentDir + File.separator + "config.cfg";

        // Act
        service.saveConfig(data);

        // Assert
        // Verify that io.saveFile was called exactly once with the correct path and formatted content
        verify(mockIo, times(1)).saveFile(eq(configPath), eq(expectedContent));
    }

    @Test
    @DisplayName("getValuesFromWMI() should correctly parse shell output")
    void testGetValuesFromWMIParsing() throws Exception {
        // Arrange
        // This is a fake string mimicking the output of the PowerShell command
        // *after* the .replace() method has run.
        String fakeShellOutput =
                "Name             : CPU Load\r\n" +
                "SensorType       : Load\r\n" +
                "Value            : 55.5\r\n" +
                "PSComputerName   : \r\n" + // Note the 3 spaces
                "\r\n" +
                "Name             : GPU Temp\r\n" +
                "SensorType       : Temperature\r\n" +
                "Value            : 68\r\n" +
                "PSComputerName   : \r\n" +
                "\r\n" +
                "Name             : RAM Used\r\n" +
                "SensorType       : SmallData\r\n" +
                "Value            : 8.2\r\n" +
                "PSComputerName   : \r\n" +
                "\r\n";
        when(mockIo.getShellOutput(anyString())).thenReturn(fakeShellOutput);

        // Act
        ArrayList<String[]> result = service.getValuesFromWMI();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size(), "Should parse three sensors");
        assertArrayEquals(new String[]{"CPU Load", "Load", "55.5"}, result.get(0));
        assertArrayEquals(new String[]{"GPU Temp", "Temperature", "68"}, result.get(1));
        assertArrayEquals(new String[]{"RAM Used", "SmallData", "8.2"}, result.get(2));
    }

    @Test
    @DisplayName("getValuesFromWMI() should propagate shell exceptions")
    void testGetValuesFromWMIShellError() throws Exception {
        // Arrange
        when(mockIo.getShellOutput(anyString())).thenThrow(new RuntimeException("PowerShell command failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            service.getValuesFromWMI();
        }, "Exception from shell should be propagated");
    }

    @Test
    @DisplayName("checkIfRunningOnStartup() should correctly query registry")
    void testCheckIfRunningOnStartup() {
        // We must use try-with-resources to mock static methods
        try (MockedStatic<Advapi32Util> mocked = mockStatic(Advapi32Util.class)) {
            // Test case 1: Value exists
            mocked.when(() -> Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Run", "ClusterManagement"))
                  .thenReturn(true);
            
            assertTrue(service.checkIfRunningOnStartup(), "Should return true when registry value exists");

            // Test case 2: Value does not exist
            mocked.when(() -> Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, "Software\\Microsoft\\Windows\\CurrentVersion\\Run", "ClusterManagement"))
                  .thenReturn(false);
            
            assertFalse(service.checkIfRunningOnStartup(), "Should return false when registry value does not exist");
        }
    }

    @Test
    @DisplayName("addToStartup() should correctly set registry value")
    void testAddToStartup() {
        try (MockedStatic<Advapi32Util> mocked = mockStatic(Advapi32Util.class)) {
            // Act
            service.addToStartup();

            // Assert
            // Verify that the static method was called with the correct arguments
            mocked.verify(() -> Advapi32Util.registrySetStringValue(
                eq(WinReg.HKEY_CURRENT_USER),
                eq("Software\\Microsoft\\Windows\\CurrentVersion\\Run"),
                eq("ClusterManagement"),
                contains("ClusterManagement.exe") // Use contains for the path part
            ));
        }
    }

    @Test
    @DisplayName("removeFromStartup() should correctly delete registry value")
    void testRemoveFromStartup() {
        try (MockedStatic<Advapi32Util> mocked = mockStatic(Advapi32Util.class)) {
            // Act
            service.removeFromStartup();

            // Assert
            // Verify that the static method was called with the correct arguments
            mocked.verify(() -> Advapi32Util.registryDeleteValue(
                eq(WinReg.HKEY_CURRENT_USER),
                eq("Software\\Microsoft\\Windows\\CurrentVersion\\Run"),
                eq("ClusterManagement")
            ));
        }
    }

    @Test
    @DisplayName("disconnectSocket() should close socket and call io.setSocket(null)")
    void testDisconnectSocketWhenConnected() throws Exception {
        // Arrange
        when(mockIo.getSocket()).thenReturn(mockSocket);

        // Act
        service.disconnectSocket();

        // Assert
        verify(mockSocket, times(1)).close(); // Verify the socket was closed
        verify(mockIo, times(1)).setSocket(null); // Verify io.setSocket was called with null
    }

    @Test
    @DisplayName("disconnectSocket() should do nothing if already disconnected")
    void testDisconnectSocketWhenNotConnected() throws Exception {
        // Arrange
        when(mockIo.getSocket()).thenReturn(null);

        // Act
        service.disconnectSocket();

        // Assert
        verify(mockSocket, never()).close(); // Verify mockSocket.close() was *never* called
        verify(mockIo, never()).setSocket(any()); // Verify io.setSocket() was *never* called
    }

    @Test
    @DisplayName("sendData() should call io.sendData()")
    void testSendData() {
        // Arrange
        String data = "hello";

        // Act
        service.sendData(data);

        // Assert
        verify(mockIo, times(1)).sendData(data);
    }

    @Test
    @DisplayName("getSocket() should call io.getSocket()")
    void testGetSocket() {
        // Arrange
        when(mockIo.getSocket()).thenReturn(mockSocket);

        // Act
        Socket s = service.getSocket();

        // Assert
        assertEquals(mockSocket, s);
        verify(mockIo, times(1)).getSocket();
    }

    // Note: connectSocket() is not unit-tested here as it creates a `new Socket()`
    // which is a concrete dependency. Testing it would require an integration
    // test with a live server port or refactoring the service to inject a SocketFactory.
}
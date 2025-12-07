package personal.cluster_management;

import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Monitor controller, focusing on logic, config, and data processing.
 * The network components (ServerSocket/Socket) and JavaFX dependencies (Platform.runLater)
 * are mocked or stubbed to achieve isolation.
 */
class MonitorTest {

    // Dependencies (Mocked)
    private IOInterface mockIO;
    private MonitorUIInterface mockView;
    private Monitor monitor;

    // Components that Monitor creates/uses internally (Mocked)
    private Socket mockSocket;
    private ServerSocket mockServerSocket;

    // --- Mocking Setup ---

    @BeforeEach
    void setUp() throws Exception {
        // Setup Mocks
        mockIO = mock(IOInterface.class);
        mockView = mock(MonitorUIInterface.class);
        mockSocket = mock(Socket.class);
        mockServerSocket = mock(ServerSocket.class);

        // Stubbing the IO readConfig dependency: Assume a valid config file is read
        when(mockIO.readFileArranged(anyString(), anyString()))
                .thenReturn(new String[]{"1000", "800", "8080"});
        
        // Stubbing View/UI dependencies created in the Monitor constructor
        when(mockView.getRootNode()).thenReturn(new StackPane());
        when(mockView.getNotConnectedPaneHeadingLabel()).thenReturn(mock(javafx.scene.control.Label.class));
        when(mockView.getNotConnectedPaneSubHeadingLabel()).thenReturn(mock(javafx.scene.control.Label.class));

        // Use a Mockito Spy on the Monitor to substitute internal components (like ServerSocket)
        // This requires the Monitor to be initialized before the Spy is created, but since we can't
        // inject ServerSocket, we rely on mocking the constructor dependencies and testing public methods.
        
        // Initialize Monitor (will call readConfig and loadNodes)
        // NOTE: Since Monitor creates MonitorUI internally (new MonitorUI()),
        // we're injecting a Mockito Spy into the class definition to allow us to replace it.
        // A better design would be dependency injection for MonitorUIInterface as well.
        // For now, we assume we can test the public methods that depend on IO.
        monitor = new Monitor(mockIO) {
            // Override the view initialization to use our mock
            @Override
            public MonitorUIInterface createView() {
                return mockView;
            }
        };
        
        // Manually set the mock view property for testing methods that use it later
        // (This is a workaround for the Monitor's internal view creation)
        java.lang.reflect.Field viewField = Monitor.class.getDeclaredField("view");
        viewField.setAccessible(true);
        viewField.set(monitor, mockView);

        // Stub gauge getters for process() test
        when(mockView.getCPULoadGauge()).thenReturn(mock(eu.hansolo.tilesfx.Tile.class));
        when(mockView.getGPULoadGauge()).thenReturn(mock(eu.hansolo.tilesfx.Tile.class));
        when(mockView.getCPUTempGauge()).thenReturn(mock(eu.hansolo.medusa.Gauge.class));
        when(mockView.getGPUTempGauge()).thenReturn(mock(eu.hansolo.medusa.Gauge.class));
        when(mockView.getCPUFanSpeedGauge()).thenReturn(mock(eu.hansolo.medusa.Gauge.class));
        when(mockView.getGPUFanSpeedGauge()).thenReturn(mock(eu.hansolo.medusa.Gauge.class));
        when(mockView.getMemoryGauge()).thenReturn(mock(eu.hansolo.medusa.Gauge.class));
        when(mockView.getVideoMemoryGauge()).thenReturn(mock(eu.hansolo.medusa.Gauge.class));
        when(mockView.getMemorySubHeadingLabel()).thenReturn(mock(javafx.scene.control.Label.class));
        when(mockView.getVideoMemorySubHeadingLabel()).thenReturn(mock(javafx.scene.control.Label.class));
    }
    
    // --- Server Control Tests ---

    @Test
    void testStopServerSetsFlagAndClosesSockets() throws Exception {
        // Use reflection to set internal ServerSocket for testing stopServer()
        java.lang.reflect.Field ssField = Monitor.class.getDeclaredField("serverSocket");
        ssField.setAccessible(true);
        ssField.set(monitor, mockServerSocket);
        
        java.lang.reflect.Field sField = Monitor.class.getDeclaredField("socket");
        sField.setAccessible(true);
        sField.set(monitor, mockSocket);
        
        // Simulate sockets being open
        when(mockServerSocket.isClosed()).thenReturn(false);
        when(mockSocket.isClosed()).thenReturn(false);

        // Act
        monitor.stopServer();

        // Assert isRunning is false
        java.lang.reflect.Field runningField = Monitor.class.getDeclaredField("isRunning");
        runningField.setAccessible(true);
        assertFalse((boolean) runningField.get(monitor), "isRunning flag must be set to false.");
        
        // Verify sockets are closed
        verify(mockSocket).close();
        verify(mockServerSocket).close();
    }
    
    // --- Configuration Tests ---

    @Test
    void testReadConfigSuccess() throws Exception {
        // Setup: done in BeforeEach (returns 1000, 800, 8080)
        
        // Act: Read config is called in constructor, but can be called again
        monitor.readConfig();
        
        // Assert: Verify IO was called to read the config file
        verify(mockIO, atLeastOnce()).readFileArranged("config", "::");

        // Assert: Verify view setup was performed with config values
        ArgumentCaptor<Double> widthCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> heightCaptor = ArgumentCaptor.forClass(Double.class);
        
        verify(mockView.getRootNode()).setPrefSize(widthCaptor.capture(), heightCaptor.capture());
        
        assertEquals(1000.0, widthCaptor.getValue(), 0.01, "Screen width from config should be applied.");
        assertEquals(800.0, heightCaptor.getValue(), 0.01, "Screen height from config should be applied.");
    }

    @Test
    void testReadConfigFailureCreatesDefaults() throws Exception {
        // Setup: Configure mock to throw an exception (simulating file not found/corrupt)
        when(mockIO.readFileArranged(anyString(), anyString())).thenThrow(new IOException("File not found"));
        
        // Re-initialize monitor so the constructor calls the failing readConfig
        monitor = new Monitor(mockIO) {
             @Override
            public MonitorUIInterface createView() {
                return mockView;
            }
        };
        
        // Act: (readConfig was called in constructor)
        
        // Assert: Verify default config was written back to disk
        verify(mockIO).writeToFile("1280::720::8080::", "config");
        
        // Assert: Verify view setup was performed with default config values
        ArgumentCaptor<Double> widthCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> heightCaptor = ArgumentCaptor.forClass(Double.class);
        
        verify(mockView.getRootNode(), atLeastOnce()).setPrefSize(widthCaptor.capture(), heightCaptor.capture());
        
        assertEquals(1280.0, widthCaptor.getValue(), 0.01, "Default screen width should be applied.");
        assertEquals(720.0, heightCaptor.getValue(), 0.01, "Default screen height should be applied.");
    }
    
    // --- Data Processing Tests ---

    @Test
    void testProcessValidDataUpdatesGauges() {
        // Sample data string matching the 10 expected fields
        String dataString = "10.5,55.0,2000,99.9,80.1,3500,20480,32768,8192,4096"; // CPU_LOAD, CPU_TEMP, CPU_FAN, GPU_LOAD, GPU_TEMP, GPU_FAN, USED_VRAM, TOTAL_VRAM, USED_RAM, AVAILABLE_RAM
        
        // Act
        // NOTE: Platform.runLater() is *not* mocked here. We rely on Mockito to verify 
        // that the setters on the mocked gauges were called, trusting that Platform.runLater 
        // works as intended in a real JavaFX environment.
        monitor.process(dataString);
        
        // Assert: Verify gauge values are set (simple fields)
        verify(mockView.getCPUTempGauge()).setValue(55.0);
        verify(mockView.getGPUFanSpeedGauge()).setValue(3500.0);
        
        // Assert: Verify gauge values are set (complex/percentage fields)
        verify(mockView.getCPULoadGauge()).setValue(10.5);
        verify(mockView.getGPULoadGauge()).setValue(99.9);

        // Assert: Verify memory calculations (VRAM: 20480/32768 * 100 = 62.5%)
        verify(mockView.getVideoMemoryGauge()).setValue(62.5);
        // Subheading (20480/1024=20GB, 32768/1024=32GB)
        verify(mockView.getVideoMemorySubHeadingLabel()).setText("20GB / 32GB");

        // Assert: Verify RAM calculations (RAM: 8192 / (8192 + 4096) * 100 = 66.66%)
        verify(mockView.getMemoryGauge()).setValue(argThat(val -> val > 66.66 && val < 66.67));
        // Subheading (8192GB / 12288GB) -> the values are cast to int in the source, so we check the text
        verify(mockView.getMemorySubHeadingLabel()).setText("8192GB / 12288GB");
    }

    @Test
    void testProcessMalformedDataLogsError() {
        String malformedData = "10.5,55.0,2000,99.9,80.1"; // Only 5 fields
        
        monitor.process(malformedData);
        
        // Assert: Verify error was logged and no gauges were updated
        verify(mockIO).pln("Received malformed data: " + malformedData);
        verify(mockView.getCPULoadGauge(), never()).setValue(anyDouble());
        verify(mockView.getVideoMemoryGauge(), never()).setValue(anyDouble());
    }

    @Test
    void testProcessNonNumericDataSkipped() {
        // GPU_LOAD is set to "N/A"
        String dataString = "10.5,55.0,2000,N/A,80.1,3500,20480,32768,8192,4096";
        
        monitor.process(dataString);
        
        // Assert: Verify error was logged for skipping
        verify(mockIO).pln("Skipping non-numeric value for GPU_LOAD: N/A");

        // Assert: Ensure other gauges *were* updated
        verify(mockView.getCPULoadGauge()).setValue(10.5);
        
        // Assert: Ensure the GPU_LOAD gauge was NOT updated with a value
        verify(mockView.getGPULoadGauge(), never()).setValue(99.9);
    }
}
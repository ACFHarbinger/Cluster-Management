package personal.cluster_management.client;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for the Dash *controller* logic.
 * This test uses Mockito to create a fake DashService,
 * allowing us to test controller logic (validation, state changes)
 * in isolation without real files or shell commands.
 *
 * It uses ApplicationExtension to allow JavaFX components (from DashUI)
 * to be instantiated, but it does *not* use FxRobot.
 */
@ExtendWith(ApplicationExtension.class)
class DashTest {

    @Mock
    private DashService mockService;
    @Mock
    private Main mockMain;
    @Mock
    private Stage mockStage;

    private DashUI realView;
    private Dash dashController;

    // A fake config map that the mock service will return
    private final HashMap<String, String> fakeConfig = new HashMap<>() {{
        put("SERVER_IP", "192.168.1.100");
        put("SERVER_PORT", "9090");
        put("CPU_LOAD_NAME", "CPU Load");
        put("GPU_LOAD_NAME", "GPU Load");
        put("CPU_TEMP_NAME", "CPU Temp");
        put("GPU_TEMP_NAME", "GPU Temp");
        put("CPU_FAN_NAME", "CPU Fan");
        put("GPU_FAN_NAME", "GPU Fan");
        put("TOTAL_VRAM_NAME", "GPU VRAM Total");
        put("USED_VRAM_NAME", "GPU VRAM Used");
        put("USED_RAM_NAME", "RAM Used");
        put("AVAILABLE_RAM_NAME", "RAM Avail");
        put("REFRESH_INTERVAL", "2000");
    }};

    @BeforeEach
    void setUp() throws InterruptedException {
        MockitoAnnotations.openMocks(this);

        // Mock the service's readConfig call
        when(mockService.readConfig()).thenReturn(fakeConfig);
        when(OSEnum.getOS()).thenReturn(OSEnum.WINDOWS);

        // We need to initialize the UI on the JavaFX thread
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            realView = new DashUI();
            realView.loadNodes();
            // Create the controller, injecting the real view and mock service
            dashController = new Dash(mockMain, mockStage, realView, mockService);
            latch.countDown();
        });
        // Wait for setup to complete
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Controller should load config from service and apply it to view fields")
    void testConfigIsLoadedAndApplied() {
        // Assert that the text fields in the view were set
        // by the controller's applyConfigReadingsToFields() method
        assertEquals("192.168.1.100", realView.serverIPAddressTextField.getText());
        assertEquals("9090", realView.serverPortTextField.getText());
        assertEquals("CPU Load", realView.CPULoadNameTextField.getText());
        assertEquals("2000", realView.dataRefreshIntervalTextField.getText());
    }

    @Test
    @DisplayName("validateConfig() should return 'OK' when all fields are valid")
    void testConfigValidationSuccess() {
        // The @BeforeEach already filled the fields with valid data
        String result = dashController.validateConfig();
        assertEquals("OK", result);
    }

    @Test
    @DisplayName("validateConfig() should detect empty CPU Load Name")
    void testConfigValidationEmptyField() {
        // Arrange: Make one field invalid
        realView.CPULoadNameTextField.setText("");

        // Act
        String result = dashController.validateConfig();

        // Assert
        assertNotEquals("OK", result);
        assertTrue(result.contains("CPU Load Name cannot be empty!"));
    }

    @Test
    @DisplayName("validateConfig() should detect invalid Refresh Interval")
    void testConfigValidationInvalidNumber() {
        // Arrange: Make a number field invalid
        realView.dataRefreshIntervalTextField.setText("not-a-number");

        // Act
        String result = dashController.validateConfig();

        // Assert
        assertNotEquals("OK", result);
        assertTrue(result.contains("Refresh Data Interval must be a number"));
    }

    @Test
    @DisplayName("saveConfig() should call service when validation is OK")
    void testSaveConfigSuccess() {
        // Arrange: (Fields are already valid from setup)

        // Act
        dashController.saveConfig();

        // Assert: Verify the service's saveConfig method was called
        // We can be specific and check the content of the array
        verify(mockService).saveConfig(argThat(arr ->
                arr.length == 13 &&
                arr[0].equals("192.168.1.100") &&
                arr[2].equals("CPU Load") &&
                arr[12].equals("2000")
        ));
    }

    @Test
    @DisplayName("saveConfig() should not call service when validation fails")
    void testSaveConfigValidationFails() {
        // Arrange: Make a field invalid
        realView.CPULoadNameTextField.setText("");

        // Act
        dashController.saveConfig(); // This will show an alert (which we don't test here)

        // Assert: Verify the service's saveConfig method was *never* called
        verify(mockService, never()).saveConfig(any(String[].class));
    }

    @Test
    @DisplayName("initGPUCPURAM() should process WMI data from service and update config map")
    void testInitGPUCPURAM() throws Exception {
        // Arrange
        // This is the *parsed* data we expect from the service
        ArrayList<String[]> fakeParsedData = new ArrayList<>();
        fakeParsedData.add(new String[]{"CPU Load", "Load", "55.5"});
        fakeParsedData.add(new String[]{"GPU Temp", "Temperature", "68"});
        fakeParsedData.add(new String[]{"RAM Used", "SmallData", "8.2"});
        
        // Tell the mock service to return this fake data
        when(mockService.getValuesFromWMI()).thenReturn(fakeParsedData);

        // Act
        dashController.initGPUCPURAM();

        // Assert
        // Check that the controller's internal config map was updated
        assertEquals("55.5", dashController.config.get("CPU_LOAD"));
        assertEquals("68", dashController.config.get("GPU_TEMP"));
        assertEquals("8.2", dashController.config.get("USED_RAM"));
        // Check that other values are null (since they weren't in the fake data)
        assertNull(dashController.config.get("GPU_LOAD"));
    }
}
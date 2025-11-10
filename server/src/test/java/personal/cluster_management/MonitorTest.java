package personal.cluster_management;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.tilesfx.Tile;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * This test class now tests the Monitor logic, which is possible
 * because we can inject a mock IO object.
 */
@ExtendWith(ApplicationExtension.class)
class MonitorTest {

    // 1. Create a mock of the IO dependency
    @Mock
    private IO mockIo;

    private Monitor monitor;

    /**
     * This setup is more complex because Monitor is a JavaFX component.
     * We must instantiate it on the JavaFX thread.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Initialize the @Mock object
        MockitoAnnotations.openMocks(this);

        // 2. Define the mock's behavior BEFORE it's used
        String[] fakeConfig = {"800", "600", "9090"};
        // When io.readFileArranged("config", "::") is called, return our fake config
        when(mockIo.readFileArranged(eq("config"), eq("::"))).thenReturn(fakeConfig);

        // 3. We must create the Monitor (a UI component) on the FX thread
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Inject the mockIo into the constructor
            monitor = new Monitor(mockIo);
            latch.countDown();
        });

        // Wait for the FX thread to finish setup
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Failed to set up Monitor on FX thread");
        }
    }

    @Test
    @DisplayName("Constructor should read config from IO and set properties")
    void testMonitorConstructor() {
        // Assert
        // Check if the config map was populated correctly from the mock
        assertEquals("800", monitor.config.get("SCREEN_WIDTH"));
        assertEquals("600", monitor.config.get("SCREEN_HEIGHT"));
        assertEquals("9090", monitor.config.get("SERVER_PORT"));

        // Check if the constructor used the config values
        // (This verifies readConfig() ran before setPrefSize())
        assertEquals(800, monitor.getPrefWidth());
        assertEquals(600, monitor.getPrefHeight());
    }

    @Test
    @DisplayName("updateGauges should parse data and update UI components")
    void testUpdateGauges(FxRobot robot) {
        // Arrange
        // A fake data string from the client
        String fakeData = "CPU_LOAD::55.0" +
                "::CPU_TEMP::68.0" +
                "::GPU_LOAD::42.0" +
                "::GPU_TEMP::75.0" +
                "::CPU_FAN::1200.0" +
                "::GPU_FAN::1500.0";

        // Act
        // We use robot.interact() to run this on the FX thread
        robot.interact(() -> {
            try {
                monitor.updateGauges(fakeData);
            } catch (Exception e) {
                fail("updateGauges threw an exception", e);
            }
        });

        // Assert
        // TestFX assertions to safely read values from UI components
        FxAssert.verifyThat(monitor.CPULoadGauge, (Tile tile) -> tile.getValue() == 55.0);
        FxAssert.verifyThat(monitor.CPUTempGauge, (Gauge gauge) -> gauge.getValue() == 68.0);
        FxAssert.verifyThat(monitor.GPULoadGauge, (Tile tile) -> tile.getValue() == 42.0);
        FxAssert.verifyThat(monitor.GPUTempGauge, (Gauge gauge) -> gauge.getValue() == 75.0);
        FxAssert.verifyThat(monitor.CPUFanSpeedGauge, (Gauge gauge) -> gauge.getValue() == 1200.0);
        FxAssert.verifyThat(monitor.GPUFanSpeedGauge, (Gauge gauge) -> gauge.getValue() == 1500.0);
    }
    
    @Test
    @DisplayName("Test RAM and VRAM updates in updateGauges")
    void testUpdateGauges_RamAndVram(FxRobot robot) {
        // Arrange
        // Test data for VRAM (16000 total, 4000 used) and RAM (32 total, 8 used)
        String fakeData = "TOTAL_VRAM::16000.0" +
                "::USED_VRAM::4000.0" +
                "::AVAILABLE_RAM::24.0" + // free RAM
                "::USED_RAM::8.0";

        // Act
        robot.interact(() -> {
            try {
                // Manually reset values to test calculations
                monitor.totalVRAM = -1;
                monitor.usedVRAM = -1;
                monitor.freeRAM = -1;
                monitor.usedRAM = -1;
                monitor.updateGauges(fakeData);
            } catch (Exception e) {
                fail("updateGauges threw an exception", e);
            }
        });

        // Assert
        // VRAM: 4000 / 16000 = 25%
        FxAssert.verifyThat(monitor.videoMemoryGauge, (Gauge gauge) -> gauge.getValue() == 25.0);
        // RAM: 8 / (8 + 24) = 8 / 32 = 25%
        FxAssert.verifyThat(monitor.memoryGauge, (Gauge gauge) -> gauge.getValue() == 25.0);
    }

    @Test
    @DisplayName("readConfig should handle IO failure gracefully")
    void testReadConfig_HandlesException() throws Exception {
        // Arrange
        // Create a new mock that throws an error
        IO failingIo = org.mockito.Mockito.mock(IO.class);
        when(failingIo.readFileArranged(eq("config"), eq("::")))
                .thenThrow(new IOException("Mock File Not Found"));

        // Act & Assert
        // The constructor calls readConfig(), which will now throw.
        // The original code has a try/catch that just prints a stack trace.
        // This means the constructor *should* complete, but the config map will be empty.
        
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Monitor newMonitor = new Monitor(failingIo);
            
            // Because config is empty, this will throw a NullPointerException
            // This test reveals a bug in the original code!
            Exception e = assertThrows(NullPointerException.class, () -> {
                newMonitor.setPrefSize(
                    Integer.parseInt(newMonitor.config.get("SCREEN_WIDTH")), // This will be null
                    Integer.parseInt(newMonitor.config.get("SCREEN_HEIGHT"))
                );
            });
            assertNotNull(e, "A NullPointerException should occur if config fails to load.");
            
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
    }
}
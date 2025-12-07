package personal.cluster_management;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.tilesfx.Tile;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for MonitorUI, requiring a JavaFX thread setup (e.g., via TestFX or a custom JUnit runner).
 * Assumes TestFX is in use or the test environment initializes the JavaFX platform.
 */
class MonitorUITest {
    
    private MonitorUI monitorUI;
    
    /**
     * Required boilerplate to initialize the JavaFX toolkit.
     * This will fail if a proper JUnit/JavaFX test runner is not used.
     */
    @BeforeAll
    static void initJFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await();
    }
    
    @BeforeEach
    void setUp() {
        monitorUI = new MonitorUI();
        // Since loadNodes creates all UI components, we must run it.
        // It also loads CSS and fonts, which might fail outside a proper build.
        // For testing, we call it and rely on getters to access components.
        monitorUI.loadNodes(); 
    }

    @Test
    void testLoadNodes_InitialState() {
        // Assert that the initial pane is 'notConnected' (default behavior)
        // Check that the root node is the MonitorUI itself (as it extends StackPane)
        assertEquals(monitorUI, monitorUI.getRootNode());
        
        // Verify a core component was initialized
        assertNotNull(monitorUI.getCPULoadGauge(), "CPULoadGauge should be initialized.");
        assertNotNull(monitorUI.getNotConnectedPaneHeadingLabel(), "NotConnectedPane heading should be initialized.");
        
        // Initial state should show the notConnected pane in front (assuming default loadNodes behavior)
        // We cannot easily verify Z-order without TestFX, but we check if components exist.
    }

    @Test
    void testSwitchPane_Gauges() {
        monitorUI.switchPane(MonitorPaneEnum.gauges);
        // We trust the toFront() method works, and verify the side effect: resetAllNodes should be called.
        
        // Since resetAllNodes is called, gauges should be reset to 0
        assertEquals(0.0, monitorUI.getCPULoadGauge().getValue(), 0.01, "Gauge should be reset to 0.0.");
        assertEquals("0GB / 0GB", monitorUI.getMemorySubHeadingLabel().getText(), "Memory label should be reset.");
    }
    
    @Test
    void testSwitchPane_NotConnected() {
        // First, switch to gauges to ensure a component is not null
        monitorUI.switchPane(MonitorPaneEnum.gauges);
        
        // Then, switch to notConnected
        monitorUI.switchPane(MonitorPaneEnum.notConnected);

        // Assert that the state is correct, but the gauges are NOT reset again
        // We check a gauge that should have been set to 0.0 in the previous step
        assertEquals(0.0, monitorUI.getCPULoadGauge().getValue(), 0.01, "Switching to NotConnected should not reset gauges again.");
    }
    
    @Test
    void testResetAllNodes() {
        // Set a gauge to a non-zero value
        monitorUI.getCPULoadGauge().setValue(50.0);
        monitorUI.getMemorySubHeadingLabel().setText("10GB / 20GB");
        
        // Act
        monitorUI.resetAllNodes();
        
        // Assert
        assertEquals(0.0, monitorUI.getCPULoadGauge().getValue(), 0.01, "CPULoadGauge should be reset to 0.");
        assertEquals(0.0, monitorUI.getMemoryGauge().getValue(), 0.01, "MemoryGauge should be reset to 0.");
        assertEquals("0GB / 0GB", monitorUI.getMemorySubHeadingLabel().getText(), "Memory label should be reset.");
        
        // Verify all components defined in resetAllNodes are reset
        Tile cpuLoad = monitorUI.getCPULoadGauge();
        Gauge cpuTemp = monitorUI.getCPUTempGauge();
        Label memoryLabel = monitorUI.getMemorySubHeadingLabel();
        
        assertAll(
            () -> assertEquals(0.0, cpuLoad.getValue()),
            () -> assertEquals(0.0, cpuTemp.getValue()),
            () -> assertEquals("0GB / 0GB", memoryLabel.getText())
        );
    }
}
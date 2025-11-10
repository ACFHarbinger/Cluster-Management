package personal.cluster_management;

import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.matcher.layout.NodeMatchers;

import static org.testfx.matcher.layout.NodeMatchers.isVisible;

// Use TestFX's extension to launch JavaFX applications
@ExtendWith(ApplicationExtension.class)
class MonitorUITest {

    private MonitorUI monitorUI;

    /**
     * This method is run by TestFX to start the JavaFX application.
     */
    @Start
    private void start(Stage stage) {
        // We test MonitorUI directly, not Monitor, to avoid
        // file/network operations in this test.
        monitorUI = new MonitorUI();
        
        // Manually load nodes, as this is part of the original constructor
        // We wrap this in runLater to ensure it's on the FX thread
        // Note: This needs assets/Roboto.ttf and assets/style.css to be on the classpath
        // You may need to add the 'resources' folder to your test resources.
        try {
            monitorUI.loadNodes();
        } catch (Exception e) {
            System.err.println("Failed to load UI nodes. Make sure test resources are set up.");
            e.printStackTrace();
        }

        Scene scene = new Scene(monitorUI, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("Should load UI in 'Not Connected' state")
    void testInitialState(FxRobot robot) {
        // Verify the 'Not Connected' pane is visible
        FxAssert.verifyThat(monitorUI.notConnectedPane, isVisible(), "Not Connected pane should be visible");
        
        // Verify the 'Gauges' pane is not visible
        FxAssert.verifyThat(monitorUI.gaugesPane, NodeMatchers.isInvisible(), "Gauges pane should not be visible");

        // Verify the text on the 'Not Connected' pane is correct
        FxAssert.verifyThat(monitorUI.notConnectedPaneHeadingLabel, LabeledMatchers.hasText("Listening for Connection"), "Heading label text mismatch");
    }

    @Test
    @DisplayName("Switching panes should make gauges visible")
    void testSwitchPane(FxRobot robot) {
        // Act
        // We must run UI manipulations on the JavaFX application thread
        robot.interact(() -> {
            monitorUI.switchPane(MonitorUI.pane.gauges);
        });

        // Assert
        FxAssert.verifyThat(monitorUI.gaugesPane, isVisible(), "Gauges pane should be visible");
        FxAssert.verifyThat(monitorUI.notConnectedPane, NodeMatchers.isInvisible(), "Not Connected pane should not be visible");
    }
}
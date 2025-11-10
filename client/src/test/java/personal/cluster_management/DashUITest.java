package personal.cluster_management;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for the Dash UI.
 * This test uses FxRobot to simulate user interactions (clicks, typing)
 * and verifies the UI state (e.g., dialogs appearing).
 * It uses a mock DashService to isolate the test from the file system and network.
 */
@ExtendWith(ApplicationExtension.class)
public class DashUITest {

    @Mock
    private DashService mockService;
    @Mock
    private Main mockMain;
    @Mock
    private Stage mockStage; // Mock stage for the controller, but TestFX provides the real one.

    private DashUI realView;
    private Dash dashController;

    private AutoCloseable openMocks;

    /**
     * TestFX's equivalent of @BeforeEach.
     * This method sets up the JavaFX scene for the robot to interact with.
     * @param stage The primary stage provided by TestFX.
     */
    @Start
    public void start(Stage stage) {
        openMocks = MockitoAnnotations.openMocks(this);

        // Mock the service to return a default empty config
        when(mockService.readConfig()).thenReturn(new HashMap<>());
        when(OSEnum.getOS()).thenReturn(OSEnum.WINDOWS);

        // We need to initialize the UI on the JavaFX thread
        // No need for CountDownLatch, @Start handles it.
        realView = new DashUI();
        realView.loadNodes();

        // Create the controller, injecting the real view and mock service
        dashController = new Dash(mockMain, stage, realView, mockService);

        // Set up the scene
        Scene scene = new Scene(dashController.getView(), 550, 800);
        stage.setScene(scene);
        stage.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        openMocks.close();
        Platform.runLater(() -> {
            Stage stage = (Stage) realView.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        });
    }

    @Test
    @TestFailsWithoutJavaFX
    void testSaveButton_ValidationFails_ShowsErrorAlert(FxRobot robot) {
        // Act: Click the "Save" button with empty fields
        robot.clickOn(LabeledMatchers.text("Save"));

        // Assert: Check that an error dialog (Alert) is shown
        Node dialogPane = robot.lookup(".dialog-pane").query();
        assertNotNull(dialogPane);
        assertTrue(dialogPane.isVisible());

        // We can even check the content
        DialogPane pane = (DialogPane) dialogPane;
        assertTrue(pane.getContentText().contains("Refresh Data Interval must be a number"));
        assertTrue(pane.getContentText().contains("Server IP cannot be empty!"));

        // Clean up: Click the "OK" button on the alert
        robot.clickOn(LabeledMatchers.text("OK"));
    }

    @Test
    @TestFailsWithoutJavaFX
    void testSaveButton_Success_CallsService(FxRobot robot) {
        // Arrange: Fill in all the fields with valid data
        // We can query by the label text, which is more robust than IDs
        // Note: FxRobot types very fast.
        robot.clickOn((TextField) robot.lookup("#dataRefreshIntervalTextField").query()).write("1500");
        robot.clickOn((TextField) robot.lookup("#serverIPAddressTextField").query()).write("127.0.0.1");
        robot.clickOn((TextField) robot.lookup("#serverPortTextField").query()).write("8080");
        robot.clickOn((TextField) robot.lookup("#CPULoadNameTextField").query()).write("CPU Load");
        robot.clickOn((TextField) robot.lookup("#GPULoadNameTextField").query()).write("GPU Load");
        robot.clickOn((TextField) robot.lookup("#CPUTemperatureNameTextField").query()).write("CPU Temp");
        robot.clickOn((TextField) robot.lookup("#GPUTemperatureNameTextField").query()).write("GPU Temp");
        robot.clickOn((TextField) robot.lookup("#CPUFanNameTextField").query()).write("CPU Fan");
        robot.clickOn((TextField) robot.lookup("#GPUFanNameTextField").query()).write("GPU Fan");
        robot.clickOn((TextField) robot.lookup("#TotalVRAMNameTextField").query()).write("VRAM Total");
        robot.clickOn((TextField) robot.lookup("#UsedVRAMNameTextField").query()).write("VRAM Used");
        robot.clickOn((TextField) robot.lookup("#UsedRAMNameTextField").query()).write("RAM Used");
        robot.clickOn((TextField) robot.lookup("#AvailableRAMNameTextField").query()).write("RAM Avail");

        // Act: Click the "Save" button
        robot.clickOn(LabeledMatchers.text("Save"));

        // Assert: Verify the service's saveConfig method was called
        verify(mockService).saveConfig(argThat(arr ->
                arr.length == 13 &&
                arr[0].equals("127.0.0.1") &&
                arr[2].equals("CPU Load") &&
                arr[12].equals("1500")
        ));

        // Assert: Check that an *information* dialog is shown
        Node dialogPane = robot.lookup(".dialog-pane").query();
        assertNotNull(dialogPane);
        DialogPane pane = (DialogPane) dialogPane;
        assertEquals("Configuration saved successfully.", pane.getContentText());

        // Clean up
        robot.clickOn(LabeledMatchers.text("OK"));
    }

    // A helper annotation for tests that might be flaky without a running FX environment
    @interface TestFailsWithoutJavaFX {}
}
package personal.cluster_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for the IO class.
 * This test does *not* test getShellOutput(), which is a native call.
 * We will mock getShellOutput() in other tests.
 */
class IOTest {

    private IO io;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        io = new IO();
    }

    @Test
    void testWriteAndReadFileRaw() throws Exception {
        // Arrange
        String testFile = "testConfig.txt";
        String testContent = "Hello::World::123";
        Path filePath = tempDir.resolve(testFile);

        // Act
        io.writeToFile(testContent, filePath.toString());

        // Assert
        assertTrue(Files.exists(filePath), "File should be created");
        String contentRead = io.readFileRaw(filePath.toString());
        assertEquals(testContent, contentRead, "Content read should match content written");
    }

    @Test
    void testReadFileArranged() throws Exception {
        // Arrange
        String testFile = "testConfigArranged.txt";
        String testContent = "192.168.1.1::8080::1000";
        Path filePath = tempDir.resolve(testFile);
        io.writeToFile(testContent, filePath.toString());

        // Act
        String[] arranged = io.readFileArranged(filePath.toString(), "::");

        // Assert
        assertNotNull(arranged);
        assertEquals(3, arranged.length, "Should be 3 elements");
        assertEquals("192.168.1.1", arranged[0]);
        assertEquals("8080", arranged[1]);
        assertEquals("1000", arranged[2]);
    }

    @Test
    void testReadFileRaw_FileNotFound() {
        // Arrange
        String nonExistentFile = tempDir.resolve("non_existent_file.txt").toString();

        // Act & Assert
        assertThrows(java.io.FileNotFoundException.class, () -> {
            io.readFileRaw(nonExistentFile);
        }, "Reading a non-existent file should throw FileNotFoundException");
    }
}
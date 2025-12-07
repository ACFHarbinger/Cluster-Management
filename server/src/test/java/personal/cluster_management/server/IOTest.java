package personal.cluster_management.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IOTest {

    private IO io;
    
    // JUnit will create a temporary directory for each test
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
        String testContent = "1920::1080::8080";
        Path filePath = tempDir.resolve(testFile);
        io.writeToFile(testContent, filePath.toString());

        // Act
        String[] arranged = io.readFileArranged(filePath.toString(), "::");

        // Assert
        assertNotNull(arranged);
        assertEquals(3, arranged.length, "Should be 3 elements");
        assertEquals("1920", arranged[0]);
        assertEquals("1080", arranged[1]);
        assertEquals("8080", arranged[2]);
    }
    
    @Test
    void testReadFileRawFileNotFound() {
        // Arrange
        String nonExistentFile = tempDir.resolve("non_existent_file.txt").toString();

        // Act & Assert
        assertThrows(java.io.FileNotFoundException.class, () -> {
            io.readFileRaw(nonExistentFile);
        }, "Reading a non-existent file should throw FileNotFoundException");
    }
}
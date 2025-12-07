package personal.cluster_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditConfigTest {

    @Test
    @DisplayName("checkNo should return true for valid numbers")
    void checkNoValidNumber() {
        assertTrue(EditConfig.checkNo("123"), "Positive integer");
        assertTrue(EditConfig.checkNo("0"), "Zero");
        assertTrue(EditConfig.checkNo("-45"), "Negative integer");
    }

    @Test
    @DisplayName("checkNo should return false for invalid numbers")
    void checkNoInvalidNumber() {
        assertFalse(EditConfig.checkNo("abc"), "Plain string");
        assertFalse(EditConfig.checkNo("12.34"), "Decimal number");
        assertFalse(EditConfig.checkNo("1 2 3"), "Number with spaces");
        assertFalse(EditConfig.checkNo(""), "Empty string");
    }

    @Test
    @DisplayName("checkNo should throw exception for null input")
    void checkNoNullInput() {
        // The implementation uses Integer.parseInt, which throws NumberFormatException for null
        assertThrows(NumberFormatException.class, () -> {
            EditConfig.checkNo(null);
        }, "Null input should throw NumberFormatException");
    }
}
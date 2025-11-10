package personal.cluster_management;

/**
 * Represents the operating system.
 * Moved to its own file to be easily shared between interfaces and classes.
 */
public enum OS {
    WINDOWS,
    LINUX;

    /**
     * Detects the current operating system.
     * @return The detected OS (currently only supports windows).
     */
    public final static OS getOS() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return OS.WINDOWS;
        }
        // Other OS logic could be added here
        return OS.WINDOWS; // Default
    }
}
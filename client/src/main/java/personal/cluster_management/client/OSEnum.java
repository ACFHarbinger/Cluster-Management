package personal.cluster_management.client;

/**
 * Represents the operating system.
 * Moved to its own file to be easily shared between interfaces and classes.
 */
public enum OSEnum {
    WINDOWS,
    LINUX;

    /**
     * Detects the current operating system.
     * @return The detected OS (currently only supports windows).
     */
    public final static OSEnum getOS() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            return OSEnum.WINDOWS;
        }
        // Other OS logic could be added here
        return OSEnum.WINDOWS; // Default
    }
}
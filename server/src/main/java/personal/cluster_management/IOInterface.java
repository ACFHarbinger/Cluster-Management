package personal.cluster_management;

/**
 * Interface for IO operations used by the Monitor and EditConfig utility.
 * This abstracts file and console I/O, allowing for
 * a mock implementation during testing.
 */
public interface IOInterface {

    /**
     * Prints a line to the standard system console.
     * @param txt The text to print.
     */
    void pln(String txt);

    /**
     * Reads the first line from a file.
     * @param fileLocation The path to the file.
     * @return The first line of the file.
     * @throws Exception If an I/O error occurs.
     */
    String readFileRaw(String fileLocation) throws Exception;

    /**
     * Writes content to a file, overwriting it.
     * @param content The string content to write.
     * @param fileName The path to the file.
     */
    void writeToFile(String content, String fileName);

    /**
     * Reads a single line from the standard system console.
     * @return The line of text from the user.
     * @throws Exception If an I/O error occurs.
     */
    String readConsoleLine() throws Exception;

    /**
     * Reads the first line of a file and splits it by a separator.
     * @param fileLocation The path to the file.
     * @param sep The separator string.
     * @return A String array of the split content.
     * @throws Exception If an I/O error occurs.
     */
    String[] readFileArranged(String fileLocation, String sep) throws Exception;

    /**
     * Executes a shell command and returns its output.
     * @param cmd The command to execute.
     * @return The standard output from the command.
     * @throws Exception If the command fails or an error occurs.
     */
    String getShellOutput(String cmd) throws Exception;
}
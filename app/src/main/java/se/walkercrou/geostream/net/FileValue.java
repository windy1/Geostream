package se.walkercrou.geostream.net;

/**
 * Represents a POST parameter for a file.
 */
public class FileValue {
    private final String fileName;
    private final byte[] data;

    public FileValue(String fileName, byte[] data) {
        this.fileName = fileName;
        this.data = data;
    }

    /**
     * Returns the name of the file.
     *
     * @return name of file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns this file's data.
     *
     * @return file data
     */
    public byte[] getData() {
        return data;
    }
}

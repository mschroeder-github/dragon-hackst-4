package net.markus.projects.dq4h.util;

/**
 *
 * @author Markus Schr&ouml;der
 */
public class MemoryUtility {
    
    //from https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static String humanReadableByteCount(long bytes) {
        return humanReadableByteCount(bytes, true);
    }

    public static String memoryStatistics() {
        Runtime instance = Runtime.getRuntime();

        StringBuilder sb = new StringBuilder();

        long t = instance.totalMemory();
        long f = instance.freeMemory();
        long u = t - f;
        long m = instance.maxMemory();

        sb
                .append(humanReadableByteCount(u)).append(" used / ")
                .append(humanReadableByteCount(t)).append(" total / ")
                .append(humanReadableByteCount(m)).append(" max");

        return sb.toString();
    }
    
}

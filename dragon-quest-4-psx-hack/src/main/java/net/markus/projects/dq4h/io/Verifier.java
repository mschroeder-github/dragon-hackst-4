package net.markus.projects.dq4h.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.markus.projects.dq4h.compare.ComparatorReport;
import net.markus.projects.dq4h.compare.ComparatorReportEntry;
import net.markus.projects.dq4h.compare.DragonQuestComparator;

/**
 * Verifies data.
 */
public class Verifier {

    /**
     * Checks if all bytes are zero.
     *
     * @param array
     * @return
     */
    public static boolean allZero(byte[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * -1 means it is equal, an index >= 0 means the bytes are different there, -2 means length is different.
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return -2;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> compareFiles(File a, File b) throws IOException {

        List<Integer> unequals = new ArrayList<>();

        InputStream A = new FileInputStream(a);
        InputStream B = new FileInputStream(b);

        byte[] bufferA = new byte[4048];
        byte[] bufferB = new byte[4048];

        int aa;
        int bb;
        for (int i = 0; i < a.length(); i++) {
            aa = A.read(bufferA);
            bb = B.read(bufferB);

            for (int j = 0; j < bufferA.length; j++) {
                if (bufferA[j] != bufferB[j]) {
                    unequals.add(i);
                }
            }
        }

        return unequals;
    }

    /**
     * Compares two files byte wise. 
     * @param a
     * @param b
     * @return list of indices where bytes are unequal.
     * @throws IOException
     * @see <a href="https://www.baeldung.com/java-compare-files">Compare the Content of Two Files in Java</a>
     */
    public static List<Long> filesCompareByByte(File a, File b) throws IOException {
        List<Long> unequals = new ArrayList<>();
        
        try ( BufferedInputStream fis1 = new BufferedInputStream(new FileInputStream(a));  BufferedInputStream fis2 = new BufferedInputStream(new FileInputStream(b))) {

            int ch = 0;
            long pos = 1;
            while ((ch = fis1.read()) != -1) {
                if (ch != fis2.read()) {
                    unequals.add(pos);
                }
                pos++;
            }
            if (fis2.read() == -1) {
                
            } else {
                unequals.add(pos);
            }
        }
        
        return unequals;
    }

    /**
     * Compares two bytes arrays and returns a {@link ComparatorReportEntry} if they differ in length or content.
     *
     * @param <T>
     * @param expectedObject
     * @param expectedBytes
     * @param actualObject
     * @param actualBytes
     * @param attributeName
     * @param report
     */
    public static <T> void compareBytes(T expectedObject, byte[] expectedBytes, T actualObject, byte[] actualBytes, String attributeName, ComparatorReport report) {
        report.increaseCheckCounter();
        if (expectedBytes.length != actualBytes.length) {
            ComparatorReportEntry entry = new ComparatorReportEntry<>(expectedObject, expectedBytes.length, actualObject, actualBytes.length, attributeName + ": Byte array length differ");
            report.add(entry);
            return;
        }

        for (int i = 0; i < expectedBytes.length; i++) {
            if (expectedBytes[i] != actualBytes[i]) {
                ComparatorReportEntry entry = new ComparatorReportEntry<>(expectedObject, expectedBytes[i], actualObject, actualBytes[i], "Byte differs at index " + i);
                report.add(entry);
                return;
            }
        }
    }

    /**
     * Compares two lists in size and their items if the items implement {@link DragonQuestComparator}.
     *
     * @param <T>
     * @param expectedObject
     * @param expectedList
     * @param actualObject
     * @param actualList
     * @param attributeName
     * @param report
     */
    public static <T> void compareLists(T expectedObject, List expectedList, T actualObject, List actualList, String attributeName, ComparatorReport report) {
        report.increaseCheckCounter();
        if (expectedList.size() != actualList.size()) {
            ComparatorReportEntry entry = new ComparatorReportEntry<>(expectedObject, expectedList.size(), actualObject, actualList.size(), attributeName + ": List size differ");
            report.add(entry);
            return;
        }

        for (int i = 0; i < expectedList.size(); i++) {
            Object expected = expectedList.get(i);
            Object actual = actualList.get(i);

            if (expected instanceof DragonQuestComparator && actual.getClass().equals(expected.getClass())) {
                DragonQuestComparator expComp = (DragonQuestComparator) expected;
                expComp.compare(actual, report);
            }
        }
    }

    /**
     * Checks if two literals are equal and if not reports it.
     *
     * @param <T>
     * @param expectedObject
     * @param expectedLiteral
     * @param actualObject
     * @param actualLiteral
     * @param attributeName
     * @param report
     */
    public static <T> void compareNumbers(T expectedObject, Number expectedLiteral, T actualObject, Number actualLiteral, String attributeName, ComparatorReport report) {
        report.increaseCheckCounter();
        int cmp = new BigDecimal(expectedLiteral.toString()).compareTo(new BigDecimal(actualLiteral.toString()));
        if (cmp != 0) {
            ComparatorReportEntry entry = new ComparatorReportEntry<>(expectedObject, expectedLiteral, actualObject, actualLiteral, attributeName + " is " + (cmp < 0 ? "<" : ">"));
            report.add(entry);
        }
    }

    /**
     * Checks if two objects are equal and if not reports it.
     *
     * @param <T>
     * @param expectedObject
     * @param expectedLiteral
     * @param actualObject
     * @param actualLiteral
     * @param attributeName
     * @param report
     */
    public static <T> void compareObjects(T expectedObject, Object expectedLiteral, T actualObject, Object actualLiteral, String attributeName, ComparatorReport report) {
        report.increaseCheckCounter();
        if (!expectedLiteral.equals(actualLiteral)) {
            ComparatorReportEntry entry = new ComparatorReportEntry<>(expectedObject, expectedLiteral, actualObject, actualLiteral, attributeName + " is not equal");
            report.add(entry);
        }
    }

}

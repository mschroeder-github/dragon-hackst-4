package net.markus.projects.ff7.lzs;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

public class FF7LZSInputStream extends InputStream {

    /**
     * true for System.out.println debug information
     */
    private final boolean DEBUG = true;

    private InputStream stream;

    private Unsigned size;
    private LinkedList<Integer> queue;

    private final int BUFFER_SIZE = 4096;
    private int[] buffer;
    private int bufferPos;

    private int writtenBytes = 0;

    /**
     * Consturcts a decoder for lzs
     *
     * @param stream a stream of (ff7 psx) lzs compressed data
     */
    public FF7LZSInputStream(InputStream stream) throws IOException {
        this.stream = stream;
        this.queue = new LinkedList<Integer>();
        this.buffer = new int[BUFFER_SIZE];
        this.bufferPos = 0;
        read(stream);
    }

    /**
     * Reads out the LZS data.
     */
    private void read(InputStream stream) throws IOException {
        //size = new Unsigned(Type.INT, readInt(stream));

        //run decode until stream is empty
        while (stream.available() > 0) {

            //read control byte
            Unsigned controlByte = new Unsigned(Type.BYTE, stream.read());

            if (DEBUG) {
                System.out.println("controlbyte=" + controlByte);
            }

            //for each bit in control byte
            for (int i = 0; i < controlByte.getBits().length; i++) {

                //true=1 -> literal (means direct read from stream)
                if (controlByte.getBits()[i]) {
                    write(stream.read());
                } else {
                    //false=0 reference (means we must look to the buffer)
                    Unsigned ref = new Unsigned(Type.SHORT, readShort(stream));

                    //offset calculation
                    Unsigned offsetPart1 = ref.extract(0, 7);
                    Unsigned offsetPart2 = ref.extract(12, 15);
                    boolean[] offsetBits = new boolean[12];//offset is 12 bits long
                    //first 8 bits of part1 
                    for (int j = 0; j < 8; j++) {
                        offsetBits[j] = offsetPart1.getBits()[j];
                    }
                    //first 4 bits of part2 
                    for (int j = 0; j < 4; j++) {
                        offsetBits[8 + j] = offsetPart2.getBits()[j];
                    }
                    //offset = offsetPart1 offsetPart2 (bits together)
                    Unsigned offset = new Unsigned(Type.SHORT, offsetBits);
                    //additional calculating to real offset
                    offset.setValue((offset.getValue() + 18) % BUFFER_SIZE);

                    //length calculation
                    Unsigned length = ref.extract(8, 11).convert(Type.BYTE);
                    length.setValue(length.getValue() + 3);

                    if (DEBUG) {
                        System.out.println("REF: offset=" + offset + ", length=" + length);
                    }

                    //TYPE {Looper, Normal}
                    //LOOPER: if it will read over the limit of the buffer it loops that what it reads
                    if (offset.getValue() + length.getValue() > writtenBytes % BUFFER_SIZE
                            && offset.getValue() < writtenBytes % BUFFER_SIZE) {

                        int available = (writtenBytes % BUFFER_SIZE) - (int) offset.getValue();
                        int over = (int) offset.getValue() + (int) length.getValue() - (writtenBytes % BUFFER_SIZE);
                        int repeat = over + available;

                        if (DEBUG) {
                            System.out.println("\tLOOPER: [available=" + available + ",over=" + over + ",repeat=" + repeat + "]");
                        }

                        //read buffer first out
                        int[] tmpBuffer = new int[available];
                        for (int j = 0; j < tmpBuffer.length; j++) {
                            tmpBuffer[j] = buffer[((int) offset.getValue() + j) % BUFFER_SIZE];
                        }

                        //repeated write
                        int k = 0;
                        for (int j = 0; j < repeat; j++) {
                            if (DEBUG) {
                                System.out.println("repeatwrite: " + j + "," + k);
                            }
                            write(tmpBuffer[k]);
                            k++;
                            if (k == available) {
                                k = 0;
                            }
                        }

                    } else {
                        //NORMAL: jump to offset in buffer and read length bytes from
                        //write length bytes from buffer to queue and into buffer

                        if (DEBUG) {
                            System.out.println("\tNORMAL");
                        }

                        //read buffer first out
                        int[] tmpBuffer = new int[(int) length.getValue()];
                        for (int j = 0; j < tmpBuffer.length; j++) {
                            tmpBuffer[j] = buffer[((int) offset.getValue() + j) % BUFFER_SIZE];
                        }

                        //write
                        for (int j = 0; j < length.getValue(); j++) {
                            write(tmpBuffer[j]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Reads the next byte of data from decoded data.
     */
    public int read() throws IOException {
        return queue.removeLast();
    }

    //MAIN WRITE METHOD
    /**
     * Writes a byte to all nessesarys: queue, buffer
     */
    private void write(int value) {
        queue.addFirst(value);
        writeToBuffer(value);

        if (DEBUG) {
            try {
                System.out.println("LIT: " + value + " | written=" + writtenBytes + "/" + stream.available());
            } catch (IOException e) {
            }
        }

        writtenBytes++;
    }

    //BUFFER METHODS
    /**
     * Write a byte to the buffer
     */
    private void writeToBuffer(int value) {
        buffer[bufferPos] = value;
        bufferPos++;
        if (bufferPos >= BUFFER_SIZE) {
            bufferPos = 0;
        }
    }

    //ATTRIBUT GET METHODS
    /**
     * @return the size of the undecoded lzs file
     */
    public long getSize() {
        return size.getValue();
    }

    //CALCULATED GET METHODS
    /**
     * @return a string representation
     */
    public String toString() {
        return "size=" + getSize() + "";
    }

    //STREAM READ METHODS
    /**
     * Reads from stream a Short as byte array
     */
    private int[] readShort(InputStream stream) throws IOException {
        return new int[]{stream.read(), stream.read()};
    }

    /**
     * Reads from stream a Integer as byte array
     */
    private int[] readInt(InputStream stream) throws IOException {
        return new int[]{stream.read(), stream.read(), stream.read(), stream.read()};
    }

    //OVERRIDES
    public int available() throws IOException {
        return queue.size();
    }

    public boolean markSupported() {
        return false;
    }
    //public void mark(int readlimit) { }
    //public void reset() throws IOException { }

    //DELEGATES
    public void close() throws IOException {
        stream.close();
    }
}

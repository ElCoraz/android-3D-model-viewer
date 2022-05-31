package org.andresoviedo.android_3d_model_engine.services.stl;

import java.io.IOException;
import java.io.InputStream;
/**************************************************************************************************/
public class LittleEndianConverter {
    /**********************************************************************************************/
    static public int convertToBigEndian(final byte[] srcBuffer, final short[] destBuffer, final int srcLength, final int destOffset, final int destLength) {
        return convertToBigEndian(srcBuffer, destBuffer, srcLength, destOffset, destLength, (short) 0xFF);
    }

    /**********************************************************************************************/
    static public int convertToBigEndian(final byte[] srcBuffer, final short[] destBuffer, final int srcLength, final int destOffset, final int destLength, final short mask) {
        final int length = Math.min(destLength * 2, (srcLength / 2) * 2);
        for (int i = 0; i < length; i += 2) {
            final int tmp = (srcBuffer[i] & 0xFF | (srcBuffer[i + 1] << 8)) & mask;
            destBuffer[(i / 2) + destOffset] = (short) tmp;
        }
        return length;
    }

    /**********************************************************************************************/
    public static int convertToBigEndian(final byte[] srcBuffer, final int[] destBuffer, final int srcLength, final int destOffset, final int destLength) {
        return convertToBigEndian(srcBuffer, destBuffer, srcLength, destOffset, destLength, 0xFFFFFFFF);
    }

    /**********************************************************************************************/
    public static int convertToBigEndian(final byte[] srcBuffer, final int[] destBuffer, final int srcLength, final int destOffset, final int destLength, final int mask) {
        final int length = Math.min(destLength * 4, (srcLength / 4) * 4);
        for (int i = 0; i < length; i += 4) {
            destBuffer[(i / 4) + destOffset] = (srcBuffer[i] & 0xFF | (srcBuffer[i + 1] << 8) & 0xFF00 | (srcBuffer[i + 2] << 16) & 0xFF0000 | (srcBuffer[i + 3] << 24)) & mask;
        }
        return length;
    }

    /**********************************************************************************************/
    public static int convertToBigEndian(final int blockSize, final byte[] srcBuffer, final int[] destBuffer, final int srcLength, final int destOffset, final int destLength) {
        return convertToBigEndian(blockSize, srcBuffer, destBuffer, srcLength, destOffset, destLength, 0xFFFFFFFF);
    }

    /**********************************************************************************************/
    public static int convertToBigEndian(final int blockSize, final byte[] srcBuffer, final int[] destBuffer, final int srcLength, final int destOffset, final int destLength, final int mask) {
        final int length = Math.min(destLength * blockSize, (srcLength / blockSize) * blockSize);
        if (blockSize == 2) {
            for (int i = 0; i < length; i += 2) {
                destBuffer[(i / 2) + destOffset] = (srcBuffer[i] & 0xFF | (srcBuffer[i + 1] << 8)) & mask;
            }
            return length;
        } else if (blockSize == 3) {
            for (int i = 0; i < length; i += 3) {
                destBuffer[(i / 3) + destOffset] = (srcBuffer[i] & 0xFF | (srcBuffer[i + 1] << 8) & 0xFF00 | (srcBuffer[i + 2] << 24)) & mask;
            }
            return length;
        } else if (blockSize == 4) {
            return convertToBigEndian(srcBuffer, destBuffer, srcLength, destOffset, destLength, mask);
        } else {
            return 0;
        }
    }

    /**********************************************************************************************/
    public static int read(final byte[] readBuffer, final short[] destBuffer, final int destOffset, final int destLength, final InputStream stream) throws IOException {
        return read(readBuffer, destBuffer, destOffset, destLength, stream, (short) 0xFF);
    }

    /**********************************************************************************************/
    public static int read(final byte[] readBuffer, final short[] destBuffer, final int destOffset, final int destLength, final InputStream stream, final short mask) throws IOException {
        int numOfBytesRead = 0;
        int numOfData = 0;
        int offset = 0;
        final int length = (readBuffer.length / 2) * 2;
        while ((numOfBytesRead >= 0) && (numOfData < destLength)) {
            final int maxBytesToRead = Math.min((destLength - numOfData) * 2, length);
            numOfBytesRead = stream.read(readBuffer, offset, maxBytesToRead - offset);
            int numOfProcessedBytes = convertToBigEndian(readBuffer, destBuffer, numOfBytesRead + offset, destOffset + numOfData, destLength - numOfData, mask);
            if (numOfBytesRead - numOfProcessedBytes == 1) {
                offset = 1;
                readBuffer[0] = readBuffer[numOfProcessedBytes];
            } else {
                offset = 0;
            }
            numOfData += (numOfProcessedBytes / 2);
        }
        return numOfData;
    }

    /**********************************************************************************************/
    public static int read(final byte[] readBuffer, final int[] destBuffer, final int destOffset, final int destLength, final InputStream stream) throws IOException {
        return read(readBuffer, destBuffer, destOffset, destLength, stream, 0xFFFFFFFF);
    }

    /**********************************************************************************************/
    public static int read(final byte[] readBuffer, final int[] destBuffer, final int destOffset, final int destLength, final InputStream stream, final int mask) throws IOException {
        int numOfBytesRead = 0;
        int numOfData = 0;
        int offset = 0;
        final int length = (readBuffer.length / 4) * 4;
        while ((numOfBytesRead >= 0) && (numOfData < destLength)) {
            final int maxBytesToRead = Math.min((destLength - numOfData) * 4, length);
            numOfBytesRead = stream.read(readBuffer, offset, maxBytesToRead - offset);
            int numOfProcessedBytes = convertToBigEndian(readBuffer, destBuffer, numOfBytesRead + offset, destOffset + numOfData, destLength - numOfData, mask);
            final int diff = numOfBytesRead - numOfProcessedBytes;

            offset = 0;
            if (diff == 1) {
                offset = 1;
                readBuffer[0] = readBuffer[numOfProcessedBytes];
            }
            if (diff == 2) {
                offset = 2;
                readBuffer[1] = readBuffer[numOfProcessedBytes + 1];
            }
            if (diff == 3) {
                offset = 3;
                readBuffer[2] = readBuffer[numOfProcessedBytes + 2];
            }
            numOfData += (numOfProcessedBytes / 4);
        }
        return numOfData;
    }

    /**********************************************************************************************/
    public static int read(final int blockSize, final byte[] readBuffer, final int[] destBuffer, final int destOffset, final int destLength, final InputStream stream) throws IOException {
        return read(blockSize, readBuffer, destBuffer, destOffset, destLength, stream, 0xFFFFFFFF);
    }

    /**********************************************************************************************/
    public static int read(final int blockSize, final byte[] readBuffer, final int[] destBuffer, final int destOffset, final int destLength, final InputStream stream, final int mask) throws IOException {
        if (blockSize == 2) {
            return read2ByteBlock(readBuffer, destBuffer, destOffset, destLength, stream, mask);
        } else if (blockSize == 3) {
            return read3ByteBlock(readBuffer, destBuffer, destOffset, destLength, stream, mask);
        } else if (blockSize == 4) {
            return read(readBuffer, destBuffer, destOffset, destLength, stream, mask);
        } else {
            return 0;
        }
    }

    /**********************************************************************************************/
    public static int read4ByteBlock(final InputStream stream) throws java.io.IOException {
        return read(stream) & 0xFF | (read(stream) << 8) & 0xFF00 | (read(stream) << 16) & 0xFF0000 | (read(stream) << 24);
    }

    /**********************************************************************************************/
    public static int read2ByteBlock(final InputStream stream) throws java.io.IOException {
        return read(stream) & 0xFF | (read(stream) << 8);
    }

    /**********************************************************************************************/
    public static int read3ByteBlock(final InputStream stream) throws java.io.IOException {
        return read(stream) & 0xFF | (read(stream) << 8) & 0xFF00 | (read(stream) << 16);
    }

    /**********************************************************************************************/
    private static int read2ByteBlock(final byte[] readBuffer, final int[] destBuffer, final int destOffset, final int destLength, final InputStream stream, final int mask) throws IOException {
        int numOfBytesRead = 0;
        int numOfData = 0;
        int offset = 0;
        final int length = (readBuffer.length / 2) * 2;
        while ((numOfBytesRead >= 0) && (numOfData < destLength)) {
            final int maxBytesToRead = Math.max((destLength - numOfData) * 2, length);
            numOfBytesRead = stream.read(readBuffer, offset, maxBytesToRead - offset);
            int numOfProcessedBytes = convertToBigEndian(2, readBuffer, destBuffer, numOfBytesRead + offset, destOffset + numOfData, destLength - numOfData, mask);
            if (numOfBytesRead - numOfProcessedBytes == 1) {
                offset = 1;
                readBuffer[0] = readBuffer[numOfProcessedBytes];
            } else {
                offset = 0;
            }
            numOfData += (numOfProcessedBytes / 2);
        }
        return numOfData;
    }

    /**********************************************************************************************/
    private static int read3ByteBlock(final byte[] readBuffer, final int[] destBuffer, final int destOffset, final int destLength, final InputStream stream, final int mask) throws IOException {
        int numOfBytesRead = 0;
        int numOfData = 0;
        int offset = 0;
        final int length = (readBuffer.length / 3) * 3;
        while ((numOfBytesRead >= 0) && (numOfData < destLength)) {
            final int maxBytesToRead = Math.max((destLength - numOfData) * 3, length);
            numOfBytesRead = stream.read(readBuffer, offset, maxBytesToRead - offset);
            int numOfProcessedBytes = convertToBigEndian(3, readBuffer, destBuffer, numOfBytesRead + offset, destOffset + numOfData, destLength - numOfData, mask);
            final int diff = numOfBytesRead - numOfProcessedBytes;

            offset = 0;

            if (diff == 1) {
                offset = 1;
                readBuffer[0] = readBuffer[numOfProcessedBytes];
            }
            if (diff == 2) {
                offset = 2;
                readBuffer[1] = readBuffer[numOfProcessedBytes + 1];
            }
            numOfData += (numOfProcessedBytes / 3);
        }
        return numOfData;
    }

    /**********************************************************************************************/
    private static int read(final InputStream stream) throws IOException {
        final int tempValue = stream.read();

        if (tempValue == -1) {
            throw new IOException("Filesize does not match blocksize");
        }
        return tempValue;
    }
}

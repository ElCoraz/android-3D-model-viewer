package org.andresoviedo.android_3d_model_engine.services.stl;

import org.andresoviedo.util.io.ProgressMonitorInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
/**************************************************************************************************/
class STLBinaryParser extends STLParser {
    /**********************************************************************************************/
    private static int HEADER_SIZE = 84;
    /**********************************************************************************************/
    private static int RECORD_SIZE = 50;
    /**********************************************************************************************/
    private static int COMMENT_SIZE = 80;
    /**********************************************************************************************/
    private BufferedInputStream itsStream;
    /**********************************************************************************************/
    private byte[] itsReadBuffer;
    /**********************************************************************************************/
    private int[] itsDataBuffer;

    /**********************************************************************************************/
    public STLBinaryParser() {
        itsReadBuffer = new byte[48];
        itsDataBuffer = new int[12];
    }

    /**********************************************************************************************/
    public STLBinaryParser(boolean strict) {
        super(strict);

        itsReadBuffer = new byte[48];
        itsDataBuffer = new int[12];
    }

    /**********************************************************************************************/
    public void close() throws IOException {
        if (itsStream != null) {
            itsStream.close();
        }
    }

    /**********************************************************************************************/
    public boolean parse(URL url) throws IllegalArgumentException, IOException {
        InputStream stream = null;
        int length = -1;
        try {
            URLConnection connection = url.openConnection();
            stream = connection.getInputStream();
            length = connection.getContentLength();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
        }

        itsStream = new BufferedInputStream(stream);

        return parse(length);
    }

    /**********************************************************************************************/
    public boolean parse(URL url, Component parentComponent) throws IllegalArgumentException, IOException {
        InputStream stream = null;
        int length = -1;
        try {
            URLConnection connection = url.openConnection();
            stream = connection.getInputStream();
            length = connection.getContentLength();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
        }
        stream = new ProgressMonitorInputStream(parentComponent, "parsing " + url.toString(), stream);

        itsStream = new BufferedInputStream(stream);

        return parse(length);
    }

    /**********************************************************************************************/
    private boolean parse(int length) throws IllegalArgumentException, IOException {
        try {
            for (int i = 0; i < COMMENT_SIZE; i++) {
                itsStream.read();
            }

            itsNumOfObjects = 1;
            itsNumOfFacets = new int[]{LittleEndianConverter.read4ByteBlock(itsStream)};
            itsNames = new String[1];

            if (strictParsing && length != -1 && length != itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) {
                String msg = "File size does not match the expected size for" +
                        " the given number of facets. Given " +
                        itsNumOfFacets[0] + " facets for a total size of " +
                        (itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) +
                        " but the file size is " + length;
                close();

                throw new IllegalArgumentException(msg);
            } else if (!strictParsing && length != -1 && length != itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) {
                String msg = "File size does not match the expected size for" +
                        " the given number of facets. Given " +
                        itsNumOfFacets[0] + " facets for a total size of " +
                        (itsNumOfFacets[0] * RECORD_SIZE + HEADER_SIZE) +
                        " but the file size is " + length;

                if (parsingMessages == null) {
                    parsingMessages = new ArrayList<String>();
                }
                parsingMessages.add(msg);
            }
        } catch (IOException e) {
            close();
            throw e;
        }
        return false;
    }

    /**********************************************************************************************/
    public boolean getNextFacet(double[] normal, double[][] vertices) throws IOException {
        LittleEndianConverter.read(itsReadBuffer, itsDataBuffer, 0, 12, itsStream);

        boolean nan_found = false;

        for (int i = 0; i < 3; i++) {
            normal[i] = Float.intBitsToFloat(itsDataBuffer[i]);
            if (Double.isNaN(normal[i]) || Double.isInfinite(normal[i])) {
                nan_found = true;
            }
        }

        if (nan_found) {
            normal[0] = 0;
            normal[1] = 0;
            normal[2] = 0;
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                vertices[i][j] = Float.intBitsToFloat(itsDataBuffer[i * 3 + j + 3]);
            }
        }

        itsStream.read();
        itsStream.read();

        return true;
    }
}

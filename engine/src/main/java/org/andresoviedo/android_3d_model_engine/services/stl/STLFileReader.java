package org.andresoviedo.android_3d_model_engine.services.stl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
/**************************************************************************************************/
public class STLFileReader {
    /**********************************************************************************************/
    private STLParser itsParser;

    /**********************************************************************************************/
    public STLFileReader(File file) throws IllegalArgumentException, IOException {
        this(file.toURL());
    }

    /**********************************************************************************************/
    public STLFileReader(String fileName) throws IllegalArgumentException, IOException {
        this(new URL(fileName));
    }

    /**********************************************************************************************/
    public STLFileReader(String fileName, boolean strict) throws IllegalArgumentException, IOException {
        this(new URL(fileName), strict);
    }

    /**********************************************************************************************/
    public STLFileReader(URL url) throws IllegalArgumentException, IOException {
        final STLASCIIParser asciiParser = new STLASCIIParser();

        if (asciiParser.parse(url)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser();
            binParser.parse(url);
            itsParser = binParser;
        }
    }

    /**********************************************************************************************/
    public STLFileReader(URL url, boolean strict) throws IllegalArgumentException, IOException {
        final STLParser asciiParser = new STLASCIIParser(strict);

        if (asciiParser.parse(url)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser(strict);
            binParser.parse(url);
            itsParser = binParser;
        }
    }

    /**********************************************************************************************/
    public STLFileReader(URL url, Component parentComponent) throws IllegalArgumentException, IOException {
        final STLASCIIParser asciiParser = new STLASCIIParser();

        if (asciiParser.parse(url, parentComponent)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser();
            binParser.parse(url, parentComponent);
            itsParser = binParser;
        }
    }

    /**********************************************************************************************/
    public STLFileReader(URL url, Component parentComponent, boolean strict) throws IllegalArgumentException, IOException {
        final STLASCIIParser asciiParser = new STLASCIIParser(strict);

        if (asciiParser.parse(url, parentComponent)) {
            itsParser = asciiParser;
        } else {
            final STLBinaryParser binParser = new STLBinaryParser(strict);
            binParser.parse(url, parentComponent);
            itsParser = binParser;
        }
    }

    /**********************************************************************************************/
    public STLFileReader(File file, Component parentComponent) throws IllegalArgumentException, IOException {
        this(file.toURL(), parentComponent);
    }

    /**********************************************************************************************/
    public STLFileReader(File file, Component parentComponent, boolean strict) throws IllegalArgumentException, IOException {
        this(file.toURL(), parentComponent, strict);
    }

    /**********************************************************************************************/
    public STLFileReader(String fileName, Component parentComponent) throws IllegalArgumentException, IOException {
        this(new URL(fileName), parentComponent);
    }

    /**********************************************************************************************/
    public STLFileReader(String fileName, Component parentComponent, boolean strict) throws IllegalArgumentException, IOException {
        this(new URL(fileName), parentComponent, strict);
    }

    /**********************************************************************************************/
    public boolean getNextFacet(double[] normal, double[][] vertices) throws IllegalArgumentException, IOException {
        return itsParser.getNextFacet(normal, vertices);
    }

    /**********************************************************************************************/
    public String[] getObjectNames() {
        return itsParser.getObjectNames();
    }

    /**********************************************************************************************/
    public int[] getNumOfFacets() {
        return itsParser.getNumOfFacets();
    }

    /**********************************************************************************************/
    public List<String> getParsingMessages() {
        return itsParser.getParsingMessages();
    }

    /**********************************************************************************************/
    public int getNumOfObjects() {
        return itsParser.getNumOfObjects();
    }

    /**********************************************************************************************/
    public void close() throws IOException {
        if (itsParser != null) {
            itsParser.close();
        }
    }
}

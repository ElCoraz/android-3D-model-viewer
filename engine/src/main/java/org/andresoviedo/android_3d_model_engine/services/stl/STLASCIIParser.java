package org.andresoviedo.android_3d_model_engine.services.stl;

import org.andresoviedo.util.io.ProgressMonitorInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
/**************************************************************************************************/
class STLASCIIParser extends STLParser {
    /**********************************************************************************************/
    private static final String UNKNOWN_KEYWORD_MSG_PROP = "org.j3d.loaders.stl.STLASCIIParser.invalidKeywordMsg";
    /**********************************************************************************************/
    private static final String EMPTY_FILE_MSG_PROP = "org.j3d.loaders.stl.STLASCIIParser.emptyFileMsg";
    /**********************************************************************************************/
    private static final String INVALID_NORMAL_DATA_MSG_PROP = "org.j3d.loaders.stl.STLASCIIParser.invalidNormalDataMsg";
    /**********************************************************************************************/
    private static final String INVALID_VERTEX_DATA_MSG_PROP = "org.j3d.loaders.stl.STLASCIIParser.invalidVertexDataMsg";
    /**********************************************************************************************/
    private static final String EOF_WTF_MSG_PROP = "org.j3d.loaders.stl.STLASCIIParser.unexpectedEofMsg";
    /**********************************************************************************************/
    private BufferedReader itsReader;
    /**********************************************************************************************/
    private int lineCount;

    /**********************************************************************************************/
    public STLASCIIParser() {
    }

    /**********************************************************************************************/
    public STLASCIIParser(boolean strict) {
        super(strict);
    }

    /**********************************************************************************************/
    public void close() throws IOException {
        if (itsReader != null)
            itsReader.close();
    }

    /**********************************************************************************************/
    public boolean getNextFacet(double[] normal, double[][] vertices) throws IOException {
        String input_line = readLine();

        if (input_line == null) {
            return false;
        }

        StringTokenizer strtok = new StringTokenizer(input_line);
        String token = strtok.nextToken();

        if (token.equals("solid")) {
            input_line = readLine();
            strtok = new StringTokenizer(input_line);
            token = strtok.nextToken();
            lineCount = 1;
        }

        if (token.equals("endsolid") || input_line.contains("end solid")) {
            try {
                return getNextFacet(normal, vertices);
            } catch (IOException ioe) {
                return false;
            }
        }

        if (!token.equals("facet")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount + " word: " + token;

            throw new IllegalArgumentException(msg);
        }

        token = strtok.nextToken();

        if (!token.equals("normal")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount;

            throw new IllegalArgumentException(msg);
        }

        readNormal(strtok, normal);

        input_line = readLine();

        if (input_line == null) {
            return false;
        }

        strtok = new StringTokenizer(input_line);
        token = strtok.nextToken();
        lineCount++;

        if (!token.equals("outer")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount;

            throw new IllegalArgumentException(msg);
        }

        token = strtok.nextToken();

        if (!token.equals("loop")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount;

            throw new IllegalArgumentException(msg);
        }

        for (int i = 0; i < 3; i++) {
            input_line = readLine();
            strtok = new StringTokenizer(input_line);
            lineCount++;

            token = strtok.nextToken();

            if (!token.equals("vertex")) {
                close();

                I18nManager intl_mgr = I18nManager.getManager();

                String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount;

                throw new IllegalArgumentException(msg);
            }

            readCoordinate(strtok, vertices[i]);
        }

        input_line = readLine();

        if (input_line == null) {
            return false;
        }

        strtok = new StringTokenizer(input_line);
        token = strtok.nextToken();

        lineCount++;

        if (!token.equals("endloop")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount;

            throw new IllegalArgumentException(msg);
        }

        input_line = readLine();
        if (input_line == null) {
            return false;
        }

        strtok = new StringTokenizer(input_line);
        token = strtok.nextToken();
        lineCount++;

        if (!token.equals("endfacet")) {
            close();

            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount;

            throw new IllegalArgumentException(msg);
        }

        return true;
    }

    /**********************************************************************************************/
    public boolean parse(URL url, Component parentComponent) throws InterruptedIOException, IOException {
        InputStream stream = null;

        try {
            stream = url.openStream();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }

            throw e;
        }

        stream = new ProgressMonitorInputStream(parentComponent, "analyzing " + url.toString(), stream);

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        boolean isAscii = false;

        try {
            isAscii = parse(reader);
        } finally {
            reader.close();
        }

        if (!isAscii) {
            return false;
        }

        try {
            stream = url.openStream();
        } catch (IOException e) {
            stream.close();
            throw e;
        }

        stream = new ProgressMonitorInputStream(parentComponent, "parsing " + url.toString(), stream);

        reader = new BufferedReader(new InputStreamReader(stream));

        itsReader = reader;

        return true;
    }

    /**********************************************************************************************/
    public boolean parse(URL url) throws IOException {
        InputStream stream = null;
        try {
            stream = url.openStream();
        } catch (IOException e) {
            if (stream != null) {
                stream.close();
            }
            throw e;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        boolean isAscii = false;

        try {
            isAscii = parse(reader);
        } catch (InterruptedIOException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }

        if (!isAscii) {
            return false;
        }

        try {
            stream = url.openStream();
        } catch (IOException e) {
            stream.close();
            throw e;
        }

        reader = new BufferedReader(new InputStreamReader(stream));

        itsReader = reader;

        return true;
    }

    /**********************************************************************************************/
    private boolean parse(BufferedReader reader) throws IOException, IllegalArgumentException {
        int numOfObjects = 0;
        int numOfFacets = 0;

        ArrayList<Integer> facetsPerObject = new ArrayList<Integer>(10);
        ArrayList<String> names = new ArrayList<String>(10);

        boolean isAscii = true;

        itsReader = reader;

        String line = readLine();

        int line_count = 1;

        line = line.trim();

        if (!line.startsWith("solid")) {
            return false;
        } else {
            if (line.length() > 6) {
                names.add(line.substring(6));
            } else {
                names.add(null);
            }
        }

        line = readLine();

        if (line == null) {
            I18nManager intl_mgr = I18nManager.getManager();

            String msg = intl_mgr.getString(EMPTY_FILE_MSG_PROP);
            throw new IllegalArgumentException(msg);
        }

        while (line != null) {
            line_count++;

            if (line.indexOf("facet") >= 0) {
                numOfFacets++;

                for (int i = 0; i < 6; i++) {
                    readLine();
                }

                line_count += 6;
            } else if ((line.indexOf("endsolid") >= 0) || (line.indexOf("end solid") >= 0)) {
                facetsPerObject.add(new Integer(numOfFacets));
                numOfFacets = 0;
                numOfObjects++;
            } else if (line.indexOf("solid") >= 0) {
                line = line.trim();

                if (line.length() > 6)
                    names.add(line.substring(6));
            } else {
                line = line.trim();
                if (line.length() != 0) {
                    I18nManager intl_mgr = I18nManager.getManager();

                    String msg = intl_mgr.getString(UNKNOWN_KEYWORD_MSG_PROP) + ": " + lineCount;

                    throw new IllegalArgumentException(msg);
                }
            }

            line = readLine();
        }

        if (numOfFacets > 0 && numOfObjects == 0) {
            numOfObjects = 1;
            facetsPerObject.add(new Integer(numOfFacets));
        }

        itsNumOfObjects = numOfObjects;
        itsNumOfFacets = new int[numOfObjects];
        itsNames = new String[numOfObjects];

        for (int i = 0; i < numOfObjects; i++) {
            Integer num = (Integer) facetsPerObject.get(i);
            itsNumOfFacets[i] = num.intValue();

            itsNames[i] = (String) names.get(i);
        }

        return true;
    }

    /**********************************************************************************************/
    private void readNormal(StringTokenizer strtok, double[] vector) throws IOException {
        boolean error_found = false;

        for (int i = 0; i < 3; i++) {
            String num_str = strtok.nextToken();

            try {
                vector[i] = Double.parseDouble(num_str);
            } catch (NumberFormatException e) {
                if (!strictParsing) {
                    error_found = true;
                    continue;
                }

                I18nManager intl_mgr = I18nManager.getManager();

                String msg = intl_mgr.getString(INVALID_NORMAL_DATA_MSG_PROP) + num_str;

                throw new IllegalArgumentException(msg);
            }
        }

        if (error_found) {
            vector[0] = 0;
            vector[1] = 0;
            vector[2] = 0;
        }
    }

    /**********************************************************************************************/
    private void readCoordinate(StringTokenizer strtok, double[] vector) throws IOException {
        for (int i = 0; i < 3; i++) {
            String num_str = strtok.nextToken();

            boolean error_found = false;

            try {
                vector[i] = Double.parseDouble(num_str);
            } catch (NumberFormatException e) {
                if (strictParsing) {
                    I18nManager intl_mgr = I18nManager.getManager();

                    String msg = intl_mgr.getString(INVALID_VERTEX_DATA_MSG_PROP) + ": Cannot parse vertex: " + num_str;
                    throw new IllegalArgumentException(msg);
                } else {
                    String new_str = num_str.replace(",", ".");

                    try {
                        vector[i] = Double.parseDouble(new_str);
                    } catch (NumberFormatException e2) {

                        I18nManager intl_mgr = I18nManager.getManager();

                        String msg = intl_mgr.getString(INVALID_VERTEX_DATA_MSG_PROP) + ": Cannot parse vertex: " + num_str;
                        throw new IllegalArgumentException(msg);
                    }
                }
            }
        }
    }

    /**********************************************************************************************/
    private String readLine() throws IOException {
        String input_line = "";

        while (input_line.length() == 0) {
            input_line = itsReader.readLine();

            if (input_line == null) {
                break;
            }

            if (input_line.length() > 0 && Character.isWhitespace(input_line.charAt(0))) {
                input_line = input_line.trim();
            }
        }

        return input_line;
    }
}

package org.andresoviedo.util.android.assets;

import org.andresoviedo.util.android.AndroidURLConnection;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
/**************************************************************************************************/
public class Handler extends URLStreamHandler {
    /**********************************************************************************************/
    @Override
    protected URLConnection openConnection(final URL url) {
        return new AndroidURLConnection(url);
    }
}
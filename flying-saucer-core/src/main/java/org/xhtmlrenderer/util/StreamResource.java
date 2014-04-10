package org.xhtmlrenderer.util;

import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.util.zip.GZIPInputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URL;

public class StreamResource {
    private String _uri;
    private String _uriFinal;
    private URLConnection _conn;
    private int _slen;
    private InputStream _inputStream;

    public StreamResource(final String uri) {
        _uri = uri;
        _uriFinal = uri;
    }

    public void connect() {
        try {
            _conn = new URL(_uri).openConnection();

            // If using Java 5+ you can set timeouts for the URL connection--useful if the remote
            // server is down etc.; the default timeout is pretty long
            //
            //uc.setConnectTimeout(10 * 1000);
            //uc.setReadTimeout(30 * 1000);
            //
            // TODO:CLEAN-JDK1.4
            // Since we target 1.4, we use a couple of system properties--note these are only supported
            // in the Sun JDK implementation--see the Net properties guide in the JDK
            // e.g. file:///usr/java/j2sdk1.4.2_17/docs/guide/net/properties.html
            //System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(10 * 1000));
            //System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(30 * 1000));
            _conn.setRequestProperty("Accept-Encoding", "gzip");
            
            _conn.connect();
            _slen = _conn.getContentLength();
            
            if (_conn instanceof HttpURLConnection)
            {
            	// Java doesn't automatically follow redirects from http
            	// to https or vice versa but browsers do.
            	// TODO: This should be configurable, especially going from
            	// https to http.
            	// TODO: Do more than one redirect.

            	final HttpURLConnection http = (HttpURLConnection) _conn;
            	final int code = http.getResponseCode();
            	
            	if (code >= 300 && code < 400)
            	{
            		final String redirect = http.getHeaderField("Location");
            		_uri = redirect;
            		_conn = new URL(_uri).openConnection();
            		_conn.setRequestProperty("Accept-Encoding", "gzip");
            		_conn.connect();
            		_slen = _conn.getContentLength();
            	}
            }
        } catch (final java.net.MalformedURLException e) {
            XRLog.exception("bad URL given: " + _uri, e);
        } catch (final FileNotFoundException e) {
            XRLog.exception("item at URI " + _uri + " not found");
        } catch (final IOException e) {
            XRLog.exception("IO problem for " + _uri, e);
        }
    }

    public boolean hasStreamLength() {
        return _slen >= 0;
    }

    public int streamLength() {
        return _slen;
    }

    public BufferedInputStream bufferedStream() throws IOException {
        _inputStream = _conn.getInputStream();

        // Check for redirects
        if (!_conn.getURL().toString().equals(_uri)) {
          _uriFinal = _conn.getURL().toString();
        }

        // Check for encoding
        final InputStream is =  "gzip".equals(_conn.getContentEncoding()) ? new GZIPInputStream(_inputStream) : _inputStream;
        return new BufferedInputStream(is);
    }

    public String getFinalUri() {
      return _uriFinal;
    }

    public void close() {
        if (_inputStream != null) {
            try {
                _inputStream.close();
            } catch (final IOException e) {
                // swallow
            }
        }
    }

	public URLConnection getUrlConnection() 
	{
		return _conn;
	}
}

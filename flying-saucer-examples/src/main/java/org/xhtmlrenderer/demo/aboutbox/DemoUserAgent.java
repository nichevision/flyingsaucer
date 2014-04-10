/*
 * PanelManager.java
 * Copyright (c) 2005 Torbjoern Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.demo.aboutbox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.HTMLResource;
import org.xhtmlrenderer.swing.AWTFSImage;
import org.xhtmlrenderer.swing.StylesheetCache;
import org.xhtmlrenderer.util.Uu;
import org.xhtmlrenderer.util.XRLog;


/**
 * Created by IntelliJ IDEA.
 * User: tobe
 * Date: 2005-jun-15
 * Time: 07:38:59
 * To change this template use File | Settings | File Templates.
 */
public class DemoUserAgent implements UserAgentCallback {
    private String baseUrl;
    private int index = -1;
    private final ArrayList history = new ArrayList();
    protected StylesheetCache _styleCache = new StylesheetCache();
    /**
     * an LRU cache
     */
    private final int imageCacheCapacity = 16;
    private final java.util.LinkedHashMap imageCache =
            new java.util.LinkedHashMap(imageCacheCapacity, 0.75f, true) {
                protected boolean removeEldestEntry(final java.util.Map.Entry eldest) {
                    return size() > imageCacheCapacity;
                }
            };

    public CSSResource getCSSResource(String uri) {
        InputStream is = null;
        uri = resolveURI(uri);
        try {
            final URLConnection uc = new URL(uri).openConnection();
            uc.connect();
            is = uc.getInputStream();
        } catch (final MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (final IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        }
        return new CSSResource(is);
    }

    public ImageResource getImageResource(String uri) {
        ImageResource ir = null;
        uri = resolveURI(uri);
        ir = (ImageResource) imageCache.get(uri);
        //TODO: check that cached image is still valid
        if (ir == null) {
            InputStream is = null;
            try {
                final URLConnection uc = new URL(uri).openConnection();
                uc.connect();
                is = uc.getInputStream();
            } catch (final MalformedURLException e1) {
                XRLog.exception("bad URL given: " + uri, e1);
            } catch (final IOException e11) {
                XRLog.exception("IO problem for " + uri, e11);
            }
            if (is != null) {
                try {
                    final BufferedImage img = ImageIO.read(is);
                    ir = new ImageResource(uri, AWTFSImage.createImage(img));
                    imageCache.put(uri, ir);
                } catch (final IOException e) {
                    XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                }
            }
        }
        if (ir == null) ir = new ImageResource(uri, null);
        return ir;
    }
    
    public byte[] getBinaryResource(final String uri) {
        InputStream is = null;
        try {
            final URL url = new URL(uri);
            final URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            final ByteArrayOutputStream result = new ByteArrayOutputStream();
            final byte[] buf = new byte[10240];
            int i;
            while ( (i = is.read(buf)) != -1) {
                result.write(buf, 0, i);
            }
            is.close();
            is = null;
            
            return result.toByteArray();
        } catch (final IOException e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }    

    public HTMLResource getXMLResource(String uri) {
        uri = resolveURI(uri);
        if (uri != null && uri.startsWith("file:")) {
            File file = null;
            try {
                file = new File(new URI(uri));
            } catch (final URISyntaxException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        HTMLResource xr = null;
        InputStream inputStream = null;
        try {
            final URLConnection uc = new URL(uri).openConnection();
            uc.connect();
            // TODO: String contentType = uc.getContentType(); Maybe should popup a choice when content/unknown!
            inputStream = uc.getInputStream();
            xr = HTMLResource.load(inputStream, uri);
        } catch (final MalformedURLException e) {
            XRLog.exception("bad URL given: " + uri, e);
        } catch (final IOException e) {
            XRLog.exception("IO problem for " + uri, e);
        } finally {
            if ( inputStream != null ) try {
                inputStream.close();
            } catch (final IOException e) {
                // swallow
            }
        }
        if (xr == null) {
            final String notFound = "<h1>Document not found</h1>";
            xr = HTMLResource.load(new StringReader(notFound));
        }
        return xr;
    }

    public boolean isVisited(String uri) {
        if (uri == null) return false;
        uri = resolveURI(uri);
        return history.contains(uri);
    }

    public void setBaseURL(final String url) {
        baseUrl = resolveURI(url);
        if (baseUrl == null) baseUrl = "error:FileNotFound";
        //setBaseURL is called by view when document is loaded
        if (index >= 0) {
            final String historic = (String) history.get(index);
            if (historic.equals(baseUrl)) return;//moved in history
        }
        index++;
        for (final int i = index; i < history.size(); history.remove(i)) ;
        history.add(index, baseUrl);
    }

    public String resolveURI(final String uri) {
        URL ref = null;
        if (uri == null) return baseUrl;
        if (uri.trim().equals("")) return baseUrl;//jar URLs don't resolve this right
        if (uri.startsWith("demo:")) {
            final DemoMarker marker = new DemoMarker();
            String short_url = uri.substring(5);
            if (!short_url.startsWith("/")) {
                short_url = "/" + short_url;
            }
            ref = marker.getClass().getResource(short_url);
            Uu.p("ref = " + ref);
        } else {
            try {
                URL base;
                if (baseUrl == null || baseUrl.length() == 0) {
                    ref = new URL(uri);
                } else {
                    base = new URL(baseUrl);
                    ref = new URL(base, uri);
                }

            } catch (final MalformedURLException e) {
                e.printStackTrace();
            }
        }

        if (ref == null)
            return null;
        else
            return ref.toExternalForm();
    }

    public String getBaseURL() {
        return baseUrl;
    }


    public String getForward() {
        index++;
        return (String) history.get(index);
    }

    public String getBack() {
        index--;
        return (String) history.get(index);
    }

    public boolean hasForward() {
        if (index + 1 < history.size() && index >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasBack() {
        if (index >= 0) {
            return true;
        } else {
            return false;
        }
    }

	@Override
	public StylesheetCache getStylesheetCache() {
		return _styleCache;
	}
}

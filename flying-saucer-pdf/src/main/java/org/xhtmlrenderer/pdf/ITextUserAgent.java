/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xhtmlrenderer.util.XRLog;

import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import org.xhtmlrenderer.util.ImageUtil;

public class ITextUserAgent extends NaiveUserAgent {
    private static final int IMAGE_CACHE_CAPACITY = 32;

    private SharedContext _sharedContext;

    private final ITextOutputDevice _outputDevice;

    public ITextUserAgent(final ITextOutputDevice outputDevice) {
        super(IMAGE_CACHE_CAPACITY);
        _outputDevice = outputDevice;
    }

    private byte[] readStream(final InputStream is) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
        final byte[] buf = new byte[10240];
        int i;
        while ((i = is.read(buf)) != -1) {
            out.write(buf, 0, i);
        }
        out.close();
        return out.toByteArray();
    }

    public ImageResource getImageResource(String uri) {
        ImageResource resource = null;
        if (ImageUtil.isEmbeddedBase64Image(uri)) {
            resource = loadEmbeddedBase64ImageResource(uri);
        } else {
            uri = resolveURI(uri);
            resource = _imageCache.get(uri);
            if (resource == null) {
                final InputStream is = resolveAndOpenStream(uri);
                if (is != null) {
                    try {
                        final URL url = new URL(uri);
                        if (url.getPath() != null && url.getPath().toLowerCase().endsWith(".pdf")) {
                            final PdfReader reader = _outputDevice.getReader(url);
                            final PDFAsImage image = new PDFAsImage(url);
                            final Rectangle rect = reader.getPageSizeWithRotation(1);
                            image.setInitialWidth(rect.getWidth() * _outputDevice.getDotsPerPoint());
                            image.setInitialHeight(rect.getHeight() * _outputDevice.getDotsPerPoint());
                            resource = new ImageResource(uri, image);
                        } else {
                            final Image image = Image.getInstance(readStream(is));
                            scaleToOutputResolution(image);
                            resource = new ImageResource(uri, new ITextFSImage(image));
                        }
                        _imageCache.put(uri, resource);
                    } catch (final Exception e) {
                        XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
                    } finally {
                        try {
                            is.close();
                        } catch (final IOException e) {
                            // ignore
                        }
                    }
                }
            }

            if (resource != null) {
                resource = new ImageResource(resource.getImageUri(), (FSImage) ((ITextFSImage) resource.getImage()).clone());
            } else {
                resource = new ImageResource(uri, null);
            }
        }
        return resource;
    }
    
    private ImageResource loadEmbeddedBase64ImageResource(final String uri) {
        try {
            final byte[] buffer = ImageUtil.getEmbeddedBase64Image(uri);
            final Image image = Image.getInstance(buffer);
            scaleToOutputResolution(image);
            return new ImageResource(null, new ITextFSImage(image));
        } catch (final Exception e) {
            XRLog.exception("Can't read XHTML embedded image.", e);
        }
        return new ImageResource(null, null);
    }

    private void scaleToOutputResolution(final Image image) {
        final float factor = _sharedContext.getDotsPerPixel();
        if (factor != 1.0f) {
            image.scaleAbsolute(image.getPlainWidth() * factor, image.getPlainHeight() * factor);
        }
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void setSharedContext(final SharedContext sharedContext) {
        _sharedContext = sharedContext;
    }
}

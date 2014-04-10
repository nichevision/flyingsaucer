/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Who?
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
 * }}}
 */
package org.xhtmlrenderer.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;

/**
 * @author Patrick Wright
 */
public class HTMLResource extends AbstractResource {
    private Document document;
    private final String _uri;
    
    private HTMLResource(final String html)
    {
    	setDocument(Jsoup.parse(html));
      _uri = null;
    }

    private HTMLResource(final InputStream stream, final String uri) {
      _uri = uri;
    	try {
			document = Jsoup.parse(stream, null, "");
		} catch (final IOException e) {
			XRLog.load(Level.WARNING, "Unable to parse input stream", e);
			throw new XRRuntimeException("Unable to parse input stream", e);
		}
    }
    
    private HTMLResource(final File file)
    {
      _uri = null;
    	try {
			document = Jsoup.parse(file, null);
		} catch (final IOException e) {
			XRLog.load(Level.WARNING, "Unable to parse file", e);
			throw new XRRuntimeException("Unable to parse file", e);
		}
    }

    public static HTMLResource load(final String html)
    {
    	return new HTMLResource(html);
    }
    
    public static HTMLResource load(final InputStream stream, final String uri) {
        return new HTMLResource(stream, uri);
    }

    public String getURI() {
      return _uri;
    }
    public static HTMLResource load(final Reader reader) {
    	final char[] cbuf = new char[4096];
    	int numChars;
    	
    	final StringBuilder builder = new StringBuilder(4096);

    	try {
			while ((numChars = reader.read(cbuf)) >= 0) {
			    builder.append(cbuf, 0, numChars);
			}
		} catch (final IOException e) {
			XRLog.load(Level.WARNING, "Unable to parse reader", e);
			throw new XRRuntimeException("Unable to parse reader", e);
		}

    	return new HTMLResource(builder.toString());
    }
 
    public Document getDocument() {
        return document;
    }

    private void setDocument(final Document document) {
        this.document = document;
    }

	public static HTMLResource load(final File file) {
		return new HTMLResource(file);
	}
}

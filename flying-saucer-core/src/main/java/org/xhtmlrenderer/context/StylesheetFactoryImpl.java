/*
 * StylesheetFactoryImpl.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm
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
package org.xhtmlrenderer.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import org.xhtmlrenderer.css.extend.StylesheetFactory;
import org.xhtmlrenderer.css.parser.CSSErrorHandler;
import org.xhtmlrenderer.css.parser.CSSParser;
import org.xhtmlrenderer.css.sheet.Ruleset;
import org.xhtmlrenderer.css.sheet.Stylesheet;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.util.XRLog;

/**
 * A Factory class for Cascading Style Sheets. Sheets are parsed using a single
 * parser instance for all sheets. Sheets are cached by URI using a LRU test,
 * but timestamp of file is not checked.
 *
 * @author Torbjoern Gannholm
 */
public class StylesheetFactoryImpl implements StylesheetFactory {

	/**
     * the UserAgentCallback to resolve uris
     */
    private UserAgentCallback _userAgentCallback;
    private final CSSParser _cssParser;

    public StylesheetFactoryImpl(final UserAgentCallback userAgentCallback) {
        _userAgentCallback = userAgentCallback;
        _cssParser = new CSSParser(new CSSErrorHandler() {
            public void error(final String uri, final String message) {
                XRLog.cssParse(Level.WARNING, "(" + uri + ") " + message);
            }
        });
    }

    public synchronized Stylesheet parse(final Reader reader, final StylesheetInfo info) {
        try {
        	final Stylesheet s1 = _cssParser.parseStylesheet(info.getUri(), info.getOrigin(), reader);
        	_userAgentCallback.getStylesheetCache().putStylesheet(info.getUri(), s1);
            return s1; 
        } catch (final IOException e) {
            XRLog.cssParse(Level.WARNING, "Couldn't parse stylesheet at URI " + info.getUri() + ": " + e.getMessage(), e);
            e.printStackTrace();
            return new Stylesheet(info.getUri(), info.getOrigin());
        }
    }

    /**
     * @return Returns null if uri could not be loaded
     */
    private Stylesheet parse(final StylesheetInfo info) {
        final CSSResource cr = _userAgentCallback.getCSSResource(info.getUri());
        // Whether by accident or design, InputStream will never be null
        // since the null resource stream is wrapped in a BufferedInputStream
        final InputStream is = cr.getResourceInputStream();
        try {
            final Stylesheet s1 = parse(new InputStreamReader(is, "UTF-8"), info);
            return s1;
        } catch (final UnsupportedEncodingException e) {
            // Shouldn't happen
            throw new RuntimeException(e.getMessage(), e);
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

    public synchronized Ruleset parseStyleDeclaration(final CSSOrigin origin, final String styleDeclaration) {
        return _cssParser.parseDeclaration(origin, styleDeclaration);
    }

    public Stylesheet getStylesheet(final StylesheetInfo info) 
    {
        XRLog.load("Requesting stylesheet: " + info.getUri());
        
        // Give the user agent the chance to return a cached Stylesheet
        // instance.
        final Stylesheet s1 = _userAgentCallback.getStylesheetCache().getStylesheet(info);

        if (s1 == null)
        	return parse(info);

        return s1;
    }

    public void setUserAgentCallback(final UserAgentCallback userAgent) {
        _userAgentCallback = userAgent;
    }
    
    public UserAgentCallback getUac()
    {
    	return _userAgentCallback;
    }
    
    public void setSupportCMYKColors(final boolean b) {
        _cssParser.setSupportCMYKColors(b);
    }
}

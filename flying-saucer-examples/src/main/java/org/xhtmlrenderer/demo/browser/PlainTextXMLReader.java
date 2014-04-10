package org.xhtmlrenderer.demo.browser;

import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Read plain text file as if it was xml with a text-tag around it.
 * <p/>
 * Fulfills minimum requirements.
 * <p/>
 * Maybe not the easiest way to do this :-)
 */
public class PlainTextXMLReader implements XMLReader {
    private EntityResolver entityResolver;
    private DTDHandler dtdHandler;
    private ContentHandler contentHandler;
    private ErrorHandler errorHandler;
    private final BufferedReader text;

    public PlainTextXMLReader(final InputStream is) {
        text = new BufferedReader(new InputStreamReader(is));
    }

    public boolean getFeature(final String s) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (s.equals("http://xml.org/sax/features/namespaces")) {
            return true;
        }
        if (s.equals("http://xml.org/sax/features/namespace-prefixes")) {
            return false;
        }
        throw new SAXNotRecognizedException(s);
    }

    public void setFeature(final String s, final boolean b) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (s.equals("http://xml.org/sax/features/namespaces")) {
            if (!b)
                throw new SAXNotSupportedException(s);
            else
                return;
        }
        if (s.equals("http://xml.org/sax/features/namespace-prefixes")) {
            if (b)
                throw new SAXNotSupportedException(s);
            else
                return;
        }
        throw new SAXNotRecognizedException(s);
    }

    public Object getProperty(final String s) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(s);
    }

    public void setProperty(final String s, final Object o) throws SAXNotRecognizedException, SAXNotSupportedException {
        throw new SAXNotRecognizedException(s);
    }

    public void setEntityResolver(final EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    public void setDTDHandler(final DTDHandler dtdHandler) {
        this.dtdHandler = dtdHandler;
    }

    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }

    public void setContentHandler(final ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    public void setErrorHandler(final ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void parse(final InputSource inputSource) throws IOException, SAXException {
        contentHandler.startDocument();
        contentHandler.startElement("http://www.w3.org/1999/xhtml", "pre", "pre", new AttributesImpl());

        String line;
        do {
            line = text.readLine();
            if (line == null) break;
            final char[] chars = (line + "\n").toCharArray();
            contentHandler.characters(chars, 0, chars.length);
        } while (line != null);

        contentHandler.endElement("http://www.w3.org/1999/xhtml", "pre", "pre");
        contentHandler.endDocument();
    }

    public void parse(final String s) throws IOException, SAXException {
        throw new SAXNotRecognizedException(s);
    }
}

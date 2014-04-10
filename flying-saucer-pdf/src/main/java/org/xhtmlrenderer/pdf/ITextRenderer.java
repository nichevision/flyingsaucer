/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
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
package org.xhtmlrenderer.pdf;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.xhtmlrenderer.context.StyleReference;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.extend.UserInterface;
import org.xhtmlrenderer.layout.BoxBuilder;
import org.xhtmlrenderer.layout.Layer;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.render.ViewportBox;
import org.xhtmlrenderer.resource.HTMLResource;
import org.xhtmlrenderer.simple.HtmlNamespaceHandler;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.JsoupUtil;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfWriter;

public class ITextRenderer {
    // These two defaults combine to produce an effective resolution of 96 px to
    // the inch
    private static final float DEFAULT_DOTS_PER_POINT = 20f * 4f / 3f;
    private static final int DEFAULT_DOTS_PER_PIXEL = 20;

    private final SharedContext _sharedContext;
    private final ITextOutputDevice _outputDevice;

    private Document _doc;
    private BlockBox _root;

    private final float _dotsPerPoint;

    private com.lowagie.text.Document _pdfDoc;
    private PdfWriter _writer;

    private PDFEncryption _pdfEncryption;

    // note: not hard-coding a default version in the _pdfVersion field as this
    // may change between iText releases
    // check for null before calling writer.setPdfVersion()
    // use one of the values in PDFWriter.VERSION...
    private Character _pdfVersion;

    private final char[] validPdfVersions = new char[] { PdfWriter.VERSION_1_2, PdfWriter.VERSION_1_3, PdfWriter.VERSION_1_4,
            PdfWriter.VERSION_1_5, PdfWriter.VERSION_1_6, PdfWriter.VERSION_1_7 };

    private PDFCreationListener _listener;

    public ITextRenderer() {
        this(DEFAULT_DOTS_PER_POINT, DEFAULT_DOTS_PER_PIXEL);
    }

    public ITextRenderer(final float dotsPerPoint, final int dotsPerPixel) {
        _dotsPerPoint = dotsPerPoint;

        _outputDevice = new ITextOutputDevice(_dotsPerPoint);

        final ITextUserAgent userAgent = new ITextUserAgent(_outputDevice);
        _sharedContext = new SharedContext();
        _sharedContext.setUserAgentCallback(userAgent);
        _sharedContext.setCss(new StyleReference(userAgent));
        userAgent.setSharedContext(_sharedContext);
        _outputDevice.setSharedContext(_sharedContext);

        final ITextFontResolver fontResolver = new ITextFontResolver(_sharedContext);
        _sharedContext.setFontResolver(fontResolver);

        final ITextReplacedElementFactory replacedElementFactory = new ITextReplacedElementFactory(_outputDevice);
        _sharedContext.setReplacedElementFactory(replacedElementFactory);

        _sharedContext.setTextRenderer(new ITextTextRenderer());
        _sharedContext.setDPI(72 * _dotsPerPoint);
        _sharedContext.setDotsPerPixel(dotsPerPixel);
        _sharedContext.setPrint(true);
        _sharedContext.setInteractive(false);
    }

    public Document getDocument() {
        return _doc;
    }

    public ITextFontResolver getFontResolver() {
        return (ITextFontResolver) _sharedContext.getFontResolver();
    }

    private Document loadDocument(final String uri) {
        return _sharedContext.getUac().getXMLResource(uri).getDocument();
    }

    public void setDocument(final String uri) {
        setDocument(loadDocument(uri), uri);
    }

    public void setDocument(final Document doc, final String url) {
        setDocument(doc, url, new HtmlNamespaceHandler());
    }

    public void setDocument(final File file) throws IOException {

        final File parent = file.getAbsoluteFile().getParentFile();
        setDocument(loadDocument(file.toURI().toURL().toExternalForm()), (parent == null ? "" : parent.toURI().toURL().toExternalForm()));
    }

    public void setDocumentFromString(final String content) {
        setDocumentFromString(content, null);
    }

    public void setDocumentFromString(final String content, final String baseUrl) 
    {
        final Document dom = HTMLResource.load(content).getDocument();
        setDocument(dom, baseUrl);
    }

    public void setDocument(final Document doc, final String url, final NamespaceHandler nsh) {
        _doc = doc;

        getFontResolver().flushFontFaceFonts();

        _sharedContext.reset();
        if (Configuration.isTrue("xr.cache.stylesheets", true)) {
            _sharedContext.getCss().flushStyleSheets();
        } else {
            _sharedContext.getCss().flushAllStyleSheets();
        }
        _sharedContext.setBaseURL(url);
        _sharedContext.setNamespaceHandler(nsh);
        _sharedContext.getCss().setDocumentContext(_sharedContext, _sharedContext.getNamespaceHandler(), doc, new NullUserInterface());
        getFontResolver().importFontFaces(_sharedContext.getCss().getFontFaceRules());
    }

    public PDFEncryption getPDFEncryption() {
        return _pdfEncryption;
    }

    public void setPDFEncryption(final PDFEncryption pdfEncryption) {
        _pdfEncryption = pdfEncryption;
    }

    public void setPDFVersion(final char _v) {
        for (int i = 0; i < validPdfVersions.length; i++) {
            if (_v == validPdfVersions[i]) {
                _pdfVersion = new Character(_v);
                return;
            }
        }
        throw new IllegalArgumentException("Invalid PDF version character; use "
                + "valid constants from PdfWriter (e.g. PdfWriter.VERSION_1_2)");
    }

    public char getPDFVersion() {
        return _pdfVersion == null ? '0' : _pdfVersion.charValue();
    }

    public void layout() {
        final LayoutContext c = newLayoutContext();
        final BlockBox root = BoxBuilder.createRootBox(c, _doc);
        root.setContainingBlock(new ViewportBox(getInitialExtents(c)));
        root.layout(c);
        final Dimension dim = root.getLayer().getPaintingDimension(c);
        root.getLayer().trimEmptyPages(c, dim.height);
        root.getLayer().layoutPages(c);
        _root = root;
    }

    private Rectangle getInitialExtents(final LayoutContext c) {
        final PageBox first = Layer.createPageBox(c, "first");

        return new Rectangle(0, 0, first.getContentWidth(c), first.getContentHeight(c));
    }

    private RenderingContext newRenderingContext() {
        final RenderingContext result = _sharedContext.newRenderingContextInstance();
        result.setFontContext(new ITextFontContext());

        result.setOutputDevice(_outputDevice);

        _sharedContext.getTextRenderer().setup(result.getFontContext());

        result.setRootLayer(_root.getLayer());

        return result;
    }

    private LayoutContext newLayoutContext() {
        final LayoutContext result = _sharedContext.newLayoutContextInstance();
        result.setFontContext(new ITextFontContext());

        _sharedContext.getTextRenderer().setup(result.getFontContext());

        return result;
    }

    public void createPDF(final OutputStream os) throws DocumentException {
        createPDF(os, true, 0);
    }

    public void writeNextDocument() throws DocumentException {
        writeNextDocument(0);
    }

    public void writeNextDocument(final int initialPageNo) throws DocumentException {
        final List<PageBox> pages = _root.getLayer().getPages();

        final RenderingContext c = newRenderingContext();
        c.setInitialPageNo(initialPageNo);
        final PageBox firstPage = (PageBox) pages.get(0);
        final com.lowagie.text.Rectangle firstPageSize = new com.lowagie.text.Rectangle(0, 0, firstPage.getWidth(c) / _dotsPerPoint,
                firstPage.getHeight(c) / _dotsPerPoint);

        _outputDevice.setStartPageNo(_writer.getPageNumber());

        _pdfDoc.setPageSize(firstPageSize);
        _pdfDoc.newPage();

        writePDF(pages, c, firstPageSize, _pdfDoc, _writer);
    }

    public void finishPDF() {
        if (_pdfDoc != null) {
            fireOnClose();
            _pdfDoc.close();
        }
    }

    public void createPDF(final OutputStream os, final boolean finish) throws DocumentException {
        createPDF(os, finish, 0);
    }

    /**
     * <B>NOTE:</B> Caller is responsible for cleaning up the OutputStream if
     * something goes wrong.
     */
    public void createPDF(final OutputStream os, final boolean finish, final int initialPageNo) throws DocumentException {
        final List<PageBox> pages = _root.getLayer().getPages();

        final RenderingContext c = newRenderingContext();
        c.setInitialPageNo(initialPageNo);
        final PageBox firstPage = (PageBox) pages.get(0);
        final com.lowagie.text.Rectangle firstPageSize = new com.lowagie.text.Rectangle(0, 0, firstPage.getWidth(c) / _dotsPerPoint,
                firstPage.getHeight(c) / _dotsPerPoint);

        final com.lowagie.text.Document doc = new com.lowagie.text.Document(firstPageSize, 0, 0, 0, 0);
        final PdfWriter writer = PdfWriter.getInstance(doc, os);
        if (_pdfVersion != null) {
            writer.setPdfVersion(_pdfVersion.charValue());
        }
        if (_pdfEncryption != null) {
            writer.setEncryption(_pdfEncryption.getUserPassword(), _pdfEncryption.getOwnerPassword(),
                    _pdfEncryption.getAllowedPrivileges(), _pdfEncryption.getEncryptionType());
        }
        _pdfDoc = doc;
        _writer = writer;

        firePreOpen();
        doc.open();

        writePDF(pages, c, firstPageSize, doc, writer);

        if (finish) {
            fireOnClose();
            doc.close();
        }
    }

    private void firePreOpen() {
        if (_listener != null) {
            _listener.preOpen(this);
        }
    }

    private void firePreWrite(final int pageCount) {
        if (_listener != null) {
            _listener.preWrite(this, pageCount);
        }
    }

    private void fireOnClose() {
        if (_listener != null) {
            _listener.onClose(this);
        }
    }

    private void writePDF(final List<PageBox> pages, final RenderingContext c, final com.lowagie.text.Rectangle firstPageSize, final com.lowagie.text.Document doc,
            final PdfWriter writer) throws DocumentException {
        _outputDevice.setRoot(_root);

        _outputDevice.start(_doc);
        _outputDevice.setWriter(writer);
        _outputDevice.initializePage(writer.getDirectContent(), firstPageSize.getHeight());

        _root.getLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);

        final int pageCount = _root.getLayer().getPages().size();
        c.setPageCount(pageCount);
        firePreWrite(pageCount); // opportunity to adjust meta data
        setDidValues(doc); // set PDF header fields from meta data
        for (int i = 0; i < pageCount; i++) {
            final PageBox currentPage = pages.get(i);
            c.setPage(i, currentPage);
            paintPage(c, writer, currentPage);
            _outputDevice.finishPage();
            if (i != pageCount - 1) {
                final PageBox nextPage = pages.get(i + 1);
                final com.lowagie.text.Rectangle nextPageSize = new com.lowagie.text.Rectangle(0, 0, nextPage.getWidth(c) / _dotsPerPoint,
                        nextPage.getHeight(c) / _dotsPerPoint);
                doc.setPageSize(nextPageSize);
                doc.newPage();
                _outputDevice.initializePage(writer.getDirectContent(), nextPageSize.getHeight());
            }
        }

        _outputDevice.finish(c, _root);
    }

    // Sets the document information dictionary values from html metadata
    private void setDidValues(final com.lowagie.text.Document doc) {
        String v = _outputDevice.getMetadataByName("title");
        if (v != null) {
            doc.addTitle(v);
        }
        v = _outputDevice.getMetadataByName("author");
        if (v != null) {
            doc.addAuthor(v);
        }
        v = _outputDevice.getMetadataByName("subject");
        if (v != null) {
            doc.addSubject(v);
        }
        v = _outputDevice.getMetadataByName("keywords");
        if (v != null) {
            doc.addKeywords(v);
        }
    }

    private void paintPage(final RenderingContext c, final PdfWriter writer, final PageBox page) {
        provideMetadataToPage(writer, page);

        page.paintBackground(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintBorder(c, 0, Layer.PAGED_MODE_PRINT);

        final Shape working = _outputDevice.getClip();

        final Rectangle content = page.getPrintClippingBounds(c);
        _outputDevice.clip(content);

        final int top = -page.getPaintingTop() + page.getMarginBorderPadding(c, CalculatedStyle.TOP);

        final int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        _outputDevice.translate(left, top);
        _root.getLayer().paint(c);
        _outputDevice.translate(-left, -top);

        _outputDevice.setClip(working);
    }

    private void provideMetadataToPage(final PdfWriter writer, final PageBox page) {
        byte[] metadata = null;
        if (page.getMetadata() != null) {
            try {
                final String metadataBody = stringfyMetadata(page.getMetadata());
                if (metadataBody != null) {
                    metadata = createXPacket(stringfyMetadata(page.getMetadata())).getBytes("UTF-8");
                }
            } catch (final UnsupportedEncodingException e) {
                // Can't happen
                throw new RuntimeException(e);
            }
        }

        if (metadata != null) {
            writer.setPageXmpMetadata(metadata);
        }
    }

    private String stringfyMetadata(final Element element) 
    {
        final Element target = getFirstChildElement(element);

        if (target == null) 
            return null;

        // TODO: I think we need valid xml here.
        return target.html();
    }

    private static Element getFirstChildElement(final Element element) 
    {
        Node n = JsoupUtil.firstChild(element);

        while (n != null) 
        {
            if (JsoupUtil.isElement(n))
                return (Element) n;

            n = n.nextSibling();
        }
        
        return null;
    }

    private String createXPacket(final String metadata) {
        final StringBuffer result = new StringBuffer(metadata.length() + 50);
        result.append("<?xpacket begin='\uFEFF' id='W5M0MpCehiHzreSzNTczkc9d'?>\n");
        result.append(metadata);
        result.append("\n<?xpacket end='r'?>");

        return result.toString();
    }

    public ITextOutputDevice getOutputDevice() {
        return _outputDevice;
    }

    public SharedContext getSharedContext() {
        return _sharedContext;
    }

    public void exportText(final Writer writer) throws IOException {
        final RenderingContext c = newRenderingContext();
        c.setPageCount(_root.getLayer().getPages().size());
        _root.exportText(c, writer);
    }

    public BlockBox getRootBox() {
        return _root;
    }

    public float getDotsPerPoint() {
        return _dotsPerPoint;
    }

    public List<PagePosition> findPagePositionsByID(final Pattern pattern) {
        return _outputDevice.findPagePositionsByID(newLayoutContext(), pattern);
    }

    private static final class NullUserInterface implements UserInterface {
        public boolean isHover(final Element e) {
            return false;
        }

        public boolean isActive(final Element e) {
            return false;
        }

        public boolean isFocus(final Element e) {
            return false;
        }
    }

    public PDFCreationListener getListener() {
        return _listener;
    }

    public void setListener(final PDFCreationListener listener) {
        _listener = listener;
    }

    public PdfWriter getWriter() {
        return _writer;
    }
}

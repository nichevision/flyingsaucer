/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package com.github.danfickle.flyingsaucer.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.print.PrinterGraphics;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.extend.NamespaceHandler;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.Layer;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.resource.HTMLResource;
import org.xhtmlrenderer.simple.HtmlNamespaceHandler;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import org.xhtmlrenderer.swing.Java2DOutputDevice;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.Uu;
import org.xhtmlrenderer.util.XRLog;

/**
 * A Swing {@link javax.swing.JPanel} that encloses the Flying Saucer renderer
 * for easy integration into Swing applications.
 *
 * @author Joshua Marinacci
 */
@SuppressWarnings("serial")
public abstract class BasicPanel extends RootPanel implements
	FormSubmissionListener 
{
    private static final int PAGE_PAINTING_CLEARANCE_WIDTH = 10;
    private static final int PAGE_PAINTING_CLEARANCE_HEIGHT = 10;

    private boolean explicitlyOpaque;

    private final MouseTracker mouseTracker;
    private boolean centeredPagedView;
    protected FormSubmissionListener formSubmissionListener;

    public BasicPanel() {
        this(new NaiveUserAgent());
    }

    public BasicPanel(UserAgentCallback uac) {
        sharedContext = new SharedContext(uac);
        mouseTracker = new MouseTracker(this);
        formSubmissionListener = new FormSubmissionListener() {
            public void submit(String query) {
                System.out.println("Form Submitted!");
                System.out.println("Data: " + query);

                JOptionPane.showMessageDialog(
                        null,
                        "Form submit called; check console to see the query string" +
                        " that would have been submitted.",
                        "Form Submission",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        };
        sharedContext.setFormSubmissionListener(formSubmissionListener);
        init();
    }

    /**
     * Adds the specified Document listener to receive Document events from this
     * component. If listener l is null, no exception is thrown and no action is
     * performed.
     *
     * @param listener Contains the DocumentListener for DocumentEvent data.
     */
    public void addDocumentListener(DocumentListener listener) {
        this.documentListeners.put(listener, listener);
    }

    /**
     * Removes the specified Document listener from receive Document events from this
     * component. If listener l is null, no exception is thrown and no action is
     * performed.
     *
     * @param listener Contains the DocumentListener to remove.
     */
    public void removeDocumentListener(DocumentListener listener) {
        this.documentListeners.remove(listener);
    }

    public void paintComponent(Graphics g) {
        if (doc == null) {
            paintDefaultBackground(g);
            return;
        }

        // if this is the first time painting this document, then calc layout
        Layer root = getRootLayer();
        if (root == null || isNeedRelayout()) {
            doDocumentLayout(g.create());
            root = getRootLayer();
        }
        setNeedRelayout(false);
        if (root == null) {
            //Uu.p("dispatching an initial resize event");
            //queue.dispatchLayoutEvent(new ReflowEvent(ReflowEvent.CANVAS_RESIZED, this.getSize()));
            XRLog.render(Level.FINE, "skipping the actual painting");
        } else {
            RenderingContext c = newRenderingContext((Graphics2D) g.create());
            long start = System.currentTimeMillis();
            doRender(c, root);
            long end = System.currentTimeMillis();
            XRLog.render(Level.FINE, "RENDERING TOOK " + (end - start) + " ms");
        }
    }

    protected void doRender(RenderingContext c, Layer root) {
        try {
            // paint the normal swing background first
            // but only if we aren't printing.
            Graphics g = ((Java2DOutputDevice)c.getOutputDevice()).getGraphics();

            paintDefaultBackground(g);

            if (enclosingScrollPane == null) {
                Insets insets = getInsets();
                g.translate(insets.left, insets.top);
            }

            long start = System.currentTimeMillis();
            if (!c.isPrint()) {
                root.paint(c);
            } else {
                paintPagedView(c, root);
            }
            long after = System.currentTimeMillis();
            if (Configuration.isTrue("xr.incremental.repaint.print-timing", false)) {
                Uu.p("repaint took ms: " + (after - start));
            }
        } catch (ThreadDeath t) {
            throw t;
        } catch (Throwable t) {
            if (documentListeners.size() > 0) {
                fireOnRenderException(t);
            } else {
                if (t instanceof Error) {
                    throw (Error)t;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException)t;
                }

                // "Shouldn't" happen
                XRLog.exception(t.getMessage(), t);
            }
        }
    }

    private void paintDefaultBackground(Graphics g) {
        if (!(g instanceof PrinterGraphics) && explicitlyOpaque) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void paintPagedView(RenderingContext c, Layer root) {
        if (root.getLastPage() == null) {
            return;
        }

        final int pagePaintingClearanceWidth = isCenteredPagedView() ?
                calcCenteredPageLeftOffset(root.getMaxPageWidth(c, 0)) :
                PAGE_PAINTING_CLEARANCE_WIDTH;
        root.assignPagePaintingPositions(
                c, Layer.PAGED_MODE_SCREEN, PAGE_PAINTING_CLEARANCE_HEIGHT);

        setPreferredSize(new Dimension(
                root.getMaxPageWidth(c, pagePaintingClearanceWidth),
                root.getLastPage().getPaintingBottom() + PAGE_PAINTING_CLEARANCE_HEIGHT));
        revalidate();

        Graphics2D g = ((Java2DOutputDevice)c.getOutputDevice()).getGraphics();
        Shape working = g.getClip();

        List<PageBox> pages = root.getPages();
        c.setPageCount(pages.size());
        for (int i = 0; i < pages.size(); i++) {
            PageBox page = (PageBox)pages.get(i);
            c.setPage(i, page);

            g.setClip(working);

            Rectangle overall = page.getScreenPaintingBounds(c, pagePaintingClearanceWidth);
            overall.x -= 1;
            overall.y -= 1;
            overall.width += 1;
            overall.height += 1;

            Rectangle bounds = new Rectangle(overall);
            bounds.width += 1;
            bounds.height += 1;
            if (working.intersects(bounds)) {
                page.paintBackground(c, pagePaintingClearanceWidth, Layer.PAGED_MODE_SCREEN);
                page.paintMarginAreas(c, pagePaintingClearanceWidth, Layer.PAGED_MODE_SCREEN);
                page.paintBorder(c, pagePaintingClearanceWidth, Layer.PAGED_MODE_SCREEN);

                Color old = g.getColor();

                g.setColor(Color.BLACK);
                g.drawRect(overall.x, overall.y, overall.width, overall.height);
                g.setColor(old);

                Rectangle content = page.getPagedViewClippingBounds(c, pagePaintingClearanceWidth);
                g.clip(content);

                int left = pagePaintingClearanceWidth +
                    page.getMarginBorderPadding(c, CalculatedStyle.LEFT);
                int top = page.getPaintingTop()
                    + page.getMarginBorderPadding(c, CalculatedStyle.TOP)
                    - page.getTop();

                g.translate(left, top);
                root.paint(c);
                g.translate(-left, -top);

                g.setClip(working);
            }
        }

        g.setClip(working);
    }

    private int calcCenteredPageLeftOffset(int maxPageWidth) {
        return (getWidth() - maxPageWidth) / 2;
    }

    public void paintPage(Graphics2D g, int pageNo) {
        Layer root = getRootLayer();

        if (root == null) {
            throw new RuntimeException("Document needs layout");
        }

        if (pageNo < 0 || pageNo >= root.getPages().size()) {
            throw new IllegalArgumentException("Page " + pageNo + " is not between 0 " +
                    "and " + root.getPages().size());
        }

        RenderingContext c = newRenderingContext(g);

        PageBox page = (PageBox)root.getPages().get(pageNo);
        c.setPageCount(root.getPages().size());
        c.setPage(pageNo, page);

        page.paintBackground(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintMarginAreas(c, 0, Layer.PAGED_MODE_PRINT);
        page.paintBorder(c, 0, Layer.PAGED_MODE_PRINT);

        Shape working = g.getClip();

        Rectangle content = page.getPrintClippingBounds(c);
        g.clip(content);

        int top = -page.getPaintingTop() +
            page.getMarginBorderPadding(c, CalculatedStyle.TOP);

        int left = page.getMarginBorderPadding(c, CalculatedStyle.LEFT);

        g.translate(left, top);
        root.paint(c);
        g.translate(-left, -top);

        g.setClip(working);
    }

    public void assignPagePrintPositions(Graphics2D g) {
        RenderingContext c = newRenderingContext(g);
        getRootLayer().assignPagePaintingPositions(c, Layer.PAGED_MODE_PRINT);
    }

    public void printTree() {
        printTree(getRootBox(), "");
    }

    private void printTree(Box box, String tab) {
        XRLog.layout(Level.FINEST, tab + "Box = " + box);
        Iterator<Box> it = box.getChildIterator();
        while (it.hasNext()) {
            Box bx = (Box) it.next();
            printTree(bx, tab + " ");
        }
    }


    /**
     * Sets the layout attribute of the BasicPanel object
     * Overrides the method to do nothing, since you shouldn't have a
     * LayoutManager on an FS panel.
     *
     * @param l The new layout value
     */
    public void setLayout(LayoutManager l) {
    }

    public void setSharedContext(SharedContext ctx) {
        this.sharedContext = ctx;
    }

    public void setSize(Dimension d) {
        XRLog.layout(Level.FINEST, "set size called");
        super.setSize(d);
        /* CLEAN: do we need this?
        if (doc != null && body_box != null) {
            if(body_box.width != d.width)
            RenderQueue.getInstance().dispatchLayoutEvent(new ReflowEvent(ReflowEvent.CANVAS_RESIZED, d));
            //don't need the below, surely
            //else if(body_box.height != d.height)
            //    RenderQueue.getInstance().dispatchRepaintEvent(new ReflowEvent(ReflowEvent.CANVAS_RESIZED, d));
    } */
    }

    /*
=========== set document utility methods =============== */

    public void setDocument(InputStream stream, String url, NamespaceHandler nsh) {
        Document dom = HTMLResource.load(stream, url).getDocument();

        setDocument(dom, url, nsh);
    }

    public void setDocumentFromString(String content, String url, NamespaceHandler nsh) {
        Document dom = HTMLResource.load(content).getDocument();

        setDocument(dom, url, nsh);
    }

    public void setDocument(Document doc, String url) {
        setDocument(doc, url, new HtmlNamespaceHandler());
    }

    public void setDocument(String url) {
        setDocument(loadDocument(url), url, new HtmlNamespaceHandler());
    }

    public void setDocument(String url, NamespaceHandler nsh) {
        setDocument(loadDocument(url), url, nsh);
    }

    // TODO: should throw more specific exception (PWW 25/07/2006)
    protected void setDocument(InputStream stream, String url)
            throws Exception {
        setDocument(stream, url, new HtmlNamespaceHandler());
    }

    /**
     * Sets the new current document, where the new document
     * is located relative, e.g using a relative URL.
     *
     * @param filename The new document to load
     */
    protected void setDocumentRelative(String filename) {
        String url = getSharedContext().getUac().resolveURI(filename);
        if (isAnchorInCurrentDocument(filename)) {
            String id = getAnchorId(filename);
            Box box = getSharedContext().getBoxById(id);
            if (box != null) {
                Point pt;
                if (box.getStyle().isInline()) {
                    pt = new Point(box.getAbsX(), box.getAbsY());
                } else {
                    RectPropertySet margin = box.getMargin(getLayoutContext());
                    pt = new Point(
                            box.getAbsX() + (int)margin.left(),
                            box.getAbsY() + (int)margin.top());
                }
                scrollTo(pt);
                return;
            }
        }
        final HTMLResource resource = loadResource(url);
        final Document dom = resource.getDocument();
        setDocument(dom, resource.getURI());
    }


    /**
     * Reloads the document using the same base URL and namespace handler. Reloading will pick up changes to styles
     * within the document.
     *
     * @param URI A URI for the Document to load, for example, file.toURL().toExternalForm().
     */
    public void reloadDocument(String URI) {
        reloadDocument(loadDocument(URI));
    }

    /**
     * Reloads the document using the same base URL and namespace handler. Reloading will pick up changes to styles
     * within the document.
     *
     * @param doc The document to reload.
     */
    public void reloadDocument(Document doc) {
        if (this.doc == null) {
            XRLog.render("Reload called on BasicPanel, but there is no document set on the panel yet.");
            return;
        }
        ;
        this.doc = doc;
        setDocument(this.doc, getSharedContext().getBaseURL(), getSharedContext().getNamespaceHandler());
    }

    public URL getURL() {
        URL base = null;
        try {
            base = new URL(getSharedContext().getUac().getBaseURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return base;
    }

    public Document getDocument() {
        return doc;
    }

    /**
     * Returns the title as reported by the NamespaceHandler assigned to the SharedContext in this panel. For an HTML
     * document, this will be the contents of /html/head/title.
     *
     * @return the document title, or "" if the namespace handler cannot find a title, or if there is no current document
     * in the panel.
     */
    public String getDocumentTitle() {
        return doc == null ? "" : getSharedContext().getNamespaceHandler().getDocumentTitle(doc);
    }

    protected Document loadDocument(final String uri) {
        HTMLResource xmlResource = sharedContext.getUac().getXMLResource(uri);
        return xmlResource.getDocument();
    }

    protected HTMLResource loadResource(final String uri) {
        return sharedContext.getUac().getXMLResource(uri);
    }

    /* ====== hover and active utility methods
========= */

    public boolean isHover(Element e) {
        if (e == hovered_element) {
            return true;
        }
        return false;
    }

    public boolean isActive(Element e) {
        if (e == active_element) {
            return true;
        }
        return false;
    }

    public boolean isFocus(Element e) {
        if (e == focus_element) {
            return true;
        }
        return false;
    }


    /**
     * Returns whether the background of this <code>BasicPanel</code> will
     * be painted when it is rendered.
     *
     * @return <code>true</code> if the background of this
     *         <code>BasicPanel</code> will be painted, <code>false</code> if it
     *         will not.
     */
    public boolean isOpaque() {
        checkOpacityMethodClient();
        return explicitlyOpaque;
    }

    /**
     * Specifies whether the background of this <code>BasicPanel</code> will
     * be painted when it is rendered.
     *
     * @param opaque <code>true</code> if the background of this
     *               <code>BasicPanel</code> should be painted, <code>false</code> if it
     *               should not.
     */
    public void setOpaque(boolean opaque) {
        checkOpacityMethodClient();
        explicitlyOpaque = opaque;
    }

    /**
     * Checks that the calling method of the method that calls this method is not in this class
     * and throws a RuntimeException if it was. This is used to ensure that parts of this class that
     * use the opacity to indicate something other than whether the background is painted do not
     * interfere with the user's intentions regarding the background painting.
     *
     * @throws IllegalStateException if the method that called this method was itself called by a
     *                               method in this same class.
     */
    private void checkOpacityMethodClient() {
        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length > 2) {
            String callingClassName = stackTrace[2].getClassName();
            if (BasicPanel.class.getName().equals(callingClassName))
                throw new IllegalStateException("BasicPanel should not use its own opacity methods. Use " +
                        "super.isOpaque()/setOpaque() instead.");
        }
    }

    public SharedContext getSharedContext() {
        return sharedContext;
    }

    public Rectangle getFixedRectangle() {
        if (enclosingScrollPane != null) {
            return enclosingScrollPane.getViewportBorderBounds();
        } else {
            Dimension dim = getSize();
            return new Rectangle(0, 0, dim.width, dim.height);
        }
    }

    private boolean isAnchorInCurrentDocument(String str) {
        return str.charAt(0) == '#';
    }

    private String getAnchorId(String url) {
        return url.substring(1, url.length());
    }

    /**
     * Scroll the panel to make the specified point be on screen. Typically
     * this will scroll the screen down to the y component of the point.
     */
    public void scrollTo(Point pt) {
        if (this.enclosingScrollPane != null) {
            this.enclosingScrollPane.getVerticalScrollBar().setValue(pt.y);
        }
    }


    public boolean isInteractive() {
        return this.getSharedContext().isInteractive();
    }

    public void setInteractive(boolean interactive) {
        this.getSharedContext().setInteractive(interactive);
    }

    public void addMouseTrackingListener(FSMouseListener l) {
        mouseTracker.addListener(l);
    }

    public void removeMouseTrackingListener(FSMouseListener l) {
        mouseTracker.removeListener(l);
    }

    public List<FSMouseListener> getMouseTrackingListeners() {
        return mouseTracker.getListeners();
    }

    protected void resetMouseTracker() {
        mouseTracker.reset();
    }

    public boolean isCenteredPagedView() {
        return centeredPagedView;
    }

    public void setCenteredPagedView(boolean centeredPagedView) {
        this.centeredPagedView = centeredPagedView;
    }
    public void submit(String url) {
        formSubmissionListener.submit(url);
    }
    public void setFormSubmissionListener(FormSubmissionListener fsl) {
        this.formSubmissionListener =fsl;
        sharedContext.setFormSubmissionListener(formSubmissionListener);
    }
}

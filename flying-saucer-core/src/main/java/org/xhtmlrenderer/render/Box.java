/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2005, 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.render;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.xhtmlrenderer.layout.Layer;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.PaintingInfo;
import org.xhtmlrenderer.layout.Styleable;
import org.xhtmlrenderer.util.XRLog;

public abstract class Box implements Styleable {
    protected static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private Element _element;

    private int _x;
    private int _y;

    private int _absY;
    private int _absX;

    /**
     * Box width.
     */
    private int _contentWidth;
    private int _rightMBP = 0;
    private int _leftMBP = 0;

    /**
     * Box height.
     */
    private int _height;

    private Layer _layer = null;
    private Layer _containingLayer;

    private Box _parent;

    private List<Box> _boxes;

    /**
     * Keeps track of the start of childrens containing block.
     */
    private int _tx;
    private int _ty;

    private CalculatedStyle _style;
    private Box _containingBlock;

    private Dimension _relativeOffset;

    private PaintingInfo _paintingInfo;

    private RectPropertySet _workingMargin;

    private int _index;

    private String _pseudoElementOrClass;

    private boolean _anonymous;

    protected Box() {
    }

    public abstract String dump(LayoutContext c, String indent, int which);

    protected void dumpBoxes(
            final LayoutContext c, final String indent, final List<Box> boxes,
            final int which, final StringBuilder result) {
        for (final Iterator<Box> i = boxes.iterator(); i.hasNext(); ) {
            final Box b = i.next();
            result.append(b.dump(c, indent + "  ", which));
            if (i.hasNext()) {
                result.append('\n');
            }
        }
    }

    public int getWidth() {
        return getContentWidth() + getLeftMBP() + getRightMBP();
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Box: ");
        sb.append(" (" + getAbsX() + "," + getAbsY() + ")->(" + getWidth() + " x " + getHeight() + ")");
        return sb.toString();
    }

    public void addChildForLayout(final LayoutContext c, final Box child) {
        addChild(child);

        child.initContainingLayer(c);
    }

    public void addChild(final Box child) {
        if (_boxes == null) {
            _boxes = new ArrayList<Box>();
        }
        if (child == null) {
            throw new NullPointerException("trying to add null child");
        }
        child.setParent(this);
        child.setIndex(_boxes.size());
        _boxes.add(child);
    }

    public void addAllChildren(final List<Box> children) {
        for (final Iterator<Box> i = children.iterator(); i.hasNext(); ) {
            final Box box = (Box)i.next();
            addChild(box);
        }
    }

    public void removeAllChildren() {
        if (_boxes != null) {
            _boxes.clear();
        }
    }

    public void removeChild(final Box target) {
        if (_boxes != null) {
            boolean found = false;
            for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
                final Box child = (Box)i.next();
                if (child.equals(target)) {
                    i.remove();
                    found = true;
                } else if (found) {
                    child.setIndex(child.getIndex()-1);
                }
            }
        }
    }

    public Box getPreviousSibling() {
        final Box parent = getParent();
        return parent == null ? null : parent.getPrevious(this);
    }

    public Box getNextSibling() {
        final Box parent = getParent();
        return parent == null ? null : parent.getNext(this);
    }

    protected Box getPrevious(final Box child) {
        return child.getIndex() == 0 ? null : getChild(child.getIndex()-1);
    }

    protected Box getNext(final Box child) {
        return child.getIndex() == getChildCount() - 1 ? null : getChild(child.getIndex()+1);
    }

    public void removeChild(final int i) {
        if (_boxes != null) {
            removeChild(getChild(i));
        }
    }

    public void setParent(final Box box) {
        _parent = box;
    }

    public Box getParent() {
        return _parent;
    }

    public Box getDocumentParent() {
        return getParent();
    }

    public int getChildCount() {
        return _boxes == null ? 0 : _boxes.size();
    }

    public Box getChild(final int i) {
        if (_boxes == null) {
            throw new IndexOutOfBoundsException();
        } else {
            return _boxes.get(i);
        }
    }

    public Iterator<Box> getChildIterator() {
        return _boxes == null ? Collections.<Box>emptyList().iterator() : _boxes.iterator();
    }

    public List<Box> getChildren() {
        return _boxes == null ? Collections.<Box>emptyList() : _boxes;
    }

    public static final int NOTHING = 0;
    public static final int FLUX = 1;
    public static final int CHILDREN_FLUX = 2;
    public static final int DONE = 3;

    private int _state = NOTHING;

    public static final int DUMP_RENDER = 2;

    public static final int DUMP_LAYOUT = 1;

    public synchronized int getState() {
        return _state;
    }

    public synchronized void setState(final int state) {
        _state = state;
    }

    public static String stateToString(final int state) {
        switch (state) {
            case NOTHING:
                return "NOTHING";
            case FLUX:
                return "FLUX";
            case CHILDREN_FLUX:
                return "CHILDREN_FLUX";
            case DONE:
                return "DONE";
            default:
                return "unknown";
        }
    }

    public final CalculatedStyle getStyle() {
        return _style;
    }

    public void setStyle(final CalculatedStyle style) {
        _style = style;
    }

    public Box getContainingBlock() {
        return _containingBlock == null ? getParent() : _containingBlock;
    }

    public void setContainingBlock(final Box containingBlock) {
        _containingBlock = containingBlock;
    }

    public Rectangle getMarginEdge(final int left, final int top, final CssContext cssCtx, final int tx, final int ty) {
        // Note that negative margins can mean this rectangle is inside the border
        // edge, but that's the way it's supposed to work...
        final Rectangle result = new Rectangle(left, top, getWidth(), getHeight());
        result.translate(tx, ty);
        return result;
    }

    public Rectangle getMarginEdge(final CssContext cssCtx, final int tx, final int ty) {
        return getMarginEdge(getX(), getY(), cssCtx, tx, ty);
    }

    public Rectangle getPaintingBorderEdge(final CssContext cssCtx) {
        return getBorderEdge(getAbsX(), getAbsY(), cssCtx);
    }

    public Rectangle getPaintingPaddingEdge(final CssContext cssCtx) {
        return getPaddingEdge(getAbsX(), getAbsY(), cssCtx);
    }

    public Rectangle getPaintingClipEdge(final CssContext cssCtx) {
        return getPaintingBorderEdge(cssCtx);
    }

    public Rectangle getChildrenClipEdge(final RenderingContext c) {
        return getPaintingPaddingEdge(c);
    }

    /**
     * <B>NOTE</B>: This method does not consider any children of this box
     */
    public boolean intersects(final CssContext cssCtx, final Shape clip) {
        return clip == null || clip.intersects(getPaintingClipEdge(cssCtx));
    }

    public Rectangle getBorderEdge(final int left, final int top, final CssContext cssCtx) {
        final RectPropertySet margin = getMargin(cssCtx);
        final Rectangle result = new Rectangle(left + (int) margin.left(),
                top + (int) margin.top(),
                getWidth() - (int) margin.left() - (int) margin.right(),
                getHeight() - (int) margin.top() - (int) margin.bottom());
        return result;
    }

    public Rectangle getPaddingEdge(final int left, final int top, final CssContext cssCtx) {
        final RectPropertySet margin = getMargin(cssCtx);
        final RectPropertySet border = getBorder(cssCtx);
        final Rectangle result = new Rectangle(left + (int) margin.left() + (int) border.left(),
                top + (int) margin.top() + (int) border.top(),
                getWidth() - (int) margin.width() - (int) border.width(),
                getHeight() - (int) margin.height() - (int) border.height());
        return result;
    }

    protected int getPaddingWidth(final CssContext cssCtx) {
        final RectPropertySet padding = getPadding(cssCtx);
        return (int)padding.left() + getContentWidth() + (int)padding.right();
    }

    public Rectangle getContentAreaEdge(final int left, final int top, final CssContext cssCtx) {
        final RectPropertySet margin = getMargin(cssCtx);
        final RectPropertySet border = getBorder(cssCtx);
        final RectPropertySet padding = getPadding(cssCtx);

        final Rectangle result = new Rectangle(
                left + (int)margin.left() + (int)border.left() + (int)padding.left(),
                top + (int)margin.top() + (int)border.top() + (int)padding.top(),
                getWidth() - (int)margin.width() - (int)border.width() - (int)padding.width(),
                getHeight() - (int) margin.height() - (int) border.height() - (int) padding.height());
        return result;
    }

    public Layer getLayer() {
        return _layer;
    }

    public void setLayer(final Layer layer) {
        _layer = layer;
    }

    public Dimension positionRelative(final CssContext cssCtx) {
        final int initialX = getX();
        final int initialY = getY();

        final CalculatedStyle style = getStyle();
        if (! style.isIdent(CSSName.LEFT, IdentValue.AUTO)) {
            setX(getX() + (int)style.getFloatPropertyProportionalWidth(
                    CSSName.LEFT, getContainingBlock().getContentWidth(), cssCtx));
        } else if (! style.isIdent(CSSName.RIGHT, IdentValue.AUTO)) {
            setX(getX() - (int)style.getFloatPropertyProportionalWidth(
                    CSSName.RIGHT, getContainingBlock().getContentWidth(), cssCtx));
        }

        int cbContentHeight = 0;
        if (! getContainingBlock().getStyle().isAutoHeight()) {
            final CalculatedStyle cbStyle = getContainingBlock().getStyle();
            cbContentHeight = (int)cbStyle.getFloatPropertyProportionalHeight(
                    CSSName.HEIGHT, 0, cssCtx);
        } else if (isInlineBlock()) {
            // FIXME Should be content height, not overall height
            cbContentHeight = getContainingBlock().getHeight();
        }

        if (!style.isIdent(CSSName.TOP, IdentValue.AUTO)) {
            setY(getY() + ((int)style.getFloatPropertyProportionalHeight(
                    CSSName.TOP, cbContentHeight, cssCtx)));
        } else if (!style.isIdent(CSSName.BOTTOM, IdentValue.AUTO)) {
            setY(getY() - ((int)style.getFloatPropertyProportionalHeight(
                    CSSName.BOTTOM, cbContentHeight, cssCtx)));
        }

        setRelativeOffset(new Dimension(getX() - initialX, getY() - initialY));
        return getRelativeOffset();
    }

    protected boolean isInlineBlock()
    {
        return false;
    }

    public void setAbsY(final int absY) {
        _absY = absY;
    }

    public int getAbsY() {
        return _absY;
    }

    public void setAbsX(final int absX) {
        _absX = absX;
    }

    public int getAbsX() {
        return _absX;
    }

    public boolean isStyled() {
        return _style != null;
    }

    public int getBorderSides() {
        return BorderPainter.ALL;
    }

    public void paintBorder(final RenderingContext c) {
        c.getOutputDevice().paintBorder(c, this);
    }

    private boolean isPaintsRootElementBackground() {
        return (isRoot() && getStyle().isHasBackground()) ||
                (isBody() && ! getParent().getStyle().isHasBackground());
    }

    public void paintBackground(final RenderingContext c) {
        if (! isPaintsRootElementBackground()) {
            c.getOutputDevice().paintBackground(c, this);
        }
    }

    public void paintRootElementBackground(final RenderingContext c) {
        final PaintingInfo pI = getPaintingInfo();
        if (pI != null) {
            if (getStyle().isHasBackground()) {
                paintRootElementBackground(c, pI);
            } else if (getChildCount() > 0) {
                final Box body = getChild(0);
                body.paintRootElementBackground(c, pI);
            }
        }
    }

    private void paintRootElementBackground(final RenderingContext c, final PaintingInfo pI) {
        final Dimension marginCorner = pI.getOuterMarginCorner();
        final Rectangle canvasBounds = new Rectangle(0, 0, marginCorner.width, marginCorner.height);
        canvasBounds.add(c.getViewportRectangle());
        c.getOutputDevice().paintBackground(c, getStyle(), canvasBounds, canvasBounds, null);
    }

    public Layer getContainingLayer() {
        return _containingLayer;
    }

    public void setContainingLayer(final Layer containingLayer) {
        _containingLayer = containingLayer;
    }

    public void initContainingLayer(final LayoutContext c) {
        if (getLayer() != null) {
            setContainingLayer(getLayer());
        } else if (getContainingLayer() == null) {
            if (getParent() == null || getParent().getContainingLayer() == null) {
                throw new RuntimeException("internal error");
            }
            setContainingLayer(getParent().getContainingLayer());

            // FIXME Will be glacially slow for large inline relative layers.  Could
            // be much more efficient.  We're just looking for block boxes which are
            // directly wrapped by an inline relative layer (i.e. block boxes sandwiched
            // between anonymous block boxes)
            if (c.getLayer().isInline()) {
                final List<Box> content =
                    ((InlineLayoutBox)c.getLayer().getMaster()).getElementWithContent();
                if (content.contains(this)) {
                    setContainingLayer(c.getLayer());
                }
            }
        }
    }

    public void connectChildrenToCurrentLayer(final LayoutContext c) {

        for (int i = 0; i < getChildCount(); i++) {
            final Box box = getChild(i);
            box.setContainingLayer(c.getLayer());
            box.connectChildrenToCurrentLayer(c);
        }
    }

    public List<Box> getElementBoxes(final Element elem) {
        final List<Box> result = new ArrayList<Box>();
        for (int i = 0; i < getChildCount(); i++) {
            final Box child = getChild(i);
            if (child.getElement() == elem) {
                result.add(child);
            }
            result.addAll(child.getElementBoxes(elem));
        }
        return result;
    }

    public void reset(final LayoutContext c) {
        resetChildren(c);
        if (_layer != null) {
            _layer.detach();
            _layer = null;
        }

        setContainingLayer(null);
        setLayer(null);
        setPaintingInfo(null);
        setContentWidth(0);

        _workingMargin = null;

        final String anchorName = c.getNamespaceHandler().getAnchorName(getElement());
        if (anchorName != null) {
            c.removeBoxId(anchorName);
        }

        final Element e = getElement();
        if (e != null) {
            final String id = c.getNamespaceHandler().getID(e);
            if (id != null) {
                c.removeBoxId(id);
            }
        }
    }

    public void detach(final LayoutContext c) {
        reset(c);

        if (getParent() != null) {
            getParent().removeChild(this);
            setParent(null);
        }
    }

    public void resetChildren(final LayoutContext c, final int start, final int end) {
        for (int i = start; i <= end; i++) {
            final Box box = getChild(i);
            box.reset(c);
        }
    }

    protected void resetChildren(final LayoutContext c) {
        final int remaining = getChildCount();
        for (int i = 0; i < remaining; i++) {
            final Box box = getChild(i);
            box.reset(c);
        }
    }

    public abstract void calcCanvasLocation();

    public void calcChildLocations() {
        for (int i = 0; i < getChildCount(); i++) {
            final Box child = getChild(i);
            child.calcCanvasLocation();
            child.calcChildLocations();
        }
    }

    public int forcePageBreakBefore(final LayoutContext c, final IdentValue pageBreakValue, final boolean pendingPageName) {
        PageBox page = c.getRootLayer().getFirstPage(c, this);
        if (page == null) {
            XRLog.layout(Level.WARNING, "Box has no page");
            return 0;
        } else {
            int pageBreakCount = 1;
            if (page.getTop() == getAbsY()) {
                pageBreakCount--;
                if (pendingPageName && page == c.getRootLayer().getLastPage()) {
                    c.getRootLayer().removeLastPage();
                    c.setPageName(c.getPendingPageName());
                    c.getRootLayer().addPage(c);
                }
            }
            if ((page.isLeftPage() && pageBreakValue == IdentValue.LEFT) ||
                    (page.isRightPage() && pageBreakValue == IdentValue.RIGHT)) {
                pageBreakCount++;
            }

            if (pageBreakCount == 0) {
                return 0;
            }

            if (pageBreakCount == 1 && pendingPageName) {
                c.setPageName(c.getPendingPageName());
            }

            int delta = page.getBottom() + c.getExtraSpaceTop() - getAbsY();
            if (page == c.getRootLayer().getLastPage()) {
                c.getRootLayer().addPage(c);
            }

            if (pageBreakCount == 2) {
                page = (PageBox)c.getRootLayer().getPages().get(page.getPageNo()+1);
                delta += page.getContentHeight(c);

                if (pageBreakCount == 2 && pendingPageName) {
                    c.setPageName(c.getPendingPageName());
                }

                if (page == c.getRootLayer().getLastPage()) {
                    c.getRootLayer().addPage(c);
                }
            }

            setY(getY() + delta);

            return delta;
        }
    }

    public void forcePageBreakAfter(final LayoutContext c, final IdentValue pageBreakValue) {
        boolean needSecondPageBreak = false;
        PageBox page = c.getRootLayer().getLastPage(c, this);

        if ((page.isLeftPage() && pageBreakValue == IdentValue.LEFT) ||
                (page.isRightPage() && pageBreakValue == IdentValue.RIGHT)) {
            needSecondPageBreak = true;
        }

        int delta = page.getBottom() + c.getExtraSpaceTop() - (getAbsY() +
                getMarginBorderPadding(c, CalculatedStyle.TOP) + getHeight());

        if (page == c.getRootLayer().getLastPage()) {
            c.getRootLayer().addPage(c);
        }

        if (needSecondPageBreak) {
            page = (PageBox)c.getRootLayer().getPages().get(page.getPageNo()+1);
            delta += page.getContentHeight(c);

            if (page == c.getRootLayer().getLastPage()) {
                c.getRootLayer().addPage(c);
            }
        }

        setHeight(getHeight() + delta);
    }

    public boolean crossesPageBreak(final LayoutContext c) {
        if (! c.isPageBreaksAllowed()) {
            return false;
        }

        final PageBox pageBox = c.getRootLayer().getFirstPage(c, this);
        if (pageBox == null) {
            return false;
        } else {
            return getAbsY() + getHeight() >= pageBox.getBottom() - c.getExtraSpaceBottom();
        }
    }

    public Dimension getRelativeOffset() {
        return _relativeOffset;
    }

    public void setRelativeOffset(final Dimension relativeOffset) {
        _relativeOffset = relativeOffset;
    }

    public Box find(final CssContext cssCtx, final int absX, final int absY, final boolean findAnonymous) {
        final PaintingInfo pI = getPaintingInfo();
        if (pI != null && ! pI.getAggregateBounds().contains(absX, absY)) {
            return null;
        }

        Box result = null;
        for (int i = 0; i < getChildCount(); i++) {
            final Box child = getChild(i);
            result = child.find(cssCtx, absX, absY, findAnonymous);
            if (result != null) {
                return result;
            }
        }

        final Rectangle edge = getContentAreaEdge(getAbsX(), getAbsY(), cssCtx);
        return edge.contains(absX, absY) && getStyle().isVisible() ? this : null;
    }

    public boolean isRoot() {
        return getElement() != null && ! isAnonymous() && (getElement() instanceof Document);
    }

    public boolean isBody() {
        return getParent() != null && getParent().isRoot();
    }

    public Element getElement() {
        return _element;
    }

    public void setElement(final Element element) {
        _element = element;
    }

    public void setMarginTop(final CssContext cssContext, final int marginTop) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setTop(marginTop);
    }

    public void setMarginBottom(final CssContext cssContext, final int marginBottom) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setBottom(marginBottom);
    }

    public void setMarginLeft(final CssContext cssContext, final int marginLeft) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setLeft(marginLeft);
    }

    public void setMarginRight(final CssContext cssContext, final int marginRight) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setRight(marginRight);
    }

    private void ensureWorkingMargin(final CssContext cssContext) {
        if (_workingMargin == null) {
            _workingMargin = getStyleMargin(cssContext).copyOf();
        }
    }

    public RectPropertySet getMargin(final CssContext cssContext) {
        return _workingMargin != null ? _workingMargin : getStyleMargin(cssContext);
    }

    protected RectPropertySet getStyleMargin(final CssContext cssContext) {
        return getStyle().getMarginRect(getContainingBlockWidth(), cssContext);
    }

    protected RectPropertySet getStyleMargin(final CssContext cssContext, final boolean useCache) {
        return getStyle().getMarginRect(getContainingBlockWidth(), cssContext, useCache);
    }

    public RectPropertySet getPadding(final CssContext cssCtx) {
        return getStyle().getPaddingRect(getContainingBlockWidth(), cssCtx);
    }

    public BorderPropertySet getBorder(final CssContext cssCtx) {
        return getStyle().getBorder(cssCtx);
    }

    protected int getContainingBlockWidth() {
        return getContainingBlock().getContentWidth();
    }

    protected void resetTopMargin(final CssContext cssContext) {
        if (_workingMargin != null) {
            final RectPropertySet styleMargin = getStyleMargin(cssContext);

            _workingMargin.setTop(styleMargin.top());
        }
    }

    public void clearSelection(final List<Box> modified) {
        for (int i = 0; i < getChildCount(); i++) {
            final Box child = getChild(i);
            child.clearSelection(modified);
        }
    }

    public void selectAll() {
        for (int i = 0; i < getChildCount(); i++) {
            final Box child = getChild(i);
            child.selectAll();
        }
    }

    public PaintingInfo calcPaintingInfo(final CssContext c, final boolean useCache) {
        final PaintingInfo cached = getPaintingInfo();
        if (cached != null && useCache) {
            return cached;
        }

        final PaintingInfo result = new PaintingInfo();

        final Rectangle bounds = getMarginEdge(getAbsX(), getAbsY(), c, 0, 0);
        result.setOuterMarginCorner(
            new Dimension(bounds.x + bounds.width, bounds.y + bounds.height));

        result.setAggregateBounds(getPaintingClipEdge(c));

        if (!getStyle().isOverflowApplies() || getStyle().isOverflowVisible()) {
            calcChildPaintingInfo(c, result, useCache);
        }

        setPaintingInfo(result);

        return result;
    }

    protected void calcChildPaintingInfo(
            final CssContext c, final PaintingInfo result, final boolean useCache) {
        for (int i = 0; i < getChildCount(); i++) {
            final Box child = getChild(i);
            final PaintingInfo info = child.calcPaintingInfo(c, useCache);
            moveIfGreater(result.getOuterMarginCorner(), info.getOuterMarginCorner());
            result.getAggregateBounds().add(info.getAggregateBounds());
        }
    }

    public int getMarginBorderPadding(final CssContext cssCtx, final int which) {
        final BorderPropertySet border = getBorder(cssCtx);
        final RectPropertySet margin = getMargin(cssCtx);
        final RectPropertySet padding = getPadding(cssCtx);

        switch (which) {
            case CalculatedStyle.LEFT:
                return (int)(margin.left() + border.left() + padding.left());
            case CalculatedStyle.RIGHT:
                return (int)(margin.right() + border.right() + padding.right());
            case CalculatedStyle.TOP:
                return (int)(margin.top() + border.top() + padding.top());
            case CalculatedStyle.BOTTOM:
                return (int)(margin.bottom() + border.bottom() + padding.bottom());
            default:
                throw new IllegalArgumentException();
        }
    }

    protected void moveIfGreater(final Dimension result, final Dimension test) {
        if (test.width > result.width) {
            result.width = test.width;
        }
        if (test.height > result.height) {
            result.height = test.height;
        }
    }

    public void restyle(final LayoutContext c) {
        Element e = getElement();
        CalculatedStyle style = null;

        final String pe = getPseudoElementOrClass();
        if (pe != null) {
            if (e != null) {
                style = c.getSharedContext().getStyle(e, true);
                style = style.deriveStyle(c.getCss().getPseudoElementStyle(e, pe));
            } else {
                final BlockBox container = (BlockBox)getParent().getParent();
                e = container.getElement();
                style = c.getSharedContext().getStyle(e, true);
                style = style.deriveStyle(c.getCss().getPseudoElementStyle(e, pe));
                style = style.createAnonymousStyle(IdentValue.INLINE);
            }
        } else {
            if (e != null) {
                style = c.getSharedContext().getStyle(e, true);
                if (isAnonymous()) {
                    style = style.createAnonymousStyle(getStyle().getIdent(CSSName.DISPLAY));
                }
            } else {
                final Box parent = getParent();
                if (parent != null) {
                    e = parent.getElement();
                    if (e != null) {
                        style = c.getSharedContext().getStyle(e, true);
                        style = style.createAnonymousStyle(IdentValue.INLINE);
                    }
                }
            }
        }

        if (style != null) {
            setStyle(style);
        }

        restyleChildren(c);
    }

    protected void restyleChildren(final LayoutContext c) {
        for (int i = 0; i < getChildCount(); i++) {
            final Box b = getChild(i);
            b.restyle(c);
        }
    }

    public Box getRestyleTarget() {
        return this;
    }

    protected int getIndex() {
        return _index;
    }

    protected void setIndex(final int index) {
        _index = index;
    }

    public String getPseudoElementOrClass() {
        return _pseudoElementOrClass;
    }

    public void setPseudoElementOrClass(final String pseudoElementOrClass) {
        _pseudoElementOrClass = pseudoElementOrClass;
    }

    public void setX(final int x) {
        _x = x;
    }

    public int getX() {
        return _x;
    }

    public void setY(final int y) {
        _y = y;
    }

    public int getY() {
        return _y;
    }

    public void setTy(final int ty) {
        _ty = ty;
    }

    public int getTy() {
        return _ty;
    }

    public void setTx(final int tx) {
        _tx = tx;
    }

    public int getTx() {
        return _tx;
    }

    public void setRightMBP(final int rightMBP) {
        _rightMBP = rightMBP;
    }

    public int getRightMBP() {
        return _rightMBP;
    }

    public void setLeftMBP(final int leftMBP) {
        _leftMBP = leftMBP;
    }

    public int getLeftMBP() {
        return _leftMBP;
    }

    public void setHeight(final int height) {
        _height = height;
    }

    public int getHeight() {
        return _height;
    }

    public void setContentWidth(final int contentWidth) {
        _contentWidth = contentWidth < 0 ? 0 : contentWidth;
    }

    public int getContentWidth() {
        return _contentWidth;
    }

    public PaintingInfo getPaintingInfo() {
        return _paintingInfo;
    }

    private void setPaintingInfo(final PaintingInfo paintingInfo) {
        _paintingInfo = paintingInfo;
    }

    public boolean isAnonymous() {
        return _anonymous;
    }

    public void setAnonymous(final boolean anonymous) {
        _anonymous = anonymous;
    }

    public BoxDimensions getBoxDimensions() {
        final BoxDimensions result = new BoxDimensions();

        result.setLeftMBP(getLeftMBP());
        result.setRightMBP(getRightMBP());
        result.setContentWidth(getContentWidth());
        result.setHeight(getHeight());

        return result;
    }

    public void setBoxDimensions(final BoxDimensions dimensions) {
        setLeftMBP(dimensions.getLeftMBP());
        setRightMBP(dimensions.getRightMBP());
        setContentWidth(dimensions.getContentWidth());
        setHeight(dimensions.getHeight());
    }

    public void collectText(final RenderingContext c, final StringBuffer buffer) throws IOException {
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final Box b = (Box)i.next();
            b.collectText(c, buffer);
        }
    }

    public void exportText(final RenderingContext c, final Writer writer) throws IOException {
        if (c.isPrint() && isRoot()) {
            c.setPage(0, (PageBox)c.getRootLayer().getPages().get(0));
            c.getPage().exportLeadingText(c, writer);
        }
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final Box b = (Box)i.next();
            b.exportText(c, writer);
        }
        if (c.isPrint() && isRoot()) {
            exportPageBoxText(c, writer);
        }
    }

    private void exportPageBoxText(final RenderingContext c, final Writer writer) throws IOException {
        c.getPage().exportTrailingText(c, writer);
        if (c.getPage() != c.getRootLayer().getLastPage()) {
            final List<PageBox> pages = c.getRootLayer().getPages();
            do {
                final PageBox next = (PageBox)pages.get(c.getPageNo()+1);
                c.setPage(next.getPageNo(), next);
                next.exportLeadingText(c, writer);
                next.exportTrailingText(c, writer);
            } while (c.getPage() != c.getRootLayer().getLastPage());
        }
    }

    protected void exportPageBoxText(final RenderingContext c, final Writer writer, final int yPos) throws IOException {
        c.getPage().exportTrailingText(c, writer);
        final List<PageBox> pages = c.getRootLayer().getPages();
        PageBox next = (PageBox)pages.get(c.getPageNo()+1);
        c.setPage(next.getPageNo(), next);
        while (next.getBottom() < yPos) {
            next.exportLeadingText(c, writer);
            next.exportTrailingText(c, writer);
            next = (PageBox)pages.get(c.getPageNo()+1);
            c.setPage(next.getPageNo(), next);
        }
        next.exportLeadingText(c, writer);
    }

    public boolean isInDocumentFlow() {
        Box flowRoot = this;
        while (true) {
            final Box parent = flowRoot.getParent();
            if (parent == null) {
                break;
            } else {
                flowRoot = parent;
            }
        }

        return flowRoot.isRoot();
    }

    public void analyzePageBreaks(final LayoutContext c, final ContentLimitContainer container) {
        container.updateTop(c, getAbsY());
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final Box b = (Box)i.next();
            b.analyzePageBreaks(c, container);
        }
        container.updateBottom(c, getAbsY() + getHeight());
    }

    public FSColor getEffBackgroundColor(final RenderingContext c) {
        FSColor result = null;
        Box current = this;
        while (current != null) {
            result = current.getStyle().getBackgroundColor();
            if (result != null) {
                return result;
            }

            current = current.getContainingBlock();
        }

        final PageBox page = c.getPage();
        result = page.getStyle().getBackgroundColor();
        if (result == null) {
            return new FSRGBColor(255, 255, 255);
        } else {
            return result;
        }
    }

    protected boolean isMarginAreaRoot() {
        return false;
    }

    public boolean isContainedInMarginBox() {
        Box current = this;
        while (true) {
            final Box parent = current.getParent();
            if (parent == null) {
                break;
            } else {
                current = parent;
            }
        }

        return current.isMarginAreaRoot();
    }

    public int getEffectiveWidth() {
        return getWidth();
    }

    protected boolean isInitialContainingBlock() {
        return false;
    }
}

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
            LayoutContext c, String indent, List<Box> boxes,
            int which, StringBuilder result) {
        for (Iterator<Box> i = boxes.iterator(); i.hasNext(); ) {
            Box b = i.next();
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
        StringBuffer sb = new StringBuffer();
        sb.append("Box: ");
        sb.append(" (" + getAbsX() + "," + getAbsY() + ")->(" + getWidth() + " x " + getHeight() + ")");
        return sb.toString();
    }

    public void addChildForLayout(LayoutContext c, Box child) {
        addChild(child);

        child.initContainingLayer(c);
    }

    public void addChild(Box child) {
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

    public void addAllChildren(List<Box> children) {
        for (Iterator<Box> i = children.iterator(); i.hasNext(); ) {
            Box box = (Box)i.next();
            addChild(box);
        }
    }

    public void removeAllChildren() {
        if (_boxes != null) {
            _boxes.clear();
        }
    }

    public void removeChild(Box target) {
        if (_boxes != null) {
            boolean found = false;
            for (Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
                Box child = (Box)i.next();
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
        Box parent = getParent();
        return parent == null ? null : parent.getPrevious(this);
    }

    public Box getNextSibling() {
        Box parent = getParent();
        return parent == null ? null : parent.getNext(this);
    }

    protected Box getPrevious(Box child) {
        return child.getIndex() == 0 ? null : getChild(child.getIndex()-1);
    }

    protected Box getNext(Box child) {
        return child.getIndex() == getChildCount() - 1 ? null : getChild(child.getIndex()+1);
    }

    public void removeChild(int i) {
        if (_boxes != null) {
            removeChild(getChild(i));
        }
    }

    public void setParent(Box box) {
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

    public Box getChild(int i) {
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

    public synchronized void setState(int state) {
        _state = state;
    }

    public static String stateToString(int state) {
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

    public void setStyle(CalculatedStyle style) {
        _style = style;
    }

    public Box getContainingBlock() {
        return _containingBlock == null ? getParent() : _containingBlock;
    }

    public void setContainingBlock(Box containingBlock) {
        _containingBlock = containingBlock;
    }

    public Rectangle getMarginEdge(int left, int top, CssContext cssCtx, int tx, int ty) {
        // Note that negative margins can mean this rectangle is inside the border
        // edge, but that's the way it's supposed to work...
        Rectangle result = new Rectangle(left, top, getWidth(), getHeight());
        result.translate(tx, ty);
        return result;
    }

    public Rectangle getMarginEdge(CssContext cssCtx, int tx, int ty) {
        return getMarginEdge(getX(), getY(), cssCtx, tx, ty);
    }

    public Rectangle getPaintingBorderEdge(CssContext cssCtx) {
        return getBorderEdge(getAbsX(), getAbsY(), cssCtx);
    }

    public Rectangle getPaintingPaddingEdge(CssContext cssCtx) {
        return getPaddingEdge(getAbsX(), getAbsY(), cssCtx);
    }

    public Rectangle getPaintingClipEdge(CssContext cssCtx) {
        return getPaintingBorderEdge(cssCtx);
    }

    public Rectangle getChildrenClipEdge(RenderingContext c) {
        return getPaintingPaddingEdge(c);
    }

    /**
     * <B>NOTE</B>: This method does not consider any children of this box
     */
    public boolean intersects(CssContext cssCtx, Shape clip) {
        return clip == null || clip.intersects(getPaintingClipEdge(cssCtx));
    }

    public Rectangle getBorderEdge(int left, int top, CssContext cssCtx) {
        RectPropertySet margin = getMargin(cssCtx);
        Rectangle result = new Rectangle(left + (int) margin.left(),
                top + (int) margin.top(),
                getWidth() - (int) margin.left() - (int) margin.right(),
                getHeight() - (int) margin.top() - (int) margin.bottom());
        return result;
    }

    public Rectangle getPaddingEdge(int left, int top, CssContext cssCtx) {
        RectPropertySet margin = getMargin(cssCtx);
        RectPropertySet border = getBorder(cssCtx);
        Rectangle result = new Rectangle(left + (int) margin.left() + (int) border.left(),
                top + (int) margin.top() + (int) border.top(),
                getWidth() - (int) margin.width() - (int) border.width(),
                getHeight() - (int) margin.height() - (int) border.height());
        return result;
    }

    protected int getPaddingWidth(CssContext cssCtx) {
        RectPropertySet padding = getPadding(cssCtx);
        return (int)padding.left() + getContentWidth() + (int)padding.right();
    }

    public Rectangle getContentAreaEdge(int left, int top, CssContext cssCtx) {
        RectPropertySet margin = getMargin(cssCtx);
        RectPropertySet border = getBorder(cssCtx);
        RectPropertySet padding = getPadding(cssCtx);

        Rectangle result = new Rectangle(
                left + (int)margin.left() + (int)border.left() + (int)padding.left(),
                top + (int)margin.top() + (int)border.top() + (int)padding.top(),
                getWidth() - (int)margin.width() - (int)border.width() - (int)padding.width(),
                getHeight() - (int) margin.height() - (int) border.height() - (int) padding.height());
        return result;
    }

    public Layer getLayer() {
        return _layer;
    }

    public void setLayer(Layer layer) {
        _layer = layer;
    }

    public Dimension positionRelative(CssContext cssCtx) {
        int initialX = getX();
        int initialY = getY();

        CalculatedStyle style = getStyle();
        if (! style.isIdent(CSSName.LEFT, IdentValue.AUTO)) {
            setX(getX() + (int)style.getFloatPropertyProportionalWidth(
                    CSSName.LEFT, getContainingBlock().getContentWidth(), cssCtx));
        } else if (! style.isIdent(CSSName.RIGHT, IdentValue.AUTO)) {
            setX(getX() - (int)style.getFloatPropertyProportionalWidth(
                    CSSName.RIGHT, getContainingBlock().getContentWidth(), cssCtx));
        }

        int cbContentHeight = 0;
        if (! getContainingBlock().getStyle().isAutoHeight()) {
            CalculatedStyle cbStyle = getContainingBlock().getStyle();
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

    public void setAbsY(int absY) {
        _absY = absY;
    }

    public int getAbsY() {
        return _absY;
    }

    public void setAbsX(int absX) {
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

    public void paintBorder(RenderingContext c) {
        c.getOutputDevice().paintBorder(c, this);
    }

    private boolean isPaintsRootElementBackground() {
        return (isRoot() && getStyle().isHasBackground()) ||
                (isBody() && ! getParent().getStyle().isHasBackground());
    }

    public void paintBackground(RenderingContext c) {
        if (! isPaintsRootElementBackground()) {
            c.getOutputDevice().paintBackground(c, this);
        }
    }

    public void paintRootElementBackground(RenderingContext c) {
        PaintingInfo pI = getPaintingInfo();
        if (pI != null) {
            if (getStyle().isHasBackground()) {
                paintRootElementBackground(c, pI);
            } else if (getChildCount() > 0) {
                Box body = getChild(0);
                body.paintRootElementBackground(c, pI);
            }
        }
    }

    private void paintRootElementBackground(RenderingContext c, PaintingInfo pI) {
        Dimension marginCorner = pI.getOuterMarginCorner();
        Rectangle canvasBounds = new Rectangle(0, 0, marginCorner.width, marginCorner.height);
        canvasBounds.add(c.getViewportRectangle());
        c.getOutputDevice().paintBackground(c, getStyle(), canvasBounds, canvasBounds, null);
    }

    public Layer getContainingLayer() {
        return _containingLayer;
    }

    public void setContainingLayer(Layer containingLayer) {
        _containingLayer = containingLayer;
    }

    public void initContainingLayer(LayoutContext c) {
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
                List<Box> content =
                    ((InlineLayoutBox)c.getLayer().getMaster()).getElementWithContent();
                if (content.contains(this)) {
                    setContainingLayer(c.getLayer());
                }
            }
        }
    }

    public void connectChildrenToCurrentLayer(LayoutContext c) {

        for (int i = 0; i < getChildCount(); i++) {
            Box box = getChild(i);
            box.setContainingLayer(c.getLayer());
            box.connectChildrenToCurrentLayer(c);
        }
    }

    public List<Box> getElementBoxes(Element elem) {
        List<Box> result = new ArrayList<Box>();
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            if (child.getElement() == elem) {
                result.add(child);
            }
            result.addAll(child.getElementBoxes(elem));
        }
        return result;
    }

    public void reset(LayoutContext c) {
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

        String anchorName = c.getNamespaceHandler().getAnchorName(getElement());
        if (anchorName != null) {
            c.removeBoxId(anchorName);
        }

        Element e = getElement();
        if (e != null) {
            String id = c.getNamespaceHandler().getID(e);
            if (id != null) {
                c.removeBoxId(id);
            }
        }
    }

    public void detach(LayoutContext c) {
        reset(c);

        if (getParent() != null) {
            getParent().removeChild(this);
            setParent(null);
        }
    }

    public void resetChildren(LayoutContext c, int start, int end) {
        for (int i = start; i <= end; i++) {
            Box box = getChild(i);
            box.reset(c);
        }
    }

    protected void resetChildren(LayoutContext c) {
        int remaining = getChildCount();
        for (int i = 0; i < remaining; i++) {
            Box box = getChild(i);
            box.reset(c);
        }
    }

    public abstract void calcCanvasLocation();

    public void calcChildLocations() {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            child.calcCanvasLocation();
            child.calcChildLocations();
        }
    }

    public int forcePageBreakBefore(LayoutContext c, IdentValue pageBreakValue, boolean pendingPageName) {
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

    public void forcePageBreakAfter(LayoutContext c, IdentValue pageBreakValue) {
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

    public boolean crossesPageBreak(LayoutContext c) {
        if (! c.isPageBreaksAllowed()) {
            return false;
        }

        PageBox pageBox = c.getRootLayer().getFirstPage(c, this);
        if (pageBox == null) {
            return false;
        } else {
            return getAbsY() + getHeight() >= pageBox.getBottom() - c.getExtraSpaceBottom();
        }
    }

    public Dimension getRelativeOffset() {
        return _relativeOffset;
    }

    public void setRelativeOffset(Dimension relativeOffset) {
        _relativeOffset = relativeOffset;
    }

    public Box find(CssContext cssCtx, int absX, int absY, boolean findAnonymous) {
        PaintingInfo pI = getPaintingInfo();
        if (pI != null && ! pI.getAggregateBounds().contains(absX, absY)) {
            return null;
        }

        Box result = null;
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            result = child.find(cssCtx, absX, absY, findAnonymous);
            if (result != null) {
                return result;
            }
        }

        Rectangle edge = getContentAreaEdge(getAbsX(), getAbsY(), cssCtx);
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

    public void setElement(Element element) {
        _element = element;
    }

    public void setMarginTop(CssContext cssContext, int marginTop) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setTop(marginTop);
    }

    public void setMarginBottom(CssContext cssContext, int marginBottom) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setBottom(marginBottom);
    }

    public void setMarginLeft(CssContext cssContext, int marginLeft) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setLeft(marginLeft);
    }

    public void setMarginRight(CssContext cssContext, int marginRight) {
        ensureWorkingMargin(cssContext);
        _workingMargin.setRight(marginRight);
    }

    private void ensureWorkingMargin(CssContext cssContext) {
        if (_workingMargin == null) {
            _workingMargin = getStyleMargin(cssContext).copyOf();
        }
    }

    public RectPropertySet getMargin(CssContext cssContext) {
        return _workingMargin != null ? _workingMargin : getStyleMargin(cssContext);
    }

    protected RectPropertySet getStyleMargin(CssContext cssContext) {
        return getStyle().getMarginRect(getContainingBlockWidth(), cssContext);
    }

    protected RectPropertySet getStyleMargin(CssContext cssContext, boolean useCache) {
        return getStyle().getMarginRect(getContainingBlockWidth(), cssContext, useCache);
    }

    public RectPropertySet getPadding(CssContext cssCtx) {
        return getStyle().getPaddingRect(getContainingBlockWidth(), cssCtx);
    }

    public BorderPropertySet getBorder(CssContext cssCtx) {
        return getStyle().getBorder(cssCtx);
    }

    protected int getContainingBlockWidth() {
        return getContainingBlock().getContentWidth();
    }

    protected void resetTopMargin(CssContext cssContext) {
        if (_workingMargin != null) {
            RectPropertySet styleMargin = getStyleMargin(cssContext);

            _workingMargin.setTop(styleMargin.top());
        }
    }

    public void clearSelection(List<Box> modified) {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            child.clearSelection(modified);
        }
    }

    public void selectAll() {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            child.selectAll();
        }
    }

    public PaintingInfo calcPaintingInfo(CssContext c, boolean useCache) {
        PaintingInfo cached = getPaintingInfo();
        if (cached != null && useCache) {
            return cached;
        }

        final PaintingInfo result = new PaintingInfo();

        Rectangle bounds = getMarginEdge(getAbsX(), getAbsY(), c, 0, 0);
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
            CssContext c, PaintingInfo result, boolean useCache) {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            PaintingInfo info = child.calcPaintingInfo(c, useCache);
            moveIfGreater(result.getOuterMarginCorner(), info.getOuterMarginCorner());
            result.getAggregateBounds().add(info.getAggregateBounds());
        }
    }

    public int getMarginBorderPadding(CssContext cssCtx, int which) {
        BorderPropertySet border = getBorder(cssCtx);
        RectPropertySet margin = getMargin(cssCtx);
        RectPropertySet padding = getPadding(cssCtx);

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

    protected void moveIfGreater(Dimension result, Dimension test) {
        if (test.width > result.width) {
            result.width = test.width;
        }
        if (test.height > result.height) {
            result.height = test.height;
        }
    }

    public void restyle(LayoutContext c) {
        Element e = getElement();
        CalculatedStyle style = null;

        String pe = getPseudoElementOrClass();
        if (pe != null) {
            if (e != null) {
                style = c.getSharedContext().getStyle(e, true);
                style = style.deriveStyle(c.getCss().getPseudoElementStyle(e, pe));
            } else {
                BlockBox container = (BlockBox)getParent().getParent();
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
                Box parent = getParent();
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

    protected void restyleChildren(LayoutContext c) {
        for (int i = 0; i < getChildCount(); i++) {
            Box b = getChild(i);
            b.restyle(c);
        }
    }

    public Box getRestyleTarget() {
        return this;
    }

    protected int getIndex() {
        return _index;
    }

    protected void setIndex(int index) {
        _index = index;
    }

    public String getPseudoElementOrClass() {
        return _pseudoElementOrClass;
    }

    public void setPseudoElementOrClass(String pseudoElementOrClass) {
        _pseudoElementOrClass = pseudoElementOrClass;
    }

    public void setX(int x) {
        _x = x;
    }

    public int getX() {
        return _x;
    }

    public void setY(int y) {
        _y = y;
    }

    public int getY() {
        return _y;
    }

    public void setTy(int ty) {
        _ty = ty;
    }

    public int getTy() {
        return _ty;
    }

    public void setTx(int tx) {
        _tx = tx;
    }

    public int getTx() {
        return _tx;
    }

    public void setRightMBP(int rightMBP) {
        _rightMBP = rightMBP;
    }

    public int getRightMBP() {
        return _rightMBP;
    }

    public void setLeftMBP(int leftMBP) {
        _leftMBP = leftMBP;
    }

    public int getLeftMBP() {
        return _leftMBP;
    }

    public void setHeight(int height) {
        _height = height;
    }

    public int getHeight() {
        return _height;
    }

    public void setContentWidth(int contentWidth) {
        _contentWidth = contentWidth < 0 ? 0 : contentWidth;
    }

    public int getContentWidth() {
        return _contentWidth;
    }

    public PaintingInfo getPaintingInfo() {
        return _paintingInfo;
    }

    private void setPaintingInfo(PaintingInfo paintingInfo) {
        _paintingInfo = paintingInfo;
    }

    public boolean isAnonymous() {
        return _anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        _anonymous = anonymous;
    }

    public BoxDimensions getBoxDimensions() {
        BoxDimensions result = new BoxDimensions();

        result.setLeftMBP(getLeftMBP());
        result.setRightMBP(getRightMBP());
        result.setContentWidth(getContentWidth());
        result.setHeight(getHeight());

        return result;
    }

    public void setBoxDimensions(BoxDimensions dimensions) {
        setLeftMBP(dimensions.getLeftMBP());
        setRightMBP(dimensions.getRightMBP());
        setContentWidth(dimensions.getContentWidth());
        setHeight(dimensions.getHeight());
    }

    public void collectText(RenderingContext c, StringBuffer buffer) throws IOException {
        for (Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            b.collectText(c, buffer);
        }
    }

    public void exportText(RenderingContext c, Writer writer) throws IOException {
        if (c.isPrint() && isRoot()) {
            c.setPage(0, (PageBox)c.getRootLayer().getPages().get(0));
            c.getPage().exportLeadingText(c, writer);
        }
        for (Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            b.exportText(c, writer);
        }
        if (c.isPrint() && isRoot()) {
            exportPageBoxText(c, writer);
        }
    }

    private void exportPageBoxText(RenderingContext c, Writer writer) throws IOException {
        c.getPage().exportTrailingText(c, writer);
        if (c.getPage() != c.getRootLayer().getLastPage()) {
            List<PageBox> pages = c.getRootLayer().getPages();
            do {
                PageBox next = (PageBox)pages.get(c.getPageNo()+1);
                c.setPage(next.getPageNo(), next);
                next.exportLeadingText(c, writer);
                next.exportTrailingText(c, writer);
            } while (c.getPage() != c.getRootLayer().getLastPage());
        }
    }

    protected void exportPageBoxText(RenderingContext c, Writer writer, int yPos) throws IOException {
        c.getPage().exportTrailingText(c, writer);
        List<PageBox> pages = c.getRootLayer().getPages();
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
            Box parent = flowRoot.getParent();
            if (parent == null) {
                break;
            } else {
                flowRoot = parent;
            }
        }

        return flowRoot.isRoot();
    }

    public void analyzePageBreaks(LayoutContext c, ContentLimitContainer container) {
        container.updateTop(c, getAbsY());
        for (Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            b.analyzePageBreaks(c, container);
        }
        container.updateBottom(c, getAbsY() + getHeight());
    }

    public FSColor getEffBackgroundColor(RenderingContext c) {
        FSColor result = null;
        Box current = this;
        while (current != null) {
            result = current.getStyle().getBackgroundColor();
            if (result != null) {
                return result;
            }

            current = current.getContainingBlock();
        }

        PageBox page = c.getPage();
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
            Box parent = current.getParent();
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

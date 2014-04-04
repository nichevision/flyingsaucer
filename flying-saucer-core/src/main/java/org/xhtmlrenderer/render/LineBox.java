/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2006, 2007 Wisconsin Court System
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

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jsoup.nodes.Element;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.layout.BoxCollector;
import org.xhtmlrenderer.layout.InlineBoxing;
import org.xhtmlrenderer.layout.InlinePaintable;
import org.xhtmlrenderer.layout.Layer;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.PaintingInfo;
import org.xhtmlrenderer.util.XRRuntimeException;

/**
 * A line box contains a single line of text (or other inline content).  It
 * is created during layout.  It also tracks floated and absolute content
 * added while laying out the line.
 */
public class LineBox extends Box implements InlinePaintable {
    private static final float JUSTIFY_NON_SPACE_SHARE = 0.20f;
    private static final float JUSTIFY_SPACE_SHARE = 1 - JUSTIFY_NON_SPACE_SHARE;
    
    private boolean _containsContent;
    private boolean _containsBlockLevelContent;
    
    private FloatDistances _floatDistances;
    
    private List<TextDecoration> _textDecorations;
    
    private int _paintingTop;
    private int _paintingHeight;
    
    private List<Box> _nonFlowContent;
    
    private MarkerData _markerData;
    
    private boolean _containsDynamicFunction;
    
    private int _contentStart;
    
    private int _baseline;
    
    private JustificationInfo _justificationInfo;
    
    public LineBox() {
    }
    
    public String dump(LayoutContext c, String indent, int which) {
        if (which != Box.DUMP_RENDER) {
            throw new IllegalArgumentException();
        }

        StringBuilder result = new StringBuilder(indent);
        result.append(this);
        result.append('\n');
        
        dumpBoxes(c, indent, getNonFlowContent(), Box.DUMP_RENDER, result);
        if (getNonFlowContent().size() > 0  ) {
            result.append('\n');
        }
        dumpBoxes(c, indent, getChildren(), Box.DUMP_RENDER, result);
        
        return result.toString();
    }

    public String toString() {
        return "LineBox: (" + getAbsX() + "," + getAbsY() + ")->(" + getWidth() + "," + getHeight() + ")";
    }

    public Rectangle getMarginEdge(CssContext cssCtx, int tx, int ty) {
        Rectangle result = new Rectangle(getX(), getY(), getContentWidth(), getHeight());
        result.translate(tx, ty);
        return result;
    }
    
    public void paintInline(RenderingContext c) {
        if (! getParent().getStyle().isVisible()) {
            return;
        }
        
        if (isContainsDynamicFunction()) {
            lookForDynamicFunctions(c);
            int totalLineWidth = InlineBoxing.positionHorizontally(c, this, 0);
            setContentWidth(totalLineWidth);
            calcChildLocations();
            align(true);
            calcPaintingInfo(c, false);
        }
        
        if (_textDecorations != null) {
            c.getOutputDevice().drawTextDecoration(c, this);
        }
        
        if (c.debugDrawLineBoxes()) {
            c.getOutputDevice().drawDebugOutline(c, this, FSRGBColor.GREEN);
        }
    }
    
    private void lookForDynamicFunctions(RenderingContext c) {
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                Box b = (Box)getChild(i);
                if (b instanceof InlineLayoutBox) {
                    ((InlineLayoutBox)b).lookForDynamicFunctions(c);
                }
            }
        }
    }
    
    public boolean isFirstLine() {
        Box parent = getParent();
        return parent != null && parent.getChildCount() > 0 && parent.getChild(0) == this;
    }
    
    public void prunePendingInlineBoxes() {
        if (getChildCount() > 0) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                Box b = (Box)getChild(i);
                if (! (b instanceof InlineLayoutBox)) {
                    break;
                }
                InlineLayoutBox iB = (InlineLayoutBox)b;
                iB.prunePending();
                if (iB.isPending()) {
                    removeChild(i);
                }
            }
        }
    }

    public boolean isContainsContent() {
        return _containsContent;
    }

    public void setContainsContent(boolean containsContent) {
        _containsContent = containsContent;
    }
    
    public void align(boolean dynamic) {
        IdentValue align = getParent().getStyle().getIdent(CSSName.TEXT_ALIGN);
        
        int calcX = 0;
        
        if (align == IdentValue.LEFT || align == IdentValue.JUSTIFY) {
            int floatDistance = getFloatDistances().getLeftFloatDistance();
            calcX = getContentStart() + floatDistance;
            if (align == IdentValue.JUSTIFY && dynamic) {
                justify();
            }
        } else if (align == IdentValue.CENTER) {
            int leftFloatDistance = getFloatDistances().getLeftFloatDistance();
            int rightFloatDistance = getFloatDistances().getRightFloatDistance();
            
            int midpoint = leftFloatDistance +
                (getParent().getContentWidth() - leftFloatDistance - rightFloatDistance) / 2;
            
            calcX = midpoint - (getContentWidth() + getContentStart()) / 2;
        } else if (align == IdentValue.RIGHT) {
            int floatDistance = getFloatDistances().getRightFloatDistance();
            calcX = getParent().getContentWidth() - floatDistance - getContentWidth();
        }
        
        if (calcX != getX()) {
            setX(calcX);
            calcCanvasLocation();
            calcChildLocations();
        }
    }
    
    public void justify() {
        if (! isLastLineWithContent()) {
            int leftFloatDistance = getFloatDistances().getLeftFloatDistance();
            int rightFloatDistance = getFloatDistances().getRightFloatDistance();
            
            int available = getParent().getContentWidth() - 
                leftFloatDistance - rightFloatDistance - getContentStart(); 
            
            if (available > getContentWidth()) {
                int toAdd = available - getContentWidth();
                
                CharCounts counts = countJustifiableChars();
                
                JustificationInfo info = new JustificationInfo();
                if (! getParent().getStyle().isIdent(CSSName.LETTER_SPACING, IdentValue.NORMAL)) {
                    info.setNonSpaceAdjust(0.0f);
                    info.setSpaceAdjust((float)toAdd / counts.getSpaceCount());
                } else {
                    if (counts.getNonSpaceCount() > 1) {
                        info.setNonSpaceAdjust((float)toAdd * JUSTIFY_NON_SPACE_SHARE / (counts.getNonSpaceCount()-1));
                    } else {
                        info.setNonSpaceAdjust(0.0f);
                    }
                    
                    if (counts.getSpaceCount() > 0) {
                        info.setSpaceAdjust((float)toAdd * JUSTIFY_SPACE_SHARE / counts.getSpaceCount());
                    } else {
                        info.setSpaceAdjust(0.0f);
                    }
                }
                
                adjustChildren(info);
                
                setJustificationInfo(info);
            }
        }
    }
    
    private void adjustChildren(JustificationInfo info) {
        float adjust = 0.0f;
        for (Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            b.setX(b.getX() + Math.round(adjust));
            
            if (b instanceof InlineLayoutBox) {
                adjust += ((InlineLayoutBox)b).adjustHorizontalPosition(info, adjust);
            }
        }
        
        calcChildLocations();
    }
    
    private boolean isLastLineWithContent() {
        LineBox current = (LineBox)getNextSibling();
        while (current != null) {
            if (current.isContainsContent()) {
                return false;
            } else {
                current = (LineBox)current.getNextSibling();
            }
        }
        
        return true;
    }
    
    private CharCounts countJustifiableChars() {
        CharCounts result = new CharCounts();
        
        for (Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            if (b instanceof InlineLayoutBox) {
                ((InlineLayoutBox)b).countJustifiableChars(result);
            }
        }
        
        return result;
    }
    
	public FloatDistances getFloatDistances() {
		return _floatDistances;
	}

	public void setFloatDistances(FloatDistances floatDistances) {
		_floatDistances = floatDistances;
	}

    public boolean isContainsBlockLevelContent() {
        return _containsBlockLevelContent;
    }

    public void setContainsBlockLevelContent(boolean containsBlockLevelContent) {
        _containsBlockLevelContent = containsBlockLevelContent;
    }
    
    public boolean intersects(CssContext cssCtx, Shape clip) {
        return clip == null || (intersectsLine(cssCtx, clip) || 
            (isContainsBlockLevelContent() && intersectsInlineBlocks(cssCtx, clip)));
    }
    
    private boolean intersectsLine(CssContext cssCtx, Shape clip) {
        Rectangle result = getPaintingClipEdge(cssCtx);
        return clip.intersects(result);
    }

    public Rectangle getPaintingClipEdge(CssContext cssCtx) {
        Box parent = getParent();
        Rectangle result = null;
        if (parent.getStyle().isIdent(
                CSSName.FS_TEXT_DECORATION_EXTENT, IdentValue.BLOCK) || 
                    getJustificationInfo() != null) {
            result = new Rectangle(
                    getAbsX(), getAbsY() + _paintingTop, 
                    parent.getAbsX() + parent.getTx() + parent.getContentWidth() - getAbsX(), 
                    _paintingHeight);
        } else {
            result = new Rectangle(
                    getAbsX(), getAbsY() + _paintingTop, getContentWidth(), _paintingHeight);
        }
        return result;
    }
    
    private boolean intersectsInlineBlocks(CssContext cssCtx, Shape clip) {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = (Box)getChild(i);
            if (child instanceof InlineLayoutBox) {
                boolean possibleResult = ((InlineLayoutBox)child).intersectsInlineBlocks(
                        cssCtx, clip);
                if (possibleResult) {
                    return true;
                }
            } else {
                BoxCollector collector = new BoxCollector();
                if (collector.intersectsAny(cssCtx, clip, child)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    public List<TextDecoration> getTextDecorations() {
        return _textDecorations;
    }

    public void setTextDecorations(List<TextDecoration> textDecorations) {
        _textDecorations = textDecorations;
    }

    public int getPaintingHeight() {
        return _paintingHeight;
    }

    public void setPaintingHeight(int paintingHeight) {
        _paintingHeight = paintingHeight;
    }

    public int getPaintingTop() {
        return _paintingTop;
    }

    public void setPaintingTop(int paintingTop) {
        _paintingTop = paintingTop;
    }
    
    
    public void addAllChildren(List<Box> list, Layer layer) {
        for (int i = 0; i < getChildCount(); i++) {
            Box child = getChild(i);
            if (getContainingLayer() == layer) {
                list.add(child);
                if (child instanceof InlineLayoutBox) {
                    ((InlineLayoutBox)child).addAllChildren(list, layer);
                }
            }
        }
    }
    
    public List<Box> getNonFlowContent() {
        return _nonFlowContent == null ? Collections.<Box>emptyList() : _nonFlowContent;
    }
    
    public void addNonFlowContent(BlockBox box) {
        if (_nonFlowContent == null) {
            _nonFlowContent = new ArrayList<>();
        }
        
        _nonFlowContent.add(box);
    }
    
    public void reset(LayoutContext c) {
        for (int i = 0; i < getNonFlowContent().size(); i++) {
            Box content = (Box)getNonFlowContent().get(i);
            content.reset(c);
        }
        if (_markerData != null) {
            _markerData.restorePreviousReferenceLine(this);
        }
        super.reset(c);
    }
    
    public void calcCanvasLocation() {
        Box parent = getParent();
        if (parent == null) {
            throw new XRRuntimeException("calcCanvasLocation() called with no parent");
        }
        setAbsX(parent.getAbsX() + parent.getTx() + getX());
        setAbsY(parent.getAbsY() + parent.getTy() + getY());        
    }
    
    public void calcChildLocations() {
        super.calcChildLocations();
        
        // Update absolute boxes too.  Not necessary most of the time, but
        // it doesn't hurt (revisit this)
        for (int i = 0; i < getNonFlowContent().size(); i++) {
            Box content = (Box)getNonFlowContent().get(i);
            if (content.getStyle().isAbsolute()) {
                content.calcCanvasLocation();
                content.calcChildLocations();
            }
        }
    }

    public MarkerData getMarkerData() {
        return _markerData;
    }

    public void setMarkerData(MarkerData markerData) {
        _markerData = markerData;
    }

    public boolean isContainsDynamicFunction() {
        return _containsDynamicFunction;
    }

    public void setContainsDynamicFunction(boolean containsPageCounter) {
        _containsDynamicFunction |= containsPageCounter;
    }

    public int getContentStart() {
        return _contentStart;
    }

    public void setContentStart(int contentOffset) {
        _contentStart = contentOffset;
    }
    
    public InlineText findTrailingText() {
        if (getChildCount() == 0) {
            return null;
        }
        
        for (int offset = getChildCount() - 1; offset >= 0; offset--) {
            Box child = getChild(offset);
            if (child instanceof InlineLayoutBox) {
                InlineText result = ((InlineLayoutBox)child).findTrailingText();
                if (result != null && result.isEmpty()) {
                    continue;
                }
                return result;
            } else {
                return null;
            }
        }
        
        return null;
    }
    
    public void trimTrailingSpace(LayoutContext c) {
        InlineText text = findTrailingText();
        
        if (text != null) {
            InlineLayoutBox iB = text.getParent();
            IdentValue whitespace = iB.getStyle().getWhitespace();
            if (whitespace == IdentValue.NORMAL || whitespace == IdentValue.NOWRAP) {
                text.trimTrailingSpace(c);
            }
        }
    }    
    
    public Box find(CssContext cssCtx, int absX, int absY, boolean findAnonymous) {
        PaintingInfo pI = getPaintingInfo();
        if (pI !=null && ! pI.getAggregateBounds().contains(absX, absY)) {
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
        
        return null;
    }

    public int getBaseline() {
        return _baseline;
    }

    public void setBaseline(int baseline) {
        _baseline = baseline;
    }
    
    public boolean isContainsOnlyBlockLevelContent() {
        if (! isContainsBlockLevelContent()) {
            return false;
        }
        
        for (int i = 0; i < getChildCount(); i++) {
            Box b = (Box)getChild(i);
            if (! (b instanceof BlockBox)) {
                return false;
            }
        }
        
        return true;
    }
    
    public Box getRestyleTarget() {
        return getParent();
    }
    
    public void restyle(LayoutContext c) {
        Box parent = getParent();
        Element e = parent.getElement();
        if (e != null) {
            CalculatedStyle style = c.getSharedContext().getStyle(e, true);
            setStyle(style.createAnonymousStyle(IdentValue.BLOCK));
        }
        
        restyleChildren(c);
    }    
    
    public boolean isContainsVisibleContent() {
        for (int i = 0; i < getChildCount(); i++) {
            Box b = getChild(i);
            if (b instanceof BlockBox) {
                if (b.getWidth() > 0 || b.getHeight() > 0) {
                    return true;
                }
            } else {
                boolean maybeResult = ((InlineLayoutBox)b).isContainsVisibleContent();
                if (maybeResult) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public void clearSelection(List<Box> modified) {
        for (Iterator<Box> i = getNonFlowContent().iterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            b.clearSelection(modified);
        }
        
        super.clearSelection(modified);
    }
    
    public void selectAll() {
        for (Iterator<Box> i = getNonFlowContent().iterator(); i.hasNext(); ) {
            BlockBox box = (BlockBox)i.next();
            box.selectAll();
        }
        
        super.selectAll();
    }
    
    public void collectText(RenderingContext c, StringBuffer buffer) throws IOException {
        for (Iterator<Box> i = getNonFlowContent().iterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            b.collectText(c, buffer);
        }
        if (isContainsDynamicFunction()) {
            lookForDynamicFunctions(c);
        }
        super.collectText(c, buffer);
    }
    
    public void exportText(RenderingContext c, Writer writer) throws IOException {
        int baselinePos = getAbsY() + getBaseline();
        if (baselinePos >= c.getPage().getBottom() && isInDocumentFlow()) {
            exportPageBoxText(c, writer, baselinePos);
        }
        
        for (Iterator<Box> i = getNonFlowContent().iterator(); i.hasNext(); ) {
            Box b = (Box)i.next();
            b.exportText(c, writer);
        }
        
        if (isContainsContent()) {
            StringBuffer result = new StringBuffer();
            collectText(c, result);
            writer.write(result.toString().trim());
            writer.write(LINE_SEPARATOR);
        }
    }
    
    public void analyzePageBreaks(LayoutContext c, ContentLimitContainer container) {
        container.updateTop(c, getAbsY());
        container.updateBottom(c, getAbsY() + getHeight());
    }
    
    public void checkPagePosition(LayoutContext c, boolean alwaysBreak) {
        if (! c.isPageBreaksAllowed()) {
            return;
        }
        
        PageBox pageBox = c.getRootLayer().getFirstPage(c, this);
        if (pageBox != null) {
            boolean needsPageBreak = 
                alwaysBreak || getAbsY() + getHeight() >= pageBox.getBottom() - c.getExtraSpaceBottom();
                
           if (needsPageBreak) {
               forcePageBreakBefore(c, IdentValue.ALWAYS, false);
               calcCanvasLocation();
           } else if (pageBox.getTop() + c.getExtraSpaceTop() > getAbsY()) {
               int diff = pageBox.getTop() + c.getExtraSpaceTop() - getAbsY();
               
               setY(getY() + diff);
               calcCanvasLocation();
           }
        }
    }

    public JustificationInfo getJustificationInfo() {
        return _justificationInfo;
    }

    private void setJustificationInfo(JustificationInfo justificationInfo) {
        _justificationInfo = justificationInfo;
    }
}

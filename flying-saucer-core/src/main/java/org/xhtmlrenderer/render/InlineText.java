/*
 * {{{ header & license
 * Copyright (c) 2005 Joshua Marinacci
 * Copyright (c) 2005 Wisconsin Court System
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
package org.xhtmlrenderer.render;

import java.awt.Rectangle;

import org.jsoup.nodes.Node;
import org.xhtmlrenderer.extend.FSGlyphVector;
import org.xhtmlrenderer.layout.FunctionData;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.WhitespaceStripper;
import org.xhtmlrenderer.util.Uu;

/**
 * A lightweight object which contains a chunk of text from an inline element.  
 * It will never extend across a line break nor will it extend across an element 
 * nested within its inline element.
 */
public class InlineText {
    private InlineLayoutBox _parent;
    
    private int _x;
    
    private String _masterText;
    private int _start;
    private int _end;
    
    private int _width;
    
    private FunctionData _functionData;
    
    private boolean _containedLF = false;
    
    private short _selectionStart;
    private short _selectionEnd;
    
    private float[] _glyphPositions;
    
    private boolean _trimmedLeadingSpace;
    private boolean _trimmedTrailingSpace;
    private Node _textNode;
    public void trimTrailingSpace(final LayoutContext c) {
        if (! isEmpty() && _masterText.charAt(_end-1) == ' ') {
            _end--;
            setWidth(c.getTextRenderer().getWidth(c.getFontContext(), 
                    getParent().getStyle().getFSFont(c),
                    getSubstring()));
            setTrimmedTrailingSpace(true);
        } 
    }
    
    public boolean isEmpty() {
        return _start == _end && ! _containedLF;
    }
    
    public String getSubstring() {
        if (getMasterText() != null) {
            if (_start == -1 || _end == -1) {
                throw new RuntimeException("negative index in InlineBox");
            }
            if (_end < _start) {
                throw new RuntimeException("end is less than setStartStyle");
            }
            return getMasterText().substring(_start, _end);
        } else {
            throw new RuntimeException("No master text set!");
        }
    }
    
    public void setSubstring(final int start, final int end) {
        if (end < start) {
            Uu.p("setting substring to: " + start + " " + end);
            throw new RuntimeException("set substring length too long: " + this);
        } else if (end < 0 || start < 0) {
            throw new RuntimeException("Trying to set negative index to inline box");
        }
        _start = start;
        _end = end;
        
        if (_end > 0 && _masterText.charAt(_end-1) == WhitespaceStripper.EOLC) {
            _containedLF = true;
            _end--;
        }
    }

    public String getMasterText() {
        return _masterText;
    }

    public void setMasterText(final String masterText) {
        _masterText = masterText;
    }

    public int getX() {
        return _x;
    }

    public void setX(final int x) {
        _x = x;
    }

    public int getWidth() {
        return _width;
    }

    public void setWidth(final int width) {
        _width = width;
    }
    
    public void paint(final RenderingContext c) {
        c.getOutputDevice().drawText(c, this);
    }
    
    public void paintSelection(final RenderingContext c) {
        c.getOutputDevice().drawSelection(c, this);
    }

    public InlineLayoutBox getParent() {
        return _parent;
    }

    public void setParent(final InlineLayoutBox parent) {
        _parent = parent;
    }

    public boolean isDynamicFunction() {
        return _functionData != null;
    }

    public FunctionData getFunctionData() {
        return _functionData;
    }

    public void setFunctionData(final FunctionData functionData) {
        _functionData = functionData;
    }
    
    public void updateDynamicValue(final RenderingContext c) {
        final String value = _functionData.getContentFunction().calculate(
                c, _functionData.getFunction(), this);
        _start = 0;
        _end = value.length();
        _masterText = value;
        _width = c.getTextRenderer().getWidth(
                c.getFontContext(), getParent().getStyle().getFSFont(c),
                value);
    }
    
    public String toString() {
        final StringBuffer result = new StringBuffer();
        result.append("InlineText: ");
        if (_containedLF || isDynamicFunction()) {
            result.append("(");
            if (_containedLF) {
                result.append('L');
            }
            if (isDynamicFunction()) {
                result.append('F');
            }
            result.append(") ");
        }
        result.append('(');
        result.append(getSubstring());
        result.append(')');
        
        return result.toString();
    }
    
    public boolean updateSelection(final RenderingContext c, final Rectangle selection) {
        ensureGlyphPositions(c);
        final float[] positions = _glyphPositions;
        final int y = getParent().getAbsY();
        final int offset = getParent().getAbsX() + getX();
        
        final int prevSelectionStart = _selectionStart;
        final int prevSelectionEnd = _selectionEnd;
        
        boolean found = false;
        _selectionStart = 0;
        _selectionEnd = 0;
        for (int i = 0; i < positions.length - 2; i += 2) {
            final Rectangle target = new Rectangle(
                    (int)(offset + (positions[i] + positions[i+2]) / 2),
                    y,
                    1,
                    getParent().getHeight());
            if (selection.intersects(target)) {
                if (! found) {
                    found = true;
                    _selectionStart = (short)(i / 2);
                    _selectionEnd = (short)(i / 2 + 1);
                } else {
                    _selectionEnd++;
                }
            }
        }
        
        return prevSelectionStart != _selectionStart || prevSelectionEnd != _selectionEnd;
    }

    private void ensureGlyphPositions(final RenderingContext c) {
        if (_glyphPositions == null) {
            final FSGlyphVector glyphVector = c.getTextRenderer().getGlyphVector(
                    c.getOutputDevice(),
                    getParent().getStyle().getFSFont(c),
                    getSubstring());
            _glyphPositions = c.getTextRenderer().getGlyphPositions(
                    c.getOutputDevice(), 
                    getParent().getStyle().getFSFont(c),
                    glyphVector);
        } 
    }
    
    public boolean clearSelection() {
        final boolean result = _selectionStart != 0 || _selectionEnd != 0;
        
        _selectionStart = 0;
        _selectionEnd = 0;
        
        return result;
    }
    
    public boolean isSelected() {
        return _selectionStart != _selectionEnd;
    }

    public short getSelectionEnd() {
        return _selectionEnd;
    }

    public short getSelectionStart() {
        return _selectionStart;
    }
    
    public String getSelection() {
        return getSubstring().substring(_selectionStart, _selectionEnd);
    }
    
    public void selectAll() {
        _selectionStart = 0;
        _selectionEnd = (short)getSubstring().length();
    }
    
    public String getTextExportText() {
        final char[] ch = getSubstring().toCharArray();
        final StringBuffer result = new StringBuffer();
        if (isTrimmedLeadingSpace()) {
            result.append(' ');
        }
        for (int i = 0; i < ch.length; i++) {
            final char c = ch[i];
            if (c != '\n') {
                result.append(c);
            }
        }
        if (isTrimmedTrailingSpace()) {
            result.append(' ');
        }
        return result.toString();
    }

    public boolean isTrimmedLeadingSpace() {
        return _trimmedLeadingSpace;
    }

    public void setTrimmedLeadingSpace(final boolean trimmedLeadingSpace) {
        _trimmedLeadingSpace = trimmedLeadingSpace;
    }

    private void setTrimmedTrailingSpace(final boolean trimmedTrailingSpace) {
        _trimmedTrailingSpace = trimmedTrailingSpace;
    }

    private boolean isTrimmedTrailingSpace() {
        return _trimmedTrailingSpace;
    }
    
    public void countJustifiableChars(final CharCounts counts) {
        final String s = getSubstring();
        final int len = s.length();
        int spaces = 0;
        int other = 0;
        
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            if (c == ' ' || c == '\u00a0' || c == '\u3000') {
                spaces++;
            } else {
                other++;
            }
        }
        
        counts.setSpaceCount(counts.getSpaceCount() + spaces);
        counts.setNonSpaceCount(counts.getNonSpaceCount() + other);
    }
    
    public float calcTotalAdjustment(final JustificationInfo info) {
        final String s = getSubstring();
        final int len = s.length();

        float result = 0.0f;
        for (int i = 0; i < len; i++) {
            final char c = s.charAt(i);
            if (c == ' ' || c == '\u00a0' || c == '\u3000') {
                result += info.getSpaceAdjust();
            } else {
                result += info.getNonSpaceAdjust();
            }
        }
        
        return result;
    }
    public int getStart(){
        return _start;
    }
    public int getEnd(){
        return _end;
    }
    public void setSelectionStart(final short s){
        _selectionStart = s;
    }
    public void setSelectionEnd(final short s){
        _selectionEnd = s;
    }

    public Node getTextNode() {
        return this._textNode;
    }

    public void setTextNode(final Node node) {
        this._textNode = node;
    }
}


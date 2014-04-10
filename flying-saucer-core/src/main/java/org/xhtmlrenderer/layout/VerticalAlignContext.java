/*
 * {{{ header & license
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
package org.xhtmlrenderer.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.InlineLayoutBox;

/**
 * This class performs the real work of vertically positioning inline boxes
 * within a line (i.e. implementing the vertical-align property).  Because
 * of the requirements of vertical-align: top/bottom, a <code>VerticalAlignContext</code>
 * is actually a tree of <code>VerticalAlignContext</code> objects which all
 * must be taken into consideration when aligning content.
 */
public class VerticalAlignContext {
    private final List<InlineBoxMeasurements> _measurements = new ArrayList<InlineBoxMeasurements>();
    
    private int _inlineTop;
    private boolean _inlineTopSet = false;
    
    private int _inlineBottom;
    private boolean _inlineBottomSet = false;
    
    private int _paintingTop;
    private boolean _paintingTopSet = false;
    
    private int _paintingBottom;
    private boolean _paintingBottomSet = false;
    
    private List<ChildContextData> _children = new ArrayList<ChildContextData>();
    
    private VerticalAlignContext _parent = null;
    
    private void moveTrackedValues(final int ty) {
        if (_inlineTopSet) {
            _inlineTop += ty;
        }
        
        if (_inlineBottomSet) {
            _inlineBottom += ty;
        }
        
        if (_paintingTopSet) {
            _paintingTop += ty;
        }
        
        if (_paintingBottomSet) {
            _paintingBottom += ty;
        }
    }
    
    public int getInlineBottom() {
        return _inlineBottom;
    }

    public int getInlineTop() {
        return _inlineTop;
    }

    public void updateInlineTop(final int inlineTop) {
        if (! _inlineTopSet || inlineTop < _inlineTop) {
            _inlineTop = inlineTop;
            _inlineTopSet = true;
        }
    }
    
    public void updatePaintingTop(final int paintingTop) {
        if (! _paintingTopSet || paintingTop < _paintingTop) {
            _paintingTop = paintingTop;
            _paintingTopSet = true;
        }
    }
    
    public void updateInlineBottom(final int inlineBottom) {
        if (! _inlineBottomSet || inlineBottom > _inlineBottom) {
            _inlineBottom = inlineBottom;
            _inlineBottomSet = true;
        }
    }
    
    public void updatePaintingBottom(final int paintingBottom) {
        if (! _paintingBottomSet || paintingBottom > _paintingBottom) {
            _paintingBottom = paintingBottom;
            _paintingBottomSet = true;
        }
    }    
    
    public int getLineBoxHeight() {
        return _inlineBottom - _inlineTop;
    }
    
    public void pushMeasurements(final InlineBoxMeasurements measurements) {
        _measurements.add(measurements);
        
        updateInlineTop(measurements.getInlineTop());
        updateInlineBottom(measurements.getInlineBottom());
        
        updatePaintingTop(measurements.getPaintingTop());
        updatePaintingBottom(measurements.getPaintingBottom());
    }
    
    public InlineBoxMeasurements getParentMeasurements() {
        return _measurements.get(_measurements.size()-1);
    }
    
    public void popMeasurements() {
        _measurements.remove(_measurements.size()-1);
    }

    public int getPaintingBottom() {
        return _paintingBottom;
    }

    public int getPaintingTop() {
        return _paintingTop;
    }
    
    public VerticalAlignContext createChild(final Box root) {
        final VerticalAlignContext result = new VerticalAlignContext();
        
        final VerticalAlignContext vaRoot = getRoot();
        
        result.setParent(vaRoot);
        
        final InlineBoxMeasurements initial = vaRoot._measurements.get(0);
        result.pushMeasurements(initial);
        
        if (vaRoot._children == null) {
            vaRoot._children = new ArrayList<ChildContextData>();
        }
        
        vaRoot._children.add(new ChildContextData(root, result));
        
        return result;
    }
    
    public List<ChildContextData> getChildren() {
        return _children == null ? Collections.<ChildContextData>emptyList() : _children;
    }

    public VerticalAlignContext getParent() {
        return _parent;
    }

    public void setParent(final VerticalAlignContext parent) {
        _parent = parent;
    }
    
    private VerticalAlignContext getRoot() {
        final VerticalAlignContext result = this;
        return result.getParent() != null ? result.getParent() : this;
    }
    
    private void merge(final VerticalAlignContext context) {
        updateInlineBottom(context.getInlineBottom());
        updateInlineTop(context.getInlineTop());
        
        updatePaintingBottom(context.getPaintingBottom());
        updatePaintingTop(context.getPaintingTop());
    }
    
    public void alignChildren() {
        final List<ChildContextData> children = getChildren();
        for (int i = 0; i < children.size(); i++) {
            final ChildContextData data = (ChildContextData)children.get(i);
            data.align();
            merge(data.getVerticalAlignContext());
        }
    }
    
    public void setInitialMeasurements(final InlineBoxMeasurements measurements) {
        _measurements.add(measurements);
    }
    
    private static final class ChildContextData {
        private Box _root;
        private VerticalAlignContext _verticalAlignContext;
        
        
        @SuppressWarnings("unused")
		public ChildContextData() {
        }
        
        public ChildContextData(final Box root, final VerticalAlignContext vaContext) {
            _root = root;
            _verticalAlignContext = vaContext;
        }
        
        @SuppressWarnings("unused")
		public Box getRoot() {
            return _root;
        }
        
        @SuppressWarnings("unused")
		public void setRoot(final Box root) {
            _root = root;
        }
        
        public VerticalAlignContext getVerticalAlignContext() {
            return _verticalAlignContext;
        }
        
        @SuppressWarnings("unused")
		public void setVerticalAlignContext(final VerticalAlignContext verticalAlignContext) {
            _verticalAlignContext = verticalAlignContext;
        }
        
        private void moveContextContents(final int ty) {
            moveInlineContents(_root, ty);
        }
        
        private void moveInlineContents(final Box box, final int ty) {
            if (canBeMoved(box)) { 
                box.setY(box.getY() + ty);
                if (box instanceof InlineLayoutBox) {
                    final InlineLayoutBox iB = (InlineLayoutBox)box;
                    for (int i = 0; i < iB.getInlineChildCount(); i++) {
                        final Object child = iB.getInlineChild(i);
                        if (child instanceof Box) {
                            moveInlineContents((Box)child, ty);
                        }
                    }
                }
            }
        }
        
        private boolean canBeMoved(final Box box) {
            final IdentValue vAlign = box.getStyle().getIdent(CSSName.VERTICAL_ALIGN);
            return box == _root ||
                ! (vAlign == IdentValue.TOP || vAlign == IdentValue.BOTTOM);
        }
        
        public void align() {
            final IdentValue vAlign = _root.getStyle().getIdent(
                    CSSName.VERTICAL_ALIGN);
            int delta = 0;
            if (vAlign == IdentValue.TOP) {
                delta = _verticalAlignContext.getRoot().getInlineTop() -
                    _verticalAlignContext.getInlineTop();
            } else if (vAlign == IdentValue.BOTTOM) {
                delta = _verticalAlignContext.getRoot().getInlineBottom() -
                    _verticalAlignContext.getInlineBottom();
            } else {
                throw new RuntimeException("internal error");
            }
            
            _verticalAlignContext.moveTrackedValues(delta);
            moveContextContents(delta);
        }
    }    
}

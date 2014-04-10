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

import java.util.Iterator;
import java.util.List;

import org.jsoup.nodes.Element;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.layout.Styleable;

/**
 * An anonymous block box as defined in the CSS spec.  This class is only used
 * when wrapping inline content in a block box in order to ensure that a block
 * box only ever contains either block or inline content.  Other anonymous block
 * boxes create a <code>BlockBox</code> directly with the anonymous property is
 * true.
 */
public class AnonymousBlockBox extends BlockBox {
    private List<InlineBox> _openInlineBoxes;
    
    public AnonymousBlockBox(final Element element) {
        setElement(element);
    }

    public void layout(final LayoutContext c) {
        layoutInlineChildren(c, 0, calcInitialBreakAtLine(c), true);
    }

    public int getContentWidth() {
        return getContainingBlock().getContentWidth();
    }
    
    public Box find(final CssContext cssCtx, final int absX, final int absY, final boolean findAnonymous) {
        final Box result = super.find(cssCtx, absX, absY, findAnonymous);
        if (! findAnonymous && result == this) {
            return getParent();
        } else {
            return result;
        }
    }

    public List<InlineBox> getOpenInlineBoxes() {
        return _openInlineBoxes;
    }

    public void setOpenInlineBoxes(final List<InlineBox> openInlineBoxes) {
        _openInlineBoxes = openInlineBoxes;
    }
    
    public boolean isSkipWhenCollapsingMargins() {
        // An anonymous block will already have its children provided to it
        for (final Iterator<Styleable> i = getInlineContent().iterator(); i.hasNext(); ) {
            final Styleable styleable = (Styleable)i.next();
            final CalculatedStyle style = styleable.getStyle();
            if (! (style.isFloated() || style.isAbsolute() || style.isFixed() || style.isRunning())) {
                return false;
            }
        }
        return true;
    }
    
    public void provideSiblingMarginToFloats(final int margin) {
        for (final Iterator<Styleable> i = getInlineContent().iterator(); i.hasNext(); ) {
            final Styleable styleable = (Styleable)i.next();
            if (styleable instanceof BlockBox) {
                final BlockBox b = (BlockBox)styleable;
                if (b.isFloated()) {
                    b.getFloatedBoxData().setMarginFromSibling(margin);
                }
            }
        }
    }
    
    public boolean isMayCollapseMarginsWithChildren() {
        return false;
    }
    
    public void styleText(final LayoutContext c) {
        styleText(c, getParent().getStyle());
    } 
    
    public BlockBox copyOf() {
        throw new IllegalArgumentException("cannot be copied");
    }
}

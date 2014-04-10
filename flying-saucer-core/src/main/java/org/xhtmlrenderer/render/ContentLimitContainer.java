/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
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

import java.util.ArrayList;
import java.util.List;

import org.xhtmlrenderer.layout.LayoutContext;

public class ContentLimitContainer {
    private ContentLimitContainer _parent;
    
    private final int _initialPageNo;
    private final List<ContentLimit> _contentLimits = new ArrayList<ContentLimit>();
    
    private PageBox _lastPage;
    
    public ContentLimitContainer(final LayoutContext c, final int startAbsY) {
        _initialPageNo = getPage(c, startAbsY).getPageNo();
    }

    public int getInitialPageNo() {
        return _initialPageNo;
    }
    
    public int getLastPageNo() {
        return _initialPageNo + _contentLimits.size() - 1;
    }
    
    public ContentLimit getContentLimit(final int pageNo) {
        return getContentLimit(pageNo, false);
    }

    private ContentLimit getContentLimit(final int pageNo, final boolean addAsNeeded) {
        if (addAsNeeded) {
            while (_contentLimits.size() < (pageNo - _initialPageNo + 1)) {
                _contentLimits.add(new ContentLimit());
            }
        }
        
        final int target = pageNo - _initialPageNo;
        if (target >= 0 && target < _contentLimits.size()) {
            return _contentLimits.get(pageNo - _initialPageNo);
        } else {
            return null;
        }
    }
    
    public void updateTop(final LayoutContext c, final int absY) {
        final PageBox page = getPage(c, absY);
        
        getContentLimit(page.getPageNo(), true).updateTop(absY);
        
        final ContentLimitContainer parent = getParent();
        if (parent != null) {
            parent.updateTop(c, absY);
        }
    }
    
    public void updateBottom(final LayoutContext c, final int absY) {
        final PageBox page = getPage(c, absY);
        
        getContentLimit(page.getPageNo(), true).updateBottom(absY);
        
        final ContentLimitContainer parent = getParent();
        if (parent != null) {
            parent.updateBottom(c, absY);
        }
    }

    public PageBox getPage(final LayoutContext c, final int absY) {
        PageBox page;
        final PageBox last = getLastPage();
        if (last != null && absY >= last.getTop() && absY < last.getBottom()) {
            page = last;
        } else {
            page = c.getRootLayer().getPage(c, absY);
            setLastPage(page);
        }
        return page;
    }
    
    private PageBox getLastPage() {
        ContentLimitContainer c = this;
        while (c.getParent() != null) {
            c = c.getParent();
        }
        return c._lastPage;
    }
    
    private void setLastPage(final PageBox page) {
        ContentLimitContainer c = this;
        while (c.getParent() != null) {
            c = c.getParent();
        }
        c._lastPage = page;
    }

    public ContentLimitContainer getParent() {
        return _parent;
    }

    public void setParent(final ContentLimitContainer parent) {
        _parent = parent;
    }
    
    public boolean isContainsMultiplePages() {
        return _contentLimits.size() > 1;
    }
    
    public String toString() {
        return "[initialPageNo=" + _initialPageNo + ", limits=" + _contentLimits + "]";
    }
}

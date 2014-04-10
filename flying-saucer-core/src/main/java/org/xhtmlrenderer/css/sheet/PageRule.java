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
package org.xhtmlrenderer.css.sheet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xhtmlrenderer.css.constants.MarginBoxName;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;

public class PageRule implements RulesetContainer {
    private String _name;
    private String _pseudoPage;
    private Ruleset _ruleset;
    private CSSOrigin _origin;
    
    private final Map<MarginBoxName, List<PropertyDeclaration>> _marginBoxes = new HashMap<MarginBoxName, List<PropertyDeclaration>>();
    
    private int _pos;
    
    private int _specificityF;
    private int _specificityG;
    private int _specificityH;
    
    public PageRule(final CSSOrigin origin) {
        _origin = origin;
    }
    
    public String getPseudoPage() {
        return _pseudoPage;
    }
    
    public void setPseudoPage(final String pseudoPage) {
        _pseudoPage = pseudoPage;
        if (pseudoPage.equals("first")) {
            _specificityG = 1;
        } else {
            _specificityH = 1;
        }
    }
    
    public Ruleset getRuleset() {
        return _ruleset;
    }
    
    public void setRuleset(final Ruleset ruleset) {
        _ruleset = ruleset;
    }
    
    public void addContent(final Ruleset ruleset) {
        if (_ruleset != null) {
            throw new IllegalStateException("Ruleset has already been set");
        }
        _ruleset = ruleset;
    }

    public CSSOrigin getOrigin() {
        return _origin;
    }

    public void setOrigin(final CSSOrigin origin) {
        _origin = origin;
    }

    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        _name = name;
        _specificityF = 1;
    }
    
    public List<PropertyDeclaration> getMarginBoxProperties(final MarginBoxName name) {
        return _marginBoxes.get(name);
    }
    
    public void addMarginBoxProperties(final MarginBoxName name, final List<PropertyDeclaration> props) {
        _marginBoxes.put(name, props);
    }
    
    public Map<MarginBoxName, List<PropertyDeclaration>> getMarginBoxes() {
        return _marginBoxes;
    }
    
    public long getOrder() {
        long result = 0;
        
        result |= (long)_specificityF << 32;
        result |= (long)_specificityG << 24;
        result |= (long)_specificityH << 16;
        result |= _pos;
        
        return result;
    }
    
    public boolean applies(final String pageName, final String pseudoPage) {
        if (_name == null && _pseudoPage == null) {
            return true;
        } else if (_name == null && _pseudoPage != null && 
                (_pseudoPage.equals(pseudoPage) || 
                        (_pseudoPage.equals("right") && pseudoPage != null && pseudoPage.equals("first")))) { // assume first page is a right page
            return true;
        } else if (_name != null && _name.equals(pageName) && _pseudoPage == null) {
            return true;
        } else if (_name != null && _name.equals(pageName) &&
                    _pseudoPage != null && _pseudoPage.equals(pseudoPage)) {
            return true;
        }
        
        return false;
    }

    public int getPos() {
        return _pos;
    }

    public void setPos(final int pos) {
        _pos = pos;
    }
}

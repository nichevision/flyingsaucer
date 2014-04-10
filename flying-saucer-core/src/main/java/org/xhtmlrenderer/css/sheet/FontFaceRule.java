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

import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.EmptyStyle;
import org.xhtmlrenderer.util.XRRuntimeException;

public class FontFaceRule implements RulesetContainer {
    private CSSOrigin _origin;
    private Ruleset _ruleset;
    private CalculatedStyle _calculatedStyle;

    public FontFaceRule(final CSSOrigin origin) {
        _origin = origin;
    }

    public void addContent(final Ruleset ruleset) {
        if (_ruleset != null) {
            throw new XRRuntimeException("Ruleset can only be set once");
        }
        _ruleset = ruleset;
    }

    public CSSOrigin getOrigin() {
        return _origin;
    }

    public void setOrigin(final CSSOrigin origin) {
        _origin = origin;
    }

    public CalculatedStyle getCalculatedStyle() {
        if (_calculatedStyle == null) {
            _calculatedStyle = new EmptyStyle().deriveStyle(
                    CascadedStyle.createLayoutStyle(_ruleset.getPropertyDeclarations()));
        }

        return _calculatedStyle;
    }

    public boolean hasFontFamily() {
        for (final PropertyDeclaration decl : _ruleset.getPropertyDeclarations()) {
            if (decl.getPropertyName().equals("font-family")) {
                return true;
            }
        }

        return false;
    }
}

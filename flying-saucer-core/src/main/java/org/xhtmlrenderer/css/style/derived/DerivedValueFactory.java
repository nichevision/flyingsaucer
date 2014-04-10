/*
 * {{{ header & license
 * Copyright (c) 2005 Patrick Wright
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
package org.xhtmlrenderer.css.style.derived;

import java.util.HashMap;
import java.util.Map;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.parser.PropertyValueImp.CSSValueType;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.FSDerivedValue;

public class DerivedValueFactory {
    private static final Map<String, FSDerivedValue> CACHED_COLORS = new HashMap<String, FSDerivedValue>();
    
    public static FSDerivedValue newDerivedValue(
            final CalculatedStyle style, final CSSName cssName, final PropertyValue value) {
        if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
            return style.getParent().valueByName(cssName);
        }
        switch (value.getPropertyValueType()) {
            case PropertyValueImp.VALUE_TYPE_LENGTH:
                return new LengthValue(style, cssName, value);
            case PropertyValueImp.VALUE_TYPE_IDENT:
                IdentValue ident = value.getIdentValue();
                if (ident == null) {
                    ident = IdentValue.getByIdentString(value.getStringValue());
                }
                return ident;
            case PropertyValueImp.VALUE_TYPE_STRING:
                return new StringValue(cssName, value);
            case PropertyValueImp.VALUE_TYPE_NUMBER:
                return new NumberValue(cssName, value);
            case PropertyValueImp.VALUE_TYPE_COLOR:
                FSDerivedValue color = CACHED_COLORS.get(value.getCssText());
                if (color == null) {
                    color = new ColorValue(cssName, value);
                    CACHED_COLORS.put(value.getCssText(), color);
                }
                return color;
            case PropertyValueImp.VALUE_TYPE_LIST:
                return new ListValue(cssName, value);
            case PropertyValueImp.VALUE_TYPE_FUNCTION:
                return new FunctionValue(cssName, value);
            default:
                throw new IllegalArgumentException();
        }
    }
}

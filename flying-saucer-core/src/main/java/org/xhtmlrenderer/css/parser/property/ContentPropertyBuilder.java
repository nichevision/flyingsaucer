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
package org.xhtmlrenderer.css.parser.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.CSSParseException;
import org.xhtmlrenderer.css.parser.FSFunction;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.parser.PropertyValueImp.CSSValueType;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;

import static org.xhtmlrenderer.css.parser.property.BuilderUtil.*;

public class ContentPropertyBuilder implements PropertyBuilder {

    public List<PropertyDeclaration> buildDeclarations(
            final CSSName cssName, final List<PropertyValue> values, final CSSOrigin origin, final boolean important, final boolean inheritAllowed) {
        if (values.size() == 1) {
            final PropertyValue value = values.get(0);
            if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
                return Collections.emptyList();
            } else if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                final IdentValue ident = checkIdent(CSSName.CONTENT, value);
                if (ident == IdentValue.NONE || ident == IdentValue.NORMAL) {
                    return Collections.singletonList(
                            new PropertyDeclaration(CSSName.CONTENT, value, important, origin));
                }
            }
        }
        
        final List<PropertyValue> resultValues = new ArrayList<PropertyValue>();
        for (final PropertyValue value : values) {
            if (value.getOperator() != null) {
                throw new CSSParseException(
                        "Found unexpected operator, " + value.getOperator().getExternalName(), -1);
            }
            
            final CSSPrimitiveUnit type = value.getPrimitiveTypeN();
            if (type == CSSPrimitiveUnit.CSS_URI) {
                continue;
            } else if (type == CSSPrimitiveUnit.CSS_STRING) {
                resultValues.add(value);
            } else if (value.getPropertyValueType() == PropertyValueImp.VALUE_TYPE_FUNCTION) {
                if (! isFunctionAllowed(value.getFunction())) {
                    throw new CSSParseException(
                            "Function " + value.getFunction().getName() + " is not allowed here", -1);
                }
                resultValues.add(value);
            } else if (type == CSSPrimitiveUnit.CSS_IDENT) {
                final IdentValue ident = checkIdent(CSSName.CONTENT, value);
                if (ident == IdentValue.OPEN_QUOTE || ident == IdentValue.CLOSE_QUOTE ||
                        ident == IdentValue.NO_CLOSE_QUOTE || ident == IdentValue.NO_OPEN_QUOTE) {
                    resultValues.add(value);
                } else {
                    throw new CSSParseException(
                            "Identifier " + ident + " is not a valid value for the content property", -1);
                }
            } else {
                throw new CSSParseException(
                        value.getCssText() + " is not a value value for the content property", -1);
            }
        }
        
        if (resultValues.size() > 0) {
            return Collections.singletonList(
                    new PropertyDeclaration(CSSName.CONTENT, new PropertyValueImp(resultValues), important, origin));
        } else {
            return Collections.emptyList();
        }
    }
    
    private boolean isFunctionAllowed(final FSFunction function) {
        final String name = function.getName();
        return name.equals("attr") || name.equals("counter") || name.equals("counters") ||
            name.equals("element") || name.startsWith("-fs") || name.equals("target-counter") ||
            name.equals("leader");
    }
}

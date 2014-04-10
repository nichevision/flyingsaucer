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
import org.xhtmlrenderer.css.parser.CSSParseException;
import org.xhtmlrenderer.css.parser.CounterData;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.parser.PropertyValueImp.CSSValueType;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import static org.xhtmlrenderer.css.parser.property.BuilderUtil.*;

public abstract class CounterPropertyBuilder implements PropertyBuilder {
    // [ <identifier> <integer>? ]+ | none | inherit 
    
    protected abstract int getDefaultValue();
    
    // XXX returns a PropertyValue of type VALUE_TYPE_LIST, but the List contains
    // CounterData objects and not PropertyValue objects
    public List<PropertyDeclaration> buildDeclarations(final CSSName cssName, final List<PropertyValue> values, final CSSOrigin origin, final boolean important, final boolean inheritAllowed) {
        if (values.size() == 1) {
            final PropertyValue value = (PropertyValue)values.get(0);
            
            checkInheritAllowed(value, inheritAllowed);
            
            if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
                return Collections.singletonList(new PropertyDeclaration(cssName, value, important, origin));
            } else if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                if (value.getCssText().equals("none")) {
                    return Collections.singletonList(new PropertyDeclaration(cssName, value, important, origin));
                } else {
                    final CounterData data = new CounterData(
                            value.getStringValue(),
                            getDefaultValue());
                    
                    return Collections.singletonList(
                            new PropertyDeclaration(cssName, new PropertyValueImp(
                                    Collections.singletonList(data)), important, origin));
                }
            }
            
            throw new CSSParseException("The syntax of the " + cssName + " property is invalid", -1);
        } else {
            final List<CounterData> result = new ArrayList<CounterData>();
            for (int i = 0; i < values.size(); i++) {
                final PropertyValue value = (PropertyValue)values.get(i);
                
                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    final String name = value.getStringValue();
                    int cValue = getDefaultValue();
                    
                    if (i < values.size() - 1) {
                        final PropertyValue next = (PropertyValue)values.get(i+1);
                        if (next.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_NUMBER) {
                            checkNumberIsInteger(cssName, next);
                            
                            cValue = (int)next.getFloatValue();
                        } 
                        
                        i++;
                    }
                    result.add(new CounterData(name, cValue));
                } else {
                    throw new CSSParseException("The syntax of the " + cssName + " property is invalid", -1);
                }
            }
            
            return Collections.singletonList(
                    new PropertyDeclaration(cssName, new PropertyValueImp(result), important, origin));
        }
    }
    
    private void checkNumberIsInteger(final CSSName cssName, final PropertyValue value) {
        if ((int)value.getFloatValue() !=
                    Math.round(value.getFloatValue())) {
            throw new CSSParseException("The value " + value.getFloatValue() + " in " + 
                    cssName + " must be an integer", -1);
        }
    }
    
    public static class CounterReset extends CounterPropertyBuilder {
        protected int getDefaultValue() {
            return 0;
        }
    }

    public static class CounterIncrement extends CounterPropertyBuilder {
        protected int getDefaultValue() {
            return 1;
        }
    }
}

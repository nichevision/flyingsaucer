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
import java.util.List;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.CSSParseException;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.parser.PropertyValueImp.CSSValueType;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;

import static org.xhtmlrenderer.css.parser.property.BuilderUtil.*;

public class BorderPropertyBuilders {
    private static abstract class BorderSidePropertyBuilder implements PropertyBuilder {
        protected abstract CSSName[][] getProperties();
        
        private void addAll(List<PropertyDeclaration> result, CSSName[] properties, PropertyValue value, CSSOrigin origin, boolean important) {
            for (CSSName property : properties) {
                result.add(new PropertyDeclaration(
                        property, value, important, origin));
            }
        }
        
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            CSSName[][] props = getProperties();
            
            List<PropertyDeclaration> result = new ArrayList<PropertyDeclaration>(3);
            
            if (values.size() == 1 && 
                (values.get(0)).getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
                PropertyValue value = values.get(0);
                addAll(result, props[0], value, origin, important);
                addAll(result, props[1], value, origin, important);
                addAll(result, props[2], value, origin, important);
                
                return result;
            } else {
                checkValueCount(cssName, 1, 3, values.size());
                boolean haveBorderStyle = false;
                boolean haveBorderColor = false;
                boolean haveBorderWidth = false;
                
                for (PropertyValue value : values) {
                    checkInheritAllowed(value, false);
                    boolean matched = false;
                    PropertyValue borderWidth = convertToBorderWidth(value);
                    if (borderWidth != null) {
                        if (haveBorderWidth) {
                            throw new CSSParseException("A border width cannot be set twice", -1);
                        }
                        haveBorderWidth = true;
                        matched = true;
                        addAll(result, props[0], borderWidth, origin, important);
                    }
                    
                    if (isBorderStyle(value)) {
                        if (haveBorderStyle) {
                            throw new CSSParseException("A border style cannot be set twice", -1);
                        }
                        haveBorderStyle = true;
                        matched = true;
                        addAll(result, props[1], value, origin, important);
                    }
                    
                    PropertyValue borderColor = convertToBorderColor(value);
                    if (borderColor != null) {
                        if (haveBorderColor) {
                            throw new CSSParseException("A border color cannot be set twice", -1);
                        }
                        haveBorderColor = true;
                        matched = true;
                        addAll(result, props[2], borderColor, origin, important);
                    }
                    
                    if (! matched) {
                        throw new CSSParseException(value.getCssText() + " is not a border width, style, or color", -1);
                    }
                }
                
                if (! haveBorderWidth) {
                    addAll(result, props[0], new PropertyValueImp(IdentValue.FS_INITIAL_VALUE), origin, important);
                }
                
                if (! haveBorderStyle) {
                    addAll(result, props[1], new PropertyValueImp(IdentValue.FS_INITIAL_VALUE), origin, important);
                }
                
                if (! haveBorderColor) {
                    addAll(result, props[2], new PropertyValueImp(IdentValue.FS_INITIAL_VALUE), origin, important);
                }
                
                return result;
            }
        }
        
        private boolean isBorderStyle(PropertyValue value) {
            if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_IDENT) {
                return false;
            }
            
            IdentValue ident = IdentValue.fsValueOf(value.getCssText());
            if (ident == null) {
                return false;
            }
            
            return PrimitivePropertyBuilders.BORDER_STYLES.contains(ident);
        }
        
        private PropertyValue convertToBorderWidth(PropertyValue value) {
        	CSSPrimitiveUnit type = value.getPrimitiveTypeN();
            if (type != CSSPrimitiveUnit.CSS_IDENT && ! isLength(value)) {
                return null;
            }
            
            if (isLength(value)) {
                return value;
            } else {
                IdentValue ident = IdentValue.fsValueOf(value.getStringValue());
                if (ident == null) {
                    return null;
                }
                
                if (PrimitivePropertyBuilders.BORDER_WIDTHS.contains(ident)) {
                    return Conversions.getBorderWidth(ident.toString());
                } else {
                    return null;
                }
            }
        } 
        
        private PropertyValue convertToBorderColor(PropertyValue value) {
        	CSSPrimitiveUnit type = value.getPrimitiveTypeN();
            if (type != CSSPrimitiveUnit.CSS_IDENT && type != CSSPrimitiveUnit.CSS_RGBCOLOR) {
                return null;
            }
            
            if (type == CSSPrimitiveUnit.CSS_RGBCOLOR) {
                return value;
            } else {
                FSRGBColor color = Conversions.getColor(value.getStringValue());
                if (color != null) {
                    return new PropertyValueImp(color);
                }
                
                IdentValue ident = IdentValue.fsValueOf(value.getCssText());
                if (ident == null || ident != IdentValue.TRANSPARENT) {
                    return null;
                }
                
                return value;
            }
        }
    }
    
    public static class BorderTop extends BorderSidePropertyBuilder {
        protected CSSName[][] getProperties() {
            return new CSSName[][] { 
                    new CSSName[] { CSSName.BORDER_TOP_WIDTH }, 
                    new CSSName[] { CSSName.BORDER_TOP_STYLE }, 
                    new CSSName[] { CSSName.BORDER_TOP_COLOR } };
        }
    }
    
    public static class BorderRight extends BorderSidePropertyBuilder {
        protected CSSName[][] getProperties() {
            return new CSSName[][] { 
                    new CSSName[] { CSSName.BORDER_RIGHT_WIDTH }, 
                    new CSSName[] { CSSName.BORDER_RIGHT_STYLE }, 
                    new CSSName[] { CSSName.BORDER_RIGHT_COLOR } };
        }
    }
    
    public static class BorderBottom extends BorderSidePropertyBuilder {
        protected CSSName[][] getProperties() {
            return new CSSName[][] { 
                    new CSSName[] { CSSName.BORDER_BOTTOM_WIDTH }, 
                    new CSSName[] { CSSName.BORDER_BOTTOM_STYLE }, 
                    new CSSName[] { CSSName.BORDER_BOTTOM_COLOR } };
        }
    }
    
    public static class BorderLeft extends BorderSidePropertyBuilder {
        protected CSSName[][] getProperties() {
            return new CSSName[][] { 
                    new CSSName[] { CSSName.BORDER_LEFT_WIDTH }, 
                    new CSSName[] { CSSName.BORDER_LEFT_STYLE }, 
                    new CSSName[] { CSSName.BORDER_LEFT_COLOR } };
        }
    }
    
    public static class Border extends BorderSidePropertyBuilder {
        protected CSSName[][] getProperties() {
            return new CSSName[][] { 
                    new CSSName[] { 
                            CSSName.BORDER_TOP_WIDTH, CSSName.BORDER_RIGHT_WIDTH,
                            CSSName.BORDER_BOTTOM_WIDTH, CSSName.BORDER_LEFT_WIDTH },
                    new CSSName[] { 
                            CSSName.BORDER_TOP_STYLE, CSSName.BORDER_RIGHT_STYLE,
                            CSSName.BORDER_BOTTOM_STYLE, CSSName.BORDER_LEFT_STYLE },                            
                    new CSSName[] { 
                            CSSName.BORDER_TOP_COLOR, CSSName.BORDER_RIGHT_COLOR,
                            CSSName.BORDER_BOTTOM_COLOR, CSSName.BORDER_LEFT_COLOR } };                            
        }
    } 
}

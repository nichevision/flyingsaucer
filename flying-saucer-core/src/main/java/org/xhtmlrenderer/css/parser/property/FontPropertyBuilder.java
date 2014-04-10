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
import java.util.ListIterator;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.CSSParseException;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.parser.Token;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import static org.xhtmlrenderer.css.parser.property.BuilderUtil.*;

public class FontPropertyBuilder implements PropertyBuilder {
    // [ [ <'font-style'> || <'font-variant'> || <'font-weight'> ]? <'font-size'> [ / <'line-height'> ]? <'font-family'> ] 
    private static final CSSName[] ALL = new CSSName[] {
        CSSName.FONT_STYLE, CSSName.FONT_VARIANT, CSSName.FONT_WEIGHT, 
        CSSName.FONT_SIZE, CSSName.LINE_HEIGHT, CSSName.FONT_FAMILY };
    
    public List<PropertyDeclaration> buildDeclarations(
            final CSSName cssName, final List<PropertyValue> values, final CSSOrigin origin, final boolean important, final boolean inheritAllowed) {
        List<PropertyDeclaration> result = checkInheritAll(ALL, values, origin, important, inheritAllowed);
        if (result != null) {
            return result;
        }

        PropertyDeclaration fontStyle = null;
        PropertyDeclaration fontVariant = null;
        PropertyDeclaration fontWeight = null;
        PropertyDeclaration fontSize = null;
        PropertyDeclaration lineHeight = null;
        PropertyDeclaration fontFamily = null;
        
        boolean keepGoing = false;
        
        final ListIterator<PropertyValue> i = values.listIterator();
        while (i.hasNext()) {
            PropertyValue value = (PropertyValue)i.next();
            final CSSPrimitiveUnit type = value.getPrimitiveTypeN();
            if (type == CSSPrimitiveUnit.CSS_IDENT) {
                // The parser will have given us ident values as they appear
                // (case-wise) in the CSS text since we might be creating
                // a font-family list out of them.  Here we want the normalized
                // (lowercase) version though.
                final String lowerCase = value.getStringValue().toLowerCase();
                value = new PropertyValueImp(CSSPrimitiveUnit.CSS_IDENT, lowerCase, lowerCase);
                final IdentValue ident = checkIdent(cssName, value);
                if (ident == IdentValue.NORMAL) { // skip to avoid double set false positives
                    continue;
                }
                if (PrimitivePropertyBuilders.FONT_STYLES.contains(ident)) {
                    if (fontStyle != null) {
                        throw new CSSParseException("font-style cannot be set twice", -1);
                    }
                    fontStyle = new PropertyDeclaration(CSSName.FONT_STYLE, value, important, origin);
                } else if (PrimitivePropertyBuilders.FONT_VARIANTS.contains(ident)) {
                    if (fontVariant != null) {
                        throw new CSSParseException("font-variant cannot be set twice", -1);
                    }
                    fontVariant = new PropertyDeclaration(CSSName.FONT_VARIANT, value, important, origin);
                } else if (PrimitivePropertyBuilders.FONT_WEIGHTS.contains(ident)) {
                    if (fontWeight != null) {
                        throw new CSSParseException("font-weight cannot be set twice", -1);
                    }
                    fontWeight = new PropertyDeclaration(CSSName.FONT_WEIGHT, value, important, origin);
                } else {
                    keepGoing = true;
                    break;
                }
            } else if (type == CSSPrimitiveUnit.CSS_NUMBER && value.getFloatValue() > 0) {
                if (fontWeight != null) {
                    throw new CSSParseException("font-weight cannot be set twice", -1);
                }
                
                final IdentValue weight = Conversions.getNumericFontWeight(value.getFloatValue());
                if (weight == null) {
                    throw new CSSParseException(value + " is not a valid font weight", -1);
                }
                
                final PropertyValue replacement = new PropertyValueImp(
                		CSSPrimitiveUnit.CSS_IDENT, weight.toString(), weight.toString());
                replacement.setIdentValue(weight);
                
                fontWeight = new PropertyDeclaration(CSSName.FONT_WEIGHT, replacement, important, origin);
            } else {
                keepGoing = true;
                break;
            }
        }
        
        if (keepGoing) {
            i.previous();
            PropertyValue value = (PropertyValue)i.next();
            
            if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                final String lowerCase = value.getStringValue().toLowerCase();
                value = new PropertyValueImp(CSSPrimitiveUnit.CSS_IDENT, lowerCase, lowerCase);
            }
            
            final PropertyBuilder fontSizeBuilder = CSSName.getPropertyBuilder(CSSName.FONT_SIZE);
            List<PropertyDeclaration> l = fontSizeBuilder.buildDeclarations(
                    CSSName.FONT_SIZE, Collections.singletonList(value), origin, important, true);
            
            fontSize = (PropertyDeclaration)l.get(0);
            
            if (i.hasNext()) {
                value = (PropertyValue)i.next();
                if (value.getOperator() == Token.TK_VIRGULE) {
                    final PropertyBuilder lineHeightBuilder = CSSName.getPropertyBuilder(CSSName.LINE_HEIGHT);
                    l = lineHeightBuilder.buildDeclarations(
                            CSSName.LINE_HEIGHT, Collections.singletonList(value), origin, important, true);
                    lineHeight = (PropertyDeclaration)l.get(0);
                } else {
                    i.previous();
                }
            }
            
            if (i.hasNext()) {
                final List<PropertyValue> families = new ArrayList<PropertyValue>();
                while (i.hasNext()) {
                    families.add(i.next());
                }
                final PropertyBuilder fontFamilyBuilder = CSSName.getPropertyBuilder(CSSName.FONT_FAMILY);
                l = fontFamilyBuilder.buildDeclarations(
                        CSSName.FONT_FAMILY, families, origin, important, true);
                fontFamily = (PropertyDeclaration)l.get(0);
            }
        }
        
        if (fontStyle == null) {
            fontStyle = new PropertyDeclaration(
                    CSSName.FONT_STYLE, new PropertyValueImp(IdentValue.NORMAL), important, origin);
        }
        
        if (fontVariant == null) {
            fontVariant = new PropertyDeclaration(
                    CSSName.FONT_VARIANT, new PropertyValueImp(IdentValue.NORMAL), important, origin);
        }
        
        if (fontWeight == null) {
            fontWeight = new PropertyDeclaration(
                    CSSName.FONT_WEIGHT, new PropertyValueImp(IdentValue.NORMAL), important, origin);
        }
        
        if (fontSize == null) {
            throw new CSSParseException("A font-size value is required", -1);
        }
        
        if (lineHeight == null) {
            lineHeight = new PropertyDeclaration(
                    CSSName.LINE_HEIGHT, new PropertyValueImp(IdentValue.NORMAL), important, origin);
        }
        
        // XXX font-family should be reset too (although does this really make sense?)
        
        result = new ArrayList<PropertyDeclaration>(ALL.length);
        result.add(fontStyle);
        result.add(fontVariant);
        result.add(fontWeight);
        result.add(fontSize);
        result.add(lineHeight);
        if (fontFamily != null) {
            result.add(fontFamily);
        }
        
        return result;
    }
}

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
import java.util.Locale;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.CSSParseException;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import static org.xhtmlrenderer.css.parser.property.BuilderUtil.*;

public class BackgroundPropertyBuilder implements PropertyBuilder {
    // [<'background-color'> || <'background-image'> || <'background-repeat'> || 
    // <'background-attachment'> || <'background-position'>] | inherit 
    private static final CSSName[] ALL = {
        CSSName.BACKGROUND_COLOR, CSSName.BACKGROUND_IMAGE, CSSName.BACKGROUND_REPEAT,
        CSSName.BACKGROUND_ATTACHMENT, CSSName.BACKGROUND_POSITION };
    
    private boolean isAppliesToBackgroundPosition(final PropertyValue value) {
    	final CSSPrimitiveUnit type = value.getPrimitiveTypeN();
        
        if (isLength(value) || type == CSSPrimitiveUnit.CSS_PERCENTAGE) {
            return true;
        } else if (type != CSSPrimitiveUnit.CSS_IDENT) {
            return false;
        } else {
            final IdentValue ident = IdentValue.fsValueOf(value.getStringValue());
            return ident != null && 
                PrimitivePropertyBuilders.BACKGROUND_POSITIONS.contains(ident);
        }
    }

    public List<PropertyDeclaration> buildDeclarations(
            final CSSName cssName, final List<PropertyValue> values, final CSSOrigin origin, final boolean important, final boolean inheritAllowed) {
        List<PropertyDeclaration> result = checkInheritAll(ALL, values, origin, important, inheritAllowed);
        if (result != null) {
            return result;
        }
        
        PropertyDeclaration backgroundColor = null;
        PropertyDeclaration backgroundImage = null;
        PropertyDeclaration backgroundRepeat = null;
        PropertyDeclaration backgroundAttachment =  null;
        PropertyDeclaration backgroundPosition = null;
        
        for (int i = 0; i < values.size(); i++) {
            final PropertyValue value = (PropertyValue)values.get(i);
            checkInheritAllowed(value, false);
            
            boolean processingBackgroundPosition = false;
            final CSSPrimitiveUnit type = value.getPrimitiveTypeN();
            if (type == CSSPrimitiveUnit.CSS_IDENT) {
                final FSRGBColor color = Conversions.getColor(value.getStringValue());
                if (color != null) {
                    if (backgroundColor != null) {
                        throw new CSSParseException("A background-color value cannot be set twice", -1);
                    }
                    
                    backgroundColor = new PropertyDeclaration(
                            CSSName.BACKGROUND_COLOR, 
                            new PropertyValueImp(color), 
                            important, origin);
                    continue;
                }
                
                final IdentValue ident = checkIdent(CSSName.BACKGROUND_SHORTHAND, value);
                
                if (PrimitivePropertyBuilders.BACKGROUND_REPEATS.contains(ident)) {
                    if (backgroundRepeat != null) {
                        throw new CSSParseException("A background-repeat value cannot be set twice", -1);
                    }
                    
                    backgroundRepeat = new PropertyDeclaration(
                            CSSName.BACKGROUND_REPEAT, value, important, origin);
                }
                
                if (PrimitivePropertyBuilders.BACKGROUND_ATTACHMENTS.contains(ident)) {
                    if (backgroundAttachment != null) {
                        throw new CSSParseException("A background-attachment value cannot be set twice", -1);
                    }
                    
                    backgroundAttachment = new PropertyDeclaration(
                            CSSName.BACKGROUND_ATTACHMENT, value, important, origin);
                }
                
                if (ident == IdentValue.TRANSPARENT) {
                    if (backgroundColor != null) {
                        throw new CSSParseException("A background-color value cannot be set twice", -1);
                    }
                    
                    backgroundColor = new PropertyDeclaration(
                            CSSName.BACKGROUND_COLOR, value, important, origin);
                }
                
                if (ident == IdentValue.NONE) {
                    if (backgroundImage != null) {
                        throw new CSSParseException("A background-image value cannot be set twice", -1);
                    }
                    
                    backgroundImage = new PropertyDeclaration(
                            CSSName.BACKGROUND_IMAGE, value, important, origin);
                }
                
                if (PrimitivePropertyBuilders.BACKGROUND_POSITIONS.contains(ident)) {
                    processingBackgroundPosition = true;
                }
            } else if (type == CSSPrimitiveUnit.CSS_RGBCOLOR) {
                if (backgroundColor != null) {
                    throw new CSSParseException("A background-color value cannot be set twice", -1);
                }
                
                backgroundColor = new PropertyDeclaration(
                        CSSName.BACKGROUND_COLOR, value, important, origin);
            } else if (type == CSSPrimitiveUnit.CSS_URI || value.toString().toLowerCase(Locale.US).startsWith(IdentValue.LINEAR_GRADIENT.asString())) {
                if (backgroundImage != null) {
                    throw new CSSParseException("A background-image value cannot be set twice", -1);
                }
                
                backgroundImage = new PropertyDeclaration(
                        CSSName.BACKGROUND_IMAGE, value, important, origin);
            }
            
            if (processingBackgroundPosition || isLength(value) || type == CSSPrimitiveUnit.CSS_PERCENTAGE) {
                if (backgroundPosition != null) {
                    throw new CSSParseException("A background-position value cannot be set twice", -1);
                }
                
                final List<PropertyValue> v = new ArrayList<PropertyValue>(2);
                v.add(value);
                if (i < values.size() - 1) {
                    final PropertyValue next = (PropertyValue)values.get(i+1);
                    if (isAppliesToBackgroundPosition(next)) {
                        v.add(next);
                        i++;
                    }
                }
                
                final PropertyBuilder builder = CSSName.getPropertyBuilder(CSSName.BACKGROUND_POSITION);
                backgroundPosition = (PropertyDeclaration)builder.buildDeclarations(
                        CSSName.BACKGROUND_POSITION, v, origin, important, true).get(0);
            }
        }
        
        if (backgroundColor == null) {
            backgroundColor = new PropertyDeclaration(
                    CSSName.BACKGROUND_COLOR, new PropertyValueImp(IdentValue.TRANSPARENT), important, origin);
        }
        
        if (backgroundImage == null) {
            backgroundImage = new PropertyDeclaration(
                    CSSName.BACKGROUND_IMAGE, new PropertyValueImp(IdentValue.NONE), important, origin);
        }
        
        if (backgroundRepeat == null) {
            backgroundRepeat = new PropertyDeclaration(
                    CSSName.BACKGROUND_REPEAT, new PropertyValueImp(IdentValue.REPEAT), important, origin);
        }
        
        if (backgroundAttachment == null) {
            backgroundAttachment = new PropertyDeclaration(
                    CSSName.BACKGROUND_ATTACHMENT, new PropertyValueImp(IdentValue.SCROLL), important, origin);
            
        }
        
        if (backgroundPosition == null) {
            final List<PropertyValue> v = new ArrayList<PropertyValue>(2);
            v.add(new PropertyValueImp(CSSPrimitiveUnit.CSS_PERCENTAGE, 0.0f, "0%"));
            v.add(new PropertyValueImp(CSSPrimitiveUnit.CSS_PERCENTAGE, 0.0f, "0%"));
            backgroundPosition = new PropertyDeclaration(
                    CSSName.BACKGROUND_POSITION, new PropertyValueImp(v), important, origin);
        }
        
        result = new ArrayList<PropertyDeclaration>(5);
        result.add(backgroundColor);
        result.add(backgroundImage);
        result.add(backgroundRepeat);
        result.add(backgroundAttachment);
        result.add(backgroundPosition);
        
        return result;
    }
}

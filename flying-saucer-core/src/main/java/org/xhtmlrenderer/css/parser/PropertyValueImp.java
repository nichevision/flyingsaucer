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
package org.xhtmlrenderer.css.parser;

import java.util.List;
import java.util.ArrayList;

import org.w3c.dom.DOMException;
import org.w3c.dom.css.Counter;
import org.w3c.dom.css.RGBColor;
import org.w3c.dom.css.Rect;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.util.ArrayUtil;

public class PropertyValueImp implements PropertyValue {
    public static final short VALUE_TYPE_NUMBER = 1;
    public static final short VALUE_TYPE_LENGTH = 2;
    public static final short VALUE_TYPE_COLOR = 3;
    public static final short VALUE_TYPE_IDENT = 4;
    public static final short VALUE_TYPE_STRING = 5;
    public static final short VALUE_TYPE_LIST = 6;
    public static final short VALUE_TYPE_FUNCTION = 7;
    
    public static enum CSSValueType
    {
        CSS_INHERIT,
        CSS_CUSTOM,
        CSS_PRIMITIVE_VALUE,
        CSS_VALUE_LIST;
    }
    
    
    private final CSSPrimitiveUnit _type;
    private final CSSValueType _cssValueType;
    
    private String _stringValue;
    private float _floatValue;
    private String[] _stringArrayValue;
    
    private final String _cssText;
    
    private FSColor _FSColor;
    
    private IdentValue _identValue;
    
    private short _propertyValueType;
    
    private Token _operator;
    
    private List<?> _values;
    private FSFunction _function;

    public PropertyValueImp(final CSSPrimitiveUnit type, final float floatValue, final String cssText) {
        _type = type;
        _floatValue = floatValue;
        _cssValueType = CSSValueType.CSS_PRIMITIVE_VALUE;
        _cssText = cssText;
        
        if (type == CSSPrimitiveUnit.CSS_NUMBER && floatValue != 0.0f) {
            _propertyValueType = VALUE_TYPE_NUMBER;
        } else {
            _propertyValueType = VALUE_TYPE_LENGTH;
        }
    }
    
    public PropertyValueImp(final FSColor color) {
        _type = CSSPrimitiveUnit.CSS_RGBCOLOR;
        _cssValueType = CSSValueType.CSS_PRIMITIVE_VALUE;
        _cssText = color.toString();
        _FSColor = color;
        
        _propertyValueType = VALUE_TYPE_COLOR;
    }
    
    public PropertyValueImp(final CSSPrimitiveUnit type, final String stringValue, final String cssText) {
        _type = type;
        _stringValue = stringValue;
        // Must be a case-insensitive compare since ident values aren't normalized
        // for font and font-family
        _cssValueType = _stringValue.equalsIgnoreCase("inherit") ? CSSValueType.CSS_INHERIT : CSSValueType.CSS_PRIMITIVE_VALUE;
        _cssText = cssText;
        
        if (type == CSSPrimitiveUnit.CSS_IDENT) {
            _propertyValueType = VALUE_TYPE_IDENT;
        } else {
            _propertyValueType = VALUE_TYPE_STRING;
        }
    }
    
    public PropertyValueImp(final IdentValue ident) {
        _type = CSSPrimitiveUnit.CSS_IDENT;
        _stringValue = ident.toString();
        _cssValueType = _stringValue.equals("inherit") ? CSSValueType.CSS_INHERIT : CSSValueType.CSS_PRIMITIVE_VALUE;
        _cssText = ident.toString();
        
        _propertyValueType = VALUE_TYPE_IDENT;
        _identValue = ident;
    }
    
    public PropertyValueImp(final List<?> values) {
        _type = CSSPrimitiveUnit.CSS_UNKNOWN; // HACK
        _cssValueType = CSSValueType.CSS_CUSTOM;
        _cssText = values.toString(); // HACK
        
        _values = values;
        _propertyValueType = VALUE_TYPE_LIST;
    }
    
    public PropertyValueImp(final FSFunction function) {
        _type = CSSPrimitiveUnit.CSS_UNKNOWN;
        _cssValueType = CSSValueType.CSS_CUSTOM;
        _cssText = function.toString();
        
        _function = function;
        _propertyValueType = VALUE_TYPE_FUNCTION;
    }

    public Counter getCounterValue() throws DOMException {
        throw new UnsupportedOperationException();
    }

    public float getFloatValue(final CSSPrimitiveUnit unitType) throws DOMException {
        return _floatValue;
    }
    
    public float getFloatValue() {
        return _floatValue;
    }

    public CSSPrimitiveUnit getPrimitiveTypeN() {
        return _type;
    }

    public RGBColor getRGBColorValue() throws DOMException {
        throw new UnsupportedOperationException();
    }

    public Rect getRectValue() throws DOMException {
        throw new UnsupportedOperationException();
    }

    public String getStringValue() throws DOMException {
        return _stringValue;
    }

    public void setFloatValue(final short unitType, final float floatValue) throws DOMException {
        throw new UnsupportedOperationException();
    }

    public void setStringValue(final short stringType, final String stringValue) throws DOMException {
        throw new UnsupportedOperationException();
    }

    public String getCssText() {
        return _cssText;
    }

    @Deprecated
    public short getCssValueType() {
        assert(false);
        throw new UnsupportedOperationException();
    }

    public void setCssText(final String cssText) throws DOMException {
        throw new UnsupportedOperationException();
    }
    
    public FSColor getFSColor() {
        return _FSColor;
    }

    public IdentValue getIdentValue() {
        return _identValue;
    }

    public void setIdentValue(final IdentValue identValue) {
        _identValue = identValue;
    }
    
    public short getPropertyValueType() {
        return _propertyValueType;
    }

    public Token getOperator() {
        return _operator;
    }

    public void setOperator(final Token operator) {
        _operator = operator;
    }

    public String[] getStringArrayValue() {
        return ArrayUtil.cloneOrEmpty(_stringArrayValue);
    }

    public void setStringArrayValue(final String[] stringArrayValue) {
        _stringArrayValue = ArrayUtil.cloneOrEmpty(stringArrayValue);
    }
    
    @Override
    public String toString() {
        return _cssText;
    }
    
    public List<?> getValues() {
        return new ArrayList<Object>(_values);
    }
    
    public FSFunction getFunction() {
        return _function;
    }
    
    public String getFingerprint() {
        if (getPropertyValueType() == VALUE_TYPE_IDENT) {
            if (_identValue == null) {
                _identValue = IdentValue.getByIdentString(getStringValue());
            }
            return "I" + _identValue.fsId;
        } else {
            return getCssText();
        }
    }

	@Deprecated
	public short getPrimitiveType() 
	{
		assert(false);
		throw new UnsupportedOperationException();
	}

	@Deprecated
	public float getFloatValue(final short arg0) throws DOMException {
		assert(false);
		throw new UnsupportedOperationException();
	}

	@Override
	public CSSValueType getCssValueTypeN() 
	{
		return _cssValueType;
	}
}

/*
 * CalculatedStyle.java
 * Copyright (c) 2004, 2005 Patrick Wright, Torbjoern Gannholm
 * Copyright (c) 2006 Wisconsin Court System
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
 *
 */
package org.xhtmlrenderer.css.style;

import java.awt.Cursor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSFunction;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.property.PrimitivePropertyBuilders;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.css.style.derived.DerivedValueFactory;
import org.xhtmlrenderer.css.style.derived.FSLinearGradient;
import org.xhtmlrenderer.css.style.derived.FunctionValue;
import org.xhtmlrenderer.css.style.derived.LengthValue;
import org.xhtmlrenderer.css.style.derived.ListValue;
import org.xhtmlrenderer.css.style.derived.NumberValue;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.util.GeneralUtil;
import org.xhtmlrenderer.util.XRLog;
import org.xhtmlrenderer.util.XRRuntimeException;


/**
 * A set of properties that apply to a single Element, derived from all matched
 * properties following the rules for CSS cascade, inheritance, importance,
 * specificity and sequence. A derived style is just like a style but
 * (presumably) has additional information that allows relative properties to be
 * assigned values, e.g. font attributes. Property values are fully resolved
 * when this style is created. A property retrieved by name should always have
 * only one value in this class (e.g. one-one map). Any methods to retrieve
 * property values from an instance of this class require a valid {@link
 * org.xhtmlrenderer.layout.Context} be given to it, for some cases of property
 * resolution. Generally, a programmer will not use this class directly, but
 * will retrieve properties using a {@link org.xhtmlrenderer.context.StyleReference}
 * implementation.
 *
 * @author Torbjoern Gannholm
 * @author Patrick Wright
 */
public class CalculatedStyle {
    /**
     * The parent-style we inherit from
     */
    private CalculatedStyle _parent;

    private BorderPropertySet _border;
    private RectPropertySet _margin;
    private RectPropertySet _padding;

    private float _lineHeight;
    private boolean _lineHeightResolved;

    private FSFont _FSFont;
    private FSFontMetrics _FSFontMetrics;

    private boolean _marginsAllowed = true;
    private boolean _paddingAllowed = true;
    private boolean _bordersAllowed = true;

    private BackgroundSize _backgroundSize;

    /**
     * Cache child styles of this style that have the same cascaded properties
     */
    private final java.util.HashMap<String, CalculatedStyle> _childCache = new java.util.HashMap<String, CalculatedStyle>();
    /*private java.util.HashMap _childCache = new java.util.LinkedHashMap(5, 0.75f, true) {
        private static final int MAX_ENTRIES = 10;

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_ENTRIES;
        }
    };*/

    /**
     * Our main array of property values defined in this style, keyed
     * by the CSSName assigned ID.
     */
    private final FSDerivedValue[] _derivedValuesById;

    /**
     * The derived Font for this style
     */
    private FontSpecification _font;


    /**
     * Default constructor; as the instance is immutable after use, don't use
     * this for class instantiation externally.
     */
    protected CalculatedStyle() {
        _derivedValuesById = new FSDerivedValue[CSSName.countCSSNames()];
    }


    /**
     * Constructor for the CalculatedStyle object. To get a derived style, use
     * the Styler objects getDerivedStyle which will cache styles
     *
     * @param parent  PARAM
     * @param matched PARAM
     */
    private CalculatedStyle(final CalculatedStyle parent, final CascadedStyle matched) {
        this();
        _parent = parent;

        derive(matched);

        checkPaddingAllowed();
        checkMarginsAllowed();
        checkBordersAllowed();
    }

    private void checkPaddingAllowed() {
        final IdentValue v = getIdent(CSSName.DISPLAY);
        if (v == IdentValue.TABLE_HEADER_GROUP || v == IdentValue.TABLE_ROW_GROUP ||
                v == IdentValue.TABLE_FOOTER_GROUP || v == IdentValue.TABLE_ROW) {
            _paddingAllowed = false;
        } else if ((v == IdentValue.TABLE || v == IdentValue.INLINE_TABLE) && isCollapseBorders()) {
            _paddingAllowed = false;
        }
    }

    private void checkMarginsAllowed() {
        final IdentValue v = getIdent(CSSName.DISPLAY);
        if (v == IdentValue.TABLE_HEADER_GROUP || v == IdentValue.TABLE_ROW_GROUP ||
                v == IdentValue.TABLE_FOOTER_GROUP || v == IdentValue.TABLE_ROW ||
                v == IdentValue.TABLE_CELL) {
            _marginsAllowed = false;
        }
    }

    private void checkBordersAllowed() {
        final IdentValue v = getIdent(CSSName.DISPLAY);
        if (v == IdentValue.TABLE_HEADER_GROUP || v == IdentValue.TABLE_ROW_GROUP ||
                v == IdentValue.TABLE_FOOTER_GROUP || v == IdentValue.TABLE_ROW) {
            _bordersAllowed = false;
        }
    }

    /**
     * derives a child style from this style.
     * <p/>
     * depends on the ability to return the identical CascadedStyle each time a child style is needed
     *
     * @param matched the CascadedStyle to apply
     * @return The derived child style
     */
    public synchronized CalculatedStyle deriveStyle(final CascadedStyle matched) {
        final String fingerprint = matched.getFingerprint();
        CalculatedStyle cs = _childCache.get(fingerprint);

        if (cs == null) {
            cs = new CalculatedStyle(this, matched);
            _childCache.put(fingerprint, cs);
        }
        return cs;
    }

    public int countAssigned() {
        int c = 0;
        for (final FSDerivedValue derivedValue : _derivedValuesById) {
            if (derivedValue != null) c++;
        }
        return c;
    }

    /**
     * Returns the parent style.
     *
     * @return Returns the parent style
     */
    public CalculatedStyle getParent() {
        return _parent;
    }

    /**
     * Converts to a String representation of the object.
     *
     * @return The borderWidth value
     */
    public String toString() {
        return genStyleKey();
    }

    public FSColor asColor(final CSSName cssName) {
        final FSDerivedValue prop = valueByName(cssName);
        if (prop == IdentValue.TRANSPARENT) {
            return FSRGBColor.TRANSPARENT;
        } else {
            return prop.asColor();
        }
    }

    public float asFloat(final CSSName cssName) {
        return valueByName(cssName).asFloat();
    }

    public String asString(final CSSName cssName) {
        return valueByName(cssName).asString();
    }

    public String[] asStringArray(final CSSName cssName) {
        return valueByName(cssName).asStringArray();
    }

    public void setDefaultValue(final CSSName cssName, final FSDerivedValue fsDerivedValue) {
        if (_derivedValuesById[cssName.FS_ID] == null) {
            _derivedValuesById[cssName.FS_ID] = fsDerivedValue;
        }
    }

    // TODO: doc
    public boolean hasAbsoluteUnit(final CSSName cssName) {
        boolean isAbs = false;
        try {
            isAbs = valueByName(cssName).hasAbsoluteUnit();
        } catch (final Exception e) {
            XRLog.layout(Level.WARNING, "Property " + cssName + " has an assignment we don't understand, " +
                    "and can't tell if it's an absolute unit or not. Assuming it is not. Exception was: " +
                    e.getMessage());
            isAbs = false;
        }
        return isAbs;
    }

    /**
     * Gets the ident attribute of the CalculatedStyle object
     *
     * @param cssName PARAM
     * @param val     PARAM
     * @return The ident value
     */
    public boolean isIdent(final CSSName cssName, final IdentValue val) {
        return valueByName(cssName) == val;
    }

    /**
     * Gets the ident attribute of the CalculatedStyle object
     *
     * @param cssName PARAM
     * @return The ident value
     */
    public IdentValue getIdent(final CSSName cssName) {
        return valueByName(cssName).asIdentValue();
    }

    /**
     * Convenience property accessor; returns a Color initialized with the
     * foreground color Uses the actual value (computed actual value) for this
     * element.
     *
     * @return The color value
     */
    public FSColor getColor() {
        return asColor(CSSName.COLOR);
    }

    /**
     * Convenience property accessor; returns a Color initialized with the
     * background color value; Uses the actual value (computed actual value) for
     * this element.
     *
     * @return The backgroundColor value
     */
    public FSColor getBackgroundColor() {
        final FSDerivedValue prop = valueByName(CSSName.BACKGROUND_COLOR);
        if (prop == IdentValue.TRANSPARENT) {
            return null;
        } else {
            return asColor(CSSName.BACKGROUND_COLOR);
        }
    }

    public BackgroundSize getBackgroundSize() {
        if (_backgroundSize == null) {
            _backgroundSize = createBackgroundSize();
        }

        return _backgroundSize;
    }

    public BorderRadiusCorner getBorderRadiusCorner(final CSSName cssName)
    {
    	final FSDerivedValue value = valueByName(cssName);
    	final ListValue valueList = (ListValue) value;
    	final List<?> values = valueList.getValues();

    	final boolean firstAuto = ((PropertyValue)values.get(0)).getIdentValue() == IdentValue.AUTO;
        final boolean secondAuto = ((PropertyValue)values.get(1)).getIdentValue() == IdentValue.AUTO;

        if (firstAuto && secondAuto)
        {
        	return BorderRadiusCorner.ZERO;
        }
        else if (secondAuto)
        {
        	return new BorderRadiusCorner(((PropertyValue)values.get(0)), null);
        }
        else
        {
        	return new BorderRadiusCorner(((PropertyValue)values.get(0)), ((PropertyValue)values.get(1)));
        }
    }
    
    private BackgroundSize createBackgroundSize() {
        final FSDerivedValue value = valueByName(CSSName.BACKGROUND_SIZE);
        if (value instanceof IdentValue) {
            final IdentValue ident = (IdentValue)value;
            if (ident == IdentValue.COVER) {
                return new BackgroundSize(false, true, false);
            } else if (ident == IdentValue.CONTAIN) {
                return new BackgroundSize(true, false, false);
            }
        } else {
            final ListValue valueList = (ListValue)value;
            final List<?> values = valueList.getValues();
            final boolean firstAuto = ((PropertyValue)values.get(0)).getIdentValue() == IdentValue.AUTO;
            final boolean secondAuto = ((PropertyValue)values.get(1)).getIdentValue() == IdentValue.AUTO;

            if (firstAuto && secondAuto) {
                return new BackgroundSize(false, false, true);
            } else {
                return new BackgroundSize((PropertyValue)values.get(0), (PropertyValue)values.get(1));
            }
        }

        throw new RuntimeException("internal error");
    }

    public BackgroundPosition getBackgroundPosition() {
        final ListValue result = (ListValue) valueByName(CSSName.BACKGROUND_POSITION);
        final List<?> values = result.getValues();

        return new BackgroundPosition(
                (PropertyValue) values.get(0), (PropertyValue) values.get(1));
    }

    public List<?> getCounterReset() {
        final FSDerivedValue value = valueByName(CSSName.COUNTER_RESET);

        if (value == IdentValue.NONE) {
            return null;
        } else {
            return ((ListValue) value).getValues();
        }
    }

    public List<?> getCounterIncrement() {
        final FSDerivedValue value = valueByName(CSSName.COUNTER_INCREMENT);

        if (value == IdentValue.NONE) {
            return null;
        } else {
            return ((ListValue) value).getValues();
        }
    }

    public BorderPropertySet getBorder(final CssContext ctx) {
        if (! _bordersAllowed) {
            return BorderPropertySet.EMPTY_BORDER;
        } else {
            final BorderPropertySet b = getBorderProperty(this, ctx);
            return b;
        }
    }

    public FontSpecification getFont(final CssContext ctx) {
        if (_font == null) {
            _font = new FontSpecification();

            _font.families = valueByName(CSSName.FONT_FAMILY).asStringArray();

            final FSDerivedValue fontSize = valueByName(CSSName.FONT_SIZE);
            if (fontSize instanceof IdentValue) {
                PropertyValue replacement;
                final IdentValue resolved = resolveAbsoluteFontSize();
                if (resolved != null) {
                    replacement = FontSizeHelper.resolveAbsoluteFontSize(resolved, _font.families);
                } else {
                    replacement = FontSizeHelper.getDefaultRelativeFontSize((IdentValue) fontSize);
                }
                _font.size = LengthValue.calcFloatProportionalValue(
                        this, CSSName.FONT_SIZE, replacement.getCssText(),
                        replacement.getFloatValue(), replacement.getPrimitiveTypeN(), 0, ctx);
            } else {
                _font.size = getFloatPropertyProportionalTo(CSSName.FONT_SIZE, 0, ctx);
            }

            _font.fontWeight = getIdent(CSSName.FONT_WEIGHT);

            _font.fontStyle = getIdent(CSSName.FONT_STYLE);
            _font.variant = getIdent(CSSName.FONT_VARIANT);
        }
        return _font;
    }

    public FontSpecification getFontSpecification() {
    return _font;
    }

    private IdentValue resolveAbsoluteFontSize() {
        final FSDerivedValue fontSize = valueByName(CSSName.FONT_SIZE);
        if (! (fontSize instanceof IdentValue)) {
            return null;
        }
        final IdentValue fontSizeIdent = (IdentValue) fontSize;
        if (PrimitivePropertyBuilders.ABSOLUTE_FONT_SIZES.contains(fontSizeIdent)) {
            return fontSizeIdent;
        }

        final IdentValue parent = getParent().resolveAbsoluteFontSize();
        if (parent != null) {
            if (fontSizeIdent == IdentValue.SMALLER) {
                return FontSizeHelper.getNextSmaller(parent);
            } else if (fontSize == IdentValue.LARGER) {
                return FontSizeHelper.getNextLarger(parent);
            }
        }

        return null;
    }

    public float getFloatPropertyProportionalTo(final CSSName cssName, final float baseValue, final CssContext ctx) {
        return valueByName(cssName).getFloatProportionalTo(cssName, baseValue, ctx);
    }

    /**
     * @param cssName
     * @param parentWidth
     * @param ctx
     * @return TODO
     */
    public float getFloatPropertyProportionalWidth(final CSSName cssName, final float parentWidth, final CssContext ctx) {
        return valueByName(cssName).getFloatProportionalTo(cssName, parentWidth, ctx);
    }

    /**
     * @param cssName
     * @param parentHeight
     * @param ctx
     * @return TODO
     */
    public float getFloatPropertyProportionalHeight(final CSSName cssName, final float parentHeight, final CssContext ctx) {
        return valueByName(cssName).getFloatProportionalTo(cssName, parentHeight, ctx);
    }

    public float getLineHeight(final CssContext ctx) {
        if (! _lineHeightResolved) {
            if (isIdent(CSSName.LINE_HEIGHT, IdentValue.NORMAL)) {
                final float lineHeight1 = getFont(ctx).size * 1.1f;
                // Make sure rasterized characters will (probably) fit inside
                // the line box
                final FSFontMetrics metrics = getFSFontMetrics(ctx);
                final float lineHeight2 = (float)Math.ceil(metrics.getDescent() + metrics.getAscent());
                _lineHeight = Math.max(lineHeight1, lineHeight2);
            } else if (isLength(CSSName.LINE_HEIGHT)) {
                //could be more elegant, I suppose
                _lineHeight = getFloatPropertyProportionalHeight(CSSName.LINE_HEIGHT, 0, ctx);
            } else {
                //must be a number
                _lineHeight = getFont(ctx).size * valueByName(CSSName.LINE_HEIGHT).asFloat();
            }
            _lineHeightResolved = true;
        }
        return _lineHeight;
    }

    /**
     * Convenience property accessor; returns a Border initialized with the
     * four-sided margin width. Uses the actual value (computed actual value)
     * for this element.
     *
     * @param cbWidth
     * @param ctx
     * @return The marginWidth value
     */
    public RectPropertySet getMarginRect(final float cbWidth, final CssContext ctx) {
        return getMarginRect(cbWidth, ctx, true);
    }

    public RectPropertySet getMarginRect(final float cbWidth, final CssContext ctx, final boolean useCache) {
        if (! _marginsAllowed) {
            return RectPropertySet.ALL_ZEROS;
        } else {
            return getMarginProperty(
                    this, CSSName.MARGIN_SHORTHAND, CSSName.MARGIN_SIDE_PROPERTIES, cbWidth, ctx, useCache);
        }
    }

    /**
     * Convenience property accessor; returns a Border initialized with the
     * four-sided padding width. Uses the actual value (computed actual value)
     * for this element.
     *
     * @param cbWidth
     * @param ctx
     * @return The paddingWidth value
     */
    public RectPropertySet getPaddingRect(final float cbWidth, final CssContext ctx, final boolean useCache) {
        if (! _paddingAllowed) {
            return RectPropertySet.ALL_ZEROS;
        } else {
            return getPaddingProperty(this, CSSName.PADDING_SHORTHAND, CSSName.PADDING_SIDE_PROPERTIES, cbWidth, ctx, useCache);
        }
    }

    public RectPropertySet getPaddingRect(final float cbWidth, final CssContext ctx) {
        return getPaddingRect(cbWidth, ctx, true);
    }

    /**
     * @param cssName
     * @return TODO
     */
    public String getStringProperty(final CSSName cssName) {
        return valueByName(cssName).asString();
    }

    /**
     * TODO: doc
     */
    public boolean isLength(final CSSName cssName) {
        final FSDerivedValue val = valueByName(cssName);
        return val instanceof LengthValue;
    }

    public boolean isLengthOrNumber(final CSSName cssName) {
        final FSDerivedValue val = valueByName(cssName);
        return val instanceof NumberValue || val instanceof LengthValue;
    }

    /**
     * Returns a {@link FSDerivedValue} by name. Because we are a derived
     * style, the property will already be resolved at this point.
     *
     * @param cssName The CSS property name, e.g. "font-family"
     * @return See desc.
     */
    public FSDerivedValue valueByName(final CSSName cssName) {
        FSDerivedValue val = _derivedValuesById[cssName.FS_ID];

        final boolean needInitialValue = val == IdentValue.FS_INITIAL_VALUE;

        // but the property may not be defined for this Element
        if (val == null || needInitialValue) {
            // if it is inheritable (like color) and we are not root, ask our parent
            // for the value
            if (! needInitialValue && CSSName.propertyInherits(cssName)
                    && _parent != null
                    //
                    && (val = _parent.valueByName(cssName)) != null) {
                // Do nothing, val is already set
            } else {
                // otherwise, use the initial value (defined by the CSS2 Spec)
                final String initialValue = CSSName.initialValue(cssName);
                if (initialValue == null) {
                    throw new XRRuntimeException("Property '" + cssName + "' has no initial values assigned. " +
                            "Check CSSName declarations.");
                }
                if (initialValue.charAt(0) == '=') {
                    final CSSName ref = CSSName.getByPropertyName(initialValue.substring(1));
                    val = valueByName(ref);
                } else {
                    val = CSSName.initialDerivedValue(cssName);
                }
            }
            _derivedValuesById[cssName.FS_ID] = val;
        }
        return val;
    }

    /**
     * <p/>
     * <p/>
     * <p/>
     * <p/>
     * Implements cascade/inherit/important logic. This should result in the
     * element for this style having a value for *each and every* (visual)
     * property in the CSS2 spec. The implementation is based on the notion that
     * the matched styles are given to us in a perfectly sorted order, such that
     * properties appearing later in the rule-set always override properties
     * appearing earlier. It also assumes that all properties in the CSS2 spec
     * are defined somewhere across all the matched styles; for example, that
     * the full-property set is given in the user-agent CSS that is always
     * loaded with styles. The current implementation makes no attempt to check
     * either of these assumptions. When this method exits, the derived property
     * list for this class will be populated with the properties defined for
     * this element, properly cascaded.</p>
     *
     * @param matched PARAM
     */
    private void derive(final CascadedStyle matched) {
        if (matched == null) {
            return;
        }//nothing to derive

        final Iterator<PropertyDeclaration> mProps = matched.getCascadedPropertyDeclarations();
        while (mProps.hasNext()) {
            final PropertyDeclaration pd = (PropertyDeclaration) mProps.next();
            final FSDerivedValue val = deriveValue(pd.getCSSName(), pd.getValue());
            _derivedValuesById[pd.getCSSName().FS_ID] = val;
        }
    }

    private FSDerivedValue deriveValue(final CSSName cssName, final PropertyValue value) {
        return DerivedValueFactory.newDerivedValue(this, cssName, value);
    }

    private String genStyleKey() {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < _derivedValuesById.length; i++) {
            final CSSName name = CSSName.getByID(i);
            final FSDerivedValue val = _derivedValuesById[i];
            if (val != null) {
                sb.append(name.toString());
            } else {
                sb.append("(no prop assigned in this pos)");
            }
            sb.append("|\n");
        }
        return sb.toString();

    }

    public RectPropertySet getCachedPadding() {
        if (_padding == null) {
            throw new XRRuntimeException("No padding property cached yet; should have called getPropertyRect() at least once before.");
        } else {
            return _padding;
        }
    }

    public RectPropertySet getCachedMargin() {
        if (_margin == null) {
            throw new XRRuntimeException("No margin property cached yet; should have called getMarginRect() at least once before.");
        } else {
            return _margin;
        }
    }

    private static RectPropertySet getPaddingProperty(final CalculatedStyle style,
                                                      final CSSName shorthandProp,
                                                      final CSSName.CSSSideProperties sides,
                                                      final float cbWidth,
                                                      final CssContext ctx,
                                                      final boolean useCache) {
        if (! useCache) {
            return newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
        } else {
            if (style._padding == null) {
                RectPropertySet result = newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
                final boolean allZeros = result.isAllZeros();

                if (allZeros) {
                    result = RectPropertySet.ALL_ZEROS;
                }

                style._padding = result;

                if (! allZeros && style._padding.hasNegativeValues()) {
                    style._padding.resetNegativeValues();
                }
            }

            return style._padding;
        }
    }

    private static RectPropertySet getMarginProperty(final CalculatedStyle style,
                                                     final CSSName shorthandProp,
                                                     final CSSName.CSSSideProperties sides,
                                                     final float cbWidth,
                                                     final CssContext ctx,
                                                     final boolean useCache) {
        if (! useCache) {
            return newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
        } else {
            if (style._margin == null) {
                RectPropertySet result = newRectInstance(style, shorthandProp, sides, cbWidth, ctx);
                if (result.isAllZeros()) {
                    result = RectPropertySet.ALL_ZEROS;
                }
                style._margin = result;
            }

            return style._margin;
        }
    }

    private static RectPropertySet newRectInstance(final CalculatedStyle style,
                                                   final CSSName shorthand,
                                                   final CSSName.CSSSideProperties sides,
                                                   final float cbWidth,
                                                   final CssContext ctx) {
        RectPropertySet rect;
        rect = RectPropertySet.newInstance(style,
                shorthand,
                sides,
                cbWidth,
                ctx);
        return rect;
    }

    private static BorderPropertySet getBorderProperty(final CalculatedStyle style,
                                                       final CssContext ctx) {
        if (style._border == null) {
            BorderPropertySet result = BorderPropertySet.newInstance(style, ctx);

            final boolean allZeros = result.isAllZeros();
            if (allZeros && ! result.hasHidden()) {
                result = BorderPropertySet.EMPTY_BORDER;
            }

            style._border = result;

            if (! allZeros && style._border.hasNegativeValues()) {
                style._border.resetNegativeValues();
            }
        }
        return style._border;
    }

    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    public static final int TOP = 3;
    public static final int BOTTOM = 4;

    public int getMarginBorderPadding(
            final CssContext cssCtx, final int cbWidth, final int which) {
        final BorderPropertySet border = getBorder(cssCtx);
        final RectPropertySet margin = getMarginRect(cbWidth, cssCtx);
        final RectPropertySet padding = getPaddingRect(cbWidth, cssCtx);

        switch (which) {
            case LEFT:
                return (int) (margin.left() + border.left() + padding.left());
            case RIGHT:
                return (int) (margin.right() + border.right() + padding.right());
            case TOP:
                return (int) (margin.top() + border.top() + padding.top());
            case BOTTOM:
                return (int) (margin.bottom() + border.bottom() + padding.bottom());
            default:
                throw new IllegalArgumentException();
        }
    }

    public IdentValue getWhitespace() {
        return getIdent(CSSName.WHITE_SPACE);
    }

    public FSFont getFSFont(final CssContext cssContext) {
        if (_FSFont == null) {
            _FSFont = cssContext.getFont(getFont(cssContext));
        }
        return _FSFont;
    }

    public FSFontMetrics getFSFontMetrics(final CssContext c) {
        if (_FSFontMetrics == null) {
            _FSFontMetrics = c.getFSFontMetrics(getFSFont(c));
        }
        return _FSFontMetrics;
    }

    public IdentValue getWordWrap() {
        return getIdent(CSSName.WORD_WRAP);
    }

    public boolean isClearLeft() {
        final IdentValue clear = getIdent(CSSName.CLEAR);
        return clear == IdentValue.LEFT || clear == IdentValue.BOTH;
    }

    public boolean isClearRight() {
        final IdentValue clear = getIdent(CSSName.CLEAR);
        return clear == IdentValue.RIGHT || clear == IdentValue.BOTH;
    }

    public boolean isCleared() {
        return ! isIdent(CSSName.CLEAR, IdentValue.NONE);
    }

    public IdentValue getBackgroundRepeat() {
        return getIdent(CSSName.BACKGROUND_REPEAT);
    }

    public IdentValue getBackgroundAttachment() {
        return getIdent(CSSName.BACKGROUND_ATTACHMENT);
    }

    public boolean isFixedBackground() {
        return getIdent(CSSName.BACKGROUND_ATTACHMENT) == IdentValue.FIXED;
    }

    public boolean isInline() {
        return isIdent(CSSName.DISPLAY, IdentValue.INLINE) &&
                ! (isFloated() || isAbsolute() || isFixed() || isRunning());
    }

    public boolean isInlineBlock() {
        return isIdent(CSSName.DISPLAY, IdentValue.INLINE_BLOCK);
    }

    public boolean isTable() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE);
    }

    public boolean isInlineTable() {
        return isIdent(CSSName.DISPLAY, IdentValue.INLINE_TABLE);
    }

    public boolean isTableCell() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_CELL);
    }

    public boolean isTableSection() {
        final IdentValue display = getIdent(CSSName.DISPLAY);

        return display == IdentValue.TABLE_ROW_GROUP ||
                display == IdentValue.TABLE_HEADER_GROUP ||
                display == IdentValue.TABLE_FOOTER_GROUP;
    }

    public boolean isTableCaption() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_CAPTION);
    }

    public boolean isTableHeader() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_HEADER_GROUP);
    }

    public boolean isTableFooter() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_FOOTER_GROUP);
    }

    public boolean isTableRow() {
        return isIdent(CSSName.DISPLAY, IdentValue.TABLE_ROW);
    }

    public boolean isDisplayNone() {
        return isIdent(CSSName.DISPLAY, IdentValue.NONE);
    }

    public boolean isSpecifiedAsBlock() {
        return isIdent(CSSName.DISPLAY, IdentValue.BLOCK);
    }

    public boolean isBlockEquivalent() {
        if (isFloated() || isAbsolute() || isFixed()) {
            return true;
        } else {
            final IdentValue display = getIdent(CSSName.DISPLAY);
            if (display == IdentValue.INLINE) {
                return false;
            } else {
                return display == IdentValue.BLOCK || display == IdentValue.LIST_ITEM ||
                        display == IdentValue.RUN_IN || display == IdentValue.INLINE_BLOCK ||
                        display == IdentValue.TABLE || display == IdentValue.INLINE_TABLE;
            }
        }
    }

    public boolean isLayedOutInInlineContext() {
        if (isFloated() || isAbsolute() || isFixed() || isRunning()) {
            return true;
        } else {
            final IdentValue display = getIdent(CSSName.DISPLAY);
            return display == IdentValue.INLINE || display == IdentValue.INLINE_BLOCK ||
                    display == IdentValue.INLINE_TABLE;
        }
    }

    public boolean isNeedAutoMarginResolution() {
        return ! (isAbsolute() || isFixed() || isFloated() || isInlineBlock());
    }

    public boolean isAbsolute() {
        return isIdent(CSSName.POSITION, IdentValue.ABSOLUTE);
    }

    public boolean isFixed() {
        return isIdent(CSSName.POSITION, IdentValue.FIXED);
    }

    public boolean isFloated() {
        final IdentValue floatVal = getIdent(CSSName.FLOAT);
        return floatVal == IdentValue.LEFT || floatVal == IdentValue.RIGHT;
    }

    public boolean isFloatedLeft() {
        return isIdent(CSSName.FLOAT, IdentValue.LEFT);
    }

    public boolean isFloatedRight() {
        return isIdent(CSSName.FLOAT, IdentValue.RIGHT);
    }

    public boolean isRelative() {
        return isIdent(CSSName.POSITION, IdentValue.RELATIVE);
    }

    public boolean isPostionedOrFloated() {
        return isAbsolute() || isFixed() || isFloated() || isRelative();
    }

    public boolean isPositioned() {
        return isAbsolute() || isFixed() || isRelative();
    }

    public boolean isAutoWidth() {
        return isIdent(CSSName.WIDTH, IdentValue.AUTO);
    }

    public boolean isAbsoluteWidth() {
        return valueByName(CSSName.WIDTH).hasAbsoluteUnit();
    }

    public boolean isAutoHeight() {
        return isIdent(CSSName.HEIGHT, IdentValue.AUTO);
    }

    public boolean isAutoLeftMargin() {
        return isIdent(CSSName.MARGIN_LEFT, IdentValue.AUTO);
    }

    public boolean isAutoRightMargin() {
        return isIdent(CSSName.MARGIN_RIGHT, IdentValue.AUTO);
    }

    public boolean isAutoZIndex() {
        return isIdent(CSSName.Z_INDEX, IdentValue.AUTO);
    }

    public boolean establishesBFC() {
        final FSDerivedValue value = valueByName(CSSName.POSITION);

        if (value instanceof FunctionValue) {  // running(header)
            return false;
        } else {
            final IdentValue display = getIdent(CSSName.DISPLAY);
            final IdentValue position = (IdentValue)value;

            return isFloated() ||
                    position == IdentValue.ABSOLUTE || position == IdentValue.FIXED ||
                    display == IdentValue.INLINE_BLOCK || display == IdentValue.TABLE_CELL ||
                    ! isIdent(CSSName.OVERFLOW, IdentValue.VISIBLE);
        }
    }

    public boolean requiresLayer() {
        final FSDerivedValue value = valueByName(CSSName.POSITION);

        if (value instanceof FunctionValue) {  // running(header)
            return false;
        } else {
            final IdentValue position = getIdent(CSSName.POSITION);

            if (position == IdentValue.ABSOLUTE ||
                    position == IdentValue.RELATIVE || position == IdentValue.FIXED) {
                return true;
            }

            final IdentValue overflow = getIdent(CSSName.OVERFLOW);
            if ((overflow == IdentValue.SCROLL || overflow == IdentValue.AUTO) &&
                    isOverflowApplies()) {
                return true;
            }

            if (getOpacity() != 1f)
            	return true;
            
            return false;
        }
    }

    public boolean isRunning() {
        final FSDerivedValue value = valueByName(CSSName.POSITION);
        return value instanceof FunctionValue;
    }

    public boolean isLinearGradient() {
        final FSDerivedValue value = valueByName(CSSName.BACKGROUND_IMAGE);    	
        return value instanceof FunctionValue &&
        		GeneralUtil.ciEquals(((FunctionValue) value).getFunction().getName(), "linear-gradient");
    }
    
    public FSLinearGradient getLinearGradient(final CssContext cssContext, final int w, final int h)
    {
    	assert(isLinearGradient());

    	final FunctionValue value = (FunctionValue) valueByName(CSSName.BACKGROUND_IMAGE);
    	return new FSLinearGradient(value.getFunction(), this, w, h, cssContext);
    }
    
    public String getRunningName() {
        final FunctionValue value = (FunctionValue)valueByName(CSSName.POSITION);
        final FSFunction function = value.getFunction();

        final PropertyValue param = (PropertyValue)function.getParameters().get(0);

        return param.getStringValue();
    }

    public boolean isOverflowApplies() {
        final IdentValue display = getIdent(CSSName.DISPLAY);
        return display == IdentValue.BLOCK || display == IdentValue.LIST_ITEM ||
                display == IdentValue.TABLE || display == IdentValue.INLINE_BLOCK ||
                display == IdentValue.TABLE_CELL;
    }

    public boolean isOverflowVisible() {
        return valueByName(CSSName.OVERFLOW) == IdentValue.VISIBLE;
    }

    public boolean isHorizontalBackgroundRepeat() {
        final IdentValue value = getIdent(CSSName.BACKGROUND_REPEAT);
        return value == IdentValue.REPEAT_X || value == IdentValue.REPEAT;
    }

    public boolean isVerticalBackgroundRepeat() {
        final IdentValue value = getIdent(CSSName.BACKGROUND_REPEAT);
        return value == IdentValue.REPEAT_Y || value == IdentValue.REPEAT;
    }

    public boolean isTopAuto() {
        return isIdent(CSSName.TOP, IdentValue.AUTO);

    }

    public boolean isBottomAuto() {
        return isIdent(CSSName.BOTTOM, IdentValue.AUTO);
    }

    public boolean isListItem() {
        return isIdent(CSSName.DISPLAY, IdentValue.LIST_ITEM);
    }

    public boolean isVisible() {
        return isIdent(CSSName.VISIBILITY, IdentValue.VISIBLE);
    }

    public boolean isForcePageBreakBefore() {
        final IdentValue val = getIdent(CSSName.PAGE_BREAK_BEFORE);
        return val == IdentValue.ALWAYS || val == IdentValue.LEFT
                || val == IdentValue.RIGHT;
    }

    public boolean isForcePageBreakAfter() {
        final IdentValue val = getIdent(CSSName.PAGE_BREAK_AFTER);
        return val == IdentValue.ALWAYS || val == IdentValue.LEFT
                || val == IdentValue.RIGHT;
    }

    public boolean isAvoidPageBreakInside() {
        return isIdent(CSSName.PAGE_BREAK_INSIDE, IdentValue.AVOID);
    }

    public CalculatedStyle createAnonymousStyle(final IdentValue display) {
        return deriveStyle(CascadedStyle.createAnonymousStyle(display));
    }

    public boolean mayHaveFirstLine() {
        final IdentValue display = getIdent(CSSName.DISPLAY);
        return display == IdentValue.BLOCK ||
                display == IdentValue.LIST_ITEM ||
                display == IdentValue.RUN_IN ||
                display == IdentValue.TABLE ||
                display == IdentValue.TABLE_CELL ||
                display == IdentValue.TABLE_CAPTION ||
                display == IdentValue.INLINE_BLOCK;
    }

    public boolean mayHaveFirstLetter() {
        final IdentValue display = getIdent(CSSName.DISPLAY);
        return display == IdentValue.BLOCK ||
                display == IdentValue.LIST_ITEM ||
                display == IdentValue.TABLE_CELL ||
                display == IdentValue.TABLE_CAPTION ||
                display == IdentValue.INLINE_BLOCK;
    }

    public boolean isNonFlowContent() {
        return isFloated() || isAbsolute() || isFixed() || isRunning();
    }

    public boolean isMayCollapseMarginsWithChildren() {
        return isIdent(CSSName.OVERFLOW, IdentValue.VISIBLE) &&
                ! (isFloated() || isAbsolute() || isFixed() || isInlineBlock());
    }

    public boolean isAbsFixedOrInlineBlockEquiv() {
        return isAbsolute() || isFixed() || isInlineBlock() || isInlineTable();
    }

    public boolean isMaxWidthNone() {
        return isIdent(CSSName.MAX_WIDTH, IdentValue.NONE);
    }

    public boolean isMaxHeightNone() {
        return isIdent(CSSName.MAX_HEIGHT, IdentValue.NONE);
    }

    public int getMinWidth(final CssContext c, final int cbWidth) {
        return (int) getFloatPropertyProportionalTo(CSSName.MIN_WIDTH, cbWidth, c);
    }

    public int getMaxWidth(final CssContext c, final int cbWidth) {
        return (int) getFloatPropertyProportionalTo(CSSName.MAX_WIDTH, cbWidth, c);
    }

    public int getMinHeight(final CssContext c, final int cbHeight) {
        return (int) getFloatPropertyProportionalTo(CSSName.MIN_HEIGHT, cbHeight, c);
    }

    public int getMaxHeight(final CssContext c, final int cbHeight) {
        return (int) getFloatPropertyProportionalTo(CSSName.MAX_HEIGHT, cbHeight, c);
    }

    public boolean isCollapseBorders() {
        return isIdent(CSSName.BORDER_COLLAPSE, IdentValue.COLLAPSE) && ! isPaginateTable();
    }

    public int getBorderHSpacing(final CssContext c) {
        return isCollapseBorders() ? 0 : (int) getFloatPropertyProportionalTo(CSSName.FS_BORDER_SPACING_HORIZONTAL, 0, c);
    }

    public int getBorderVSpacing(final CssContext c) {
        return isCollapseBorders() ? 0 : (int) getFloatPropertyProportionalTo(CSSName.FS_BORDER_SPACING_VERTICAL, 0, c);
    }

    public int getRowSpan() {
        final int result = (int) asFloat(CSSName.FS_ROWSPAN);
        return result > 0 ? result : 1;
    }

    public int getColSpan() {
        final int result = (int) asFloat(CSSName.FS_COLSPAN);
        return result > 0 ? result : 1;
    }

    public Length asLength(final CssContext c, final CSSName cssName) {
        final Length result = new Length();

        final FSDerivedValue value = valueByName(cssName);
        if (value instanceof LengthValue || value instanceof NumberValue) {
            if (value.hasAbsoluteUnit()) {
                result.setValue((int) value.getFloatProportionalTo(cssName, 0, c));
                result.setType(Length.FIXED);
            } else {
                result.setValue((int) value.asFloat());
                result.setType(Length.PERCENT);
            }
        }

        return result;
    }

    public boolean isShowEmptyCells() {
        return isCollapseBorders() || isIdent(CSSName.EMPTY_CELLS, IdentValue.SHOW);
    }

    public boolean isHasBackground() {
        return ! (isIdent(CSSName.BACKGROUND_COLOR, IdentValue.TRANSPARENT) &&
                isIdent(CSSName.BACKGROUND_IMAGE, IdentValue.NONE));
    }

    public List<FSDerivedValue> getTextDecorations() {
        final FSDerivedValue value = valueByName(CSSName.TEXT_DECORATION);
        if (value == IdentValue.NONE) {
            return null;
        } else {
            final List<?> idents = ((ListValue) value).getValues();
            final List<FSDerivedValue> result = new ArrayList<FSDerivedValue>(idents.size());
            for (final Object ident : idents) {
                result.add(DerivedValueFactory.newDerivedValue(
                        this, CSSName.TEXT_DECORATION, (PropertyValue) ident));
            }
            return result;
        }
    }

    public Cursor getCursor() {
        final FSDerivedValue value = valueByName(CSSName.CURSOR);

        if (value == IdentValue.AUTO || value == IdentValue.DEFAULT) {
            return Cursor.getDefaultCursor();
        } else if (value == IdentValue.CROSSHAIR) {
            return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
        } else if (value == IdentValue.POINTER) {
            return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        } else if (value == IdentValue.MOVE) {
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        } else if (value == IdentValue.E_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        } else if (value == IdentValue.NE_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
        } else if (value == IdentValue.NW_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
        } else if (value == IdentValue.N_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        } else if (value == IdentValue.SE_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
        } else if (value == IdentValue.SW_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
        } else if (value == IdentValue.S_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
        } else if (value == IdentValue.W_RESIZE) {
            return Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
        } else if (value == IdentValue.TEXT) {
            return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
        } else if (value == IdentValue.WAIT) {
            return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        } else if (value == IdentValue.HELP) {
            // We don't have a cursor for this by default, maybe we need
            // a custom one for this (but I don't like it).
            return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        } else if (value == IdentValue.PROGRESS) {
            // We don't have a cursor for this by default, maybe we need
            // a custom one for this (but I don't like it).
            return Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        }

        return null;
    }

    public boolean isPaginateTable() {
        return isIdent(CSSName.FS_TABLE_PAGINATE, IdentValue.PAGINATE);
    }

    public boolean isTextJustify() {
        return isIdent(CSSName.TEXT_ALIGN, IdentValue.JUSTIFY) &&
                ! (isIdent(CSSName.WHITE_SPACE, IdentValue.PRE) ||
                        isIdent(CSSName.WHITE_SPACE, IdentValue.PRE_LINE));
    }

    public boolean isListMarkerInside() {
        return isIdent(CSSName.LIST_STYLE_POSITION, IdentValue.INSIDE);
    }

    public boolean isKeepWithInline() {
        return isIdent(CSSName.FS_KEEP_WITH_INLINE, IdentValue.KEEP);
    }

    public boolean isDynamicAutoWidth() {
        return isIdent(CSSName.FS_DYNAMIC_AUTO_WIDTH, IdentValue.DYNAMIC);
    }

    public boolean isDynamicAutoWidthApplicable() {
        return isDynamicAutoWidth() && isAutoWidth() && ! isCanBeShrunkToFit();
    }

    public boolean isCanBeShrunkToFit() {
        return isInlineBlock() || isFloated() || isAbsolute() || isFixed();
    }

	public float getOpacity()
	{
		return asFloat(CSSName.OPACITY);
	}
}

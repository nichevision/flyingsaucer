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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSFunction;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.parser.Token;
import org.xhtmlrenderer.css.parser.PropertyValueImp.CSSValueType;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import org.xhtmlrenderer.util.GeneralUtil;
import org.xhtmlrenderer.util.LangId;

import static org.xhtmlrenderer.css.parser.property.BuilderUtil.*;

public class PrimitivePropertyBuilders {
    // none | hidden | dotted | dashed | solid | double | groove | ridge | inset | outset
    public static final EnumSet<IdentValue> BORDER_STYLES = setFor(
            new IdentValue[] { IdentValue.NONE, IdentValue.HIDDEN, IdentValue.DOTTED,
                    IdentValue.DASHED, IdentValue.SOLID, IdentValue.DOUBLE,
                    IdentValue.GROOVE, IdentValue.RIDGE, IdentValue.INSET,
                    IdentValue.OUTSET });

    // thin | medium | thick
    public static final EnumSet<IdentValue> BORDER_WIDTHS = setFor(
            new IdentValue[] { IdentValue.THIN, IdentValue.MEDIUM, IdentValue.THICK });

    // normal | small-caps | inherit
    public static final EnumSet<IdentValue> FONT_VARIANTS = setFor(
            new IdentValue[] { IdentValue.NORMAL, IdentValue.SMALL_CAPS });

    // normal | italic | oblique | inherit
    public static final EnumSet<IdentValue> FONT_STYLES = setFor(
            new IdentValue[] { IdentValue.NORMAL, IdentValue.ITALIC, IdentValue.OBLIQUE });

    public static final EnumSet<IdentValue> FONT_WEIGHTS = setFor(
            new IdentValue[] { IdentValue.NORMAL, IdentValue.BOLD, IdentValue.BOLDER, IdentValue.LIGHTER });

    public static final EnumSet<IdentValue> PAGE_ORIENTATIONS = setFor(
            new IdentValue[] { IdentValue.AUTO, IdentValue.PORTRAIT, IdentValue.LANDSCAPE });

    // inside | outside | inherit
    public static final EnumSet<IdentValue> LIST_STYLE_POSITIONS = setFor(new IdentValue[] {
            IdentValue.INSIDE, IdentValue.OUTSIDE });

    // disc | circle | square | decimal
    // | decimal-leading-zero | lower-roman | upper-roman
    // | lower-greek | lower-latin | upper-latin | armenian
    // | georgian | lower-alpha | upper-alpha | none | inherit
    public static final EnumSet<IdentValue> LIST_STYLE_TYPES = setFor(new IdentValue[] {
            IdentValue.DISC, IdentValue.CIRCLE, IdentValue.SQUARE,
            IdentValue.DECIMAL, IdentValue.DECIMAL_LEADING_ZERO,
            IdentValue.LOWER_ROMAN, IdentValue.UPPER_ROMAN,
            IdentValue.LOWER_GREEK, IdentValue.LOWER_LATIN,
            IdentValue.UPPER_LATIN, IdentValue.ARMENIAN,
            IdentValue.GEORGIAN, IdentValue.LOWER_ALPHA,
            IdentValue.UPPER_ALPHA, IdentValue.NONE });

    // repeat | repeat-x | repeat-y | no-repeat | inherit
    public static final EnumSet<IdentValue> BACKGROUND_REPEATS = setFor(
            new IdentValue[] {
                    IdentValue.REPEAT, IdentValue.REPEAT_X,
                    IdentValue.REPEAT_Y, IdentValue.NO_REPEAT });

    // scroll | fixed | inherit
    public static final EnumSet<IdentValue> BACKGROUND_ATTACHMENTS = setFor(
            new IdentValue[] { IdentValue.SCROLL, IdentValue.FIXED });

    // left | right | top | bottom | center
    public static final EnumSet<IdentValue> BACKGROUND_POSITIONS = setFor(
            new IdentValue[] {
                    IdentValue.LEFT, IdentValue.RIGHT, IdentValue.TOP,
                    IdentValue.BOTTOM, IdentValue.CENTER });

    public static final EnumSet<IdentValue> ABSOLUTE_FONT_SIZES = setFor(
            new IdentValue[] {
                    IdentValue.XX_SMALL, IdentValue.X_SMALL, IdentValue.SMALL,
                    IdentValue.MEDIUM, IdentValue.LARGE, IdentValue.X_LARGE,
                    IdentValue.XX_LARGE });

    public static final EnumSet<IdentValue> RELATIVE_FONT_SIZES = setFor(
            new IdentValue[] {
                    IdentValue.SMALLER, IdentValue.LARGER });

    public static final PropertyBuilder COLOR = new GenericColor();
    public static final PropertyBuilder BORDER_STYLE = new GenericBorderStyle();
    public static final PropertyBuilder BORDER_WIDTH = new GenericBorderWidth();
    public static final PropertyBuilder MARGIN = new LengthLikeWithAuto();
    public static final PropertyBuilder PADDING = new NonNegativeLengthLike();

    private static EnumSet<IdentValue> setFor(IdentValue[] values) {
        EnumSet<IdentValue> result = EnumSet.copyOf(Arrays.asList(values));
        return result;
    }

    private static abstract class SingleIdent implements PropertyBuilder {
        protected abstract EnumSet<IdentValue> getAllowed();

        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) 
        {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = values.get(0);
            checkInheritAllowed(value, inheritAllowed);

            if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT)
            {
                return Collections.singletonList(
                        new PropertyDeclaration(cssName, value, important, origin));
            }

            checkValueType(cssName, value, EnumSet.of(CSSPrimitiveUnit.CSS_IDENT));
            checkIdentValidity(cssName, getAllowed(), value);
            return Collections.singletonList(
                   new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    private static class GenericColor implements PropertyBuilder {
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.TRANSPARENT });
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = values.get(0);
            checkInheritAllowed(value, inheritAllowed);

            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) 
            {
            	checkValueType(cssName, value, EnumSet.of(CSSPrimitiveUnit.CSS_IDENT, CSSPrimitiveUnit.CSS_RGBCOLOR));

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    FSRGBColor color = Conversions.getColor(value.getStringValue());
                    if (color != null) {
                        return Collections.singletonList(
                                new PropertyDeclaration(
                                        cssName, new PropertyValueImp(color),
                                        important, origin));
                    }

                    checkIdentValidity(cssName, ALLOWED, value);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    private static class GenericBorderStyle extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return BORDER_STYLES;
        }
    }

    private static class GenericBorderWidth implements PropertyBuilder {
        @Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentOrLengthType(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, BORDER_WIDTHS, ident);

                    return Collections.singletonList(
                            new PropertyDeclaration(
                                    cssName, Conversions.getBorderWidth(ident.toString()), important, origin));
                } else {
                    if (value.getFloatValue() < 0.0f) {
                        cssThrowError(LangId.NO_NEGATIVE, cssName);
                    }
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    private static abstract class LengthWithIdent implements PropertyBuilder {
        protected abstract EnumSet<IdentValue> getAllowed();
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentOrLengthType(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, getAllowed(), ident);
                } else if (! isNegativeValuesAllowed() && value.getFloatValue() < 0.0f) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }

        protected boolean isNegativeValuesAllowed() {
            return true;
        }
    }

    private static abstract class LengthLikeWithIdent implements PropertyBuilder {
        protected abstract EnumSet<IdentValue> getAllowed();
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentLengthOrPercentType(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, getAllowed(), ident);
                } else if (! isNegativeValuesAllowed() && value.getFloatValue() < 0.0f) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }

        protected boolean isNegativeValuesAllowed() {
            return true;
        }
    }

    private static class LengthLike implements PropertyBuilder {
        @Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkLengthOrPercentType(cssName, value);

                if (! isNegativeValuesAllowed() && value.getFloatValue() < 0.0f) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }

        protected boolean isNegativeValuesAllowed() {
            return true;
        }
    }

    private static class NonNegativeLengthLike extends LengthLike {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    private static class ColOrRowSpan implements PropertyBuilder {
        @Override
    	public List<PropertyDeclaration> buildDeclarations(CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkNumberType(cssName, value);

                if (value.getFloatValue() < 1) {
                    cssThrowError(LangId.SPAN_MUST_BE_GT_ZERO);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    private static class PlainInteger implements PropertyBuilder {
        @Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkInteger(cssName, value);

                if (! isNegativeValuesAllowed() && value.getFloatValue() < 0.0f) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }

        protected boolean isNegativeValuesAllowed() {
            return true;
        }
    }

    private static class Length implements PropertyBuilder {
        @Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkLengthType(cssName, value);

                if (! isNegativeValuesAllowed() && value.getFloatValue() < 0.0f) {
                	cssThrowError(LangId.NO_NEGATIVE, cssName);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }

        protected boolean isNegativeValuesAllowed() {
            return true;
        }
    }

    /*
    private static class SingleString extends AbstractPropertyBuilder {
        public List buildDeclarations(
                CSSName cssName, List values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            CSSPrimitiveValue value = (CSSPrimitiveValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueType() != CSSPrimitiveValue.CSS_INHERIT) {
                checkStringType(cssName, value);
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }
    }
    */

    /*
    private static abstract class SingleStringWithIdent extends AbstractPropertyBuilder {
        protected abstract EnumSet<IdentValue> getAllowed();

        public List buildDeclarations(
                CSSName cssName, List values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            CSSPrimitiveValue value = (CSSPrimitiveValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueType() != CSSPrimitiveValue.CSS_INHERIT) {
                checkIdentOrString(cssName, value);

                if (value.getPrimitiveType() == CSSPrimitiveValue.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);

                    checkValidity(cssName, getAllowed(), ident);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }
    }
    */

    /*
    private static class SingleStringWithNone extends SingleStringWithIdent {
        private static final EnumSet<IdentValue> ALLOWED = setFor(new IdentValue[] { IdentValue.NONE });

        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }
    */

    private static class LengthLikeWithAuto extends LengthLikeWithIdent {
        // <length> | <percentage> | auto | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.AUTO });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    private static class LengthWithNormal extends LengthWithIdent {
        // <length> | normal | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.NORMAL });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    private static class LengthLikeWithNone extends LengthLikeWithIdent {
        // <length> | <percentage> | none | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.NONE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    private static class GenericURIWithNone implements PropertyBuilder {
        // <uri> | none | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(new IdentValue[] { IdentValue.NONE });
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentOrURIType(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, ALLOWED, ident);
                }
            }
            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    public static class BackgroundAttachment extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return BACKGROUND_ATTACHMENTS;
        }
    }

    public static class BackgroundColor extends GenericColor {
    }

	public static class BackgroundImage extends GenericURIWithNone {
		@Override
		public List<PropertyDeclaration> buildDeclarations(CSSName cssName,
				List<PropertyValue> values, CSSOrigin origin,
				boolean important, boolean inheritAllowed) {

			checkValueCount(cssName, 1, values.size());
			PropertyValue value = values.get(0);

			if (value.getFunction() == null) 
			{
				return super.buildDeclarations(cssName, values, origin,
						important, inheritAllowed);
			}

			return Collections.singletonList(new PropertyDeclaration(cssName,
					value, important, origin));
		}
	}

    public static class BackgroundSize implements PropertyBuilder {
        private static final EnumSet<IdentValue> ALL_ALLOWED = setFor(new IdentValue[] {
                IdentValue.AUTO, IdentValue.CONTAIN, IdentValue.COVER
        });
        @Override
        public List<PropertyDeclaration> buildDeclarations(CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, 2, values.size());

            PropertyValue first = values.get(0);
            PropertyValue second = null;
            if (values.size() == 2) {
                second = values.get(1);
            }

            checkInheritAllowed(first, inheritAllowed);
            if (values.size() == 1 &&
                    first.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
                return Collections.singletonList(
                        new PropertyDeclaration(cssName, first, important, origin));
            }

            if (second != null) {
                checkInheritAllowed(second, false);
            }

            checkIdentLengthOrPercentType(cssName, first);
            if (second == null) {
                if (first.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue firstIdent = checkIdent(cssName, first);
                    checkValidity(cssName, ALL_ALLOWED, firstIdent);

                    if (firstIdent == IdentValue.CONTAIN || firstIdent == IdentValue.COVER) {
                        return Collections.singletonList(
                                new PropertyDeclaration(cssName, first, important, origin));
                    } else {
                        return createTwoValueResponse(first, first, origin, important);
                    }
                } else {
                    return createTwoValueResponse(first, new PropertyValueImp(IdentValue.AUTO), origin, important);
                }
            } else {
                checkIdentLengthOrPercentType(cssName, second);

                if (first.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue firstIdent = checkIdent(cssName, first);
                    if (firstIdent != IdentValue.AUTO) {
                        cssThrowError(LangId.ONLY_AUTO_ALLOWED, cssName);
                    }
                } else if (((PropertyValue)first).getFloatValue() < 0.0f) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }

                if (second.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue secondIdent = checkIdent(cssName, second);
                    if (secondIdent != IdentValue.AUTO) {
                        cssThrowError(LangId.ONLY_AUTO_ALLOWED, cssName);
                    }
                } else if (((PropertyValue)second).getFloatValue() < 0.0f) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }

                return createTwoValueResponse(first, second, origin, important);
            }
        }

        private List<PropertyDeclaration> createTwoValueResponse(PropertyValue value1, PropertyValue value2,
                CSSOrigin origin, boolean important) {
            List<PropertyValue> values = new ArrayList<>(2);
            values.add(value1);
            values.add(value2);

            PropertyDeclaration result = new PropertyDeclaration(
                    CSSName.BACKGROUND_SIZE,
                    new PropertyValueImp(values), important, origin);

            return Collections.singletonList(result);
        }
    }

    public static class BackgroundPosition implements PropertyBuilder {
        @Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, 2, values.size());

            PropertyValue first = values.get(0);
            PropertyValue second = null;
            if (values.size() == 2) {
                second = values.get(1);
            }

            checkInheritAllowed(first, inheritAllowed);
            if (values.size() == 1 &&
                    first.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
                return Collections.singletonList(
                        new PropertyDeclaration(cssName, first, important, origin));
            }

            if (second != null) {
                checkInheritAllowed(second, false);
            }

            checkIdentLengthOrPercentType(cssName, first);
            if (second == null) {
                if (isLength(first) || first.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_PERCENTAGE) {
                    List<PropertyValue> responseValues = new ArrayList<>(2);
                    responseValues.add((PropertyValue) first);
                    responseValues.add(new PropertyValueImp(
                    		CSSPrimitiveUnit.CSS_PERCENTAGE, 50.0f, "50%"));
                    return Collections.singletonList(new PropertyDeclaration(
                                CSSName.BACKGROUND_POSITION,
                                new PropertyValueImp(responseValues), important, origin));
                }
            } else {
                checkIdentLengthOrPercentType(cssName, second);
            }


            IdentValue firstIdent = null;
            if (first.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                firstIdent = checkIdent(cssName, first);
                checkValidity(cssName, getAllowed(), firstIdent);
            }

            IdentValue secondIdent = null;
            if (second == null) {
                secondIdent = IdentValue.CENTER;
            } else if (second.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                secondIdent = checkIdent(cssName, second);
                checkValidity(cssName, getAllowed(), secondIdent);
            }

            if (firstIdent == null && secondIdent == null) {
                return Collections.singletonList(new PropertyDeclaration(
                        CSSName.BACKGROUND_POSITION, new PropertyValueImp(values), important, origin));
            } else if (firstIdent != null && secondIdent != null) {
                if (firstIdent == IdentValue.TOP || firstIdent == IdentValue.BOTTOM ||
                        secondIdent == IdentValue.LEFT || secondIdent == IdentValue.RIGHT) {
                    IdentValue temp = firstIdent;
                    firstIdent = secondIdent;
                    secondIdent = temp;
                }

                checkIdentPosition(cssName, firstIdent, secondIdent);

                return createTwoPercentValueResponse(
                        getPercentForIdent(firstIdent),
                        getPercentForIdent(secondIdent),
                        important,
                        origin);
            } else {
                checkIdentPosition(cssName, firstIdent, secondIdent);

                List<PropertyValue> responseValues = new ArrayList<>(2);

                if (firstIdent == null) {
                    responseValues.add((PropertyValue) first);
                    responseValues.add(createValueForIdent(secondIdent));
                } else {
                    responseValues.add(createValueForIdent(firstIdent));
                    responseValues.add((PropertyValue) second);
                }

                return Collections.singletonList(new PropertyDeclaration(
                        CSSName.BACKGROUND_POSITION,
                        new PropertyValueImp(responseValues), important, origin));
            }
        }

        private void checkIdentPosition(CSSName cssName, IdentValue firstIdent, IdentValue secondIdent) {
            if (firstIdent == IdentValue.TOP || firstIdent == IdentValue.BOTTOM ||
                    secondIdent == IdentValue.LEFT || secondIdent == IdentValue.RIGHT) {
            	cssThrowError(LangId.INVALID_KEYWORD_COMBINATION, cssName);
            }
        }

        private float getPercentForIdent(IdentValue ident) {
            float percent = 0.0f;

            if (ident == IdentValue.CENTER) {
                percent = 50.f;
            } else if (ident == IdentValue.BOTTOM || ident == IdentValue.RIGHT) {
                percent = 100.0f;
            }

            return percent;
        }

        private PropertyValue createValueForIdent(IdentValue ident) {
            float percent = getPercentForIdent(ident);
            return new PropertyValueImp(
            		CSSPrimitiveUnit.CSS_PERCENTAGE, percent, percent + "%");
        }

        private List<PropertyDeclaration> createTwoPercentValueResponse(
                float percent1, float percent2, boolean important, CSSOrigin origin) {
            PropertyValue value1 = new PropertyValueImp(
            		CSSPrimitiveUnit.CSS_PERCENTAGE, percent1, percent1 + "%");
            PropertyValue value2 = new PropertyValueImp(
            		CSSPrimitiveUnit.CSS_PERCENTAGE, percent2, percent2 + "%");

            List<PropertyValue> values = new ArrayList<>(2);
            values.add(value1);
            values.add(value2);

            PropertyDeclaration result = new PropertyDeclaration(
                    CSSName.BACKGROUND_POSITION,
                    new PropertyValueImp(values), important, origin);

            return Collections.singletonList(result);
        }

        private EnumSet<IdentValue> getAllowed() {
            return BACKGROUND_POSITIONS;
        }
    }

    public static class BackgroundRepeat extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return BACKGROUND_REPEATS;
        }
    }

    public static class BorderCollapse extends SingleIdent {
        // collapse | separate | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.COLLAPSE, IdentValue.SEPARATE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class BorderTopColor extends GenericColor {
    }

    public static class BorderRightColor extends GenericColor {
    }

    public static class BorderBottomColor extends GenericColor {
    }

    public static class BorderLeftColor extends GenericColor {
    }

    public static class BorderTopStyle extends GenericBorderStyle {
    }

    public static class BorderRightStyle extends GenericBorderStyle {
    }

    public static class BorderBottomStyle extends GenericBorderStyle {
    }

    public static class BorderLeftStyle extends GenericBorderStyle {
    }

    public static class BorderTopWidth extends GenericBorderWidth {
    }

    public static class BorderRightWidth extends GenericBorderWidth {
    }

    public static class BorderBottomWidth extends GenericBorderWidth {
    }

    public static class BorderLeftWidth extends GenericBorderWidth {
    }

    public static class Bottom extends LengthLikeWithAuto {
    }

    public static class CaptionSide extends SingleIdent {
        // top | bottom | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.TOP, IdentValue.BOTTOM });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Clear extends SingleIdent {
        // none | left | right | both | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.NONE, IdentValue.LEFT, IdentValue.RIGHT, IdentValue.BOTH });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Color extends GenericColor {
    }

    public static class Cursor extends SingleIdent {
        // [ [<uri> ,]* [ auto | crosshair | default | pointer | move | e-resize
        // | ne-resize | nw-resize | n-resize | se-resize | sw-resize | s-resize
        // | w-resize | text | wait | help | progress ] ] | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.AUTO, IdentValue.CROSSHAIR,
                        IdentValue.DEFAULT, IdentValue.POINTER,
                        IdentValue.MOVE, IdentValue.E_RESIZE,
                        IdentValue.NE_RESIZE, IdentValue.NW_RESIZE,
                        IdentValue.N_RESIZE, IdentValue.SE_RESIZE,
                        IdentValue.SW_RESIZE, IdentValue.S_RESIZE,
                        IdentValue.W_RESIZE, IdentValue.TEXT,
                        IdentValue.WAIT, IdentValue.HELP,
                        IdentValue.PROGRESS});
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Display extends SingleIdent {
        // inline | block | list-item | run-in | inline-block | table | inline-table
        // | table-row-group | table-header-group
        // | table-footer-group | table-row | table-column-group | table-column
        // | table-cell | table-caption | none | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.INLINE, IdentValue.BLOCK,
                        IdentValue.LIST_ITEM, /* IdentValue.RUN_IN, */
                        IdentValue.INLINE_BLOCK, IdentValue.TABLE,
                        IdentValue.INLINE_TABLE, IdentValue.TABLE_ROW_GROUP,
                        IdentValue.TABLE_HEADER_GROUP, IdentValue.TABLE_FOOTER_GROUP,
                        IdentValue.TABLE_ROW, IdentValue.TABLE_COLUMN_GROUP,
                        IdentValue.TABLE_COLUMN, IdentValue.TABLE_CELL,
                        IdentValue.TABLE_CAPTION, IdentValue.NONE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class EmptyCells extends SingleIdent {
        // show | hide | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.SHOW, IdentValue.HIDE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Float extends SingleIdent {
        // left | right | none | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.LEFT, IdentValue.RIGHT, IdentValue.NONE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class FontFamily implements PropertyBuilder {
        // [[ <family-name> | <generic-family> ] [, <family-name>| <generic-family>]* ] | inherit

        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            if (values.size() == 1) {
                PropertyValue value = values.get(0);
                checkInheritAllowed(value, inheritAllowed);
                if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
                    return Collections.singletonList(
                            new PropertyDeclaration(cssName, value, important, origin));
                }
            }

            // Both Opera and Firefox parse "Century Gothic" Arial sans-serif as
            // [Century Gothic], [Arial sans-serif] (i.e. the comma is assumed
            // after a string).  Seems wrong per the spec, but FF (at least)
            // does it in standards mode so we do too.
            List<String> consecutiveIdents = new ArrayList<>();
            List<String> normalized = new ArrayList<>(values.size());
            for (PropertyValue value : values) {
                Token operator = value.getOperator();
                if (operator != null && operator != Token.TK_COMMA) {
                    cssThrowError(LangId.INVALID_FONT_FAMILY);
                }

                if (operator != null) {
                    if (consecutiveIdents.size() > 0) {
                        normalized.add(concat(consecutiveIdents, ' '));
                        consecutiveIdents.clear();
                    }
                }

                checkInheritAllowed(value, false);
                CSSPrimitiveUnit type = value.getPrimitiveTypeN();
                if (type == CSSPrimitiveUnit.CSS_STRING) {
                    if (consecutiveIdents.size() > 0) {
                        normalized.add(concat(consecutiveIdents, ' '));
                        consecutiveIdents.clear();
                    }
                    normalized.add(value.getStringValue());
                } else if (type == CSSPrimitiveUnit.CSS_IDENT) {
                    consecutiveIdents.add(value.getStringValue());
                } else {
                    cssThrowError(LangId.INVALID_FONT_FAMILY);
                }
            }
            if (consecutiveIdents.size() > 0) {
                normalized.add(concat(consecutiveIdents, ' '));
            }

            String text = concat(normalized, ',');
            PropertyValue result = new PropertyValueImp(
            		CSSPrimitiveUnit.CSS_STRING, text, text);  // HACK cssText can be wrong
            result.setStringArrayValue((String[]) normalized.toArray(new String[normalized.size()]));

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, result, important, origin));
        }

        private String concat(List<String> strings, char separator) {
            StringBuilder buf = new StringBuilder(64);
            for (Iterator<String> i = strings.iterator(); i.hasNext(); ) {
                String s = i.next();
                buf.append(s);
                if (i.hasNext()) {
                    buf.append(separator);
                }
            }
            return buf.toString();
        }
    }

    public static class FontSize implements PropertyBuilder {
        // <absolute-size> | <relative-size> | <length> | <percentage> | inherit
        private static final EnumSet<IdentValue> ALLOWED;

        static {
            ALLOWED = EnumSet.copyOf(ABSOLUTE_FONT_SIZES);
            ALLOWED.addAll(RELATIVE_FONT_SIZES);
        }
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentLengthOrPercentType(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, ALLOWED, ident);
                } else if (value.getFloatValue() < 0.0f) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }
    }

    public static class FontStyle extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return FONT_STYLES;
        }
    }

    public static class FontVariant extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return FONT_VARIANTS;
        }
    }

    public static class FontWeight implements PropertyBuilder {
        // normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900 | inherit
        @Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentOrNumberType(cssName, value);

                CSSPrimitiveUnit type = value.getPrimitiveTypeN();
                if (type == CSSPrimitiveUnit.CSS_IDENT) {
                    checkIdentType(cssName, value);
                    IdentValue ident = checkIdent(cssName, value);

                    checkValidity(cssName, getAllowed(), ident);
                } else if (type == CSSPrimitiveUnit.CSS_NUMBER) {
                    IdentValue weight = Conversions.getNumericFontWeight(value.getFloatValue());
                    if (weight == null) {
                       cssThrowError(LangId.INVALID_FONT_WEIGHT, value);
                    }

                    PropertyValue replacement = new PropertyValueImp(
                    		CSSPrimitiveUnit.CSS_IDENT, weight.toString(), weight.toString());
                    replacement.setIdentValue(weight);
                    return Collections.singletonList(
                            new PropertyDeclaration(cssName, replacement, important, origin));

                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }

        private EnumSet<IdentValue> getAllowed() {
            return FONT_WEIGHTS;
        }
    }

    public static class FSBorderSpacingHorizontal extends Length {
    }

    public static class FSBorderSpacingVertical extends Length {
    }

    public static class FSFontMetricSrc extends GenericURIWithNone {
    }

    public static class FSPageHeight extends LengthLikeWithAuto {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class FSPageWidth extends LengthLikeWithAuto {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class FSPageSequence extends SingleIdent {
        // start | auto
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.START, IdentValue.AUTO });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class FSPageOrientation extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return PAGE_ORIENTATIONS;
        }
    }

    public static class FSPDFFontEmbed extends SingleIdent {
        // auto | embed
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.AUTO, IdentValue.EMBED });

        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class FSPDFFontEncoding implements PropertyBuilder {
        @Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentOrString(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    // Convert to string
                    return Collections.singletonList(
                            new PropertyDeclaration(
                                    cssName,
                                    new PropertyValueImp(
                                    		CSSPrimitiveUnit.CSS_STRING,
                                            value.getStringValue(),
                                            value.getCssText()),
                                    important,
                                    origin));
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    public static class FSTableCellColspan extends ColOrRowSpan {
    }

    public static class FSTableCellRowspan extends ColOrRowSpan {
    }

    public static class FSTablePaginate extends SingleIdent {
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.PAGINATE, IdentValue.AUTO });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
     }

    public static class FSTextDecorationExtent extends SingleIdent {
       private static final EnumSet<IdentValue> ALLOWED = setFor(
               new IdentValue[] { IdentValue.LINE, IdentValue.BLOCK });
       @Override
       protected EnumSet<IdentValue> getAllowed() {
           return ALLOWED;
       }
    }

    public static class FSFitImagesToWidth extends LengthLikeWithAuto {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
     }

    public static class Height extends LengthLikeWithAuto {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class FSDynamicAutoWidth extends SingleIdent {
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.DYNAMIC, IdentValue.STATIC });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class FSKeepWithInline extends SingleIdent {
        // auto | keep
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.AUTO, IdentValue.KEEP });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class FSNamedDestination extends SingleIdent {
        // none | create
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.NONE, IdentValue.CREATE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Left extends LengthLikeWithAuto {
    }

    public static class LetterSpacing extends LengthWithNormal {
    }

    public static class LineHeight implements PropertyBuilder {
        // normal | <number> | <length> | <percentage> | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.NORMAL });
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentLengthNumberOrPercentType(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, ALLOWED, ident);
                } else if (value.getFloatValue() < 0.0) {
                    cssThrowError(LangId.NO_NEGATIVE, cssName);
                }
            }
            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    public static class ListStyleImage extends GenericURIWithNone {
    }

    public static class ListStylePosition extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return LIST_STYLE_POSITIONS;
        }
    }

    public static class ListStyleType extends SingleIdent {
        @Override
    	protected EnumSet<IdentValue> getAllowed() {
            return LIST_STYLE_TYPES;
        }
    }

    public static class MarginTop extends LengthLikeWithAuto {
    }

    public static class MarginRight extends LengthLikeWithAuto {
    }

    public static class MarginBottom extends LengthLikeWithAuto {
    }

    public static class MarginLeft extends LengthLikeWithAuto {
    }

    public static class MaxHeight extends LengthLikeWithNone {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class MaxWidth extends LengthLikeWithNone {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class MinHeight extends NonNegativeLengthLike {
    }

    public static class MinWidth extends NonNegativeLengthLike {
    }

    public static class Orphans extends PlainInteger {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class Overflow extends SingleIdent {
        // visible | hidden | scroll | auto | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.VISIBLE, IdentValue.HIDDEN,
                        /* IdentValue.SCROLL, IdentValue.AUTO, */ });

        // We only support visible or hidden for now
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class PaddingTop extends NonNegativeLengthLike {
    }

    public static class PaddingRight extends NonNegativeLengthLike {
    }

    public static class PaddingBottom extends NonNegativeLengthLike {
    }

    public static class PaddingLeft extends NonNegativeLengthLike {
    }

    public static class PageBreakBefore extends SingleIdent {
        // auto | always | avoid | left | right | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.AUTO, IdentValue.ALWAYS,
                        IdentValue.AVOID, IdentValue.LEFT,
                        IdentValue.RIGHT });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Page implements PropertyBuilder {

    	@Override
    	public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentType(cssName, value);

                if (! GeneralUtil.ciEquals(value.getStringValue(), "auto")) {
                    // Treat as string since it won't be a proper IdentValue
                    value = new PropertyValueImp(
                    		CSSPrimitiveUnit.CSS_STRING, value.getStringValue(), value.getCssText());
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }

    public static class PageBreakAfter extends SingleIdent {
        // auto | always | avoid | left | right | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.AUTO, IdentValue.ALWAYS,
                        IdentValue.AVOID, IdentValue.LEFT,
                        IdentValue.RIGHT });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class PageBreakInside extends SingleIdent {
        // avoid | auto | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.AVOID, IdentValue.AUTO });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Position implements PropertyBuilder {
        // static | relative | absolute | fixed | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.STATIC, IdentValue.RELATIVE,
                        IdentValue.ABSOLUTE, IdentValue.FIXED });
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = (PropertyValue)values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    checkIdentType(cssName, value);
                    IdentValue ident = checkIdent(cssName, value);

                    checkValidity(cssName, getAllowed(), ident);
                } else if (value.getPropertyValueType() == PropertyValueImp.VALUE_TYPE_FUNCTION) {
                    FSFunction function = value.getFunction();
                    if (GeneralUtil.ciEquals(function.getName(), "running")) {
                        List<PropertyValue> params = function.getParameters();
                        if (params.size() == 1) {
                            PropertyValue param = (PropertyValue)params.get(0);
                            if (param.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_IDENT) {
                            	cssThrowError(LangId.RUNNING_NEED_IDENTIFIER, cssName);
                            }
                        } else {
                        	cssThrowError(LangId.VALUE_COUNT_MISMATCH, params.size(), cssName, 1);
                        }
                    } else {
                        cssThrowError(LangId.ONLY_RUNNING_ALLOWED, cssName);
                    }
                } else {
                   cssThrowError(LangId.MUST_BE_FUNC_OR_IDENTIFIER, cssName);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));

        }

        private EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Right extends LengthLikeWithAuto {
    }

    public static class Src extends GenericURIWithNone {
    }

    public static class TabSize extends PlainInteger {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class Top extends LengthLikeWithAuto {
    }

    public static class TableLayout extends SingleIdent {
        // auto | fixed | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.AUTO, IdentValue.FIXED });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class TextAlign extends SingleIdent {
        // left | right | center | justify | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.LEFT, IdentValue.RIGHT,
                        IdentValue.CENTER, IdentValue.JUSTIFY });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class TextDecoration implements PropertyBuilder {
        // none | [ underline || overline || line-through || blink ] | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        /* IdentValue.NONE, */ IdentValue.UNDERLINE,
                        IdentValue.OVERLINE, IdentValue.LINE_THROUGH,
                        /* IdentValue.BLINK */ });

        private EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            if (values.size() == 1) {
                PropertyValue value = values.get(0);
                boolean goWithSingle = false;
                if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
                    goWithSingle = true;
                } else {
                    checkIdentType(CSSName.TEXT_DECORATION, value);
                    IdentValue ident = checkIdent(cssName, value);
                    if (ident == IdentValue.NONE) {
                        goWithSingle = true;
                    }
                }

                if (goWithSingle) {
                    return Collections.singletonList(
                            new PropertyDeclaration(cssName, value, important, origin));
                }
            }

            for (PropertyValue value : values) {
                checkInheritAllowed(value, false);
                checkIdentType(cssName, value);
                IdentValue ident = checkIdent(cssName, value);
                if (ident == IdentValue.NONE) {
                	cssThrowError(LangId.NO_NONE_VALUE, cssName);
                }
                checkValidity(cssName, getAllowed(), ident);
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, new PropertyValueImp(values), important, origin));

        }
    }

    public static class TextIndent extends LengthLike {
    }

    public static class TextTransform extends SingleIdent {
       // capitalize | uppercase | lowercase | none | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.CAPITALIZE, IdentValue.UPPERCASE,
                        IdentValue.LOWERCASE, IdentValue.NONE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class VerticalAlign extends LengthLikeWithIdent {
        // baseline | sub | super | top | text-top | middle
        // | bottom | text-bottom | <percentage> | <length> | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.BASELINE, IdentValue.SUB,
                        IdentValue.SUPER, IdentValue.TOP,
                        IdentValue.TEXT_TOP, IdentValue.MIDDLE,
                        IdentValue.BOTTOM, IdentValue.TEXT_BOTTOM });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class Visibility extends SingleIdent {
        // visible | hidden | collapse | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.VISIBLE, IdentValue.HIDDEN, IdentValue.COLLAPSE });
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class WhiteSpace extends SingleIdent {
        // normal | pre | nowrap | pre-wrap | pre-line | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.NORMAL, IdentValue.PRE, IdentValue.NOWRAP,
                        IdentValue.PRE_WRAP, IdentValue.PRE_LINE});
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }

    public static class WordWrap extends SingleIdent {
        // normal | break-word
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] {
                        IdentValue.NORMAL, IdentValue.BREAK_WORD});
        @Override
        protected EnumSet<IdentValue> getAllowed() {
            return ALLOWED;
        }
    }


    public static class Widows extends PlainInteger {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class Width extends LengthLikeWithAuto {
        @Override
    	protected boolean isNegativeValuesAllowed() {
            return false;
        }
    }

    public static class WordSpacing extends LengthWithNormal {
    }

    public static class ZIndex implements PropertyBuilder {
        // auto | <integer> | inherit
        private static final EnumSet<IdentValue> ALLOWED = setFor(
                new IdentValue[] { IdentValue.AUTO });
        @Override
        public List<PropertyDeclaration> buildDeclarations(
                CSSName cssName, List<PropertyValue> values, CSSOrigin origin, boolean important, boolean inheritAllowed) {
            checkValueCount(cssName, 1, values.size());
            PropertyValue value = values.get(0);
            checkInheritAllowed(value, inheritAllowed);
            if (value.getCssValueTypeN() != CSSValueType.CSS_INHERIT) {
                checkIdentOrIntegerType(cssName, value);

                if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) {
                    IdentValue ident = checkIdent(cssName, value);
                    checkValidity(cssName, ALLOWED, ident);
                }
            }

            return Collections.singletonList(
                    new PropertyDeclaration(cssName, value, important, origin));
        }
    }
    
	public static class Opacity implements PropertyBuilder {
		@Override
		public List<PropertyDeclaration> buildDeclarations(CSSName cssName,
				List<PropertyValue> values, CSSOrigin origin,
				boolean important, boolean inheritAllowed) {
			checkValueCount(cssName, 1, values.size());
			PropertyValue value = values.get(0);
			checkInheritAllowed(value, inheritAllowed);
			checkNumberType(cssName, value);

			if (value.getFloatValue() > 1 || value.getFloatValue() < 0) {
				cssThrowError(LangId.OPACITY_OUT_OF_RANGE);
			}

			return Collections.singletonList(new PropertyDeclaration(cssName,
					value, important, origin));
		}
	}

	private static class GenericBorderCornerRadius implements PropertyBuilder {
		@Override
		public List<PropertyDeclaration> buildDeclarations(CSSName cssName,
				List<PropertyValue> values, CSSOrigin origin,
				boolean important, boolean inheritAllowed) {
			checkValueCount(cssName, 1, 2, values.size());

			PropertyValue first = values.get(0);
			PropertyValue second = values.size() == 2 ? values.get(1)
					: null;

			checkInheritAllowed(first, inheritAllowed);

			if (second != null) {
				checkInheritAllowed(second, false);
			}

			checkLengthOrPercentType(cssName, first);

			if (second == null) {
				return createTwoValueResponse(cssName, first, first, origin,
						important);
			} else {
				checkLengthOrPercentType(cssName, second);
				return createTwoValueResponse(cssName, first, second, origin,
						important);
			}
		}
	}

	public static class BorderTopLeftRadius extends GenericBorderCornerRadius {
	}

	public static class BorderTopRightRadius extends GenericBorderCornerRadius {
	}

	public static class BorderBottomRightRadius extends GenericBorderCornerRadius {
	}

	public static class BorderBottomLeftRadius extends GenericBorderCornerRadius {
	}

	private static List<PropertyDeclaration> createTwoValueResponse(
			CSSName cssName, PropertyValue value1,
			PropertyValue value2,
			CSSOrigin origin, boolean important) {

		List<PropertyValue> values = Arrays.asList(value1, value2);

		PropertyDeclaration result = new PropertyDeclaration(cssName,
				new PropertyValueImp(values), important, origin);

		return Collections.singletonList(result);
	}

	public static class BorderRadius implements PropertyBuilder
	{
		@Override
		public List<PropertyDeclaration> buildDeclarations(CSSName cssName,
				List<PropertyValue> values, CSSOrigin origin,
				boolean important, boolean inheritAllowed)
		{
			// border-radius: 2em 1em 4em 1em / 0.5em 3em 5em 6em;
			// Note: The four values for each radii are given in the order top-left, top-right,
			// bottom-right, bottom-left. If bottom-left is omitted it is the same as top-right. 
			// If bottom-right is omitted it is the same as top-left. If top-right is omitted 
			// it is the same as top-left.

			// TODO: Not pickup up forward slash.
			checkValueCount(cssName, 1, 9, values.size());
			
			if (values.size() == 1)
			{
				// Deal with the common case first.
				List<PropertyDeclaration> declarations = new ArrayList<>(4);
				
				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_TOP_LEFT_RADIUS).buildDeclarations(
						CSSName.BORDER_TOP_LEFT_RADIUS,
						values, origin, important, true));

				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_TOP_RIGHT_RADIUS).buildDeclarations(
						CSSName.BORDER_TOP_RIGHT_RADIUS,
						values, origin, important, true));

				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_BOTTOM_RIGHT_RADIUS).buildDeclarations(
						CSSName.BORDER_BOTTOM_RIGHT_RADIUS,
						values, origin, important, true));

				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_BOTTOM_LEFT_RADIUS).buildDeclarations(
						CSSName.BORDER_BOTTOM_LEFT_RADIUS,
						values, origin, important, true));
				
				return declarations;
			}
			else
			{
				List<PropertyDeclaration> declarations = new ArrayList<>(4);
				List<PropertyValue> leftValues = new ArrayList<>(4);
				List<PropertyValue> rightValues = new ArrayList<>(4);

				boolean addToLeft = true;

				for (PropertyValue value : values) {
					if (value.getOperator() == Token.TK_VIRGULE) {
						addToLeft = false;
					}
					
					if (addToLeft) {
						leftValues.add(value);
					} else {
						rightValues.add(value);
					}
				}
			
				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_TOP_LEFT_RADIUS).buildDeclarations(
						CSSName.BORDER_TOP_LEFT_RADIUS,
						getValues(leftValues, rightValues, 0), origin,
						important, true));

				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_TOP_RIGHT_RADIUS).buildDeclarations(
						CSSName.BORDER_TOP_RIGHT_RADIUS,
						getValues(leftValues, rightValues, 1), origin,
						important, true));

				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_BOTTOM_RIGHT_RADIUS).buildDeclarations(
						CSSName.BORDER_BOTTOM_RIGHT_RADIUS,
						getValues(leftValues, rightValues, 2), origin,
						important, true));

				declarations.addAll(CSSName.getPropertyBuilder(
						CSSName.BORDER_BOTTOM_LEFT_RADIUS).buildDeclarations(
						CSSName.BORDER_BOTTOM_LEFT_RADIUS,
						getValues(leftValues, rightValues, 3), origin,
						important, true));

				return declarations;
			}

		}

		private List<PropertyValue> getValues(List<PropertyValue> leftValues,
				List<PropertyValue> rightValues, int index)
		{
			List<PropertyValue> values = new ArrayList<>(2);
			values.add(getValue(leftValues, index));

			if (!rightValues.isEmpty())
			{
				values.add(getValue(rightValues, index));
			}

			return values;
		}

		private PropertyValue getValue(List<PropertyValue> list, int index)
		{
			if (index < list.size())
			{
				return list.get(index);
			}
			else
			{
				if (index == 3 && list.size() >= 2)
				{
					// If bottom-left(3) is omitted it is the same as top-right(1).
					return list.get(1);
				}

				// Everything else matches to zero if missing.
				return list.get(0);
			}
		}
	}
}

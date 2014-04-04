package org.xhtmlrenderer.css.parser.property;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.CSSParseException;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp.CSSValueType;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.util.LangId;
import org.xhtmlrenderer.util.XRLog;

/**
 * Static utility functions to check types, etc for builders to use.
 */
public class BuilderUtil {
	private BuilderUtil() {
	}

	
	public static final EnumSet<CSSPrimitiveUnit> LENGTH_UNITS = EnumSet.of(
			CSSPrimitiveUnit.CSS_EMS,
			CSSPrimitiveUnit.CSS_EXS,
			CSSPrimitiveUnit.CSS_PX,
			CSSPrimitiveUnit.CSS_IN,
			CSSPrimitiveUnit.CSS_CM,
			CSSPrimitiveUnit.CSS_MM,
			CSSPrimitiveUnit.CSS_PT,
			CSSPrimitiveUnit.CSS_PC);
	
	public static void cssThrowError(LangId key, Object... args) 
	{
		String msg = String.format(SharedContext.ERRS.get().getString(key.toString()), args);
		SharedContext.USER_ERRORS.get().add(msg);
		throw new CSSParseException(msg, -1);
	}

	public static void cssNoThrowError(LangId key, Object... args) 
	{
		String msg = String.format(SharedContext.ERRS.get().getString(key.toString()), args);
		SharedContext.USER_ERRORS.get().add(msg);
		XRLog.cssParse(Level.WARNING, msg);
	}
	
	public static void checkValueType(CSSName cssName, PropertyValue value, EnumSet<CSSPrimitiveUnit> in)
	{
		if (!in.contains(value.getPrimitiveTypeN()))
			cssThrowError(LangId.UNSUPPORTED_TYPE, value.getPrimitiveTypeN(), cssName);
	}
	
	public static void checkValueType(CSSName cssName, PropertyValue value, EnumSet<CSSPrimitiveUnit> in, EnumSet<CSSPrimitiveUnit> in2)
	{
		if (!in.contains(value.getPrimitiveTypeN()) && !in2.contains(value.getPrimitiveTypeN()))
			cssThrowError(LangId.UNSUPPORTED_TYPE, value.getPrimitiveTypeN(), cssName);
	}
	
	public static void checkValueCount(CSSName cssName, int expected, int found) {
		if (expected != found)
			cssThrowError(LangId.VALUE_COUNT_MISMATCH, found, cssName, expected);
	}

	public static void checkValueCount(CSSName cssName, int min, int max, int found) {
		if (!(found >= min && found <= max))
			cssThrowError(LangId.MIN_MAX_VALUE_COUNT_MISMATCH, found, cssName, min, max);
	}

	public static void checkIdentType(CSSName cssName, PropertyValue value) {
		if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_IDENT) 
			cssThrowError(LangId.MUST_BE_IDENTIFIER, cssName);
	}

	public static void checkIdentOrURIType(CSSName cssName, PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();

		if (type != CSSPrimitiveUnit.CSS_IDENT &&
			type != CSSPrimitiveUnit.CSS_URI)
			cssThrowError(LangId.MUST_BE_URI_OR_IDENTIFIER, cssName);
	}

	public static void checkIdentOrColorType(CSSName cssName, PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();

		if (type != CSSPrimitiveUnit.CSS_IDENT
			&& type != CSSPrimitiveUnit.CSS_RGBCOLOR)
		cssThrowError(LangId.MUST_BE_COLOR_OR_IDENTIFIER, cssName);
	}

	public static void checkIdentOrIntegerType(CSSName cssName, PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if ((type != CSSPrimitiveUnit.CSS_IDENT && type != CSSPrimitiveUnit.CSS_NUMBER)
			|| (type == CSSPrimitiveUnit.CSS_NUMBER &&
			(int) value.getFloatValue() != 
			Math.round(value.getFloatValue()))) 
		{
			cssThrowError(LangId.MUST_BE_INT_OR_IDENTIFIER, cssName);
		}
	}

	public static void checkInteger(CSSName cssName, PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if (type != CSSPrimitiveUnit.CSS_NUMBER ||
		   (type == CSSPrimitiveUnit.CSS_NUMBER && 
		    value.getFloatValue() % 1f != 0f))
		{
			cssThrowError(LangId.MUST_BE_INT, cssName);
		}
	}

	public static void checkIdentOrLengthType(CSSName cssName, PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if (type != CSSPrimitiveUnit.CSS_IDENT && !isLength(value)) {
			cssThrowError(LangId.MUST_BE_LENGTH_OR_IDENTIFIER, cssName);
		}
	}

	public static void checkIdentOrNumberType(CSSName cssName, PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if (type != CSSPrimitiveUnit.CSS_IDENT
				&& type != CSSPrimitiveUnit.CSS_NUMBER) {
			cssThrowError(LangId.MUST_BE_NUMBER_OR_IDENTIFIER, cssName);
		}
	}

	public static void checkIdentLengthOrPercentType(CSSName cssName,
			PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if (type != CSSPrimitiveUnit.CSS_IDENT && !isLength(value)
				&& type != CSSPrimitiveUnit.CSS_PERCENTAGE) {
			cssThrowError(LangId.MUST_BE_LENGTH_PERCENT_OR_IDENTIFIER, cssName);
		}
	}

	public static void checkLengthOrPercentType(CSSName cssName,
			PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if (!isLength(value) && type != CSSPrimitiveUnit.CSS_PERCENTAGE) {
			cssThrowError(LangId.MUST_BE_LENGTH_OR_PERCENT, cssName);
		}
	}

	public static void checkLengthType(CSSName cssName, PropertyValue value) {
		if (!isLength(value)) {
			cssThrowError(LangId.MUST_BE_LENGTH, cssName);
		}
	}

	public static void checkNumberType(CSSName cssName, PropertyValue value) {
		if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_NUMBER) {
			cssThrowError(LangId.MUST_BE_NUMBER, cssName);
		}
	}

	public static void checkStringType(CSSName cssName, PropertyValue value) {
		if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_STRING) {
			cssThrowError(LangId.MUST_BE_STRING, cssName);
		}
	}

	public static void checkIdentOrString(CSSName cssName, PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if (type != CSSPrimitiveUnit.CSS_STRING
				&& type != CSSPrimitiveUnit.CSS_IDENT) {
			cssThrowError(LangId.MUST_BE_STRING_OR_IDENTIFIER, cssName);
		}
	}

	public static void checkIdentLengthNumberOrPercentType(CSSName cssName,
			PropertyValue value) {
		CSSPrimitiveUnit type = value.getPrimitiveTypeN();
		if (type != CSSPrimitiveUnit.CSS_IDENT && !isLength(value)
				&& type != CSSPrimitiveUnit.CSS_PERCENTAGE
				&& type != CSSPrimitiveUnit.CSS_NUMBER) {
			cssThrowError(LangId.MUST_BE_LENGTH_NUMBER_PERCENT_OR_IDENTIFIER, cssName);
		}
	}

	public static boolean isLength(PropertyValue value) {
		CSSPrimitiveUnit unit = value.getPrimitiveTypeN();
		return unit == CSSPrimitiveUnit.CSS_EMS
				|| unit == CSSPrimitiveUnit.CSS_EXS
				|| unit == CSSPrimitiveUnit.CSS_PX
				|| unit == CSSPrimitiveUnit.CSS_IN
				|| unit == CSSPrimitiveUnit.CSS_CM
				|| unit == CSSPrimitiveUnit.CSS_MM
				|| unit == CSSPrimitiveUnit.CSS_PT
				|| unit == CSSPrimitiveUnit.CSS_PC
				|| (unit == CSSPrimitiveUnit.CSS_NUMBER && value
					.getFloatValue() == 0.0f);
	}

	public static void checkValidity(CSSName cssName, EnumSet<IdentValue> validValues,
			IdentValue value) {
		if (!validValues.contains(value)) {
			cssThrowError(LangId.UNSUPPORTED_IDENTIFIER, value, cssName);
		}
	}

	public static void checkIdentValidity(CSSName cssName, EnumSet<IdentValue> validValues,
			PropertyValue value) {
		IdentValue ident = checkIdent(cssName, value);
		
		if (ident == null || !validValues.contains(ident)) {
			cssThrowError(LangId.UNSUPPORTED_IDENTIFIER, value, cssName);
		}
	}
	
	public static IdentValue checkIdent(CSSName cssName, PropertyValue value) {
		IdentValue result = IdentValue.fsValueOf(value.getStringValue());
		if (result == null) {
			cssThrowError(LangId.UNRECOGNIZED_IDENTIFIER, value.getStringValue(), cssName);
		}
		value.setIdentValue(result);
		return result;
	}

	public static PropertyDeclaration copyOf(PropertyDeclaration decl, CSSName newName) {
		return new PropertyDeclaration(newName, decl.getValue(),
				decl.isImportant(), decl.getOrigin());
	}

	public static void checkInheritAllowed(PropertyValue value,
			boolean inheritAllowed) {
		if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT
				&& !inheritAllowed) {
			cssThrowError(LangId.INVALID_INHERIT);
		}
	}

	public static List<PropertyDeclaration> checkInheritAll(CSSName[] all,
			List<PropertyValue> values, CSSOrigin origin, boolean important,
			boolean inheritAllowed) {
		if (values.size() == 1) {
			PropertyValue value = values.get(0);
			checkInheritAllowed(value, inheritAllowed);
			if (value.getCssValueTypeN() == CSSValueType.CSS_INHERIT) {
				List<PropertyDeclaration> result = new ArrayList<PropertyDeclaration>( all.length);
				for (CSSName element : all) {
					result.add(new PropertyDeclaration(element, value, important, origin));
				}
				return result;
			}
		}

		return null;
	}
}

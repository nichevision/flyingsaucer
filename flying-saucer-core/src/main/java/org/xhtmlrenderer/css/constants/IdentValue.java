/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Patrick Wright
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
package org.xhtmlrenderer.css.constants;

import java.util.HashMap;
import java.util.Map;

import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.FSDerivedValue;
import org.xhtmlrenderer.util.XRRuntimeException;


/**
 * An IdentValue represents a string that you can assign to a CSS property,
 * where the string is one of several enumerated values. For example,
 * "whitespace" can take the values "nowrap", "pre" and "normal". There is a
 * static instance for all idents in the CSS 2 spec, which you can retrieve
 * using the {@link #getByIdentString(String)} method. The instance doesn't have
 * any behavior: it's just a marker so that you can retrieve an ident from a
 * DerivedValue or CalculatedStyle, then compare to the instance here. For
 * example: <pre>
 * CalculatedStyle style = ...getstyle from somewhere
 * IdentValue whitespace = style.getIdent(CSSName.WHITESPACE);
 * if ( whitespace == IdentValue.NORMAL ) {
 *      // perform normal spacing
 * } else if ( whitespace == IdentValue.NOWRAP ) {
 *      // space with no wrapping
 * } else if ( whitespace == IdentValue.PRE ) {
 *      // preserve spacing
 * }
 * </pre> All static instances are instantiated automatically, and are
 * Singletons, so you can compare using a simple Object comparison using <code>==</code>
 * .
 *
 * @author Patrick Wright
 */
public enum IdentValue implements FSDerivedValue
{
	ABSOLUTE("absolute"),
	ALWAYS("always"),
	ARMENIAN("armenian"),
	AUTO("auto"),
	AVOID("avoid"),
	BASELINE("baseline"),
	BLINK("blink"),
	BLOCK("block"),
	BOLD("bold"),
	BOLDER("bolder"),
	BOTH("both"),
	BOTTOM("bottom"),
	CAPITALIZE("capitalize"),
	CENTER("center"),
	CIRCLE("circle"),
	CJK_IDEOGRAPHIC("cjk-ideographic"),
    CLOSE_QUOTE("close-quote"),
    COLLAPSE("collapse"),
    COMPACT("compact"),
    CONTAIN("contain"),
    COVER("cover"),
    CREATE("create"),
    DASHED("dashed"),
    DECIMAL("decimal"),
    DECIMAL_LEADING_ZERO("decimal-leading-zero"),
    DISC("disc"),
    DOTTED("dotted"),
    DOUBLE("double"),
    DYNAMIC("dynamic"),
    FIXED("fixed"),
    FONT_WEIGHT_100("100"),
    FONT_WEIGHT_200("200"),
    FONT_WEIGHT_300("300"),
    FONT_WEIGHT_400("400"),
    FONT_WEIGHT_500("500"),
    FONT_WEIGHT_600("600"),
    FONT_WEIGHT_700("700"),
    FONT_WEIGHT_800("800"),
    FONT_WEIGHT_900("900"),
    FS_CONTENT_PLACEHOLDER("-fs-content-placeholder"),
    FS_INITIAL_VALUE("-fs-initial-value"),
    GEORGIAN("georgian"),
    GROOVE("groove"),
    HEBREW("hebrew"),
    HIDDEN("hidden"),
    HIDE("hide"),
    HIRAGANA("hiragana"),
    HIRAGANA_IROHA("hiragana-iroha"),
    INHERIT("inherit"),
    INLINE("inline"),
    INLINE_BLOCK("inline-block"),
    INLINE_TABLE("inline-table"),
    INSET("inset"),
    INSIDE("inside"),
    ITALIC("italic"),
    JUSTIFY("justify"),
    KATAKANA("katakana"),
    KATAKANA_IROHA("katakana-iroha"),
    KEEP("keep"),
    LANDSCAPE("landscape"),
    LEFT("left"),
    LIGHTER("lighter"),
    LINE("line"),
    LINEAR_GRADIENT("linear-gradient"),
    LINE_THROUGH("line-through"),
    LIST_ITEM("list-item"),
    LOWER_ALPHA("lower-alpha"),
    LOWER_GREEK("lower-greek"),
    LOWER_LATIN("lower-latin"),
    LOWER_ROMAN("lower-roman"),
    LOWERCASE("lowercase"),
    LTR("ltr"),
    MARKER("marker"),
    MIDDLE("middle"),
    NO_CLOSE_QUOTE("no-close-quote"),
    NO_OPEN_QUOTE("no-open-quote"),
    NO_REPEAT("no-repeat"),
    NONE("none"),
    NORMAL("normal"),
    NOWRAP("nowrap"),
    BREAK_WORD("break-word"),
    OBLIQUE("oblique"),
    OPEN_QUOTE("open-quote"),
    OUTSET("outset"),
    OUTSIDE("outside"),
    OVERLINE("overline"),
    PAGINATE("paginate"),
    POINTER("pointer"),
    PORTRAIT("portrait"),
    PRE("pre"),
    PRE_LINE("pre-line"),
    PRE_WRAP("pre-wrap"),
    RELATIVE("relative"),
    REPEAT("repeat"),
    REPEAT_X("repeat-x"),
    REPEAT_Y("repeat-y"),
    RIDGE("ridge"),
    RIGHT("right"),
    RUN_IN("run-in"),
    SCROLL("scroll"),
    SEPARATE("separate"),
    SHOW("show"),
    SMALL_CAPS("small-caps"),
    SOLID("solid"),
    SQUARE("square"),
    STATIC("static"),
    SUB("sub"),
    SUPER("super"),
    TABLE("table"),
    TABLE_CAPTION("table-caption"),
    TABLE_CELL("table-cell"),
    TABLE_COLUMN("table-column"),
    TABLE_COLUMN_GROUP("table-column-group"),
    TABLE_FOOTER_GROUP("table-footer-group"),
    TABLE_HEADER_GROUP("table-header-group"),
    TABLE_ROW("table-row"),
    TABLE_ROW_GROUP("table-row-group"),
    TEXT_BOTTOM("text-bottom"),
    TEXT_TOP("text-top"),
    THICK("thick"),
    THIN("thin"),
    TOP("top"),
    TRANSPARENT("transparent"),
    UNDERLINE("underline"),
    UPPER_ALPHA("upper-alpha"),
    UPPER_LATIN("upper-latin"),
    UPPER_ROMAN("upper-roman"),
    UPPERCASE("uppercase"),
    VISIBLE("visible"),
    CROSSHAIR("crosshair"),
    DEFAULT("default"),
    EMBED("embed"),
    E_RESIZE("e-resize"),
    HELP("help"),
    LARGE("large"),
    LARGER("larger"),
    MEDIUM("medium"),
    MOVE("move"),
    N_RESIZE("n-resize"),
    NE_RESIZE("ne-resize"),
    NW_RESIZE("nw-resize"),
    PROGRESS("progress"),
    S_RESIZE("s-resize"),
    SE_RESIZE("se-resize"),
    SMALL("small"),
    SMALLER("smaller"),
    START("start"),
    SW_RESIZE("sw-resize"),
    TEXT("text"),
    W_RESIZE("w-resize"),
    WAIT("wait"),
    X_LARGE("x-large"),
    X_SMALL("x-small"),
    XX_LARGE("xx-large"),
    XX_SMALL("xx-small"),
    INITIAL("initial");

	private final String ident;
    public final int fsId;

    private static final Map<String, IdentValue> ALL_IDENT_VALUES = new HashMap<>(values().length);

    private IdentValue(final String ident) 
    {
        this.ident = ident;
        this.fsId = this.ordinal();
    }

    /**
     * Returns a string representation of the object, in this case, the ident as
     * a string (as it appears in the CSS spec).
     */
    @Override
    public String toString() {
        return ident;
    }

    /**
     * Returns the Singleton IdentValue that corresponds to the given string,
     * e.g. for "normal" will return IdentValue.NORMAL. Use this when you have
     * the string but need to look up the Singleton. If the string doesn't match
     * an ident in the CSS spec, a runtime exception is thrown.
     *
     * @param ident The identifier to retrieve the Singleton IdentValue for.
     * @return see desc.
     */
    public static IdentValue getByIdentString(final String ident) {
        final IdentValue val = ALL_IDENT_VALUES.get(ident);
        if (val == null) {
            throw new XRRuntimeException("Ident named " + ident + " has no IdentValue instance assigned to it.");
        }
        return val;
    }

    public static boolean looksLikeIdent(final String ident) {
        return ALL_IDENT_VALUES.containsKey(ident);
    }

    public static IdentValue fsValueOf(final String ident) {
        return ALL_IDENT_VALUES.get(ident);
    }

    public static int getIdentCount() {
        return ALL_IDENT_VALUES.size();
    }

    static {
    	for (final IdentValue id : values())
    	{
    		ALL_IDENT_VALUES.put(id.ident, id);
    	}
    }
    
    /*
     * METHODS USED TO SUPPORT IdentValue as an FSDerivedValue, used in CalculatedStyle.
     * Most of these throw exceptions--makes use of the interface easier in CS (avoids casting)
     */

    @Override
    public boolean isDeclaredInherit() {
        return this == INHERIT;
    }

    @Override
    public float asFloat() {
        throw new XRRuntimeException("Ident value is never a float; wrong class used for derived value.");
    }

    @Override
    public FSColor asColor() {
        throw new XRRuntimeException("Ident value is never a color; wrong class used for derived value.");
    }

    @Override
    public float getFloatProportionalTo(final CSSName cssName,
                                        final float baseValue,
                                        final CssContext ctx) {
        throw new XRRuntimeException("Ident value (" + toString() + ") is never a length; wrong class used for derived value.");
    }

    @Override
    public String asString() {
        return toString();
    }

    @Override
    public String[] asStringArray() {
        throw new XRRuntimeException("Ident value is never a string array; wrong class used for derived value.");
    }

    @Override
    public IdentValue asIdentValue() {
        return this;
    }

    @Override
    public boolean hasAbsoluteUnit() {
        // log and return false
        throw new XRRuntimeException("Ident value is never an absolute unit; wrong class used for derived value; this " +
                "ident value is a " + this.asString());
    }

    @Override
    public boolean isIdent() {
        return true;
    }

    @Override
    public boolean isDependentOnFontSize() {
        return false;
    }
}

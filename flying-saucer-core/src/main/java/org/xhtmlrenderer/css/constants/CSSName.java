/*
 * {{{ header & license
 * CSSName.java
 * Copyright (c) 2004, 2005 Patrick Wright
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.css.constants;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.xhtmlrenderer.css.parser.CSSErrorHandler;
import org.xhtmlrenderer.css.parser.CSSParser;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.property.BackgroundPropertyBuilder;
import org.xhtmlrenderer.css.parser.property.BorderPropertyBuilders;
import org.xhtmlrenderer.css.parser.property.BorderSpacingPropertyBuilder;
import org.xhtmlrenderer.css.parser.property.ContentPropertyBuilder;
import org.xhtmlrenderer.css.parser.property.CounterPropertyBuilder;
import org.xhtmlrenderer.css.parser.property.FontPropertyBuilder;
import org.xhtmlrenderer.css.parser.property.ListStylePropertyBuilder;
import org.xhtmlrenderer.css.parser.property.OneToFourPropertyBuilders;
import org.xhtmlrenderer.css.parser.property.PrimitivePropertyBuilders;
import org.xhtmlrenderer.css.parser.property.PropertyBuilder;
import org.xhtmlrenderer.css.parser.property.QuotesPropertyBuilder;
import org.xhtmlrenderer.css.parser.property.SizePropertyBuilder;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.css.style.FSDerivedValue;
import org.xhtmlrenderer.css.style.derived.DerivedValueFactory;
import org.xhtmlrenderer.util.XRLog;


/**
 * A CSSName is a Singleton representing a single CSS property name, like
 * border-width. The class declares a Singleton static instance for each CSS
 * Level 2 property. A CSSName instance has the property name available from the
 * {@link #toString()} method, as well as a unique (among all CSSName instances)
 * integer id ranging from 0...n instances, incremented by 1, available using
 * the final public int FS_ID (e.g. CSSName.COLOR.FS_ID).
 *
 * @author Patrick Wright
 */
public enum CSSName {

    /**
     * Unique CSSName instance for CSS2 property.
     * TODO: UA dependent
     */
    COLOR
            (
                    "color",
                    Marker.PRIMITIVE,
                    "black",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.Color()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BACKGROUND_COLOR
            (
                    "background-color",
                    Marker.PRIMITIVE,
                    "transparent",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BackgroundColor()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BACKGROUND_IMAGE
            (
                    "background-image",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BackgroundImage()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BACKGROUND_REPEAT
            (
                    "background-repeat",
                    Marker.PRIMITIVE,
                    "repeat",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BackgroundRepeat()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BACKGROUND_ATTACHMENT
            (
                    "background-attachment",
                    Marker.PRIMITIVE,
                    "scroll",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BackgroundAttachment()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BACKGROUND_POSITION
            (
                    "background-position",
                    Marker.PRIMITIVE,
                    "0% 0%",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BackgroundPosition()
            ),

    BACKGROUND_SIZE
        (
                "background-size",
                Marker.PRIMITIVE,
                "auto auto",
                Marker.NOT_INHERITED,
                new PrimitivePropertyBuilders.BackgroundSize()
        ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_COLLAPSE
            (
                    "border-collapse",
                    Marker.PRIMITIVE,
                    "separate",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.BorderCollapse()
            ),

    /**
     * Unique CSSName instance for fictitious property.
     */
    FS_BORDER_SPACING_HORIZONTAL
            (
                    "-fs-border-spacing-horizontal",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSBorderSpacingHorizontal()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_BORDER_SPACING_VERTICAL
            (
                    "-fs-border-spacing-vertical",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSBorderSpacingVertical()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_DYNAMIC_AUTO_WIDTH
            (
                    "-fs-dynamic-auto-width",
                    Marker.PRIMITIVE,
                    "static",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSDynamicAutoWidth()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_FONT_METRIC_SRC
            (
                    "-fs-font-metric-src",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSFontMetricSrc()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_KEEP_WITH_INLINE
            (
                    "-fs-keep-with-inline",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSKeepWithInline()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_PAGE_WIDTH
            (
                    "-fs-page-width",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSPageWidth()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_PAGE_HEIGHT
            (
                    "-fs-page-height",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSPageHeight()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_PAGE_SEQUENCE
            (
                    "-fs-page-sequence",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSPageSequence()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_PDF_FONT_EMBED
            (
                    "-fs-pdf-font-embed",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSPDFFontEmbed()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_PDF_FONT_ENCODING
            (
                    "-fs-pdf-font-encoding",
                    Marker.PRIMITIVE,
                    "Cp1252",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSPDFFontEncoding()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_PAGE_ORIENTATION
            (
                    "-fs-page-orientation",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSPageOrientation()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_TABLE_PAGINATE
            (
                    "-fs-table-paginate",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSTablePaginate()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_TEXT_DECORATION_EXTENT
            (
                    "-fs-text-decoration-extent",
                    Marker.PRIMITIVE,
                    "line",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSTextDecorationExtent()
            ),

    /**
     * Used for forcing images to scale to a certain width
     */
    FS_FIT_IMAGES_TO_WIDTH
        (
                "-fs-fit-images-to-width",
                Marker.PRIMITIVE,
                "auto",
                Marker.NOT_INHERITED,
                new PrimitivePropertyBuilders.FSFitImagesToWidth()
        ),

    /**
     * Used to control creation of named destinations for boxes having the id attribute set.
     */
    FS_NAMED_DESTINATION
            (
                    "-fs-named-destination",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSNamedDestination()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BOTTOM
            (
                    "bottom",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Bottom()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    CAPTION_SIDE
            (
                    "caption-side",
                    Marker.PRIMITIVE,
                    "top",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.CaptionSide()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    CLEAR
            (
                    "clear",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Clear()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    CLIP
            (
                    "clip",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    false,
                    null
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    CONTENT
            (
                    "content",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.NOT_INHERITED,
                    new ContentPropertyBuilder()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    COUNTER_INCREMENT
            (
                    "counter-increment",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    true,
                    new CounterPropertyBuilder.CounterIncrement()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    COUNTER_RESET
            (
                    "counter-reset",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    true,
                    new CounterPropertyBuilder.CounterReset()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    CURSOR
            (
                    "cursor",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.INHERITS,
                    true,
                    new PrimitivePropertyBuilders.Cursor()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    DIRECTION
            (
                    "direction",
                    Marker.PRIMITIVE,
                    "ltr",
                    Marker.INHERITS,
                    false,
                    null
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    DISPLAY
            (
                    "display",
                    Marker.PRIMITIVE,
                    "inline",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Display()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    EMPTY_CELLS
            (
                    "empty-cells",
                    Marker.PRIMITIVE,
                    "show",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.EmptyCells()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FLOAT
            (
                    "float",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Float()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FONT_STYLE
            (
                    "font-style",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.FontStyle()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FONT_VARIANT
            (
                    "font-variant",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.FontVariant()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FONT_WEIGHT
            (
                    "font-weight",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.FontWeight()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FONT_SIZE
            (
                    "font-size",
                    Marker.PRIMITIVE,
                    "medium",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.FontSize()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    LINE_HEIGHT
            (
                    "line-height",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.LineHeight()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     * TODO: UA dependent
     */
    FONT_FAMILY
            (
                    "font-family",
                    Marker.PRIMITIVE,
                    "serif",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.FontFamily()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_COLSPAN
            (
                    "-fs-table-cell-colspan",
                    Marker.PRIMITIVE,
                    "1",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSTableCellColspan()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FS_ROWSPAN
            (
                    "-fs-table-cell-rowspan",
                    Marker.PRIMITIVE,
                    "1",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.FSTableCellRowspan()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    HEIGHT
            (
                    "height",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Height()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    LEFT
            (
                    "left",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Left()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    LETTER_SPACING
            (
                    "letter-spacing",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    true,
                    new PrimitivePropertyBuilders.LetterSpacing()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    LIST_STYLE_TYPE
            (
                    "list-style-type",
                    Marker.PRIMITIVE,
                    "disc",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.ListStyleType()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    LIST_STYLE_POSITION
            (
                    "list-style-position",
                    Marker.PRIMITIVE,
                    "outside",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.ListStylePosition()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    LIST_STYLE_IMAGE
            (
                    "list-style-image",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.ListStyleImage()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    MAX_HEIGHT
            (
                    "max-height",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MaxHeight()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    MAX_WIDTH
            (
                    "max-width",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MaxWidth()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    MIN_HEIGHT
            (
                    "min-height",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MinHeight()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     * TODO: UA dependent
     */
    MIN_WIDTH
            (
                    "min-width",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MinWidth()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    ORPHANS
            (
                    "orphans",
                    Marker.PRIMITIVE,
                    "2",
                    Marker.INHERITS,
                    true,
                    new PrimitivePropertyBuilders.Orphans()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    OUTLINE_COLOR
            (
                    "outline-color",
                    Marker.PRIMITIVE,
                    /* "invert", */ "black",  // XXX Wrong (but doesn't matter for now)
                    Marker.NOT_INHERITED,
                    false,
                    null
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    OUTLINE_STYLE
            (
                    "outline-style",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    false,
                    null
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    OUTLINE_WIDTH
            (
                    "outline-width",
                    Marker.PRIMITIVE,
                    "medium",
                    Marker.NOT_INHERITED,
                    false,
                    null
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    OVERFLOW
            (
                    "overflow",
                    Marker.PRIMITIVE,
                    "visible",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Overflow()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    PAGE
            (
                    "page",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.Page()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    PAGE_BREAK_AFTER
            (
                    "page-break-after",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.PageBreakAfter()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    PAGE_BREAK_BEFORE
            (
                    "page-break-before",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.PageBreakBefore()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    PAGE_BREAK_INSIDE
            (
                    "page-break-inside",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.PageBreakInside()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    POSITION
            (
                    "position",
                    Marker.PRIMITIVE,
                    "static",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Position()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     * TODO: UA dependent
     */
    QUOTES
            (
                    "quotes",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.INHERITS,
                    new QuotesPropertyBuilder()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    RIGHT
            (
                    "right",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Right()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    SRC
            (
                    "src",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Src()
            ),

    /**
     * Used for controlling tab size in pre tags. See http://dev.w3.org/csswg/css3-text/#tab-size
     */
    TAB_SIZE
            (
                    "tab-size",
                    Marker.PRIMITIVE,
                    "8",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.TabSize()
                    ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    TABLE_LAYOUT
            (
                    "table-layout",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.TableLayout()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     * TODO: UA dependent
     */
    TEXT_ALIGN
            (
                    "text-align",
                    Marker.PRIMITIVE,
                    "left",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.TextAlign()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    TEXT_DECORATION
            (
                    "text-decoration",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.TextDecoration()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    TEXT_INDENT
            (
                    "text-indent",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.TextIndent()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    TEXT_TRANSFORM
            (
                    "text-transform",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.TextTransform()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    TOP
            (
                    "top",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Top()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    UNICODE_BIDI
            (
                    "unicode-bidi",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.NOT_INHERITED,
                    false,
                    null
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    VERTICAL_ALIGN
            (
                    "vertical-align",
                    Marker.PRIMITIVE,
                    "baseline",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.VerticalAlign()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    VISIBILITY
            (
                    "visibility",
                    Marker.PRIMITIVE,
                    "visible",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.Visibility()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    WHITE_SPACE
            (
                    "white-space",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.WhiteSpace()
            ),

    /**
     * Unique CSSName instance for CSS3 property.
     */
    WORD_WRAP
            (
                    "word-wrap",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    new PrimitivePropertyBuilders.WordWrap()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    WIDOWS
            (
                    "widows",
                    Marker.PRIMITIVE,
                    "2",
                    Marker.INHERITS,
                    true,
                    new PrimitivePropertyBuilders.Widows()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    WIDTH
            (
                    "width",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.Width()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    WORD_SPACING
            (
                    "word-spacing",
                    Marker.PRIMITIVE,
                    "normal",
                    Marker.INHERITS,
                    true,
                    new PrimitivePropertyBuilders.WordSpacing()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    Z_INDEX
            (
                    "z-index",
                    Marker.PRIMITIVE,
                    "auto",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.ZIndex()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_TOP_COLOR
            (
                    "border-top-color",
                    Marker.PRIMITIVE,
                    "=color",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderTopColor()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_RIGHT_COLOR
            (
                    "border-right-color",
                    Marker.PRIMITIVE,
                    "=color",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderLeftColor()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_BOTTOM_COLOR
            (
                    "border-bottom-color",
                    Marker.PRIMITIVE,
                    "=color",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderBottomColor()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_LEFT_COLOR
            (
                    "border-left-color",
                    Marker.PRIMITIVE,
                    "=color",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderLeftColor()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_TOP_STYLE
            (
                    "border-top-style",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderTopStyle()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_RIGHT_STYLE
            (
                    "border-right-style",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderRightStyle()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_BOTTOM_STYLE
            (
                    "border-bottom-style",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderBottomStyle()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_LEFT_STYLE
            (
                    "border-left-style",
                    Marker.PRIMITIVE,
                    "none",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderLeftStyle()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_TOP_WIDTH
            (
                    "border-top-width",
                    Marker.PRIMITIVE,
                    "medium",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderTopWidth()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_RIGHT_WIDTH
            (
                    "border-right-width",
                    Marker.PRIMITIVE,
                    "medium",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderRightWidth()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_BOTTOM_WIDTH
            (
                    "border-bottom-width",
                    Marker.PRIMITIVE,
                    "medium",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderBottomWidth()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_LEFT_WIDTH
            (
                    "border-left-width",
                    Marker.PRIMITIVE,
                    "medium",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.BorderLeftWidth()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    MARGIN_TOP
            (
                    "margin-top",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MarginTop()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    MARGIN_RIGHT
            (
                    "margin-right",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MarginRight()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    MARGIN_BOTTOM
            (
                    "margin-bottom",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MarginBottom()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    MARGIN_LEFT
            (
                    "margin-left",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.MarginLeft()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    PADDING_TOP
            (
                    "padding-top",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.PaddingTop()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    PADDING_RIGHT
            (
                    "padding-right",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.PaddingRight()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    PADDING_BOTTOM
            (
                    "padding-bottom",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.PaddingBottom()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    PADDING_LEFT
            (
                    "padding-left",
                    Marker.PRIMITIVE,
                    "0",
                    Marker.NOT_INHERITED,
                    new PrimitivePropertyBuilders.PaddingLeft()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BACKGROUND_SHORTHAND
            (
                    "background",
                    Marker.SHORTHAND,
                    "transparent none repeat scroll 0% 0%",
                    Marker.NOT_INHERITED,
                    new BackgroundPropertyBuilder()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_WIDTH_SHORTHAND
            (
                    "border-width",
                    Marker.SHORTHAND,
                    "medium",
                    Marker.NOT_INHERITED,
                    new OneToFourPropertyBuilders.BorderWidth()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_STYLE_SHORTHAND
            (
                    "border-style",
                    Marker.SHORTHAND,
                    "none",
                    Marker.NOT_INHERITED,
                    new OneToFourPropertyBuilders.BorderStyle()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_SHORTHAND
            (
                    "border",
                    Marker.SHORTHAND,
                    "medium none black",
                    Marker.NOT_INHERITED,
                    new BorderPropertyBuilders.Border()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_TOP_SHORTHAND
            (
                    "border-top",
                    Marker.SHORTHAND,
                    "medium none black",
                    Marker.NOT_INHERITED,
                    new BorderPropertyBuilders.BorderTop()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_RIGHT_SHORTHAND
            (
                    "border-right",
                    Marker.SHORTHAND,
                    "medium none black",
                    Marker.NOT_INHERITED,
                    new BorderPropertyBuilders.BorderRight()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_BOTTOM_SHORTHAND
            (
                    "border-bottom",
                    Marker.SHORTHAND,
                    "medium none black",
                    Marker.NOT_INHERITED,
                    new BorderPropertyBuilders.BorderBottom()
            ),
    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_LEFT_SHORTHAND
            (
                    "border-left",
                    Marker.SHORTHAND,
                    "medium none black",
                    Marker.NOT_INHERITED,
                    new BorderPropertyBuilders.BorderLeft()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_COLOR_SHORTHAND
            (
                    "border-color",
                    Marker.SHORTHAND,
                    "black",
                    Marker.NOT_INHERITED,
                    new OneToFourPropertyBuilders.BorderColor()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    BORDER_SPACING
            (
                    "border-spacing",
                    Marker.SHORTHAND,
                    "0",
                    Marker.INHERITS,
                    new BorderSpacingPropertyBuilder()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    FONT_SHORTHAND
            (
                    "font",
                    Marker.SHORTHAND,
                    "",
                    Marker.INHERITS,
                    new FontPropertyBuilder()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    LIST_STYLE_SHORTHAND
            (
                    "list-style",
                    Marker.SHORTHAND,
                    "disc outside none",
                    Marker.INHERITS,
                    new ListStylePropertyBuilder()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    MARGIN_SHORTHAND
            (
                    "margin",
                    Marker.SHORTHAND,
                    "0",
                    Marker.NOT_INHERITED,
                    new OneToFourPropertyBuilders.Margin()
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    OUTLINE_SHORTHAND
            (
                    "outline",
                    Marker.SHORTHAND,
                    "invert none medium",
                    Marker.NOT_INHERITED,
                    false,
                    null
            ),

    /**
     * Unique CSSName instance for CSS2 property.
     */
    PADDING_SHORTHAND
            (
                    "padding",
                    Marker.SHORTHAND,
                    "0",
                    Marker.NOT_INHERITED,
                    new OneToFourPropertyBuilders.Padding()
            ),


    /**
     * Unique CSSName instance for CSS2 property.
     */
    SIZE_SHORTHAND
            (
                    "size",
                    Marker.SHORTHAND,
                    "auto",
                    Marker.NOT_INHERITED,
                    new SizePropertyBuilder()
            ),
            
    OPACITY(
            "opacity",
            Marker.PRIMITIVE,
            "1",
            Marker.NOT_INHERITED,
            true,
            new PrimitivePropertyBuilders.Opacity()
           ),
    
    BORDER_TOP_LEFT_RADIUS(
                               "border-top-left-radius",
                               Marker.PRIMITIVE,
                               "0",
                               Marker.NOT_INHERITED,
                               new PrimitivePropertyBuilders.BorderTopLeftRadius()
                       ),

               

    BORDER_TOP_RIGHT_RADIUS(
                               "border-top-right-radius",
                               Marker.PRIMITIVE,
                               "0",
                               Marker.NOT_INHERITED,
                               new PrimitivePropertyBuilders.BorderTopRightRadius()
                       ),

           

    BORDER_BOTTOM_RIGHT_RADIUS(
                               "border-bottom-right-radius",
                               Marker.PRIMITIVE,
                               "0",
                               Marker.NOT_INHERITED,
                               new PrimitivePropertyBuilders.BorderBottomRightRadius()
                       ),

               

    BORDER_BOTTOM_LEFT_RADIUS(
                               "border-bottom-left-radius",
                               Marker.PRIMITIVE,
                               "0",
                               Marker.NOT_INHERITED,
                               new PrimitivePropertyBuilders.BorderBottomLeftRadius()
                       ),
           
    BORDER_RADIUS_SHORTHAND(
                       "border-radius",
                       Marker.SHORTHAND,
                       "0",
                       Marker.NOT_INHERITED,
                       new PrimitivePropertyBuilders.BorderRadius()
                       );
    
    private static enum Marker
    {
    	PRIMITIVE,
    	SHORTHAND,
    	INHERITS,
    	NOT_INHERITED;
    }

    /**
     * The CSS 2 property name, e.g. "border"
     */
    private final String propName;

    /**
     * A (String) initial value from the CSS 2.1 specification
     */
    private final String initialValue;

    /**
     * True if the property inherits by default, false if not inherited
     */
    private final boolean propertyInherits;

    private FSDerivedValue initialDerivedValue;

    private final boolean implemented;

    private final PropertyBuilder builder;

    private final Marker type;
    
    /**
     * Unique integer id for a CSSName.
     */
    public final int FS_ID;

    /**
     * Map of all CSS properties
     */
    private static final Map<String, CSSName> ALL_PROPERTY_NAMES = new TreeMap<>();

    /**
     * Map of all non-shorthand CSS properties
     */
    private static final Map<String, CSSName> ALL_PRIMITIVE_PROPERTY_NAMES = new TreeMap<>();


    public final static CSSSideProperties MARGIN_SIDE_PROPERTIES =
            new CSSSideProperties(
                    CSSName.MARGIN_TOP,
                    CSSName.MARGIN_RIGHT,
                    CSSName.MARGIN_BOTTOM,
                    CSSName.MARGIN_LEFT);

    public final static CSSSideProperties PADDING_SIDE_PROPERTIES =
            new CSSSideProperties(
                    CSSName.PADDING_TOP,
                    CSSName.PADDING_RIGHT,
                    CSSName.PADDING_BOTTOM,
                    CSSName.PADDING_LEFT);

    public final static CSSSideProperties BORDER_SIDE_PROPERTIES =
            new CSSSideProperties(
                    CSSName.BORDER_TOP_WIDTH,
                    CSSName.BORDER_RIGHT_WIDTH,
                    CSSName.BORDER_BOTTOM_WIDTH,
                    CSSName.BORDER_LEFT_WIDTH);

    public final static CSSSideProperties BORDER_STYLE_PROPERTIES =
            new CSSSideProperties(
                    CSSName.BORDER_TOP_STYLE,
                    CSSName.BORDER_RIGHT_STYLE,
                    CSSName.BORDER_BOTTOM_STYLE,
                    CSSName.BORDER_LEFT_STYLE);

    public final static CSSSideProperties BORDER_COLOR_PROPERTIES =
            new CSSSideProperties(
                    CSSName.BORDER_TOP_COLOR,
                    CSSName.BORDER_RIGHT_COLOR,
                    CSSName.BORDER_BOTTOM_COLOR,
                    CSSName.BORDER_LEFT_COLOR);

    private CSSName(
            final String propName, final Marker type, final String initialValue, final boolean inherits,
            final boolean implemented, final PropertyBuilder builder) {
        this.propName = propName;
        this.FS_ID = ordinal();
        this.initialValue = initialValue;
        this.propertyInherits = inherits;
        this.implemented = implemented;
        this.builder = builder;
        this.type = type;
    }

    private CSSName (
            final String propName,
            final Marker type,
            final String initialValue,
            final Marker inherit,
            final PropertyBuilder builder
    ) {
    	this(propName, type, initialValue, inherit == Marker.INHERITS, true, builder);
    }
    
    private CSSName (
            final String propName,
            final Marker type,
            final String initialValue,
            final Marker inherit,
            final boolean implemented,
            final PropertyBuilder builder
    ) {
    	this(propName, type, initialValue, inherit == Marker.INHERITS, implemented, builder);
    }
    /**
     * Returns a string representation of the object, in this case, always the
     * full CSS property name in lowercase.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return this.propName;
    }

    /**
     * Returns a count of all CSS properties known to this class, shorthand and primitive.
     *
     * @return Returns
     */
    public static int countCSSNames() {
        return values().length;
    }

    /**
     * Returns a count of all CSS primitive (non-shorthand) properties known to this class.
     *
     * @return Returns
     */
    public static int countCSSPrimitiveNames() {
        return ALL_PRIMITIVE_PROPERTY_NAMES.size();
    }

    /**
     * Iterator of ALL CSS 2 visual property names.
     *
     * @return Returns
     */
    public static Iterator<String> allCSS2PropertyNames() {
        return ALL_PROPERTY_NAMES.keySet().iterator();
    }

    /**
     * Iterator of ALL primitive (non-shorthand) CSS 2 visual property names.
     *
     * @return Returns
     */
    public static Iterator<String> allCSS2PrimitivePropertyNames() {
        return ALL_PRIMITIVE_PROPERTY_NAMES.keySet().iterator();
    }

    /**
     * Returns true if the named property inherits by default, according to the
     * CSS2 spec.
     *
     * @param cssName PARAM
     * @return Returns
     */
    // CLEAN: method is now unnecessary
    public static boolean propertyInherits(final CSSName cssName) {
        return cssName.propertyInherits;
    }

    /**
     * Returns the initial value of the named property, according to the CSS2
     * spec, as a String. Casting must be taken care of by the caller, as there
     * is too much variation in value-types.
     *
     * @param cssName PARAM
     * @return Returns
     */
    // CLEAN: method is now unnecessary
    public static String initialValue(final CSSName cssName) {
        return cssName.initialValue;
    }

    public static FSDerivedValue initialDerivedValue(final CSSName cssName) {
        return cssName.initialDerivedValue;
    }

    public static boolean isImplemented(final CSSName cssName) {
        return cssName.implemented;
    }

    public static PropertyBuilder getPropertyBuilder(final CSSName cssName) {
        return cssName.builder;
    }

    /**
     * Gets the byPropertyName attribute of the CSSName class
     *
     * @param propName PARAM
     * @return The byPropertyName value
     */
    public static CSSName getByPropertyName(final String propName) {

        return ALL_PROPERTY_NAMES.get(propName);
    }

    public static CSSName getByID(final int id) {
        return values()[id];
    }

    static 
    {
    	for (final CSSName nm : values())
    	{
    		ALL_PROPERTY_NAMES.put(nm.propName, nm);

    		if (nm.type == Marker.PRIMITIVE) {
    			ALL_PRIMITIVE_PROPERTY_NAMES.put(nm.propName, nm);
    		}
    	}
    }

    static {
        final CSSParser parser = new CSSParser(new CSSErrorHandler() {
            public void error(final String uri, final String message) {
                XRLog.cssParse("(" + uri + ") " + message);
            }
        });
        for (final CSSName cssName : ALL_PRIMITIVE_PROPERTY_NAMES.values()) {
            if (cssName.initialValue.charAt(0) != '=' && cssName.implemented) {
                final PropertyValue value = parser.parsePropertyValue(
                        cssName, StylesheetInfo.CSSOrigin.USER_AGENT, cssName.initialValue);

                if (value == null) {
                    XRLog.exception("Unable to derive initial value for " + cssName);
                } else {
                    cssName.initialDerivedValue = DerivedValueFactory.newDerivedValue(
                            null,
                            cssName,
                            value);
                }
            }
        }
    }

    public static class CSSSideProperties 
    {
        public final CSSName top;
        public final CSSName right;
        public final CSSName bottom;
        public final CSSName left;

        public CSSSideProperties(final CSSName top, final CSSName right, final CSSName bottom, final CSSName left) {
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.left = left;
        }
    }
}

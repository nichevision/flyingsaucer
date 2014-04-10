/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Torbjoern Gannholm, Joshua Marinacci
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
 * }}}
 */
package org.xhtmlrenderer.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.constants.MarginBoxName;
import org.xhtmlrenderer.css.constants.PageElementPosition;
import org.xhtmlrenderer.css.extend.ContentFunction;
import org.xhtmlrenderer.css.newmatch.CascadedStyle;
import org.xhtmlrenderer.css.newmatch.PageInfo;
import org.xhtmlrenderer.css.parser.FSFunction;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.parser.PropertyValueImp;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.EmptyStyle;
import org.xhtmlrenderer.css.style.FSDerivedValue;
import org.xhtmlrenderer.newtable.TableBox;
import org.xhtmlrenderer.newtable.TableCellBox;
import org.xhtmlrenderer.newtable.TableColumn;
import org.xhtmlrenderer.newtable.TableRowBox;
import org.xhtmlrenderer.newtable.TableSectionBox;
import org.xhtmlrenderer.render.AnonymousBlockBox;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.FloatedBoxData;
import org.xhtmlrenderer.render.InlineBox;

/**
 * This class is responsible for creating the box tree from the DOM.  This is
 * mostly just a one-to-one translation from the <code>Element</code> to an
 * <code>InlineBox</code> or a <code>BlockBox</code> (or some subclass of
 * <code>BlockBox</code>), but the tree is reorganized according to the CSS rules.
 * This includes inserting anonymous block and inline boxes, anonymous table
 * content, and <code>:before</code> and <code>:after</code> content.  White
 * space is also normalized at this point.  Table columns and table column groups
 * are added to the table which owns them, but are not created as regular boxes.
 * Floated and absolutely positioned content is always treated as inline
 * content for purposes of inserting anonymous block boxes and calculating
 * the kind of content contained in a given block box.
 */
public class BoxBuilder {
    public static final int MARGIN_BOX_VERTICAL = 1;
    public static final int MARGIN_BOX_HORIZONTAL = 2;

    private static final int CONTENT_LIST_DOCUMENT = 1;
    private static final int CONTENT_LIST_MARGIN_BOX = 2;

    public static BlockBox createRootBox(final LayoutContext c, final Document document) {
        final Element root = document;

        final CalculatedStyle style = c.getSharedContext().getStyle(root);

        BlockBox result;
        if (style.isTable() || style.isInlineTable()) {
            result = new TableBox();
        } else {
            result = new BlockBox();
        }

        result.setStyle(style);
        result.setElement(root);

        c.resolveCounters(style);

        c.pushLayer(result);
        if (c.isPrint()) {
            if (! style.isIdent(CSSName.PAGE, IdentValue.AUTO)) {
                c.setPageName(style.getStringProperty(CSSName.PAGE));
            }
            c.getRootLayer().addPage(c);
        }

        return result;
    }

    public static void createChildren(final LayoutContext c, final BlockBox parent) {
        final List<Styleable> children = new ArrayList<Styleable>();

        final ChildBoxInfo info = new ChildBoxInfo();

        createChildren(c, parent, parent.getElement(), children, info, false);

        final boolean parentIsNestingTableContent = isNestingTableContent(parent.getStyle().getIdent(
                CSSName.DISPLAY));
        if (!parentIsNestingTableContent && !info.isContainsTableContent()) {
            resolveChildren(c, parent, children, info);
        } else {
            stripAllWhitespace(children);
            if (parentIsNestingTableContent) {
                resolveTableContent(c, parent, children, info);
            } else {
                resolveChildTableContent(c, parent, children, info, IdentValue.TABLE_CELL);
            }
        }
    }

    public static TableBox createMarginTable(
            final LayoutContext c,
            final PageInfo pageInfo,
            final MarginBoxName[] names,
            final int height,
            final int direction)
    {
        if (! pageInfo.hasAny(names)) {
            return null;
        }

        final Element source = c.getRootLayer().getMaster().getElement(); // HACK

        final ChildBoxInfo info = new ChildBoxInfo();
        final CalculatedStyle pageStyle = new EmptyStyle().deriveStyle(pageInfo.getPageStyle());

        final CalculatedStyle tableStyle = pageStyle.deriveStyle(
                CascadedStyle.createLayoutStyle(new PropertyDeclaration[] {
                        new PropertyDeclaration(
                                CSSName.DISPLAY,
                                new PropertyValueImp(IdentValue.TABLE),
                                true,
                                StylesheetInfo.CSSOrigin.USER),
                        new PropertyDeclaration(
                                CSSName.WIDTH,
                                new PropertyValueImp(CSSPrimitiveUnit.CSS_PERCENTAGE, 100.0f, "100%"),
                                true,
                                StylesheetInfo.CSSOrigin.USER),
                }));
        final TableBox result = (TableBox)createBlockBox(tableStyle, info, false);
        result.setMarginAreaRoot(true);
        result.setStyle(tableStyle);
        result.setElement(source);
        result.setAnonymous(true);
        result.setChildrenContentType(BlockBox.CONTENT_BLOCK);

        final CalculatedStyle tableSectionStyle = pageStyle.createAnonymousStyle(IdentValue.TABLE_ROW_GROUP);
        final TableSectionBox section = (TableSectionBox)createBlockBox(tableSectionStyle, info, false);
        section.setStyle(tableSectionStyle);
        section.setElement(source);
        section.setAnonymous(true);
        section.setChildrenContentType(BlockBox.CONTENT_BLOCK);

        result.addChild(section);

        TableRowBox row = null;
        if (direction == MARGIN_BOX_HORIZONTAL) {
            final CalculatedStyle tableRowStyle = pageStyle.createAnonymousStyle(IdentValue.TABLE_ROW);
            row = (TableRowBox)createBlockBox(tableRowStyle, info, false);
            row.setStyle(tableRowStyle);
            row.setElement(source);
            row.setAnonymous(true);
            row.setChildrenContentType(BlockBox.CONTENT_BLOCK);

            row.setHeightOverride(height);

            section.addChild(row);
        }

        int cellCount = 0;
        final boolean alwaysCreate = names.length > 1 && direction == MARGIN_BOX_HORIZONTAL;

        for (final MarginBoxName name : names) {
            final CascadedStyle cellStyle = pageInfo.createMarginBoxStyle(name, alwaysCreate);
            if (cellStyle != null) {
                final TableCellBox cell = createMarginBox(c, cellStyle, alwaysCreate);
                if (cell != null) {
                    if (direction == MARGIN_BOX_VERTICAL) {
                        final CalculatedStyle tableRowStyle = pageStyle.createAnonymousStyle(IdentValue.TABLE_ROW);
                        row = (TableRowBox)createBlockBox(tableRowStyle, info, false);
                        row.setStyle(tableRowStyle);
                        row.setElement(source);
                        row.setAnonymous(true);
                        row.setChildrenContentType(BlockBox.CONTENT_BLOCK);

                        row.setHeightOverride(height);

                        section.addChild(row);
                    }
                    row.addChild(cell);
                    cellCount++;
                }
            }
        }

        if (direction == MARGIN_BOX_VERTICAL && cellCount > 0) {
            int rHeight = 0;
            for (final Iterator<Box> i = section.getChildIterator(); i.hasNext(); ) {
                final TableRowBox r = (TableRowBox)i.next();
                r.setHeightOverride(height / cellCount);
                rHeight += r.getHeightOverride();
            }

            for (final Iterator<Box> i = section.getChildIterator(); i.hasNext() && rHeight < height; ) {
                final TableRowBox r = (TableRowBox)i.next();
                r.setHeightOverride(r.getHeightOverride()+1);
                rHeight++;
            }
        }

        return cellCount > 0 ? result : null;
    }

    private static TableCellBox createMarginBox(
            final LayoutContext c,
            final CascadedStyle cascadedStyle,
            final boolean alwaysCreate) {
        boolean hasContent = true;

        final PropertyDeclaration contentDecl = cascadedStyle.propertyByName(CSSName.CONTENT);

        final CalculatedStyle style = new EmptyStyle().deriveStyle(cascadedStyle);

        if (style.isDisplayNone() && ! alwaysCreate) {
            return null;
        }

        if (style.isIdent(CSSName.CONTENT, IdentValue.NONE) ||
                style.isIdent(CSSName.CONTENT, IdentValue.NORMAL)) {
            hasContent = false;
        }

        if (style.isAutoWidth() && ! alwaysCreate && ! hasContent) {
            return null;
        }

        final List<Styleable> children = new ArrayList<Styleable>();

        final ChildBoxInfo info = new ChildBoxInfo();
        info.setContainsTableContent(true);
        info.setLayoutRunningBlocks(true);

        final TableCellBox result = new TableCellBox();
        result.setAnonymous(true);
        result.setStyle(style);
        result.setElement(c.getRootLayer().getMaster().getElement()); // XXX Doesn't make sense, but we need something here

        if (hasContent && ! style.isDisplayNone()) {
            children.addAll(createGeneratedMarginBoxContent(
                    c,
                    c.getRootLayer().getMaster().getElement(),
                    (PropertyValue)contentDecl.getValue(),
                    style,
                    info));

            stripAllWhitespace(children);
        }

        if (children.size() == 0 && style.isAutoWidth() && ! alwaysCreate) {
            return null;
        }

        resolveChildTableContent(c, result, children, info, IdentValue.TABLE_CELL);

        return result;
    }

    private static void resolveChildren(
            final LayoutContext c, final BlockBox owner, final List<Styleable> children, final ChildBoxInfo info) {
        if (children.size() > 0) {
            if (info.isContainsBlockLevelContent()) {
                insertAnonymousBlocks(
                        c.getSharedContext(), owner, children, info.isLayoutRunningBlocks());
                owner.setChildrenContentType(BlockBox.CONTENT_BLOCK);
            } else {
                WhitespaceStripper.stripInlineContent(children);
                if (children.size() > 0) {
                    owner.setInlineContent(children);
                    owner.setChildrenContentType(BlockBox.CONTENT_INLINE);
                } else {
                    owner.setChildrenContentType(BlockBox.CONTENT_EMPTY);
                }
            }
        } else {
            owner.setChildrenContentType(BlockBox.CONTENT_EMPTY);
        }
    }

    private static boolean isAllProperTableNesting(final IdentValue parentDisplay, final List<Styleable> children) {
        for (final Styleable child : children) {
            if (!isProperTableNesting(parentDisplay, child.getStyle().getIdent(CSSName.DISPLAY))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Handles the situation when we find table content, but our parent is not
     * table related.  For example, <code>div</code> -> <code>td</td></code>.
     * Anonymous tables are then constructed by repeatedly pulling together
     * consecutive same-table-level siblings and wrapping them in the next
     * highest table level (e.g. consecutive <code>td</code> elements will
     * be wrapped in an anonymous <code>tr</code>, then a <code>tbody</code>, and
     * finally a <code>table</code>).
     */
    private static void resolveChildTableContent(
            final LayoutContext c, final BlockBox parent, final List<Styleable> children, final ChildBoxInfo info, final IdentValue target) {
        List<Styleable> childrenForAnonymous = new ArrayList<Styleable>();
        final List<Styleable> childrenWithAnonymous = new ArrayList<Styleable>();

        final IdentValue nextUp = getPreviousTableNestingLevel(target);
        for (final Styleable styleable : children) {
            if (matchesTableLevel(target, styleable.getStyle().getIdent(CSSName.DISPLAY))) {
                childrenForAnonymous.add(styleable);
            } else {
                if (childrenForAnonymous.size() > 0) {
                    createAnonymousTableContent(c, (BlockBox) childrenForAnonymous.get(0), nextUp,
                            childrenForAnonymous, childrenWithAnonymous);

                    childrenForAnonymous = new ArrayList<Styleable>();
                }
                childrenWithAnonymous.add(styleable);
            }
        }

        if (childrenForAnonymous.size() > 0) {
            createAnonymousTableContent(c, (BlockBox) childrenForAnonymous.get(0), nextUp,
                    childrenForAnonymous, childrenWithAnonymous);
        }

        if (nextUp == IdentValue.TABLE) {
            rebalanceInlineContent(childrenWithAnonymous);
            info.setContainsBlockLevelContent(true);
            resolveChildren(c, parent, childrenWithAnonymous, info);
        } else {
            resolveChildTableContent(c, parent, childrenWithAnonymous, info, nextUp);
        }
    }

    private static boolean matchesTableLevel(final IdentValue target, final IdentValue value) {
        if (target == IdentValue.TABLE_ROW_GROUP) {
            return value == IdentValue.TABLE_ROW_GROUP || value == IdentValue.TABLE_HEADER_GROUP
                    || value == IdentValue.TABLE_FOOTER_GROUP || value == IdentValue.TABLE_CAPTION;
        } else {
            return target == value;
        }
    }

    /**
     * Makes sure that any <code>InlineBox</code> in <code>content</code>
     * both starts and ends within <code>content</code>. Used to ensure that
     * it is always possible to construct anonymous blocks once an element's
     * children has been distributed among anonymous table objects.
     */
    private static void rebalanceInlineContent(final List<Styleable> content) {
        final Map<Element, InlineBox> boxesByElement = new HashMap<Element, InlineBox>();
        for (final Styleable styleable : content) {
            if (styleable instanceof InlineBox) {
                final InlineBox iB = (InlineBox) styleable;
                final Element elem = iB.getElement();

                if (!boxesByElement.containsKey(elem)) {
                    iB.setStartsHere(true);
                }

                boxesByElement.put(elem, iB);
            }
        }

        for (final InlineBox iB : boxesByElement.values()) {
            iB.setEndsHere(true);
        }
    }

    private static void stripAllWhitespace(final List<Styleable> content) {
        int start = 0;
        int current = 0;
        boolean started = false;
        for (current = 0; current < content.size(); current++) {
            final Styleable styleable = content.get(current);
            if (! styleable.getStyle().isLayedOutInInlineContext()) {
                if (started) {
                    final int before = content.size();
                    WhitespaceStripper.stripInlineContent(content.subList(start, current));
                    final int after = content.size();
                    current -= (before - after);
                }
                started = false;
            } else {
                if (! started) {
                    started = true;
                    start = current;
                }
            }
        }

        if (started) {
            WhitespaceStripper.stripInlineContent(content.subList(start, current));
        }
    }

    /**
     * Handles the situation when our current parent is table related.  If
     * everything is properly nested (e.g. a <code>tr</code> contains only
     * <code>td</code> elements), nothing is done.  Otherwise anonymous boxes
     * are inserted to ensure the integrity of the table model.
     */
    private static void resolveTableContent(
            final LayoutContext c, final BlockBox parent, final List<Styleable> children, final ChildBoxInfo info) {
        final IdentValue parentDisplay = parent.getStyle().getIdent(CSSName.DISPLAY);
        final IdentValue next = getNextTableNestingLevel(parentDisplay);
        if (next == null && parent.isAnonymous() && containsOrphanedTableContent(children)) {
            resolveChildTableContent(c, parent, children, info, IdentValue.TABLE_CELL);
        } else if (next == null || isAllProperTableNesting(parentDisplay, children)) {
            if (parent.isAnonymous()) {
                rebalanceInlineContent(children);
            }
            resolveChildren(c, parent, children, info);
        } else {
            List<Styleable> childrenForAnonymous = new ArrayList<Styleable>();
            final List<Styleable> childrenWithAnonymous = new ArrayList<Styleable>();
            for (final Styleable child : children) {
                final IdentValue childDisplay = child.getStyle().getIdent(CSSName.DISPLAY);

                if (isProperTableNesting(parentDisplay, childDisplay)) {
                    if (childrenForAnonymous.size() > 0) {
                        createAnonymousTableContent(c, parent, next, childrenForAnonymous,
                                childrenWithAnonymous);

                        childrenForAnonymous = new ArrayList<Styleable>();
                    }
                    childrenWithAnonymous.add(child);
                } else {
                    childrenForAnonymous.add(child);
                }
            }

            if (childrenForAnonymous.size() > 0) {
                createAnonymousTableContent(c, parent, next, childrenForAnonymous,
                        childrenWithAnonymous);
            }

            info.setContainsBlockLevelContent(true);
            resolveChildren(c, parent, childrenWithAnonymous, info);
        }
    }

    private static boolean containsOrphanedTableContent(final List<Styleable> children) {
        for (final Styleable child : children) {
            final IdentValue display = child.getStyle().getIdent(CSSName.DISPLAY);
            if (display == IdentValue.TABLE_HEADER_GROUP ||
                    display == IdentValue.TABLE_ROW_GROUP ||
                    display == IdentValue.TABLE_FOOTER_GROUP ||
                    display == IdentValue.TABLE_ROW) {
                return true;
            }
        }

        return false;
    }

    private static boolean isParentInline(final BlockBox box) {
        final CalculatedStyle parentStyle = box.getStyle().getParent();
        return parentStyle != null && parentStyle.isInline();
    }

    private static void createAnonymousTableContent(final LayoutContext c, final BlockBox source,
                                                    final IdentValue next, final List<Styleable> childrenForAnonymous, final List<Styleable> childrenWithAnonymous) {
        final ChildBoxInfo nested = lookForBlockContent(childrenForAnonymous);
        IdentValue anonDisplay;
        if (isParentInline(source) && next == IdentValue.TABLE) {
            anonDisplay = IdentValue.INLINE_TABLE;
        } else {
            anonDisplay = next;
        }
        final CalculatedStyle anonStyle = source.getStyle().createAnonymousStyle(anonDisplay);
        final BlockBox anonBox = createBlockBox(anonStyle, nested, false);
        anonBox.setStyle(anonStyle);
        anonBox.setAnonymous(true);
        // XXX Doesn't really make sense, but what to do?
        anonBox.setElement(source.getElement());
        resolveTableContent(c, anonBox, childrenForAnonymous, nested);

        if (next == IdentValue.TABLE) {
            childrenWithAnonymous.add(reorderTableContent(c, (TableBox) anonBox));
        } else {
            childrenWithAnonymous.add(anonBox);
        }
    }

    /**
     * Reorganizes a table so that the header is the first row group and the
     * footer the last.  If the table has caption boxes, they will be pulled
     * out and added to an anonymous block box along with the table itself.
     * If not, the table is returned.
     */
    private static BlockBox reorderTableContent(final LayoutContext c, final TableBox table) {
        final List<Box> topCaptions = new LinkedList<Box>();
        Box header = null;
        final List<Box> bodies = new LinkedList<Box>();
        Box footer = null;
        final List<Box> bottomCaptions = new LinkedList<Box>();

        for (final Iterator<Box> i = table.getChildIterator(); i.hasNext();) {
            final Box b = (Box) i.next();
            final IdentValue display = b.getStyle().getIdent(CSSName.DISPLAY);
            if (display == IdentValue.TABLE_CAPTION) {
                final IdentValue side = b.getStyle().getIdent(CSSName.CAPTION_SIDE);
                if (side == IdentValue.BOTTOM) {
                    bottomCaptions.add(b);
                } else { /* side == IdentValue.TOP */
                    topCaptions.add(b);
                }
            } else if (display == IdentValue.TABLE_HEADER_GROUP && header == null) {
                header = b;
            } else if (display == IdentValue.TABLE_FOOTER_GROUP && footer == null) {
                footer = b;
            } else {
                bodies.add(b);
            }
        }

        table.removeAllChildren();
        if (header != null) {
            ((TableSectionBox)header).setHeader(true);
            table.addChild(header);
        }
        table.addAllChildren(bodies);
        if (footer != null) {
            ((TableSectionBox)footer).setFooter(true);
            table.addChild(footer);
        }

        if (topCaptions.size() == 0 && bottomCaptions.size() == 0) {
            return table;
        } else {
            // If we have a floated table with a caption, we need to float the
            // outer anonymous box and not the table
            CalculatedStyle anonStyle;
            if (table.getStyle().isFloated()) {
                final CascadedStyle cascadedStyle = CascadedStyle.createLayoutStyle(
                        new PropertyDeclaration[]{
                                CascadedStyle.createLayoutPropertyDeclaration(
                                        CSSName.DISPLAY, IdentValue.BLOCK),
                                CascadedStyle.createLayoutPropertyDeclaration(
                                        CSSName.FLOAT, table.getStyle().getIdent(CSSName.FLOAT))});

                anonStyle = table.getStyle().deriveStyle(cascadedStyle);
            } else {
                anonStyle = table.getStyle().createAnonymousStyle(IdentValue.BLOCK);
            }

            final BlockBox anonBox = new BlockBox();
            anonBox.setStyle(anonStyle);
            anonBox.setAnonymous(true);
            anonBox.setFromCaptionedTable(true);
            anonBox.setElement(table.getElement());

            anonBox.setChildrenContentType(BlockBox.CONTENT_BLOCK);
            anonBox.addAllChildren(topCaptions);
            anonBox.addChild(table);
            anonBox.addAllChildren(bottomCaptions);

            if (table.getStyle().isFloated()) {
                anonBox.setFloatedBoxData(new FloatedBoxData());
                table.setFloatedBoxData(null);

                final CascadedStyle original = c.getSharedContext().getCss().getCascadedStyle(
                        table.getElement(), false);
                final CascadedStyle modified = CascadedStyle.createLayoutStyle(
                        original,
                        new PropertyDeclaration[]{
                                CascadedStyle.createLayoutPropertyDeclaration(
                                        CSSName.FLOAT, IdentValue.NONE)
                        });
                table.setStyle(table.getStyle().getParent().deriveStyle(modified));
            }

            return anonBox;
        }
    }

    private static ChildBoxInfo lookForBlockContent(final List<Styleable> styleables) {
        final ChildBoxInfo result = new ChildBoxInfo();
        for (final Styleable s : styleables) {
            if (!s.getStyle().isLayedOutInInlineContext()) {
                result.setContainsBlockLevelContent(true);
                break;
            }
        }
        return result;
    }

    private static IdentValue getNextTableNestingLevel(final IdentValue display) {
        if (display == IdentValue.TABLE || display == IdentValue.INLINE_TABLE) {
            return IdentValue.TABLE_ROW_GROUP;
        } else if (display == IdentValue.TABLE_HEADER_GROUP
                || display == IdentValue.TABLE_ROW_GROUP
                || display == IdentValue.TABLE_FOOTER_GROUP) {
            return IdentValue.TABLE_ROW;
        } else if (display == IdentValue.TABLE_ROW) {
            return IdentValue.TABLE_CELL;
        } else {
            return null;
        }
    }

    private static IdentValue getPreviousTableNestingLevel(final IdentValue display) {
        if (display == IdentValue.TABLE_CELL) {
            return IdentValue.TABLE_ROW;
        } else if (display == IdentValue.TABLE_ROW) {
            return IdentValue.TABLE_ROW_GROUP;
        } else if (display == IdentValue.TABLE_HEADER_GROUP
                || display == IdentValue.TABLE_ROW_GROUP
                || display == IdentValue.TABLE_FOOTER_GROUP) {
            return IdentValue.TABLE;
        } else {
            return null;
        }
    }

    private static boolean isProperTableNesting(final IdentValue parent, final IdentValue child) {
        return (parent == IdentValue.TABLE && (child == IdentValue.TABLE_HEADER_GROUP ||
                child == IdentValue.TABLE_ROW_GROUP ||
                child == IdentValue.TABLE_FOOTER_GROUP ||
                child == IdentValue.TABLE_CAPTION))
                || ((parent == IdentValue.TABLE_HEADER_GROUP ||
                parent == IdentValue.TABLE_ROW_GROUP ||
                parent == IdentValue.TABLE_FOOTER_GROUP) &&
                child == IdentValue.TABLE_ROW)
                || (parent == IdentValue.TABLE_ROW && child == IdentValue.TABLE_CELL)
                || (parent == IdentValue.INLINE_TABLE && (child == IdentValue.TABLE_HEADER_GROUP ||
                child == IdentValue.TABLE_ROW_GROUP ||
                child == IdentValue.TABLE_FOOTER_GROUP));

    }

    private static boolean isNestingTableContent(final IdentValue display) {
        return display == IdentValue.TABLE || display == IdentValue.INLINE_TABLE ||
                display == IdentValue.TABLE_HEADER_GROUP || display == IdentValue.TABLE_ROW_GROUP ||
                display == IdentValue.TABLE_FOOTER_GROUP || display == IdentValue.TABLE_ROW;
    }

    private static boolean isAttrFunction(final FSFunction function) {
        if (function.getName().equals("attr")) {
            final List<PropertyValue> params = function.getParameters();
            if (params.size() == 1) {
                final PropertyValue value = (PropertyValue) params.get(0);
                return value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT;
            }
        }

        return false;
    }

    public static boolean isElementFunction(final FSFunction function) {
        if (function.getName().equals("element")) {
            final List<PropertyValue> params = function.getParameters();
            if (params.size() < 1 || params.size() > 2) {
                return false;
            }
            boolean ok = true;
            final PropertyValue value1 = (PropertyValue) params.get(0);
            ok = value1.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT;
            if (ok && params.size() == 2) {
                final PropertyValue value2 = (PropertyValue) params.get(1);
                ok = value2.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT;
            }

            return ok;
        }

        return false;
    }

    private static CounterFunction makeCounterFunction(final FSFunction function, final LayoutContext c, final CalculatedStyle style) {
        if (function.getName().equals("counter")) {
            final List<PropertyValue> params = function.getParameters();
            if (params.size() < 1 || params.size() > 2) {
                return null;
            }

            PropertyValue value = (PropertyValue) params.get(0);
            if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_IDENT) {
                return null;
            }

            final String s = value.getStringValue();
            // counter(page) and counter(pages) are handled separately
            if (s.equals("page") || s.equals("pages")) {
                return null;
            }

            final String counter = value.getStringValue();
            IdentValue listStyleType = IdentValue.DECIMAL;
            if (params.size() == 2) {
                value = (PropertyValue) params.get(1);
                if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_IDENT) {
                    return null;
                }

                final IdentValue identValue = IdentValue.fsValueOf(value.getStringValue());
                if (identValue != null) {
                    value.setIdentValue(identValue);
                    listStyleType = identValue;
                }
            }

            final int counterValue = c.getCounterContext(style).getCurrentCounterValue(counter);

            return new CounterFunction(counterValue, listStyleType);
        } else if (function.getName().equals("counters")) {
            final List<PropertyValue> params = function.getParameters();
            if (params.size() < 2 || params.size() > 3) {
                return null;
            }

            PropertyValue value = (PropertyValue) params.get(0);
            if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_IDENT) {
                return null;
            }

            final String counter = value.getStringValue();

            value = (PropertyValue) params.get(1);
            if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_STRING) {
                return null;
            }

            final String separator = value.getStringValue();

            IdentValue listStyleType = IdentValue.DECIMAL;
            if (params.size() == 3) {
                value = (PropertyValue) params.get(2);
                if (value.getPrimitiveTypeN() != CSSPrimitiveUnit.CSS_IDENT) {
                    return null;
                }

                final IdentValue identValue = IdentValue.fsValueOf(value.getStringValue());
                if (identValue != null) {
                    value.setIdentValue(identValue);
                    listStyleType = identValue;
                }
            }

            final List<Integer> counterValues = c.getCounterContext(style).getCurrentCounterValues(counter);

            return new CounterFunction(counterValues, separator, listStyleType);
        } else {
            return null;
        }
    }

    private static String getAttributeValue(final FSFunction attrFunc, final Element e) {
        final PropertyValue value = (PropertyValue) attrFunc.getParameters().get(0);
        return e.attr(value.getStringValue());
    }

    private static List<Styleable> createGeneratedContentList(
            final LayoutContext c, final Element element, final PropertyValue propValue,
            final String peName, final CalculatedStyle style, final int mode, final ChildBoxInfo info) {
        final List<?> values = propValue.getValues();

        if (values == null) {
            // content: normal or content: none
            return Collections.emptyList();
        }

        final List<Styleable> result = new ArrayList<Styleable>(values.size());

        for (final Object valueObj : values) {
            final PropertyValue value = (PropertyValue) valueObj;

            ContentFunction contentFunction = null;
            FSFunction function = null;

            String content = null;

            final CSSPrimitiveUnit type = value.getPrimitiveTypeN();
            if (type == CSSPrimitiveUnit.CSS_STRING) {
                content = value.getStringValue();
            } else if (value.getPropertyValueType() == PropertyValueImp.VALUE_TYPE_FUNCTION) {
                if (mode == CONTENT_LIST_DOCUMENT && isAttrFunction(value.getFunction())) {
                    content = getAttributeValue(value.getFunction(), element);
                } else {
                    CounterFunction cFunc = null;

                    if (mode == CONTENT_LIST_DOCUMENT) {
                        cFunc = makeCounterFunction(value.getFunction(), c, style);
                    }

                    if (cFunc != null) {
                        //TODO: counter functions may be called with non-ordered list-style-types, e.g. disc
                        content = cFunc.evaluate();
                        contentFunction = null;
                        function = null;
                    } else if (mode == CONTENT_LIST_MARGIN_BOX && isElementFunction(value.getFunction())) {
                        final BlockBox target = getRunningBlock(c, value);
                        if (target != null) {
                            result.add(target.copyOf());
                            info.setContainsBlockLevelContent(true);
                        }
                    } else {
                        contentFunction =
                                c.getContentFunctionFactory().lookupFunction(c, value.getFunction());
                        if (contentFunction != null) {
                            function = value.getFunction();

                            if (contentFunction.isStatic()) {
                                content = contentFunction.calculate(c, function);
                                contentFunction = null;
                                function = null;
                            } else {
                                content = contentFunction.getLayoutReplacementText();
                            }
                        }
                    }
                }
            } else if (type == CSSPrimitiveUnit.CSS_IDENT) {
                final FSDerivedValue dv = style.valueByName(CSSName.QUOTES);
                
                if (dv != IdentValue.NONE) {
                    final IdentValue ident = value.getIdentValue();
                    
                    if (ident == IdentValue.OPEN_QUOTE) {
                        final String[] quotes = style.asStringArray(CSSName.QUOTES);
                        content = quotes[0];
                    } else if (ident == IdentValue.CLOSE_QUOTE) {
                        final String[] quotes = style.asStringArray(CSSName.QUOTES);
                        content = quotes[1];
                    }
                }
            }

            if (content != null) {
                final InlineBox iB = new InlineBox(content, null);
                iB.setContentFunction(contentFunction);
                iB.setFunction(function);
                iB.setElement(element);
                iB.setPseudoElementOrClass(peName);
                iB.setStartsHere(true);
                iB.setEndsHere(true);

                result.add(iB);
            }
        }

        return result;
    }

    public static BlockBox getRunningBlock(final LayoutContext c, final PropertyValue value) {
        final List<PropertyValue> params = value.getFunction().getParameters();
        final String ident = ((PropertyValue)params.get(0)).getStringValue();
        PageElementPosition position = null;
        if (params.size() == 2) {
            position = PageElementPosition.valueOf(
                    ((PropertyValue)params.get(1)).getStringValue());
        }
        if (position == null) {
            position = PageElementPosition.FIRST;
        }
        final BlockBox target = c.getRootDocumentLayer().getRunningBlock(ident, c.getPage(), position);
        return target;
    }

    private static void insertGeneratedContent(
            final LayoutContext c, final Element element, final CalculatedStyle parentStyle,
            final String peName, final List<Styleable> children, final ChildBoxInfo info) {
        final CascadedStyle peStyle = c.getCss().getPseudoElementStyle(element, peName);
        if (peStyle != null) {
            final PropertyDeclaration contentDecl = peStyle.propertyByName(CSSName.CONTENT);
            final PropertyDeclaration counterResetDecl = peStyle.propertyByName(CSSName.COUNTER_RESET);
            final PropertyDeclaration counterIncrDecl = peStyle.propertyByName(CSSName.COUNTER_INCREMENT);

            CalculatedStyle calculatedStyle = null;
            if (contentDecl != null || counterResetDecl != null || counterIncrDecl != null) {
                calculatedStyle = parentStyle.deriveStyle(peStyle);
                if (calculatedStyle.isDisplayNone()) return;
                if (calculatedStyle.isIdent(CSSName.CONTENT, IdentValue.NONE)) return;
                if (calculatedStyle.isIdent(CSSName.CONTENT, IdentValue.NORMAL) && (peName.equals("before") || peName.equals("after")))
                    return;

                if (calculatedStyle.isTable() || calculatedStyle.isTableRow() || calculatedStyle.isTableSection()) {
                    final CascadedStyle newPeStyle =
                        CascadedStyle.createLayoutStyle(peStyle, new PropertyDeclaration[] {
                            CascadedStyle.createLayoutPropertyDeclaration(
                                CSSName.DISPLAY,
                                IdentValue.BLOCK),
                        });
                    calculatedStyle = parentStyle.deriveStyle(newPeStyle);
                }
                c.resolveCounters(calculatedStyle);
            }

            if (contentDecl != null) {
                final PropertyValue propValue = contentDecl.getValue();
                children.addAll(createGeneratedContent(c, element, peName, calculatedStyle,
                        propValue, info));
            }
        }
    }

    private static List<Styleable> createGeneratedContent(
            final LayoutContext c, final Element element, final String peName,
            final CalculatedStyle style, final PropertyValue property, final ChildBoxInfo info) {
        if (style.isDisplayNone() || style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN)
                || style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN_GROUP)) {
            return Collections.emptyList();
        }

        final List<Styleable> inlineBoxes = createGeneratedContentList(
                c, element, property, peName, style, CONTENT_LIST_DOCUMENT, null);

        if (style.isInline()) {
            for (final Styleable styleable : inlineBoxes) {
                final InlineBox iB = (InlineBox) styleable;
                iB.setStyle(style);
                iB.applyTextTransform();
            }
            return inlineBoxes;
        } else {
            final CalculatedStyle anon = style.createAnonymousStyle(IdentValue.INLINE);
            for (final Styleable styleable : inlineBoxes) {
                final InlineBox iB = (InlineBox) styleable;
                iB.setStyle(anon);
                iB.applyTextTransform();
                iB.setElement(null);
            }

            final BlockBox result = createBlockBox(style, info, true);
            result.setStyle(style);
            result.setInlineContent(inlineBoxes);
            result.setElement(element);
            result.setChildrenContentType(BlockBox.CONTENT_INLINE);
            result.setPseudoElementOrClass(peName);

            if (! style.isLayedOutInInlineContext()) {
                info.setContainsBlockLevelContent(true);
            }

            return new ArrayList<Styleable>(Collections.singletonList(result));
        }
    }

    private static List<Styleable> createGeneratedMarginBoxContent(
            final LayoutContext c, final Element element, final PropertyValue property,
            final CalculatedStyle style, final ChildBoxInfo info) {
        final List<Styleable> result = createGeneratedContentList(
                c, element, property, null, style, CONTENT_LIST_MARGIN_BOX, info);

        final CalculatedStyle anon = style.createAnonymousStyle(IdentValue.INLINE);
        for (final Styleable s : result) {
            if (s instanceof InlineBox) {
                final InlineBox iB = (InlineBox)s;
                iB.setElement(null);
                iB.setStyle(anon);
                iB.applyTextTransform();
            }
        }

        return result;
    }

    private static BlockBox createBlockBox(
            final CalculatedStyle style, final ChildBoxInfo info, final boolean generated) {
        if (style.isFloated() && !(style.isAbsolute() || style.isFixed())) {
            BlockBox result;
            if (style.isTable() || style.isInlineTable()) {
                result = new TableBox();
            } else {
                result = new BlockBox();
            }
            result.setFloatedBoxData(new FloatedBoxData());
            return result;
        } else if (style.isSpecifiedAsBlock()) {
            return new BlockBox();
        } else if (! generated && (style.isTable() || style.isInlineTable())) {
            return new TableBox();
        } else if (style.isTableCell()) {
            info.setContainsTableContent(true);
            return new TableCellBox();
        } else if (! generated && style.isTableRow()) {
            info.setContainsTableContent(true);
            return new TableRowBox();
        } else if (! generated && style.isTableSection()) {
            info.setContainsTableContent(true);
            return new TableSectionBox();
        } else if (style.isTableCaption()) {
            info.setContainsTableContent(true);
            return new BlockBox();
        } else {
            return new BlockBox();
        }
    }

    private static void addColumns(final LayoutContext c, final TableBox table, final TableColumn parent) {
        final SharedContext sharedContext = c.getSharedContext();

        Node working = parent.getElement().childNodes().isEmpty() ? null : parent.getElement().childNode(0);
        boolean found = false;
        while (working != null) {
            if (working instanceof Element) {
                final Element element = (Element) working;
                final CalculatedStyle style = sharedContext.getStyle(element);

                if (style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN)) {
                    found = true;
                    final TableColumn col = new TableColumn(element, style);
                    col.setParent(parent);
                    table.addStyleColumn(col);
                }
            }
            working = working.nextSibling();
        }
        if (! found) {
            table.addStyleColumn(parent);
        }
    }

    private static void addColumnOrColumnGroup(
            final LayoutContext c, final TableBox table, final Element e, final CalculatedStyle style) {
        if (style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN)) {
            table.addStyleColumn(new TableColumn(e, style));
        } else { /* style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN_GROUP) */
            addColumns(c, table, new TableColumn(e, style));
        }
    }

    private static InlineBox createInlineBox(
            final String text, final Element parent, final CalculatedStyle parentStyle, final Node node) {
        final InlineBox result = new InlineBox(text, node);

        if (parentStyle.isInline() && ! (parent.parentNode() instanceof Document)) {
            result.setStyle(parentStyle);
            result.setElement(parent);
        } else {
            result.setStyle(parentStyle.createAnonymousStyle(IdentValue.INLINE));
        }

        result.applyTextTransform();

        return result;
    }

    private static void createChildren(
            final LayoutContext c, final BlockBox blockParent, final Element parent,
            final List<Styleable> children, final ChildBoxInfo info, final boolean inline) {
        final SharedContext sharedContext = c.getSharedContext();

        final CalculatedStyle parentStyle = sharedContext.getStyle(parent);

        insertGeneratedContent(c, parent, parentStyle, "before", children, info);

        Node working = parent.childNodes().isEmpty() ? null : parent.childNode(0);
        boolean needStartText = inline;
        boolean needEndText = inline;
        if (working != null) {
            InlineBox previousIB = null;
            do {
                Styleable child = null;
                final Node nodeType = working;
                if (nodeType instanceof Element) {
                    final Element element = (Element) working;
                    final CalculatedStyle style = sharedContext.getStyle(element);

                    if (style.isDisplayNone()) {
                        continue;
                    }

                    c.resolveCounters(style);

                    if (style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN)
                            || style.isIdent(CSSName.DISPLAY, IdentValue.TABLE_COLUMN_GROUP)) {
                        if ((blockParent != null) &&
                                (blockParent.getStyle().isTable() || blockParent.getStyle().isInlineTable())) {
                            final TableBox table = (TableBox) blockParent;
                            addColumnOrColumnGroup(c, table, element, style);
                        }

                        continue;
                    }

                    if (style.isInline()) {
                        if (needStartText) {
                            needStartText = false;
                            final InlineBox iB = createInlineBox("", parent, parentStyle, null);
                            iB.setStartsHere(true);
                            iB.setEndsHere(false);
                            children.add(iB);
                            previousIB = iB;
                        }
                        createChildren(c, null, element, children, info, true);
                        if (inline) {
                            if (previousIB != null) {
                                previousIB.setEndsHere(false);
                            }
                            needEndText = true;
                        }
                    } else {
                        child = createBlockBox(style, info, false);
                        child.setStyle(style);
                        child.setElement(element);
                        if (style.isListItem()) {
                            final BlockBox block = (BlockBox) child;
                            block.setListCounter(c.getCounterContext(style).getCurrentCounterValue("list-item"));
                        }

                        if (style.isTable() || style.isInlineTable()) {
                            final TableBox table = (TableBox) child;
                            table.ensureChildren(c);

                            child = reorderTableContent(c, table);
                        }

                        if (!info.isContainsBlockLevelContent()
                                && !style.isLayedOutInInlineContext()) {
                            info.setContainsBlockLevelContent(true);
                        }

                        final BlockBox block = (BlockBox) child;
                        if (block.getStyle().mayHaveFirstLine()) {
                            block.setFirstLineStyle(c.getCss().getPseudoElementStyle(element,
                                    "first-line"));
                        }
                        if (block.getStyle().mayHaveFirstLetter()) {
                            block.setFirstLetterStyle(c.getCss().getPseudoElementStyle(element,
                                    "first-letter"));
                        }
                        //I think we need to do this to evaluate counters correctly
                        block.ensureChildren(c);
                    }
                } else if (nodeType instanceof TextNode || nodeType instanceof DataNode) {
                    needStartText = false;
                    needEndText = false;

                    final Node textNode = (Node)working;

                    /*
                    StringBuffer text = new StringBuffer(textNode.getData());

                    Node maybeText = textNode;
                    while (true) {
                        maybeText = textNode.getNextSibling();
                        if (maybeText != null) {
                            short maybeNodeType = maybeText.getNodeType();
                            if (maybeNodeType == Node.TEXT_NODE ||
                                    maybeNodeType == Node.CDATA_SECTION_NODE) {
                                textNode = (Text)maybeText;
                                text.append(textNode.getData());
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }

                    working = textNode;
                    child = createInlineBox(text.toString(), parent, parentStyle, textNode);
                    */

                    child = createInlineBox(textNode instanceof DataNode ? ((DataNode) textNode).getWholeData() : ((TextNode) textNode).getWholeText(), parent, parentStyle, textNode);

                    final InlineBox iB = (InlineBox) child;
                    iB.setEndsHere(true);
                    if (previousIB == null) {
                        iB.setStartsHere(true);
                    } else {
                        previousIB.setEndsHere(false);
                    }
                    previousIB = iB;
                }

                if (child != null) {
                    children.add(child);
                }
            } while ((working = working.nextSibling()) != null);
        }
        if (needStartText || needEndText) {
            final InlineBox iB = createInlineBox("", parent, parentStyle, null);
            iB.setStartsHere(needStartText);
            iB.setEndsHere(needEndText);
            children.add(iB);
        }
        insertGeneratedContent(c, parent, parentStyle, "after", children, info);
    }

    private static void insertAnonymousBlocks(
            final SharedContext c, final Box parent, final List<Styleable> children, final boolean layoutRunningBlocks) {
        List<Styleable> inline = new ArrayList<Styleable>();

        final LinkedList<InlineBox> parents = new LinkedList<InlineBox>();
        List<InlineBox> savedParents = null;

        for (final Styleable child : children) {
            if (child.getStyle().isLayedOutInInlineContext() &&
                    ! (layoutRunningBlocks && child.getStyle().isRunning())) {
                inline.add(child);

                if (child.getStyle().isInline()) {
                    final InlineBox iB = (InlineBox) child;
                    if (iB.isStartsHere()) {
                        parents.add(iB);
                    }
                    if (iB.isEndsHere()) {
                        parents.removeLast();
                    }
                }
            } else {
                if (inline.size() > 0) {
                    createAnonymousBlock(c, parent, inline, savedParents);
                    inline = new ArrayList<Styleable>();
                    savedParents = new ArrayList<InlineBox>(parents);
                }
                parent.addChild((Box) child);
            }
        }

        createAnonymousBlock(c, parent, inline, savedParents);
    }

    private static void createAnonymousBlock(final SharedContext c, final Box parent, final List<Styleable> inline,
                                             final List<InlineBox> savedParents) {
        WhitespaceStripper.stripInlineContent(inline);
        if (inline.size() > 0) {
            final AnonymousBlockBox anon = new AnonymousBlockBox(parent.getElement());
            anon.setStyle(parent.getStyle().createAnonymousStyle(IdentValue.BLOCK));
            anon.setAnonymous(true);
            if (savedParents != null && savedParents.size() > 0) {
                anon.setOpenInlineBoxes(savedParents);
            }
            parent.addChild(anon);
            anon.setChildrenContentType(BlockBox.CONTENT_INLINE);
            anon.setInlineContent(inline);
        }
    }

    private static class ChildBoxInfo {
        private boolean _containsBlockLevelContent;
        private boolean _containsTableContent;
        private boolean _layoutRunningBlocks;

        public ChildBoxInfo() {
        }

        public boolean isContainsBlockLevelContent() {
            return _containsBlockLevelContent;
        }

        public void setContainsBlockLevelContent(final boolean containsBlockLevelContent) {
            _containsBlockLevelContent = containsBlockLevelContent;
        }

        public boolean isContainsTableContent() {
            return _containsTableContent;
        }

        public void setContainsTableContent(final boolean containsTableContent) {
            _containsTableContent = containsTableContent;
        }

        public boolean isLayoutRunningBlocks() {
            return _layoutRunningBlocks;
        }

        public void setLayoutRunningBlocks(final boolean layoutRunningBlocks) {
            _layoutRunningBlocks = layoutRunningBlocks;
        }
    }
}

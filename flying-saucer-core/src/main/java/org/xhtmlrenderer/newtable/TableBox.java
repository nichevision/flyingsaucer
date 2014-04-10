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
package org.xhtmlrenderer.newtable;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.Length;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.ContentLimit;
import org.xhtmlrenderer.render.ContentLimitContainer;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;
import org.xhtmlrenderer.util.ArrayUtil;
import org.xhtmlrenderer.util.XRLog;

// Much of this code is directly inspired by (and even copied from)
// the equivalent code in KHTML (including the idea of "effective columns" to
// manage colspans and the details of the table layout algorithms).  Many kudos
// to the KHTML developers for making such an amazing piece of software!
public class TableBox extends BlockBox {
    private final List<ColumnData> _columns = new ArrayList<ColumnData>();
    private int[] _columnPos;
    private TableLayout _tableLayout;

    private List<TableColumn> _styleColumns;

    private int _pageClearance;

    private boolean _marginAreaRoot;

    private ContentLimitContainer _contentLimitContainer;

    private int _extraSpaceTop;
    private int _extraSpaceBottom;

    public boolean isMarginAreaRoot() {
        return _marginAreaRoot;
    }

    public void setMarginAreaRoot(final boolean marginAreaRoot) {
        _marginAreaRoot = marginAreaRoot;
    }

    public BlockBox copyOf() {
        final TableBox result = new TableBox();
        result.setStyle(getStyle());
        result.setElement(getElement());

        return result;
    }

    public void addStyleColumn(final TableColumn col) {
        if (_styleColumns == null) {
            _styleColumns = new ArrayList<TableColumn>();
        }
        _styleColumns.add(col);
    }

    public List<TableColumn> getStyleColumns() {
        return _styleColumns == null ? Collections.<TableColumn>emptyList() : _styleColumns;
    }

    public int[] getColumnPos() {
        return ArrayUtil.cloneOrEmpty(_columnPos);
    }

    private void setColumnPos(final int[] columnPos) {
        _columnPos = columnPos;
    }

    public int numEffCols() {
        return _columns.size();
    }

    public int spanOfEffCol(final int effCol) {
        return _columns.get(effCol).getSpan();
    }

    public int colToEffCol(final int col) {
        int c = 0;
        int i = 0;
        while (c < col && i < numEffCols()) {
            c += spanOfEffCol(i);
            i++;
        }
        return i;
    }

    public int effColToCol(final int effCol) {
        int c = 0;
        for (int i = 0; i < effCol; i++) {
            c += spanOfEffCol(i);
        }
        return c;
    }

    public void appendColumn(final int span) {
        final ColumnData data = new ColumnData();
        data.setSpan(span);

        _columns.add(data);

        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableSectionBox section = (TableSectionBox)i.next();
            section.extendGridToColumnCount(_columns.size());
        }
    }

    public void setStyle(final CalculatedStyle style) {
        super.setStyle(style);

        if (isMarginAreaRoot()) {
            _tableLayout = new MarginTableLayout(this);
        } else if (getStyle().isIdent(CSSName.TABLE_LAYOUT, IdentValue.AUTO) || getStyle().isAutoWidth()) {
            _tableLayout = new AutoTableLayout(this);
        } else {
            _tableLayout = new FixedTableLayout(this);
        }
    }

    public void calcMinMaxWidth(final LayoutContext c) {
        if (! isMinMaxCalculated()) {
            recalcSections(c);
            if (getStyle().isCollapseBorders()) {
                calcBorders(c);
            }
            _tableLayout.calcMinMaxWidth(c);
            setMinMaxCalculated(true);
        }
    }

    public void splitColumn(final int pos, final int firstSpan) {
        final ColumnData newColumn = new ColumnData();
        newColumn.setSpan(firstSpan);
        _columns.add(pos, newColumn);

        final ColumnData leftOver = _columns.get(pos+1);
        leftOver.setSpan(leftOver.getSpan() - firstSpan);

        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableSectionBox section = (TableSectionBox)i.next();
            section.splitColumn(pos);
        }
    }

    public int marginsBordersPaddingAndSpacing(final CssContext c, final boolean ignoreAutoMargins) {
        int result = 0;
        final RectPropertySet margin = getMargin(c);
        if (! ignoreAutoMargins || ! getStyle().isAutoLeftMargin()) {
            result += (int)margin.left();
        }
        if (! ignoreAutoMargins || ! getStyle().isAutoRightMargin()) {
            result += (int)margin.right();
        }
        final BorderPropertySet border = getBorder(c);
        result += (int)border.left() + (int)border.right();
        if (! getStyle().isCollapseBorders()) {
            final RectPropertySet padding = getPadding(c);
            final int hSpacing = getStyle().getBorderHSpacing(c);
            result += padding.left() + padding.right() + (numEffCols()+1) * hSpacing;
        }
        return result;
    }

    public List<ColumnData> getColumns() {
        return _columns;
    }

    private void recalcSections(final LayoutContext c) {
        ensureChildren(c);
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableSectionBox section = (TableSectionBox)i.next();
            section.recalcCells(c);
        }
    }

    private void calcBorders(final LayoutContext c) {
        ensureChildren(c);
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableSectionBox section = (TableSectionBox)i.next();
            section.calcBorders(c);
        }
    }

    protected boolean isAllowHeightToShrink() {
        return false;
    }

    public void layout(final LayoutContext c) {
        calcMinMaxWidth(c);
        calcDimensions(c);
        calcWidth();
        calcPageClearance(c);

        // Recalc to pick up auto margins now that layout has been called on
        // containing block and the table has a content width
        if (! isAnonymous()) {
            setDimensionsCalculated(false);
            calcDimensions(c, getContentWidth());
        }

        _tableLayout.layout(c);

        setCellWidths(c);

        layoutTable(c);
    }

    protected void resolveAutoMargins(final LayoutContext c, final int cssWidth, final RectPropertySet padding,
            final BorderPropertySet border) {
        // If our minimum width is greater than the calculated CSS width,
        // don't try to allocate any margin space to auto margins.  It
        // will just confuse the issue later when we expand the effective
        // table width to its minimum width.
        if (getMinWidth() <= getContentWidth() + marginsBordersPaddingAndSpacing(c, true)) {
            super.resolveAutoMargins(c, cssWidth, padding, border);
        } else {
            if (getStyle().isAutoLeftMargin()) {
                setMarginLeft(c, 0);
            }
            if (getStyle().isAutoRightMargin()) {
                setMarginRight(c, 0);
            }
        }
    }

    private void layoutTable(final LayoutContext c) {
        final boolean running = c.isPrint() && getStyle().isPaginateTable();
        int prevExtraTop = 0;
        int prevExtraBottom = 0;

        if (running) {
            prevExtraTop = c.getExtraSpaceTop();
            prevExtraBottom = c.getExtraSpaceBottom();

            c.setExtraSpaceTop(c.getExtraSpaceTop() +
                    (int)getPadding(c).top() +
                    (int)getBorder(c).top() +
                    getStyle().getBorderVSpacing(c));
            c.setExtraSpaceBottom(c.getExtraSpaceBottom() +
                    (int)getPadding(c).bottom() +
                    (int)getBorder(c).bottom() +
                    getStyle().getBorderVSpacing(c));
        }

        super.layout(c);

        if (running) {
            if (isNeedAnalyzePageBreaks()) {
                analyzePageBreaks(c);

                setExtraSpaceTop(0);
                setExtraSpaceBottom(0);
            } else {
                setExtraSpaceTop(c.getExtraSpaceTop() - prevExtraTop);
                setExtraSpaceBottom(c.getExtraSpaceBottom() - prevExtraBottom);
            }
            c.setExtraSpaceTop(prevExtraTop);
            c.setExtraSpaceBottom(prevExtraBottom);
        }
    }

    protected void layoutChildren(final LayoutContext c, final int contentStart) {
        ensureChildren(c);
        // If we have a running footer, we need its dimensions right away
        final boolean running = c.isPrint() && getStyle().isPaginateTable();
        if (running) {
            final int headerHeight = layoutRunningHeader(c);
            final int footerHeight = layoutRunningFooter(c);
            final int spacingHeight = footerHeight == 0 ? 0 : getStyle().getBorderVSpacing(c);

            final PageBox first = c.getRootLayer().getFirstPage(c, this);
            if (getAbsY() + getTy() + headerHeight + footerHeight + spacingHeight > first.getBottom()) {
                // XXX Performance problem here.  This forces the table
                // to move to the next page (which we want), but the initial
                // table layout run still completes (which we don't)
                setNeedPageClear(true);
            }
        }
        super.layoutChildren(c, contentStart);
    }

    private int layoutRunningHeader(final LayoutContext c) {
        int result = 0;
        if (getChildCount() > 0) {
            final TableSectionBox section = (TableSectionBox)getChild(0);
            if (section.isHeader()) {
                c.setNoPageBreak(c.getNoPageBreak() + 1);

                section.initContainingLayer(c);
                section.layout(c);

                c.setExtraSpaceTop(c.getExtraSpaceTop() + section.getHeight());

                result = section.getHeight();

                section.reset(c);

                c.setNoPageBreak(c.getNoPageBreak() - 1);
            }
        }

        return result;
    }

    private int layoutRunningFooter(final LayoutContext c) {
        int result = 0;
        if (getChildCount() > 0) {
            final TableSectionBox section = (TableSectionBox)getChild(getChildCount()-1);
            if (section.isFooter()) {
                c.setNoPageBreak(c.getNoPageBreak() + 1);

                section.initContainingLayer(c);
                section.layout(c);

                c.setExtraSpaceBottom(c.getExtraSpaceBottom() +
                        section.getHeight() +
                        getStyle().getBorderVSpacing(c));

                result = section.getHeight();

                section.reset(c);

                c.setNoPageBreak(c.getNoPageBreak() - 1);
            }
        }
        return result;
    }

    private boolean isNeedAnalyzePageBreaks() {
        Box b = getParent();
        while (b != null) {
            if (b.getStyle().isTable() && b.getStyle().isPaginateTable()) {
                return false;
            }

            b = b.getParent();
        }

        return true;
    }

    private void analyzePageBreaks(final LayoutContext c) {
        analyzePageBreaks(c, null);
    }

    public void analyzePageBreaks(final LayoutContext c, final ContentLimitContainer container) {
        _contentLimitContainer = new ContentLimitContainer(c, getAbsY());
        _contentLimitContainer.setParent(container);

        if (container != null) {
            container.updateTop(c, getAbsY());
            container.updateBottom(c, getAbsY() + getHeight());
        }

        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final Box b = (Box)i.next();
            b.analyzePageBreaks(c, _contentLimitContainer);
        }

        if (container != null && _contentLimitContainer.isContainsMultiplePages() &&
                (getExtraSpaceTop() > 0 || getExtraSpaceBottom() > 0)) {
            propagateExtraSpace(c, container, _contentLimitContainer, getExtraSpaceTop(), getExtraSpaceBottom());
        }
    }

    public void paintBackground(final RenderingContext c) {
        if (_contentLimitContainer == null) {
            super.paintBackground(c);
        } else if (getStyle().isVisible()) {
            c.getOutputDevice().paintBackground(
                    c, getStyle(), getContentLimitedBorderEdge(c), getPaintingBorderEdge(c),
                    getStyle().getBorder(c));
        }
    }

    public void paintBorder(final RenderingContext c) {
        if (_contentLimitContainer == null) {
            super.paintBorder(c);
        } else if (getStyle().isVisible()) {
            c.getOutputDevice().paintBorder(c, getStyle(), getContentLimitedBorderEdge(c), getBorderSides());
        }
    }

    private Rectangle getContentLimitedBorderEdge(final RenderingContext c) {
        final Rectangle result = getPaintingBorderEdge(c);

        final ContentLimit limit = _contentLimitContainer.getContentLimit(c.getPageNo());

        if (limit == null) {
            XRLog.layout(Level.WARNING, "No content limit found");
            return result;
        } else {
            if (limit.getTop() == ContentLimit.UNDEFINED ||
                    limit.getBottom() == ContentLimit.UNDEFINED) {
                return result;
            }

            final RectPropertySet padding = getPadding(c);
            final BorderPropertySet border = getBorder(c);

            int top;
            if (c.getPageNo() == _contentLimitContainer.getInitialPageNo()) {
                top = result.y;
            } else {
                top = limit.getTop() - (int)padding.top() -
                    (int)border.top() - getStyle().getBorderVSpacing(c);
                if (getChildCount() > 0) {
                    final TableSectionBox section = (TableSectionBox)getChild(0);
                    if (section.isHeader()) {
                        top -= section.getHeight();
                    }
                }
            }

            int bottom;
            if (c.getPageNo() == _contentLimitContainer.getLastPageNo()) {
                bottom = result.y + result.height;
            } else {
                bottom = limit.getBottom() + (int)padding.bottom() +
                            (int)border.bottom() + getStyle().getBorderVSpacing(c);
                if (getChildCount() > 0) {
                    final TableSectionBox section = (TableSectionBox)getChild(getChildCount()-1);
                    if (section.isFooter()) {
                        bottom += section.getHeight();
                    }
                }
            }

            result.y = top;
            result.height = bottom - top;

            return result;
        }
    }

    public void updateHeaderFooterPosition(final RenderingContext c) {
        final ContentLimit limit = _contentLimitContainer.getContentLimit(c.getPageNo());

        if (limit != null) {
            updateHeaderPosition(c, limit);
            updateFooterPosition(c, limit);
        }
    }

    private void updateHeaderPosition(final RenderingContext c, final ContentLimit limit) {
        if (limit.getTop() != ContentLimit.UNDEFINED ||
                c.getPageNo() == _contentLimitContainer.getInitialPageNo()) {
            if (getChildCount() > 0) {
                final TableSectionBox section = (TableSectionBox)getChild(0);
                if (section.isHeader()) {
                    if (! section.isCapturedOriginalAbsY()) {
                        section.setOriginalAbsY(section.getAbsY());
                        section.setCapturedOriginalAbsY(true);
                    }

                    int newAbsY;
                    if (c.getPageNo() == _contentLimitContainer.getInitialPageNo()) {
                        newAbsY = section.getOriginalAbsY();
                    } else {
                        newAbsY = limit.getTop() -
                            getStyle().getBorderVSpacing(c) -
                            section.getHeight();
                    }

                    final int diff = newAbsY - section.getAbsY();

                    if (diff != 0) {
                        section.setY(section.getY() + diff);
                        section.calcCanvasLocation();
                        section.calcChildLocations();
                        section.calcPaintingInfo(c, false);
                    }
                }
            }
        }
    }

    private void updateFooterPosition(final RenderingContext c, final ContentLimit limit) {
        if (limit.getBottom() != ContentLimit.UNDEFINED ||
                c.getPageNo() == _contentLimitContainer.getLastPageNo()) {
            if (getChildCount() > 0) {
                final TableSectionBox section = (TableSectionBox)getChild(getChildCount()-1);
                if (section.isFooter()) {
                    if (! section.isCapturedOriginalAbsY()) {
                        section.setOriginalAbsY(section.getAbsY());
                        section.setCapturedOriginalAbsY(true);
                    }

                    int newAbsY;
                    if (c.getPageNo() == _contentLimitContainer.getLastPageNo()) {
                        newAbsY = section.getOriginalAbsY();
                    } else {
                        newAbsY = limit.getBottom();
                    }

                    final int diff = newAbsY - section.getAbsY();

                    if (diff != 0) {
                        section.setY(section.getY() + diff);
                        section.calcCanvasLocation();
                        section.calcChildLocations();
                        section.calcPaintingInfo(c, false);
                    }
                }
            }
        }
    }

    private void calcPageClearance(final LayoutContext c) {
        if (c.isPrint() && getStyle().isCollapseBorders()) {
            final PageBox page = c.getRootLayer().getFirstPage(c, this);
            if (page != null) {
                final TableRowBox row = getFirstRow();
                if (row != null) {
                    int spill = 0;
                    for (final Iterator<Box> i = row.getChildIterator(); i.hasNext(); ) {
                        final TableCellBox cell = (TableCellBox)i.next();
                        final BorderPropertySet collapsed = cell.getCollapsedPaintingBorder();
                        final int tmp = (int)collapsed.top() / 2;
                        if (tmp > spill) {
                            spill = tmp;
                        }
                    }

                    final int borderTop = getAbsY() + (int)getMargin(c).top() - spill;
                    final int delta = page.getTop() - borderTop;
                    if (delta > 0) {
                        setY(getY() + delta);
                        setPageClearance(delta);
                        calcCanvasLocation();
                        c.translate(0, delta);
                    }
                }
            }
        }
    }

    private void calcWidth() {
        if (getMinWidth() > getWidth()) {
            setContentWidth(getContentWidth() + getMinWidth() - getWidth());
        } else if (getStyle().isIdent(CSSName.WIDTH, IdentValue.AUTO) &&
                    getMaxWidth() < getWidth()) {
            setContentWidth(getContentWidth() - (getWidth() - getMaxWidth()));
        }
    }

    public TableRowBox getFirstRow() {
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableSectionBox section = (TableSectionBox)i.next();
            if (section.getChildCount() > 0) {
                return (TableRowBox)section.getChild(0);
            }
        }

        return null;
    }

    public TableRowBox getFirstBodyRow() {
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableSectionBox section = (TableSectionBox)i.next();
            if (section.isHeader() || section.isFooter()) {
                continue;
            }
            if (section.getChildCount() > 0) {
                return (TableRowBox)section.getChild(0);
            }
        }

        return null;
    }

    private void setCellWidths(final LayoutContext c) {
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final BlockBox box = (BlockBox)i.next();
            if (box.getStyle().isTableSection()) {
                ((TableSectionBox)box).setCellWidths(c);
            }
        }
    }

    protected void calcLayoutHeight(final LayoutContext c, final BorderPropertySet border,
            final RectPropertySet margin, final RectPropertySet padding) {
        super.calcLayoutHeight(c, border, margin, padding);

        if (getChildCount() > 0) {
            setHeight(getHeight() + getStyle().getBorderVSpacing(c));
        }
    }

    public void reset(final LayoutContext c) {
        super.reset(c);

        _contentLimitContainer = null;

        _tableLayout.reset();
    }

    protected int getCSSWidth(final CssContext c) {
        if (getStyle().isAutoWidth()) {
            return -1;
        } else {
            // XHTML 1.0 specifies that a table width refers to the border
            // width.  This can be removed if/when we support the box-sizing
            // property.
            int result = (int)getStyle().getFloatPropertyProportionalWidth(
                    CSSName.WIDTH, getContainingBlock().getContentWidth(), c);

            final BorderPropertySet border = getBorder(c);
            result -= (int)border.left() + (int)border.right();
            if (! getStyle().isCollapseBorders()) {
                final RectPropertySet padding = getPadding(c);
                result -= (int)padding.left() + (int)padding.right();
            }

            return result >= 0 ? result : -1;
        }
    }

    public TableColumn colElement(final int col) {
        final List<TableColumn> styleColumns = getStyleColumns();
        if (styleColumns.size() == 0) {
            return null;
        }
        int cCol = 0;
        for (final Iterator<TableColumn> i = styleColumns.iterator(); i.hasNext();) {
            final TableColumn colElem = (TableColumn)i.next();
            final int span = colElem.getStyle().getColSpan();
            cCol += span;
            if (cCol > col) {
                return colElem;
            }
        }
        return null;
    }

    public Rectangle getColumnBounds(final CssContext c, final int col) {
        final int effCol = colToEffCol(col);

        final int hspacing = getStyle().getBorderHSpacing(c);
        final int vspacing = getStyle().getBorderVSpacing(c);

        final Rectangle result = getContentAreaEdge(getAbsX(), getAbsY(), c);

        result.y += vspacing;
        result.height -= vspacing*2;

        result.x += _columnPos[effCol] + hspacing;

        return result;
    }

    public BorderPropertySet getBorder(final CssContext cssCtx) {
        if (getStyle().isCollapseBorders()) {
            return BorderPropertySet.EMPTY_BORDER;
        } else {
            return super.getBorder(cssCtx);
        }
    }

    public int calcFixedHeightRowBottom(final CssContext c) {
        if (! isAnonymous()) {
            final int cssHeight = getCSSHeight(c);
            if (cssHeight != -1) {
                return getAbsY() + cssHeight
                    - (int)getBorder(c).bottom() - (int)getPadding(c).bottom()
                    - getStyle().getBorderVSpacing(c);
            }
        }

        return -1;
    }

    protected boolean isMayCollapseMarginsWithChildren() {
        return false;
    }

    protected TableSectionBox sectionAbove(
            final TableSectionBox section, final boolean skipEmptySections) {
        TableSectionBox prevSection = (TableSectionBox)section.getPreviousSibling();

        if (prevSection == null) {
            return null;
        }

        while (prevSection != null) {
            if (prevSection.numRows() > 0 || !skipEmptySections) {
                break;
            }
            prevSection = (TableSectionBox)prevSection.getPreviousSibling();
        }

        return prevSection;
    }

    protected TableSectionBox sectionBelow(
            final TableSectionBox section, final boolean skipEmptySections) {
        TableSectionBox nextSection = (TableSectionBox)section.getNextSibling();

        if (nextSection == null) {
            return null;
        }

        while (nextSection != null) {
            if (nextSection.numRows() > 0 || !skipEmptySections) {
                break;
            }
            nextSection = (TableSectionBox)nextSection.getNextSibling();
        }

        return nextSection;
    }

    protected TableCellBox cellAbove(final TableCellBox cell) {
        // Find the section and row to look in
        final int r = cell.getRow();
        TableSectionBox section = null;
        int rAbove = 0;
        if (r > 0) {
            // cell is not in the first row, so use the above row in its own
            // section
            section = cell.getSection();
            rAbove = r - 1;
        } else {
            section = sectionAbove(cell.getSection(), true);
            if (section != null) {
                rAbove = section.numRows() - 1;
            }
        }

        // Look up the cell in the section's grid, which requires effective col
        // index
        if (section != null) {
            int effCol = colToEffCol(cell.getCol());
            TableCellBox aboveCell;
            // If we hit a span back up to a real cell.
            do {
                aboveCell = section.cellAt(rAbove, effCol);
                effCol--;
            } while (aboveCell == TableCellBox.SPANNING_CELL && effCol >= 0);
            return (aboveCell == TableCellBox.SPANNING_CELL) ? null : aboveCell;
        } else {
            return null;
        }
    }

    protected TableCellBox cellBelow(final TableCellBox cell) {
        // Find the section and row to look in
        final int r = cell.getRow() + cell.getStyle().getRowSpan() - 1;
        TableSectionBox section = null;
        int rBelow = 0;
        if (r < cell.getSection().numRows() - 1) {
            // The cell is not in the last row, so use the next row in the
            // section.
            section = cell.getSection();
            rBelow = r + 1;
        } else {
            section = sectionBelow(cell.getSection(), true);
            if (section != null)
                rBelow = 0;
        }

        // Look up the cell in the section's grid, which requires effective col
        // index
        if (section != null) {
            int effCol = colToEffCol(cell.getCol());
            TableCellBox belowCell;
            // If we hit a colspan back up to a real cell.
            do {
                belowCell = section.cellAt(rBelow, effCol);
                effCol--;
            } while (belowCell == TableCellBox.SPANNING_CELL && effCol >= 0);
            return (belowCell == TableCellBox.SPANNING_CELL) ? null : belowCell;
        } else {
            return null;
        }
    }

    protected TableCellBox cellLeft(final TableCellBox cell) {
        final TableSectionBox section = cell.getSection();
        int effCol = colToEffCol(cell.getCol());
        if (effCol == 0) {
            return null;
        }

        // If we hit a colspan back up to a real cell.
        TableCellBox prevCell;
        do {
            prevCell = section.cellAt(cell.getRow(), effCol - 1);
            effCol--;
        } while (prevCell == TableCellBox.SPANNING_CELL && effCol >= 0);
        return (prevCell == TableCellBox.SPANNING_CELL) ? null : prevCell;
    }


    protected TableCellBox cellRight(final TableCellBox cell) {
        final int effCol = colToEffCol(cell.getCol() + cell.getStyle().getColSpan());
        if (effCol >= numEffCols()) {
            return null;
        }
        final TableCellBox result = cell.getSection().cellAt(cell.getRow(), effCol);
        return (result == TableCellBox.SPANNING_CELL) ? null : result;
    }

    public int calcInlineBaseline(final CssContext c) {
        int result = 0;
        boolean found = false;
        OUTER:
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableSectionBox section = (TableSectionBox)i.next();
            for (final Iterator<Box> j = section.getChildIterator(); j.hasNext(); ) {
                final TableRowBox row = (TableRowBox)j.next();
                found = true;
                result = row.getAbsY() + row.getBaseline() - getAbsY();
                break OUTER;
            }
        }

        if (! found) {
            result = getHeight();
        }

        return result;
    }

    protected int getPageClearance() {
        return _pageClearance;
    }

    protected void setPageClearance(final int pageClearance) {
        _pageClearance = pageClearance;
    }

    public boolean hasContentLimitContainer() {
        return _contentLimitContainer != null;
    }

    public int getExtraSpaceTop() {
        return _extraSpaceTop;
    }

    public void setExtraSpaceTop(final int extraSpaceTop) {
        _extraSpaceTop = extraSpaceTop;
    }

    public int getExtraSpaceBottom() {
        return _extraSpaceBottom;
    }

    public void setExtraSpaceBottom(final int extraSpaceBottom) {
        _extraSpaceBottom = extraSpaceBottom;
    }

    private interface TableLayout {
        public void calcMinMaxWidth(LayoutContext c);
        public void layout(LayoutContext c);
        public void reset();
    }

    /**
     * A specialization of <code>AutoTableLayout</code> used for laying out the
     * tables used to approximate the margin box layout algorithm from CSS3
     * GCPM.
     */
    private static class MarginTableLayout extends AutoTableLayout {
        public MarginTableLayout(final TableBox table) {
            super(table);
        }

        protected int getMinColWidth() {
            return 0;
        }

        public void calcMinMaxWidth(final LayoutContext c) {
            super.calcMinMaxWidth(c);

            final Layout[] layoutStruct = getLayoutStruct();

            if (layoutStruct.length == 3) {
                final Layout center = layoutStruct[1];

                if (! (center.width().isVariable() && center.maxWidth() == 0)) {
                    if (layoutStruct[0].minWidth() > layoutStruct[2].minWidth()) {
                        layoutStruct[2] = layoutStruct[0];
                    } else if (layoutStruct[2].minWidth() > layoutStruct[0].minWidth()) {
                        layoutStruct[0] = layoutStruct[2];
                    } else {
                        final Layout l = new Layout();
                        l.setMinWidth(Math.max(layoutStruct[0].minWidth(), layoutStruct[2].minWidth()));
                        l.setEffMinWidth(l.minWidth());
                        l.setMaxWidth(Math.max(layoutStruct[0].maxWidth(), layoutStruct[2].maxWidth()));
                        l.setEffMaxWidth(l.maxWidth());

                        layoutStruct[0] = l;
                        layoutStruct[2] = l;
                    }
                }
            }
        }
    }

    private static class FixedTableLayout implements TableLayout {
        private final TableBox _table;
        private List<Length> _widths;

        public FixedTableLayout(final TableBox table) {
            _table = table;
        }

        public void reset() {
            _widths = null;
        }

        private void initWidths() {
            _widths = new ArrayList<Length>(_table.numEffCols());
            for (int i = 0; i < _table.numEffCols(); i++) {
                _widths.add(new Length());
            }
        }

        private int calcWidthArray(final LayoutContext c) {
            initWidths();

            final TableBox table = _table;

            int cCol = 0;
            int nEffCols = table.numEffCols();
            int usedWidth = 0;

            for (final Iterator<TableColumn> j = table.getStyleColumns().iterator(); j.hasNext();) {
                final TableColumn col = (TableColumn) j.next();
                final int span = col.getStyle().getColSpan();
                Length w = col.getStyle().asLength(c, CSSName.WIDTH);
                if (w.isVariable() && col.getParent() != null) {
                    w = col.getParent().getStyle().asLength(c, CSSName.WIDTH);
                }

                long effWidth = 0;
                if (w.isFixed() && w.value() > 0) {
                    effWidth = w.value();
                    effWidth = Math.min(effWidth, Length.MAX_WIDTH);
                }

                int usedSpan = 0;
                int i = 0;
                while (usedSpan < span) {
                    if (cCol + i >= nEffCols) {
                        table.appendColumn(span - usedSpan);
                        nEffCols++;
                        _widths.add(new Length());
                    }
                    final int eSpan = table.spanOfEffCol(cCol + i);
                    if ((w.isFixed() || w.isPercent()) && w.value() > 0) {
                        _widths.set(cCol + i, new Length(w.value() * eSpan, w.type()));
                        usedWidth += effWidth * eSpan;
                    }
                    usedSpan += eSpan;
                    i++;
                }
                cCol += i;
            }

            cCol = 0;
            final TableRowBox firstRow = _table.getFirstRow();
            if (firstRow != null) {
                for (final Iterator<Box> j = firstRow.getChildIterator(); j.hasNext();) {
                    final TableCellBox cell = (TableCellBox) j.next();
                    final Length w = cell.getOuterStyleWidth(c);
                    final int span = cell.getStyle().getColSpan();
                    long effWidth = 0;
                    if (w.isFixed() && w.value() > 0) {
                        effWidth = w.value();
                    }

                    int usedSpan = 0;
                    int i = 0;
                    while (usedSpan < span) {
                        final int eSpan = _table.spanOfEffCol(cCol + i);

                        final Length columnWidth = _widths.get(cCol + i);
                        // only set if no col element has already set it.
                        if (columnWidth.isVariable() && !w.isVariable()) {
                            _widths.set(cCol + i, new Length(w.value() * eSpan, w.type()));
                            usedWidth += effWidth * eSpan;
                        }

                        usedSpan += eSpan;
                        i++;
                    }

                    cCol += i;
                }
            }

            return usedWidth;
        }

        public void calcMinMaxWidth(final LayoutContext c) {
            final int bs = _table.marginsBordersPaddingAndSpacing(c, true);

            _table.calcDimensions(c);

            // Reset to allow layout to have another crack at this.  If we're
            // participating in a nested max/min-width calculation, the values
            // calculated above may be wrong and may need updating once our
            // parent has a width.
            _table.setDimensionsCalculated(false);

            final int mw = calcWidthArray(c) + bs;
            _table.setMinWidth(Math.max(mw, _table.getWidth()));
            _table.setMaxWidth(_table.getMinWidth());

            boolean haveNonFixed = false;
            for (int i = 0; i < _widths.size(); i++) {
                final Length w = _widths.get(i);
                if (! w.isFixed()) {
                    haveNonFixed = true;
                    break;
                }
            }

            if (haveNonFixed) {
                _table.setMaxWidth(Length.MAX_WIDTH);
            }
        }

        public void layout(final LayoutContext c) {
            final int tableWidth = _table.getWidth() - _table.marginsBordersPaddingAndSpacing(c, false);
            int available = tableWidth;
            final int nEffCols = _table.numEffCols();

            final long[] calcWidth = new long[nEffCols];
            for (int i = 0; i < calcWidth.length; i++) {
                calcWidth[i] = -1;
            }

            // first assign fixed width
            for ( int i = 0; i < nEffCols; i++ ) {
                final Length l = _widths.get(i);
                if ( l.isFixed() ) {
                    calcWidth[i] = l.value();
                    available -= l.value();
                }
            }

            // assign percent width
            if ( available > 0 ) {
                int totalPercent = 0;
                for ( int i = 0; i < nEffCols; i++ ) {
                    final Length l = _widths.get(i);
                    if ( l.isPercent() ) {
                        totalPercent += l.value();
                    }
                }

                // calculate how much to distribute to percent cells.
                int base = tableWidth * totalPercent / 100;
                if (base > available) {
                    base = available;
                }

                for ( int i = 0; available > 0 && i < nEffCols; i++ ) {
                    final Length l = _widths.get(i);
                    if ( l.isPercent() ) {
                        final long w = base * l.value() / totalPercent;
                        available -= w;
                        calcWidth[i] = w;
                    }
                }
            }

            // assign variable width
            if ( available > 0 ) {
                int totalVariable = 0;
                for ( int i = 0; i < nEffCols; i++ ) {
                    final Length l = _widths.get(i);
                    if ( l.isVariable() ) {
                        totalVariable++;
                    }
                }

                for ( int i = 0; available > 0 && i < nEffCols; i++ ) {
                    final Length l = _widths.get(i);
                    if ( l.isVariable() ) {
                        final int w = available / totalVariable;
                        available -= w;
                        calcWidth[i] = w;
                        totalVariable--;
                    }
                }
            }

            for ( int i = 0; i < nEffCols; i++ ) {
                if ( calcWidth[i] < 0 ) {
                    calcWidth[i] = 0; // IE gives min 1 px...
                }
            }

            // spread extra space over columns
            if ( available > 0 ) {
                int total = nEffCols;
                // still have some width to spread
                int i = nEffCols;
                while ( i-- > 0 ) {
                    final int w = available / total;
                    available -= w;
                    total--;
                    calcWidth[i] += w;
                }
            }

            int pos = 0;
            final int hspacing = _table.getStyle().getBorderHSpacing(c);
            final int[] columnPos = new int[nEffCols+1];
            for ( int i = 0; i < nEffCols; i++ ) {
                columnPos[i] = pos;
                pos += calcWidth[i] + hspacing;
            }

            columnPos[columnPos.length-1] = pos;

            _table.setColumnPos(columnPos);
        }
    }

    private static class AutoTableLayout implements TableLayout {
        private final TableBox _table;
        private Layout[] _layoutStruct;
        private List<TableCellBox> _spanCells;

        public AutoTableLayout(final TableBox table) {
            _table = table;
        }

        public void reset() {
            _layoutStruct = null;
            _spanCells = null;
        }

        protected Layout[] getLayoutStruct() {
            return _layoutStruct;
        }

        private void fullRecalc(final LayoutContext c) {
            _layoutStruct = new Layout[_table.numEffCols()];
            for (int i = 0; i < _layoutStruct.length; i++) {
                _layoutStruct[i] = new Layout();
                _layoutStruct[i].setMinWidth(getMinColWidth());
                _layoutStruct[i].setMaxWidth(getMinColWidth());
            }

            _spanCells = new ArrayList<TableCellBox>();

            final TableBox table = _table;
            final int nEffCols = table.numEffCols();

            int cCol = 0;
            for (final Iterator<TableColumn> j = table.getStyleColumns().iterator(); j.hasNext();) {
                final TableColumn col = (TableColumn) j.next();
                final int span = col.getStyle().getColSpan();
                Length w = col.getStyle().asLength(c, CSSName.WIDTH);
                if (w.isVariable() && col.getParent() != null) {
                    w = col.getParent().getStyle().asLength(c, CSSName.WIDTH);
                }

                if ((w.isFixed() && w.value() == 0) || (w.isPercent() && w.value() == 0)) {
                    w = new Length();
                }
                final int cEffCol = table.colToEffCol(cCol);
                if (!w.isVariable() && span == 1 && cEffCol < nEffCols) {
                    if (table.spanOfEffCol(cEffCol) == 1) {
                        _layoutStruct[cEffCol].setWidth(w);
                        if (w.isFixed() && _layoutStruct[cEffCol].maxWidth() < w.value()) {
                            _layoutStruct[cEffCol].setMaxWidth(w.value());
                        }
                    }
                }
                cCol += span;

            }

            for (int i = 0; i < nEffCols; i++) {
                recalcColumn(c, i);
            }
        }

        protected int getMinColWidth() {
            return 1;
        }

        private void recalcColumn(final LayoutContext c, final int effCol) {
            final Layout l = _layoutStruct[effCol];

            // first we iterate over all rows.
            for (final Iterator<Box> j = _table.getChildIterator(); j.hasNext();) {
                final TableSectionBox section = (TableSectionBox) j.next();
                final int numRows = section.numRows();
                for (int i = 0; i < numRows; i++) {
                    final TableCellBox cell = section.cellAt(i, effCol);
                    if (cell == TableCellBox.SPANNING_CELL || cell == null) {
                        continue;
                    }
                    if (cell.getStyle().getColSpan() == 1) {
                        // A cell originates in this column. Ensure we have
                        // a min/max width of at least 1px for this column now.
                        l.setMinWidth(Math.max(l.minWidth(), getMinColWidth()));
                        l.setMaxWidth(Math.max(l.maxWidth(), getMinColWidth()));

                        cell.calcMinMaxWidth(c);
                        if (cell.getMinWidth() > l.minWidth()) {
                            l.setMinWidth(cell.getMinWidth());
                        }
                        if (cell.getMaxWidth() > l.maxWidth()) {
                            l.setMaxWidth(cell.getMaxWidth());
                        }

                        final Length w = cell.getOuterStyleOrColWidth(c);
                        w.setValue(Math.min(Length.MAX_WIDTH, Math.max(0, w.value())));

                        switch (w.type()) {
                        case Length.FIXED:
                            if (w.value() > 0 && !l.width().isPercent()) {
                                if (l.width().isFixed()) {
                                    if (w.value() > l.width().value()) {
                                        l.width().setValue(w.value());
                                    }
                                } else {
                                    l.setWidth(w);
                                }
                                if (w.value() > l.maxWidth()) {
                                    l.setMaxWidth(w.value());
                                }
                            }
                            break;
                        case Length.PERCENT:
                            if (w.value() > 0
                                    && (!l.width().isPercent() || w.value() > l.width().value())) {
                                l.setWidth(w);
                                break;
                            }
                        }
                    } else {
                        if (effCol == 0 || section.cellAt(i, effCol - 1) != cell) {
                            // This spanning cell originates in this column.
                            // Ensure we have a min/max width of at least 1px for this column now.
                            l.setMinWidth(Math.max(l.minWidth(), getMinColWidth()));
                            l.setMaxWidth(Math.max(l.maxWidth(), getMinColWidth()));

                            _spanCells.add(cell);
                        }
                    }
                }
            }

            l.setMaxWidth(Math.max(l.maxWidth(), l.minWidth()));
        }

        /*
         * This method takes care of colspans. effWidth is the same as width for
         * cells without colspans. If we have colspans, they get modified.
         */
        private long calcEffectiveWidth(final LayoutContext c) {
            long tMaxWidth = 0;

            final Layout[] layoutStruct = _layoutStruct;

            final int nEffCols = layoutStruct.length;
            final int hspacing = _table.getStyle().getBorderHSpacing(c);

            for (int i = 0; i < nEffCols; i++ ) {
                layoutStruct[i].setEffWidth(layoutStruct[i].width());
                layoutStruct[i].setEffMinWidth(layoutStruct[i].minWidth());
                layoutStruct[i].setEffMaxWidth(layoutStruct[i].maxWidth());
            }

            Collections.sort(_spanCells, new Comparator<TableCellBox>() {
                public int compare(final TableCellBox o1, final TableCellBox o2) {
                    final TableCellBox c1 = (TableCellBox)o1;
                    final TableCellBox c2 = (TableCellBox)o2;

                    return c1.getStyle().getColSpan() - c2.getStyle().getColSpan();
                }
            });

            for (final Iterator<TableCellBox> i = _spanCells.iterator(); i.hasNext(); ) {
                final TableCellBox cell = i.next();

                cell.calcMinMaxWidth(c);

                int span = cell.getStyle().getColSpan();
                Length w = cell.getOuterStyleOrColWidth(c);
                if (w.value() == 0) {
                    w =  new Length(); // make it Variable
                }

                final int col = _table.colToEffCol(cell.getCol());
                int lastCol = col;
                int cMinWidth = cell.getMinWidth() + hspacing;
                int cMaxWidth = cell.getMaxWidth() + hspacing;
                int totalPercent = 0;
                int minWidth = 0;
                int maxWidth = 0;
                boolean allColsArePercent = true;
                boolean allColsAreFixed = true;
                boolean haveVariable = false;
                int fixedWidth = 0;

                while (lastCol < nEffCols && span > 0) {
                    switch (layoutStruct[lastCol].width().type()) {
                    case Length.PERCENT:
                        totalPercent += layoutStruct[lastCol].width().value();
                        allColsAreFixed = false;
                        break;
                    case Length.FIXED:
                        if (layoutStruct[lastCol].width().value() > 0) {
                            fixedWidth += layoutStruct[lastCol].width().value();
                            allColsArePercent = false;
                            break;
                        }
                        // fall through
                    case Length.VARIABLE:
                        haveVariable = true;
                        // fall through
                    default:
                        // If the column is a percentage width, do not let the spanning cell overwrite the
                        // width value.  This caused a mis-rendering on amazon.com.
                        // Sample snippet:
                        // <table border=2 width=100%><
                        //   <tr><td>1</td><td colspan=2>2-3</tr>
                        //   <tr><td>1</td><td colspan=2 width=100%>2-3</td></tr>
                        // </table>
                        if (!layoutStruct[lastCol].effWidth().isPercent()) {
                            layoutStruct[lastCol].setEffWidth(new Length());
                            allColsArePercent = false;
                        } else {
                            totalPercent += layoutStruct[lastCol].effWidth().value();
                        }
                        allColsAreFixed = false;
                    }

                    span -= _table.spanOfEffCol(lastCol);
                    minWidth += layoutStruct[lastCol].effMinWidth();
                    maxWidth += layoutStruct[lastCol].effMaxWidth();
                    lastCol++;
                    cMinWidth -= hspacing;
                    cMaxWidth -= hspacing;
                }

                // adjust table max width if needed
                if (w.isPercent()) {
                    if (totalPercent > w.value() || allColsArePercent) {
                        // can't satify this condition, treat as variable
                        w = new Length();
                    } else {
                        final int spanMax = Math.max(maxWidth, cMaxWidth);
                        tMaxWidth = Math.max(tMaxWidth, spanMax * 100 / w.value());

                        // all non percent columns in the span get percent
                        // values to sum up correctly.
                        long percentMissing = w.value() - totalPercent;
                        int totalWidth = 0;
                        for (int pos = col; pos < lastCol; pos++) {
                            if (!(layoutStruct[pos].width().isPercent())) {
                                totalWidth += layoutStruct[pos].effMaxWidth();
                            }
                        }

                        for (int pos = col; pos < lastCol && totalWidth > 0; pos++) {
                            if (!(layoutStruct[pos].width().isPercent())) {
                                final long percent = percentMissing * layoutStruct[pos].effMaxWidth()
                                        / totalWidth;
                                totalWidth -= layoutStruct[pos].effMaxWidth();
                                percentMissing -= percent;
                                if (percent > 0) {
                                    layoutStruct[pos].setEffWidth(new Length(percent,
                                            Length.PERCENT));
                                } else {
                                    layoutStruct[pos].setEffWidth(new Length());
                                }
                            }
                        }
                    }
                }

                // make sure minWidth and maxWidth of the spanning cell are honoured
                if (cMinWidth > minWidth) {
                    if (allColsAreFixed) {
                        for (int pos = col; fixedWidth > 0 && pos < lastCol; pos++) {
                            final long cWidth = Math.max(layoutStruct[pos].effMinWidth(), cMinWidth
                                    * layoutStruct[pos].width().value() / fixedWidth);
                            fixedWidth -= layoutStruct[pos].width().value();
                            cMinWidth -= cWidth;
                            layoutStruct[pos].setEffMinWidth(cWidth);
                        }
                    } else if (allColsArePercent) {
                        int maxw = maxWidth;
                        int minw = minWidth;
                        final int cminw = cMinWidth;

                        for (int pos = col; maxw > 0 && pos < lastCol; pos++) {
                            if (layoutStruct[pos].effWidth().isPercent()
                                    && layoutStruct[pos].effWidth().value() > 0
                                    && fixedWidth <= cMinWidth) {
                                long cWidth = layoutStruct[pos].effMinWidth();
                                cWidth = Math.max(cWidth, cminw
                                        * layoutStruct[pos].effWidth().value() / totalPercent);
                                cWidth = Math.min(layoutStruct[pos].effMinWidth()
                                        + (cMinWidth - minw), cWidth);
                                maxw -= layoutStruct[pos].effMaxWidth();
                                minw -= layoutStruct[pos].effMinWidth();
                                cMinWidth -= cWidth;
                                layoutStruct[pos].setEffMinWidth(cWidth);
                            }
                        }
                    } else {
                        int maxw = maxWidth;
                        int minw = minWidth;

                        // Give min to variable first, to fixed second, and to
                        // others third.
                        for (int pos = col; maxw > 0 && pos < lastCol; pos++) {
                            if (layoutStruct[pos].width().isFixed() && haveVariable
                                    && fixedWidth <= cMinWidth) {
                                final long cWidth = Math.max(layoutStruct[pos].effMinWidth(),
                                        layoutStruct[pos].width().value());
                                fixedWidth -= layoutStruct[pos].width().value();
                                minw -= layoutStruct[pos].effMinWidth();
                                maxw -= layoutStruct[pos].effMaxWidth();
                                cMinWidth -= cWidth;
                                layoutStruct[pos].setEffMinWidth(cWidth);
                            }
                        }

                        for (int pos = col; maxw > 0 && pos < lastCol && minw < cMinWidth; pos++) {
                            if (!(layoutStruct[pos].width().isFixed() && haveVariable && fixedWidth <= cMinWidth)) {
                                long cWidth = Math.max(layoutStruct[pos].effMinWidth(), cMinWidth
                                        * layoutStruct[pos].effMaxWidth() / maxw);
                                cWidth = Math.min(layoutStruct[pos].effMinWidth()
                                        + (cMinWidth - minw), cWidth);

                                maxw -= layoutStruct[pos].effMaxWidth();
                                minw -= layoutStruct[pos].effMinWidth();
                                cMinWidth -= cWidth;
                                layoutStruct[pos].setEffMinWidth(cWidth);
                            }
                        }
                    }
                }

                if (!w.isPercent()) {
                    if (cMaxWidth > maxWidth) {
                        for (int pos = col; maxWidth > 0 && pos < lastCol; pos++) {
                            final long cWidth = Math.max(layoutStruct[pos].effMaxWidth(), cMaxWidth
                                    * layoutStruct[pos].effMaxWidth() / maxWidth);
                            maxWidth -= layoutStruct[pos].effMaxWidth();
                            cMaxWidth -= cWidth;
                            layoutStruct[pos].setEffMaxWidth(cWidth);
                        }
                    }
                } else {
                    for (int pos = col; pos < lastCol; pos++) {
                        layoutStruct[pos].setMaxWidth(Math.max(layoutStruct[pos].maxWidth(),
                                layoutStruct[pos].minWidth()));
                    }
                }
            }

            return tMaxWidth;
        }

        private boolean shouldScaleColumns(final TableBox table) {
            return true;
        }

        public void calcMinMaxWidth(final LayoutContext c) {
            final TableBox table = _table;

            fullRecalc(c);

            final Layout[] layoutStruct = _layoutStruct;

            final long spanMaxWidth = calcEffectiveWidth(c);
            long minWidth = 0;
            long maxWidth = 0;
            long maxPercent = 0;
            long maxNonPercent = 0;

            int remainingPercent = 100;
            for (int i = 0; i < layoutStruct.length; i++) {
                minWidth += layoutStruct[i].effMinWidth();
                maxWidth += layoutStruct[i].effMaxWidth();
                if (layoutStruct[i].effWidth().isPercent()) {
                    final long percent = Math.min(layoutStruct[i].effWidth().value(), remainingPercent);
                    final long pw = (layoutStruct[i].effMaxWidth() * 100) / Math.max(percent, 1);
                    remainingPercent -= percent;
                    maxPercent = Math.max(pw, maxPercent);
                } else {
                    maxNonPercent += layoutStruct[i].effMaxWidth();
                }
            }

            if (shouldScaleColumns(table)) {
                maxNonPercent = (maxNonPercent * 100 + 50) / Math.max(remainingPercent, 1);
                maxWidth = Math.max(maxNonPercent, maxWidth);
                maxWidth = Math.max(maxWidth, maxPercent);
            }

            maxWidth = Math.max(maxWidth, spanMaxWidth);

            final int bs = table.marginsBordersPaddingAndSpacing(c, true);
            minWidth += bs;
            maxWidth += bs;

            final Length tw = table.getStyle().asLength(c, CSSName.WIDTH);
            if (tw.isFixed() && tw.value() > 0) {
                table.calcDimensions(c);
                final int width = table.getContentWidth() + table.marginsBordersPaddingAndSpacing(c, true);
                minWidth = Math.max(minWidth, width);
                maxWidth = minWidth;
            }

            table.setMaxWidth((int)Math.min(maxWidth, Length.MAX_WIDTH));
            table.setMinWidth((int)Math.min(minWidth, Length.MAX_WIDTH));
        }


        public void layout(final LayoutContext c) {
            final TableBox table = _table;
            // table layout based on the values collected in the layout
            // structure.
            final int tableWidth = table.getWidth() - table.marginsBordersPaddingAndSpacing(c, false);
            int available = tableWidth;
            final int nEffCols = table.numEffCols();

            boolean havePercent = false;
            int numVariable = 0;
            int numFixed = 0;
            int totalVariable = 0;
            int totalFixed = 0;
            int totalPercent = 0;
            int allocVariable = 0;

            final Layout[] layoutStruct = _layoutStruct;

            // fill up every cell with it's minWidth
            for (int i = 0; i < nEffCols; i++) {
                final long w = layoutStruct[i].effMinWidth();
                layoutStruct[i].setCalcWidth(w);
                available -= w;
                final Length width = layoutStruct[i].effWidth();
                switch (width.type()) {
                case Length.PERCENT:
                    havePercent = true;
                    totalPercent += width.value();
                    break;
                case Length.FIXED:
                    numFixed++;
                    totalFixed += layoutStruct[i].effMaxWidth();
                    // fall through
                    break;
                case Length.VARIABLE:
                    numVariable++;
                    totalVariable += layoutStruct[i].effMaxWidth();
                    allocVariable += w;
                }
            }

            // allocate width to percent cols
            if (available > 0 && havePercent) {
                for (int i = 0; i < nEffCols; i++) {
                    final Length width = layoutStruct[i].effWidth();
                    if (width.isPercent()) {
                        final long w = Math.max(layoutStruct[i].effMinWidth(), width.minWidth(tableWidth));
                        available += layoutStruct[i].calcWidth() - w;
                        layoutStruct[i].setCalcWidth(w);
                    }
                }
                if (totalPercent > 100) {
                    // remove overallocated space from the last columns
                    int excess = tableWidth * (totalPercent - 100) / 100;
                    for (int i = nEffCols - 1; i >= 0; i--) {
                        if (layoutStruct[i].effWidth().isPercent()) {
                            final long w = layoutStruct[i].calcWidth();
                            final long reduction = Math.min(w, excess);
                            // the lines below might look inconsistent, but
                            // that's the way it's handled in mozilla
                            excess -= reduction;
                            final long newWidth = Math.max(layoutStruct[i].effMinWidth(), w - reduction);
                            available += w - newWidth;
                            layoutStruct[i].setCalcWidth(newWidth);
                            // qDebug("col %d: reducing to %d px
                            // (reduction=%d)", i, newWidth, reduction );
                        }
                    }
                }
            }

            // then allocate width to fixed cols
            if (available > 0) {
                for (int i = 0; i < nEffCols; ++i) {
                    final Length width = layoutStruct[i].effWidth();
                    if (width.isFixed() && width.value() > layoutStruct[i].calcWidth()) {
                        available += layoutStruct[i].calcWidth() - width.value();
                        layoutStruct[i].setCalcWidth(width.value());
                    }
                }
            }

            // now satisfy variable
            if (available > 0 && numVariable > 0) {
                available += allocVariable; // this gets redistributed
                // qDebug("redistributing %dpx to %d variable columns.
                // totalVariable=%d", available, numVariable, totalVariable );
                for (int i = 0; i < nEffCols; i++) {
                    final Length width = layoutStruct[i].effWidth();
                    if (width.isVariable() && totalVariable != 0) {
                        final long w = Math.max(layoutStruct[i].calcWidth(), available
                                * layoutStruct[i].effMaxWidth() / totalVariable);
                        available -= w;
                        totalVariable -= layoutStruct[i].effMaxWidth();
                        layoutStruct[i].setCalcWidth(w);
                    }
                }
            }

            // spread over fixed colums
            if (available > 0 && numFixed > 0) {
                // still have some width to spread, distribute to fixed columns
                for (int i = 0; i < nEffCols; i++) {
                    final Length width = layoutStruct[i].effWidth();
                    if (width.isFixed()) {
                        final long w = available * layoutStruct[i].effMaxWidth() / totalFixed;
                        available -= w;
                        totalFixed -= layoutStruct[i].effMaxWidth();
                        layoutStruct[i].setCalcWidth(layoutStruct[i].calcWidth() + w);
                    }
                }
            }

            // spread over percent colums
            if (available > 0 && havePercent && totalPercent < 100) {
                // still have some width to spread, distribute weighted to
                // percent columns
                for (int i = 0; i < nEffCols; i++) {
                    final Length width = layoutStruct[i].effWidth();
                    if (width.isPercent()) {
                        final long w = available * width.value() / totalPercent;
                        available -= w;
                        totalPercent -= width.value();
                        layoutStruct[i].setCalcWidth(layoutStruct[i].calcWidth() + w);
                        if (available == 0 || totalPercent == 0) {
                            break;
                        }
                    }
                }
            }

            // spread over the rest
            if (available > 0) {
                int total = nEffCols;
                // still have some width to spread
                int i = nEffCols;
                while (i-- > 0) {
                    final int w = available / total;
                    available -= w;
                    total--;
                    layoutStruct[i].setCalcWidth(layoutStruct[i].calcWidth() + w);
                }
            }

            // if we have overallocated, reduce every cell according to the
            // difference between desired width and minwidth
            // this seems to produce to the pixel exaxt results with IE. Wonder
            // is some of this also holds for width distributing.
            if (available < 0) {
                // Need to reduce cells with the following prioritization:
                // (1) Variable
                // (2) Relative
                // (3) Fixed
                // (4) Percent
                // This is basically the reverse of how we grew the cells.
                if (available < 0) {
                    int mw = 0;
                    for (int i = nEffCols - 1; i >= 0; i--) {
                        final Length width = layoutStruct[i].effWidth();
                        if (width.isVariable())
                            mw += layoutStruct[i].calcWidth() - layoutStruct[i].effMinWidth();
                    }

                    for (int i = nEffCols - 1; i >= 0 && mw > 0; i--) {
                        final Length width = layoutStruct[i].effWidth();
                        if (width.isVariable()) {
                            final long minMaxDiff = layoutStruct[i].calcWidth()
                                    - layoutStruct[i].effMinWidth();
                            final long reduce = available * minMaxDiff / mw;
                            layoutStruct[i].setCalcWidth(layoutStruct[i].calcWidth() + reduce);
                            available -= reduce;
                            mw -= minMaxDiff;
                            if (available >= 0)
                                break;
                        }
                    }
                }

                if (available < 0) {
                    int mw = 0;
                    for (int i = nEffCols - 1; i >= 0; i--) {
                        final Length width = layoutStruct[i].effWidth();
                        if (width.isFixed())
                            mw += layoutStruct[i].calcWidth() - layoutStruct[i].effMinWidth();
                    }

                    for (int i = nEffCols - 1; i >= 0 && mw > 0; i--) {
                        final Length width = layoutStruct[i].effWidth();
                        if (width.isFixed()) {
                            final long minMaxDiff = layoutStruct[i].calcWidth()
                                    - layoutStruct[i].effMinWidth();
                            final long reduce = available * minMaxDiff / mw;
                            layoutStruct[i].setCalcWidth(layoutStruct[i].calcWidth() + reduce);
                            available -= reduce;
                            mw -= minMaxDiff;
                            if (available >= 0)
                                break;
                        }
                    }
                }

                if (available < 0) {
                    int mw = 0;
                    for (int i = nEffCols - 1; i >= 0; i--) {
                        final Length width = layoutStruct[i].effWidth();
                        if (width.isPercent())
                            mw += layoutStruct[i].calcWidth() - layoutStruct[i].effMinWidth();
                    }

                    for (int i = nEffCols - 1; i >= 0 && mw > 0; i--) {
                        final Length width = layoutStruct[i].effWidth();
                        if (width.isPercent()) {
                            final long minMaxDiff = layoutStruct[i].calcWidth()
                                    - layoutStruct[i].effMinWidth();
                            final long reduce = available * minMaxDiff / mw;
                            layoutStruct[i].setCalcWidth(layoutStruct[i].calcWidth() + reduce);
                            available -= reduce;
                            mw -= minMaxDiff;
                            if (available >= 0)
                                break;
                        }
                    }
                }
            }

            int pos = 0;
            final int hspacing = _table.getStyle().getBorderHSpacing(c);
            final int[] columnPos = new int[nEffCols + 1];
            for (int i = 0; i < nEffCols; i++) {
                columnPos[i] = pos;
                pos += layoutStruct[i].calcWidth() + hspacing;
            }

            columnPos[columnPos.length - 1] = pos;

            _table.setColumnPos(columnPos);
        }

        protected static class Layout {
            private Length _width = new Length();
            private Length _effWidth = new Length();
            private long _minWidth = 1;
            private long _maxWidth = 1;
            private long _effMinWidth = 0;
            private long _effMaxWidth = 0;
            private long _calcWidth = 0;

            public Layout() {
            }

            public Length width() {
                return _width;
            }

            public void setWidth(final Length l) {
                _width = l;
            }

            public Length effWidth() {
                return _effWidth;
            }

            public void setEffWidth(final Length l) {
                _effWidth = l;
            }

            public long minWidth() {
                return _minWidth;
            }

            public void setMinWidth(final long i) {
                _minWidth = i;
            }

            public long maxWidth() {
                return _maxWidth;
            }

            public void setMaxWidth(final long i) {
                _maxWidth = i;
            }

            public long effMinWidth() {
                return _effMinWidth;
            }

            public void setEffMinWidth(final long i) {
                _effMinWidth = i;
            }

            public long effMaxWidth() {
                return _effMaxWidth;
            }

            public void setEffMaxWidth(final long i) {
                _effMaxWidth = i;
            }

            public long calcWidth() {
                return _calcWidth;
            }

            public void setCalcWidth(final long i) {
                _calcWidth = i;
            }
        };
    }
}

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.RenderingContext;

public class TableSectionBox extends BlockBox {
    private List<RowData> _grid = new ArrayList<RowData>();
    
    private boolean _needCellWidthCalc;
    private boolean _needCellRecalc;
    
    private boolean _footer;
    private boolean _header;
    
    private boolean _capturedOriginalAbsY;
    private int _originalAbsY;
    
    public TableSectionBox() {
    }
    
    public BlockBox copyOf() {
        final TableSectionBox result = new TableSectionBox();
        result.setStyle(getStyle());
        result.setElement(getElement());
        
        return result;
    }
    
    public List<RowData> getGrid() {
        return _grid;
    }

    public void setGrid(final List<RowData> grid) {
        _grid = grid;
    }
    
    public void extendGridToColumnCount(final int columnCount) {
        for (final Iterator<RowData> i = _grid.iterator(); i.hasNext(); ) {
            final RowData row = i.next();
            row.extendToColumnCount(columnCount);
        }
    }
    
    public void splitColumn(final int pos) {
        for (final Iterator<RowData> i = _grid.iterator(); i.hasNext(); ) {
            final RowData row = i.next();
            row.splitColumn(pos);
        }
    }
    
    public void recalcCells(final LayoutContext c) {
        int cRow = 0;
        _grid.clear();
        ensureChildren(c);
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); cRow++) {
            final TableRowBox row = (TableRowBox)i.next();
            row.ensureChildren(c);
            for (final Iterator<Box> j = row.getChildIterator(); j.hasNext(); ) {
                final TableCellBox cell = (TableCellBox)j.next();
                addCell(row, cell, cRow);
            }
        }
    }
    
    public void calcBorders(final LayoutContext c) {
        ensureChildren(c);
        for (final Iterator<Box> i = getChildIterator(); i.hasNext(); ) {
            final TableRowBox row = (TableRowBox)i.next();
            row.ensureChildren(c);
            for (final Iterator<Box> j = row.getChildIterator(); j.hasNext(); ) {
                final TableCellBox cell = (TableCellBox)j.next();
                cell.calcCollapsedBorder(c);
            }
        }
    }
    
    public TableCellBox cellAt(final int row, final int col) {
        if (row >= _grid.size()) return null;
        final RowData rowData = _grid.get(row);
        if (col >= rowData.getRow().size()) return null;
        return (TableCellBox)rowData.getRow().get(col);
    }
    
    private void setCellAt(final int row, final int col, final TableCellBox cell) {
        _grid.get(row).getRow().set(col, cell);
    }
    
    private void ensureRows(final int numRows) {
        int nRows = _grid.size();
        final int nCols = getTable().numEffCols();
        
        while (nRows < numRows) {
            final RowData row = new RowData();
            row.extendToColumnCount(nCols);
            _grid.add(row);
            nRows++;
        }
    }
    
    private TableBox getTable() {
        return (TableBox)getParent();
    }
    
    protected void layoutChildren(final LayoutContext c, final int contentStart) {
        if (isNeedCellRecalc()) {
            recalcCells(c);
            setNeedCellRecalc(false);
        }
        
        if (isNeedCellWidthCalc()) {
            setCellWidths(c);
            setNeedCellWidthCalc(false);
        }
        
        super.layoutChildren(c, contentStart);
    }
    
    private void addCell(final TableRowBox row, final TableCellBox cell, final int cRow) {
        final int rSpan = cell.getStyle().getRowSpan();
        int cSpan = cell.getStyle().getColSpan();
        
        final List<ColumnData> columns = getTable().getColumns();
        final int nCols = columns.size();
        int cCol = 0;
        
        ensureRows(cRow + rSpan);
        
        while ( cCol < nCols && cellAt(cRow, cCol) != null) {
            cCol++;
        }
        
        final int col = cCol;
        TableCellBox set = cell;
        while (cSpan > 0) {
            int currentSpan;
            while ( cCol >= getTable().getColumns().size() ) {
                getTable().appendColumn(1);
            }
            ColumnData cData = (ColumnData)columns.get(cCol);
            if (cSpan < cData.getSpan()) {
                getTable().splitColumn(cCol, cSpan);
            }
            cData = (ColumnData)columns.get(cCol);
            currentSpan = cData.getSpan();
            
            int r = 0;
            while (r < rSpan) {
                if (cellAt(cRow + r, cCol) == null) {
                    setCellAt(cRow + r, cCol, set);
                }
                r++;
            }
            cCol++;
            cSpan -= currentSpan;
            set = TableCellBox.SPANNING_CELL;
        }
        
        cell.setRow(cRow);
        cell.setCol(getTable().effColToCol(col));
    }
    
    public void reset(final LayoutContext c) {
        super.reset(c);
        _grid.clear();
        setNeedCellWidthCalc(true);
        setNeedCellRecalc(true);
        setCapturedOriginalAbsY(false);
    }
    
    void setCellWidths(final LayoutContext c)
    {
        final int[] columnPos = getTable().getColumnPos();
        
        for (final Iterator<RowData> i = _grid.iterator(); i.hasNext(); ) {
            final RowData row = i.next();
            final List<TableCellBox> cols = row.getRow();
            final int hspacing = getTable().getStyle().getBorderHSpacing(c);
            for (int j = 0; j < cols.size(); j++) {
                final TableCellBox cell = (TableCellBox)cols.get(j);
                
                if (cell == null || cell == TableCellBox.SPANNING_CELL) {
                    continue;
                }
                
                int endCol = j;
                int cspan = cell.getStyle().getColSpan();
                while (cspan > 0 && endCol < cols.size()) {
                    cspan -= getTable().spanOfEffCol(endCol);
                    endCol++;
                }
                
                final int w = columnPos[endCol] - columnPos[j] - hspacing;
                cell.setLayoutWidth(c, w);
                cell.setX(columnPos[j] + hspacing);
            }
        }
    }
    
    public boolean isAutoHeight() {
        // FIXME Should properly handle absolute heights (%s resolve to auto)
        return true;
    }
    
    public int numRows() { 
        return _grid.size(); 
    }
    
    protected boolean isSkipWhenCollapsingMargins() {
        return true;
    }
    
    public void paintBorder(final RenderingContext c) {
        // row groups never have borders
    }
    
    public void paintBackground(final RenderingContext c) {
        // painted at the cell level
    }
    
    public TableRowBox getLastRow() {
        if (getChildCount() > 0) {
            return (TableRowBox)getChild(getChildCount()-1);
        } else {
            return null;
        }
    }

    boolean isNeedCellWidthCalc() {
        return _needCellWidthCalc;
    }

    void setNeedCellWidthCalc(final boolean needCellWidthCalc) {
        _needCellWidthCalc = needCellWidthCalc;
    }

    private boolean isNeedCellRecalc() {
        return _needCellRecalc;
    }

    private void setNeedCellRecalc(final boolean needCellRecalc) {
        _needCellRecalc = needCellRecalc;
    }
    
    public void layout(final LayoutContext c, final int contentStart) {
        final boolean running = c.isPrint() && (isHeader() || isFooter()) && getTable().getStyle().isPaginateTable();
        
        if (running) {
            c.setNoPageBreak(c.getNoPageBreak()+1);
        }
        
        super.layout(c, contentStart);
        
        if (running) {
            c.setNoPageBreak(c.getNoPageBreak()-1);
        }
    }

    public boolean isFooter() {
        return _footer;
    }

    public void setFooter(final boolean footer) {
        _footer = footer;
    }

    public boolean isHeader() {
        return _header;
    }

    public void setHeader(final boolean header) {
        _header = header;
    }

    public boolean isCapturedOriginalAbsY() {
        return _capturedOriginalAbsY;
    }

    public void setCapturedOriginalAbsY(final boolean capturedOriginalAbsY) {
        _capturedOriginalAbsY = capturedOriginalAbsY;
    }

    public int getOriginalAbsY() {
        return _originalAbsY;
    }

    public void setOriginalAbsY(final int originalAbsY) {
        _originalAbsY = originalAbsY;
    }
}

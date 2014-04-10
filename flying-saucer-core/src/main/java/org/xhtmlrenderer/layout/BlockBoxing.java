/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
 * Copyright (c) 2005 Wisconsin Court System
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
package org.xhtmlrenderer.layout;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.render.PageBox;

/**
 * Utility class for laying block content.  It is called when a block box
 * contains block level content.  {@link BoxBuilder} will have made sure that
 * the block we're working on will either contain only inline or block content.
 * If we're in a paged media environment, the various page break related
 * properties are also handled here.  If a rule is violated, the affected run
 * of boxes will be layed out again.  If the rule still cannot be satisfied,
 * the rule will be dropped.
 */
public class BlockBoxing {
    private static final int NO_PAGE_TRIM = -1;

    private BlockBoxing() {
    }

    public static void layoutContent(final LayoutContext c, final BlockBox block, final int contentStart) {
        int offset = -1;

        List<Box> localChildren = block.getChildren();
        if (c.isPrint() && ! (localChildren instanceof RandomAccess)) {
            localChildren = new ArrayList<Box>(localChildren);
        }

        int childOffset = block.getHeight() + contentStart;

        RelayoutDataList relayoutDataList = null;
        if (c.isPrint()) {
            relayoutDataList = new RelayoutDataList(localChildren.size());
        }

        int pageCount = NO_PAGE_TRIM;
        BlockBox previousChildBox = null;
        for (final Box box : localChildren) {
            final BlockBox child = (BlockBox) box;
            offset++;

            RelayoutData relayoutData = null;

            boolean mayCheckKeepTogether = false;
            if (c.isPrint()) {
                relayoutData = relayoutDataList.get(offset);
                relayoutData.setLayoutState(c.copyStateForRelayout());
                relayoutData.setChildOffset(childOffset);
                pageCount = c.getRootLayer().getPages().size();

                child.setNeedPageClear(false);

                if ((child.getStyle().isAvoidPageBreakInside() || child.getStyle().isKeepWithInline())
                        && c.isMayCheckKeepTogether()) {
                    mayCheckKeepTogether = true;
                    c.setMayCheckKeepTogether(false);
                }
            }

            layoutBlockChild(
                    c, block, child, false, childOffset, NO_PAGE_TRIM,
                    relayoutData == null ? null : relayoutData.getLayoutState());

            if (c.isPrint()) {
                final boolean needPageClear = child.isNeedPageClear();
                if (needPageClear || mayCheckKeepTogether) {
                    c.setMayCheckKeepTogether(mayCheckKeepTogether);
                    final boolean tryToAvoidPageBreak = child.getStyle().isAvoidPageBreakInside() && child.crossesPageBreak(c);
                    final boolean keepWithInline = child.isNeedsKeepWithInline(c);
                    if (tryToAvoidPageBreak || needPageClear || keepWithInline) {
                        c.restoreStateForRelayout(relayoutData.getLayoutState());
                        child.reset(c);
                        layoutBlockChild(
                                c, block, child, true, childOffset, pageCount, relayoutData.getLayoutState());

                        if (tryToAvoidPageBreak && child.crossesPageBreak(c) && ! keepWithInline) {
                            c.restoreStateForRelayout(relayoutData.getLayoutState());
                            child.reset(c);
                            layoutBlockChild(
                                    c, block, child, false, childOffset, pageCount, relayoutData.getLayoutState());
                        }
                    }
                }
                c.getRootLayer().ensureHasPage(c, child);
            }

            final Dimension relativeOffset = child.getRelativeOffset();
            if (relativeOffset == null) {
                childOffset = child.getY() + child.getHeight();
            } else {
                // Box will have been positioned by this point so calculate
                // relative to where it would have been if it hadn't been
                // moved
                childOffset = child.getY() - relativeOffset.height + child.getHeight();
            }

            if (childOffset > block.getHeight()) {
                block.setHeight(childOffset);
            }

            if (c.isPrint()) {
                if (child.getStyle().isForcePageBreakAfter()) {
                    block.forcePageBreakAfter(c, child.getStyle().getIdent(CSSName.PAGE_BREAK_AFTER));
                    childOffset = block.getHeight();
                }

                if (previousChildBox != null) {
                    relayoutDataList.markRun(offset, previousChildBox, child);
                }

                final RelayoutRunResult runResult =
                        processPageBreakAvoidRun(
                                c, block, localChildren, offset, relayoutDataList, relayoutData, child);
                if (runResult.isChanged()) {
                    childOffset = runResult.getChildOffset();
                    if (childOffset > block.getHeight()) {
                        block.setHeight(childOffset);
                    }
                }
            }

            previousChildBox = child;
        }
    }

    private static RelayoutRunResult processPageBreakAvoidRun(final LayoutContext c, final BlockBox block,
                                                              final List<Box> localChildren, final int offset,
                                                              final RelayoutDataList relayoutDataList, final RelayoutData relayoutData,
                                                              final BlockBox childBox) {
        final RelayoutRunResult result = new RelayoutRunResult();
        if (offset > 0) {
            boolean mightNeedRelayout = false;
            int runEnd = -1;
            if (offset == localChildren.size() - 1 && relayoutData.isEndsRun()) {
                mightNeedRelayout = true;
                runEnd = offset;
            } else if (offset > 0) {
                final RelayoutData previousRelayoutData = relayoutDataList.get(offset - 1);
                if (previousRelayoutData.isEndsRun()) {
                    mightNeedRelayout = true;
                    runEnd = offset - 1;
                }
            }
            if (mightNeedRelayout) {
                final int runStart = relayoutDataList.getRunStart(runEnd);
                if ( isPageBreakBetweenChildBoxes(relayoutDataList, runStart, runEnd, c, block) ) {
                    result.setChanged(true);
                    block.resetChildren(c, runStart, offset);
                    result.setChildOffset(relayoutRun(c, localChildren, block,
                            relayoutDataList, runStart, offset, true));
                    if ( isPageBreakBetweenChildBoxes(relayoutDataList, runStart, runEnd, c, block) ) {
                        block.resetChildren(c, runStart, offset);
                        result.setChildOffset(relayoutRun(c, localChildren, block,
                                relayoutDataList, runStart, offset, false));
                    }
                }
            }
        }
        return result;
    }

    private static boolean isPageBreakBetweenChildBoxes(final RelayoutDataList relayoutDataList,
            final int runStart, final int runEnd, final LayoutContext c, final BlockBox block) {
        for ( int i = runStart; i < runEnd; i++ ) {
            final Box prevChild = block.getChild(i);
            final Box nextChild = block.getChild(i+1);
            // if nextChild is made of several lines, then only the first line
            // is relevant for "page-break-before: avoid".
            final Box nextLine = getFirstLine(nextChild) == null ? nextChild : getFirstLine(nextChild);
            final int prevChildEnd = prevChild.getAbsY() + prevChild.getHeight();
            final int nextLineEnd = nextLine.getAbsY() + nextLine.getHeight();
            if ( c.getRootLayer().crossesPageBreak(c, prevChildEnd, nextLineEnd) ) {
                return true;
            }
        }
        return false;
    }

    private static LineBox getFirstLine(final Box box) {
        for ( Box child = box; child.getChildCount()>0; child = child.getChild(0) ) {
            if ( child instanceof LineBox ) {
                return (LineBox) child;
            }
        }
        return null;
    }

    private static int relayoutRun(
            final LayoutContext c, final List<Box> localChildren, final BlockBox block,
            final RelayoutDataList relayoutDataList, final int start, final int end, final boolean onNewPage) {
        int childOffset = relayoutDataList.get(start).getChildOffset();

        if (onNewPage) {
            final Box startBox = (Box) localChildren.get(start);
            final PageBox startPageBox = c.getRootLayer().getFirstPage(c, startBox);
            childOffset += startPageBox.getBottom() - startBox.getAbsY();
        }

        // reset height of parent as it is used for Y-setting of children
        block.setHeight(childOffset);


        for (int i = start; i <= end; i++) {
            final BlockBox child = (BlockBox) localChildren.get(i);

            final RelayoutData relayoutData = relayoutDataList.get(i);

            final int pageCount = c.getRootLayer().getPages().size();

            //TODO:handle run-ins. For now, treat them as blocks

            c.restoreStateForRelayout(relayoutData.getLayoutState());
            relayoutData.setChildOffset(childOffset);
            boolean mayCheckKeepTogether = false;
            if ((child.getStyle().isAvoidPageBreakInside() || child.getStyle().isKeepWithInline())
                    && c.isMayCheckKeepTogether()) {
                mayCheckKeepTogether = true;
                c.setMayCheckKeepTogether(false);
            }
            layoutBlockChild(
                    c, block, child, false, childOffset, NO_PAGE_TRIM, relayoutData.getLayoutState());

            if (mayCheckKeepTogether) {
                c.setMayCheckKeepTogether(true);
                final boolean tryToAvoidPageBreak =
                    child.getStyle().isAvoidPageBreakInside() && child.crossesPageBreak(c);
                final boolean needPageClear = child.isNeedPageClear();
                final boolean keepWithInline = child.isNeedsKeepWithInline(c);
                if (tryToAvoidPageBreak || needPageClear || keepWithInline) {
                    c.restoreStateForRelayout(relayoutData.getLayoutState());
                    child.reset(c);
                    layoutBlockChild(
                            c, block, child, true, childOffset, pageCount, relayoutData.getLayoutState());

                    if (tryToAvoidPageBreak && child.crossesPageBreak(c) && ! keepWithInline) {
                        c.restoreStateForRelayout(relayoutData.getLayoutState());
                        child.reset(c);
                        layoutBlockChild(
                                c, block, child, false, childOffset, pageCount, relayoutData.getLayoutState());
                    }
                }
            }

            c.getRootLayer().ensureHasPage(c, child);

            final Dimension relativeOffset = child.getRelativeOffset();
            if (relativeOffset == null) {
                childOffset = child.getY() + child.getHeight();
            } else {
                childOffset = child.getY() - relativeOffset.height + child.getHeight();
            }

            if (childOffset > block.getHeight()) {
                block.setHeight(childOffset);
            }

            if (child.getStyle().isForcePageBreakAfter()) {
                block.forcePageBreakAfter(c, child.getStyle().getIdent(CSSName.PAGE_BREAK_AFTER));
                childOffset = block.getHeight();
            }
        }

        return childOffset;
    }

    private static void layoutBlockChild(
            final LayoutContext c, final BlockBox parent, final BlockBox child,
            final boolean needPageClear, final int childOffset, final int trimmedPageCount, final LayoutState layoutState) {
        layoutBlockChild0(c, parent, child, needPageClear, childOffset, trimmedPageCount);
        final BreakAtLineContext bContext = child.calcBreakAtLineContext(c);
        if (bContext != null) {
            c.setBreakAtLineContext(bContext);
            c.restoreStateForRelayout(layoutState);
            child.reset(c);
            layoutBlockChild0(c, parent, child, needPageClear, childOffset, trimmedPageCount);
            c.setBreakAtLineContext(null);
        }
    }

    private static void layoutBlockChild0(final LayoutContext c, final BlockBox parent, final BlockBox child,
            final boolean needPageClear, final int childOffset, final int trimmedPageCount) {
        child.setNeedPageClear(needPageClear);

        child.initStaticPos(c, parent, childOffset);

        child.initContainingLayer(c);
        child.calcCanvasLocation();

        c.translate(0, childOffset);
        repositionBox(c, child, trimmedPageCount);
        child.layout(c);
        c.translate(-child.getX(), -child.getY());
    }

    private static void repositionBox(final LayoutContext c, final BlockBox child, final int trimmedPageCount) {
        boolean moved = false;
        if (child.getStyle().isRelative()) {
            final Dimension delta = child.positionRelative(c);
            c.translate(delta.width, delta.height);
            moved = true;
        }
        if (c.isPrint()) {
            final boolean pageClear = child.isNeedPageClear() ||
                                    child.getStyle().isForcePageBreakBefore();
            final boolean needNewPageContext = child.checkPageContext(c);

            if (needNewPageContext && trimmedPageCount != NO_PAGE_TRIM) {
                c.getRootLayer().trimPageCount(trimmedPageCount);
            }

            if (pageClear || needNewPageContext) {
                final int delta = child.forcePageBreakBefore(
                        c,
                        child.getStyle().getIdent(CSSName.PAGE_BREAK_BEFORE),
                        needNewPageContext);
                c.translate(0, delta);
                moved = true;
                child.setNeedPageClear(false);
            }
        }
        if (moved) {
            child.calcCanvasLocation();
        }
    }

    private static class RelayoutDataList {
        private final List<RelayoutData> _hints;

        public RelayoutDataList(final int size) {
            _hints = new ArrayList<RelayoutData>(size);
            for (int i = 0; i < size; i++) {
                _hints.add(new RelayoutData());
            }
        }

        public RelayoutData get(final int index) {
            return _hints.get(index);
        }

        public void markRun(final int offset, final BlockBox previous, final BlockBox current) {
            final RelayoutData previousData = get(offset - 1);
            final RelayoutData currentData = get(offset);

            final IdentValue previousAfter =
                    previous.getStyle().getIdent(CSSName.PAGE_BREAK_AFTER);
            final IdentValue currentBefore =
                    current.getStyle().getIdent(CSSName.PAGE_BREAK_BEFORE);

            if ((previousAfter == IdentValue.AVOID && currentBefore == IdentValue.AUTO) ||
                    (previousAfter == IdentValue.AUTO && currentBefore == IdentValue.AVOID) ||
                    (previousAfter == IdentValue.AVOID && currentBefore == IdentValue.AVOID)) {
                if (! previousData.isInRun()) {
                    previousData.setStartsRun(true);
                }
                previousData.setInRun(true);
                currentData.setInRun(true);

                if (offset == _hints.size() - 1) {
                    currentData.setEndsRun(true);
                }
            } else {
                if (previousData.isInRun()) {
                    previousData.setEndsRun(true);
                }
            }
        }

        public int getRunStart(final int runEnd) {
            int offset = runEnd;
            RelayoutData current = get(offset);
            if (! current.isEndsRun()) {
                throw new RuntimeException("Not the end of a run");
            }
            while (! current.isStartsRun()) {
                current = get(--offset);
            }
            return offset;
        }
    }

    private static class RelayoutRunResult {
        private boolean _changed;
        private int _childOffset;

        public boolean isChanged() {
            return _changed;
        }

        public void setChanged(final boolean changed) {
            _changed = changed;
        }

        public int getChildOffset() {
            return _childOffset;
        }

        public void setChildOffset(final int childOffset) {
            _childOffset = childOffset;
        }
    }

    private static class RelayoutData {
        private LayoutState _layoutState;
        private int _listIndex;

        private boolean _startsRun;
        private boolean _endsRun;
        private boolean _inRun;

        private int _childOffset;

        public RelayoutData() {
        }

        public boolean isEndsRun() {
            return _endsRun;
        }

        public void setEndsRun(final boolean endsRun) {
            _endsRun = endsRun;
        }

        public boolean isInRun() {
            return _inRun;
        }

        public void setInRun(final boolean inRun) {
            _inRun = inRun;
        }

        public LayoutState getLayoutState() {
            return _layoutState;
        }

        public void setLayoutState(final LayoutState layoutState) {
            _layoutState = layoutState;
        }

        public boolean isStartsRun() {
            return _startsRun;
        }

        public void setStartsRun(final boolean startsRun) {
            _startsRun = startsRun;
        }

        public int getChildOffset() {
            return _childOffset;
        }

        public void setChildOffset(final int childOffset) {
            _childOffset = childOffset;
        }

        @SuppressWarnings("unused")
		public int getListIndex() {
            return _listIndex;
        }

        @SuppressWarnings("unused")
		public void setListIndex(final int listIndex) {
            _listIndex = listIndex;
        }
    }
}

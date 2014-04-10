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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.FSDerivedValue;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.css.style.derived.RectPropertySet;
import org.xhtmlrenderer.render.AnonymousBlockBox;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.FloatDistances;
import org.xhtmlrenderer.render.InlineBox;
import org.xhtmlrenderer.render.InlineLayoutBox;
import org.xhtmlrenderer.render.InlineText;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.render.MarkerData;
import org.xhtmlrenderer.render.StrutMetrics;
import org.xhtmlrenderer.render.TextDecoration;

/**
 * This class is responsible for flowing inline content into lines.  Block
 * content which participates in an inline formatting context is also handled
 * here as well as floating and absolutely positioned content.
 */
public class InlineBoxing {
    private InlineBoxing() {
    }

    public static void layoutContent(final LayoutContext c, final BlockBox box, final int initialY, final int breakAtLine) {
        final int maxAvailableWidth = box.getContentWidth();
        int remainingWidth = maxAvailableWidth;

        LineBox currentLine = newLine(c, initialY, box);
        LineBox previousLine = null;

        InlineLayoutBox currentIB = null;
        InlineLayoutBox previousIB = null;

        int contentStart = 0;

        List<InlineBox> openInlineBoxes = null;

        final Map<InlineBox, InlineLayoutBox> iBMap = new HashMap<InlineBox, InlineLayoutBox>();

        if (box instanceof AnonymousBlockBox) {
            openInlineBoxes = ((AnonymousBlockBox)box).getOpenInlineBoxes();
            if (openInlineBoxes != null) {
                openInlineBoxes = new ArrayList<InlineBox>(openInlineBoxes);
                currentIB = addOpenInlineBoxes(
                        c, currentLine, openInlineBoxes, maxAvailableWidth, iBMap);
            }
        }

        if (openInlineBoxes == null) {
            openInlineBoxes = new ArrayList<InlineBox>();
        }

        remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, currentLine, remainingWidth);

        final CalculatedStyle parentStyle = box.getStyle();
        final int minimumLineHeight = (int) parentStyle.getLineHeight(c);
        final int indent = (int) parentStyle.getFloatPropertyProportionalWidth(CSSName.TEXT_INDENT, maxAvailableWidth, c);
        remainingWidth -= indent;
        contentStart += indent;

        MarkerData markerData = c.getCurrentMarkerData();
        if (markerData != null && box.getStyle().isListMarkerInside()) {
            remainingWidth -= markerData.getLayoutWidth();
            contentStart += markerData.getLayoutWidth();
        }
        c.setCurrentMarkerData(null);

        final List<FloatLayoutResult> pendingFloats = new ArrayList<FloatLayoutResult>();
        int pendingLeftMBP = 0;
        int pendingRightMBP = 0;

        boolean hasFirstLinePEs = false;
        final List<Layer> pendingInlineLayers = new ArrayList<Layer>();

        if (c.getFirstLinesTracker().hasStyles()) {
            box.styleText(c, c.getFirstLinesTracker().deriveAll(box.getStyle()));
            hasFirstLinePEs = true;
        }

        boolean needFirstLetter = c.getFirstLettersTracker().hasStyles();
        boolean zeroWidthInlineBlock = false;

        int lineOffset = 0;

        for (final Styleable styleable : box.getInlineContent()) {
            final Styleable node = (Styleable)styleable;

            if (node.getStyle().isInline()) {
                final InlineBox iB = (InlineBox)node;

                final CalculatedStyle style = iB.getStyle();
                if (iB.isStartsHere()) {
                    previousIB = currentIB;
                    currentIB = new InlineLayoutBox(c, iB.getElement(), style, maxAvailableWidth);

                    openInlineBoxes.add(iB);
                    iBMap.put(iB, currentIB);

                    if (previousIB == null) {
                        currentLine.addChildForLayout(c, currentIB);
                    } else {
                        previousIB.addInlineChild(c, currentIB);
                    }

                    if (currentIB.getElement() != null) {
                        final String name = c.getNamespaceHandler().getAnchorName(currentIB.getElement());
                        if (name != null) {
                            c.addBoxId(name, currentIB);
                        }
                        final String id = c.getNamespaceHandler().getID(currentIB.getElement());
                        if (id != null) {
                            c.addBoxId(id, currentIB);
                        }
                    }

                    //To break the line well, assume we don't just want to paint padding on next line
                    pendingLeftMBP += style.getMarginBorderPadding(
                            c, maxAvailableWidth, CalculatedStyle.LEFT);
                    pendingRightMBP += style.getMarginBorderPadding(
                            c, maxAvailableWidth, CalculatedStyle.RIGHT);
                }

                final LineBreakContext lbContext = new LineBreakContext();
                lbContext.setMaster(iB.getText());
                lbContext.setTextNode(iB.getTextNode());
                if (iB.isDynamicFunction()) {
                    lbContext.setMaster(iB.getContentFunction().getLayoutReplacementText());
                }

                do {
                    lbContext.reset();

                    int fit = 0;
                    if (lbContext.getStart() == 0) {
                        fit += pendingLeftMBP + pendingRightMBP;
                    }

                    boolean trimmedLeadingSpace = false;
                    if (hasTrimmableLeadingSpace(
                            currentLine, style, lbContext, zeroWidthInlineBlock)) {
                        trimmedLeadingSpace = true;
                        trimLeadingSpace(lbContext);
                    }

                    lbContext.setEndsOnNL(false);

                    zeroWidthInlineBlock = false;

                    if (lbContext.getStartSubstring().length() == 0) {
                        break;
                    }

                    if (needFirstLetter && !lbContext.isFinished()) {
                        final InlineLayoutBox firstLetter =
                            addFirstLetterBox(c, currentLine, currentIB, lbContext,
                                    maxAvailableWidth, remainingWidth);
                        remainingWidth -= firstLetter.getInlineWidth();

                        if (currentIB.isStartsHere()) {
                            pendingLeftMBP -= currentIB.getStyle().getMarginBorderPadding(
                                    c, maxAvailableWidth, CalculatedStyle.LEFT);
                        }

                        needFirstLetter = false;
                    } else {
                        lbContext.saveEnd();
                        final InlineText inlineText = layoutText(
                                c, iB.getStyle(), remainingWidth - fit, lbContext, false);
                        if (lbContext.isUnbreakable() && ! currentLine.isContainsContent()) {
                            final int delta = c.getBlockFormattingContext().getNextLineBoxDelta(c, currentLine, maxAvailableWidth);
                            if (delta > 0) {
                                currentLine.setY(currentLine.getY() + delta);
                                currentLine.calcCanvasLocation();
                                remainingWidth = maxAvailableWidth;
                                remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, currentLine, maxAvailableWidth);
                                lbContext.resetEnd();
                                continue;
                            }
                        }

                        if (!lbContext.isUnbreakable() ||
                                (lbContext.isUnbreakable() && ! currentLine.isContainsContent())) {
                            if (iB.isDynamicFunction()) {
                                inlineText.setFunctionData(new FunctionData(
                                        iB.getContentFunction(), iB.getFunction()));
                            }
                            inlineText.setTrimmedLeadingSpace(trimmedLeadingSpace);
                            currentLine.setContainsDynamicFunction(inlineText.isDynamicFunction());
                            currentIB.addInlineChild(c, inlineText);
                            currentLine.setContainsContent(true);
                            lbContext.setStart(lbContext.getEnd());
                            remainingWidth -= inlineText.getWidth();

                            if (currentIB.isStartsHere()) {
                                final int marginBorderPadding =
                                    currentIB.getStyle().getMarginBorderPadding(
                                        c, maxAvailableWidth, CalculatedStyle.LEFT);
                                pendingLeftMBP -= marginBorderPadding;
                                remainingWidth -= marginBorderPadding;
                            }
                        } else {
                            lbContext.resetEnd();
                        }
                    }

                    if (lbContext.isNeedsNewLine()) {
                        saveLine(currentLine, c, box, minimumLineHeight,
                                maxAvailableWidth, pendingFloats,
                                hasFirstLinePEs, pendingInlineLayers, markerData,
                                contentStart, isAlwaysBreak(c, box, breakAtLine, lineOffset));
                        lineOffset++;
                        markerData = null;
                        contentStart = 0;
                        if (currentLine.isFirstLine() && hasFirstLinePEs) {
                            lbContext.setMaster(TextUtil.transformText(iB.getText(), iB.getStyle()));
                        }
                        previousLine = currentLine;
                        currentLine = newLine(c, previousLine, box);
                        currentIB = addOpenInlineBoxes(
                                c, currentLine, openInlineBoxes,  maxAvailableWidth, iBMap);
                        previousIB = currentIB.getParent() instanceof LineBox ?
                                null : (InlineLayoutBox) currentIB.getParent();
                        remainingWidth = maxAvailableWidth;
                        remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, currentLine, remainingWidth);
                    }
                } while (!lbContext.isFinished());

                if (iB.isEndsHere()) {
                    final int rightMBP = style.getMarginBorderPadding(
                            c, maxAvailableWidth, CalculatedStyle.RIGHT);

                    pendingRightMBP -= rightMBP;
                    remainingWidth -= rightMBP;

                    openInlineBoxes.remove(openInlineBoxes.size() - 1);

                    if (currentIB.isPending()) {
                        currentIB.unmarkPending(c);

                        // Reset to correct value
                        currentIB.setStartsHere(iB.isStartsHere());
                    }

                    currentIB.setEndsHere(true);

                    if (currentIB.getStyle().requiresLayer()) {
                        if (! currentIB.isPending() && (currentIB.getElement() == null ||
                                currentIB.getElement() != c.getLayer().getMaster().getElement())) {
                            throw new RuntimeException("internal error");
                        }
                        if (! currentIB.isPending()) {
                            c.getLayer().setEnd(currentIB);
                            c.popLayer();
                            pendingInlineLayers.add(currentIB.getContainingLayer());
                        }
                    }

                    previousIB = currentIB;
                    currentIB = currentIB.getParent() instanceof LineBox ?
                            null : (InlineLayoutBox) currentIB.getParent();
                }
            } else {
               final BlockBox child = (BlockBox)node;

               if (child.getStyle().isNonFlowContent()) {
                   remainingWidth -= processOutOfFlowContent(
                           c, currentLine, child, remainingWidth, pendingFloats);
               } else if (child.getStyle().isInlineBlock() || child.getStyle().isInlineTable()) {
                   layoutInlineBlockContent(c, box, child, initialY);

                   if (child.getWidth() > remainingWidth && currentLine.isContainsContent()) {
                       saveLine(currentLine, c, box, minimumLineHeight,
                               maxAvailableWidth, pendingFloats,  hasFirstLinePEs,
                               pendingInlineLayers, markerData, contentStart,
                               isAlwaysBreak(c, box, breakAtLine, lineOffset));
                       lineOffset++;
                       markerData = null;
                       contentStart = 0;
                       previousLine = currentLine;
                       currentLine = newLine(c, previousLine, box);
                       currentIB = addOpenInlineBoxes(
                               c, currentLine, openInlineBoxes, maxAvailableWidth, iBMap);
                       previousIB = currentIB == null || currentIB.getParent() instanceof LineBox ?
                               null : (InlineLayoutBox) currentIB.getParent();
                       remainingWidth = maxAvailableWidth;
                       remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, currentLine, remainingWidth);

                       child.reset(c);
                       layoutInlineBlockContent(c, box, child, initialY);
                   }

                   if (currentIB == null) {
                       currentLine.addChildForLayout(c, child);
                   } else {
                       currentIB.addInlineChild(c, child);
                   }

                   currentLine.setContainsContent(true);
                   currentLine.setContainsBlockLevelContent(true);

                   remainingWidth -= child.getWidth();

                   if (currentIB != null && currentIB.isStartsHere()) {
                       pendingLeftMBP -= currentIB.getStyle().getMarginBorderPadding(
                               c, maxAvailableWidth, CalculatedStyle.LEFT);
                   }

                   needFirstLetter = false;

                   if (child.getWidth() == 0) {
                       zeroWidthInlineBlock = true;
                   }
               }
            }
        }

        currentLine.trimTrailingSpace(c);
        saveLine(currentLine, c, box, minimumLineHeight,
                maxAvailableWidth, pendingFloats, hasFirstLinePEs,
                pendingInlineLayers, markerData, contentStart,
                isAlwaysBreak(c, box, breakAtLine, lineOffset));
        if (currentLine.isFirstLine() && currentLine.getHeight() == 0 && markerData != null) {
            c.setCurrentMarkerData(markerData);
        }
        markerData = null;

        box.setContentWidth(maxAvailableWidth);
        box.setHeight(currentLine.getY() + currentLine.getHeight());
    }

    private static boolean isAlwaysBreak(final LayoutContext c, final BlockBox parent, final int breakAtLine, final int lineOffset) {
        if (parent.isCurrentBreakAtLineContext(c)) {
            return lineOffset == breakAtLine;
        } else {
            return breakAtLine > 0 && lineOffset == breakAtLine;
        }
    }


    private static InlineLayoutBox addFirstLetterBox(final LayoutContext c, final LineBox current,
            final InlineLayoutBox currentIB, final LineBreakContext lbContext, final int maxAvailableWidth,
            final int remainingWidth) {
        final CalculatedStyle previous = currentIB.getStyle();

        currentIB.setStyle(c.getFirstLettersTracker().deriveAll(currentIB.getStyle()));

        final InlineLayoutBox iB = new InlineLayoutBox(c, null, currentIB.getStyle(), maxAvailableWidth);
        iB.setStartsHere(true);
        iB.setEndsHere(true);

        currentIB.addInlineChild(c, iB);
        current.setContainsContent(true);

        final InlineText text = layoutText(c, iB.getStyle(), remainingWidth, lbContext, true);
        iB.addInlineChild(c, text);
        iB.setInlineWidth(text.getWidth());

        lbContext.setStart(lbContext.getEnd());

        c.getFirstLettersTracker().clearStyles();
        currentIB.setStyle(previous);

        return iB;
    }

    private static void layoutInlineBlockContent(
            final LayoutContext c, final BlockBox containingBlock, final BlockBox inlineBlock, final int initialY) {
        inlineBlock.setContainingBlock(containingBlock);
        inlineBlock.setContainingLayer(c.getLayer());
        inlineBlock.initStaticPos(c, containingBlock, initialY);
        inlineBlock.calcCanvasLocation();
        inlineBlock.layout(c);
    }

    public static int positionHorizontally(final CssContext c, final Box current, final int start) {
        int x = start;

        InlineLayoutBox currentIB = null;

        if (current instanceof InlineLayoutBox) {
            currentIB = (InlineLayoutBox)current;
            x += currentIB.getLeftMarginBorderPadding(c);
        }

        for (int i = 0; i < current.getChildCount(); i++) {
            final Box b = current.getChild(i);
            if (b instanceof InlineLayoutBox) {
                final InlineLayoutBox iB = (InlineLayoutBox) current.getChild(i);
                iB.setX(x);
                x += positionHorizontally(c, iB, x);
            } else {
                b.setX(x);
                x += b.getWidth();
            }
        }

        if (currentIB != null) {
            x += currentIB.getRightMarginPaddingBorder(c);
            currentIB.setInlineWidth(x - start);
        }

        return x - start;
    }

    private static int positionHorizontally(final CssContext c, final InlineLayoutBox current, final int start) {
        int x = start;

        x += current.getLeftMarginBorderPadding(c);

        for (int i = 0; i < current.getInlineChildCount(); i++) {
            final Object child = current.getInlineChild(i);
            if (child instanceof InlineLayoutBox) {
                final InlineLayoutBox iB = (InlineLayoutBox) child;
                iB.setX(x);
                x += positionHorizontally(c, iB, x);
            } else if (child instanceof InlineText) {
                final InlineText iT = (InlineText) child;
                iT.setX(x - start);
                x += iT.getWidth();
            } else if (child instanceof Box) {
                final Box b = (Box) child;
                b.setX(x);
                x += b.getWidth();
            }
        }

        x += current.getRightMarginPaddingBorder(c);

        current.setInlineWidth(x - start);

        return x - start;
    }

    public static StrutMetrics createDefaultStrutMetrics(final LayoutContext c, final Box container) {
        final FSFontMetrics strutM = container.getStyle().getFSFontMetrics(c);
        final InlineBoxMeasurements measurements = getInitialMeasurements(c, container, strutM);

        return new StrutMetrics(
                strutM.getAscent(), measurements.getBaseline(), strutM.getDescent());
    }

    private static void positionVertically(
            final LayoutContext c, final Box container, final LineBox current, final MarkerData markerData) {
        if (current.getChildCount() == 0 || ! current.isContainsVisibleContent()) {
            current.setHeight(0);
        } else {
            final FSFontMetrics strutM = container.getStyle().getFSFontMetrics(c);
            final VerticalAlignContext vaContext = new VerticalAlignContext();
            final InlineBoxMeasurements measurements = getInitialMeasurements(c, container, strutM);
            vaContext.setInitialMeasurements(measurements);

            final List<TextDecoration> lBDecorations = calculateTextDecorations(
                    container, measurements.getBaseline(), strutM);
            if (lBDecorations != null) {
                current.setTextDecorations(lBDecorations);
            }

            for (int i = 0; i < current.getChildCount(); i++) {
                final Box child = current.getChild(i);
                positionInlineContentVertically(c, vaContext, child);
            }

            vaContext.alignChildren();

            current.setHeight(vaContext.getLineBoxHeight());

            int paintingTop = vaContext.getPaintingTop();
            int paintingBottom = vaContext.getPaintingBottom();

            if (vaContext.getInlineTop() < 0) {
                moveLineContents(current, -vaContext.getInlineTop());
                if (lBDecorations != null) {
                    for (final TextDecoration lBDecoration : lBDecorations) {
                        lBDecoration.setOffset(lBDecoration.getOffset() - vaContext.getInlineTop());
                    }
                }
                paintingTop -= vaContext.getInlineTop();
                paintingBottom -= vaContext.getInlineTop();
            }

            if (markerData != null) {
                final StrutMetrics strutMetrics = markerData.getStructMetrics();
                strutMetrics.setBaseline(measurements.getBaseline() - vaContext.getInlineTop());
                markerData.setReferenceLine(current);
                current.setMarkerData(markerData);
            }

            current.setBaseline(measurements.getBaseline() - vaContext.getInlineTop());

            current.setPaintingTop(paintingTop);
            current.setPaintingHeight(paintingBottom - paintingTop);
        }
    }

    private static void positionInlineVertically(final LayoutContext c,
            final VerticalAlignContext vaContext, final InlineLayoutBox iB) {
        final InlineBoxMeasurements iBMeasurements = calculateInlineMeasurements(c, iB, vaContext);
        vaContext.pushMeasurements(iBMeasurements);
        positionInlineChildrenVertically(c, iB, vaContext);
        vaContext.popMeasurements();
    }

    private static void positionInlineBlockVertically(
            final LayoutContext c, final VerticalAlignContext vaContext, final BlockBox inlineBlock) {
        final int baseline = inlineBlock.calcInlineBaseline(c);
        final int ascent = baseline;
        final int descent = inlineBlock.getHeight() - baseline;
        alignInlineContent(c, inlineBlock, ascent, descent, vaContext);

        vaContext.updateInlineTop(inlineBlock.getY());
        vaContext.updatePaintingTop(inlineBlock.getY());

        vaContext.updateInlineBottom(inlineBlock.getY() + inlineBlock.getHeight());
        vaContext.updatePaintingBottom(inlineBlock.getY() + inlineBlock.getHeight());
    }

    private static void moveLineContents(final LineBox current, final int ty) {
        for (int i = 0; i < current.getChildCount(); i++) {
            final Box child = current.getChild(i);
            child.setY(child.getY() + ty);
            if (child instanceof InlineLayoutBox) {
                moveInlineContents((InlineLayoutBox) child, ty);
            }
        }
    }

    private static void moveInlineContents(final InlineLayoutBox box, final int ty) {
        for (int i = 0; i < box.getInlineChildCount(); i++) {
            final Object obj = box.getInlineChild(i);
            if (obj instanceof Box) {
                ((Box) obj).setY(((Box) obj).getY() + ty);

                if (obj instanceof InlineLayoutBox) {
                    moveInlineContents((InlineLayoutBox) obj, ty);
                }
            }
        }
    }

    private static InlineBoxMeasurements calculateInlineMeasurements(final LayoutContext c, final InlineLayoutBox iB,
                                                                     final VerticalAlignContext vaContext) {
        final FSFontMetrics fm = iB.getStyle().getFSFontMetrics(c);

        final CalculatedStyle style = iB.getStyle();
        final float lineHeight = style.getLineHeight(c);

        int halfLeading = Math.round((lineHeight - iB.getStyle().getFont(c).size) / 2);
        if (halfLeading > 0) {
            halfLeading = Math.round((lineHeight -
                    (fm.getDescent() + fm.getAscent())) / 2);
        }

        iB.setBaseline(Math.round(fm.getAscent()));

        alignInlineContent(c, iB, fm.getAscent(), fm.getDescent(), vaContext);
        final List<TextDecoration> decorations = calculateTextDecorations(iB, iB.getBaseline(), fm);
        if (decorations != null) {
            iB.setTextDecorations(decorations);
        }

        final InlineBoxMeasurements result = new InlineBoxMeasurements();
        result.setBaseline(iB.getY() + iB.getBaseline());
        result.setInlineTop(iB.getY() - halfLeading);
        result.setInlineBottom(Math.round(result.getInlineTop() + lineHeight));
        result.setTextTop(iB.getY());
        result.setTextBottom((int) (result.getBaseline() + fm.getDescent()));

        final RectPropertySet padding = iB.getPadding(c);
        final BorderPropertySet border = iB.getBorder(c);

        result.setPaintingTop((int)Math.floor(iB.getY() - border.top() - padding.top()));
        result.setPaintingBottom((int)Math.ceil(iB.getY() +
                fm.getAscent() + fm.getDescent() +
                border.bottom() + padding.bottom()));

        return result;
    }

    public static List<TextDecoration> calculateTextDecorations(final Box box, final int baseline,
            final FSFontMetrics fm) {
        List<TextDecoration> result = null;
        final CalculatedStyle style = box.getStyle();

        final List<FSDerivedValue> idents = style.getTextDecorations();
        if (idents != null) {
            result = new ArrayList<TextDecoration>(idents.size());
            if (idents.contains(IdentValue.UNDERLINE)) {
                final TextDecoration decoration = new TextDecoration(IdentValue.UNDERLINE);
                // JDK returns zero so create additional space equal to one
                // "underlineThickness"
                if (fm.getUnderlineOffset() == 0) {
                    decoration.setOffset(Math.round((baseline + fm.getUnderlineThickness())));
                } else {
                    decoration.setOffset(Math.round((baseline + fm.getUnderlineOffset())));
                }
                decoration.setThickness(Math.round(fm.getUnderlineThickness()));

                // JDK on Linux returns some goofy values for
                // LineMetrics.getUnderlineOffset(). Compensate by always
                // making sure underline fits inside the descender
                if (fm.getUnderlineOffset() == 0) {  // HACK, are we running under the JDK
                    final int maxOffset =
                        baseline + (int)fm.getDescent() - decoration.getThickness();
                    if (decoration.getOffset() > maxOffset) {
                        decoration.setOffset(maxOffset);
                    }
                }
                result.add(decoration);
            }

            if (idents.contains(IdentValue.LINE_THROUGH)) {
                final TextDecoration decoration = new TextDecoration(IdentValue.LINE_THROUGH);
                decoration.setOffset(Math.round(baseline + fm.getStrikethroughOffset()));
                decoration.setThickness(Math.round(fm.getStrikethroughThickness()));
                result.add(decoration);
            }

            if (idents.contains(IdentValue.OVERLINE)) {
                final TextDecoration decoration = new TextDecoration(IdentValue.OVERLINE);
                decoration.setOffset(0);
                decoration.setThickness(Math.round(fm.getUnderlineThickness()));
                result.add(decoration);
            }
        }

        return result;
    }

    // XXX vertical-align: super/middle/sub could be improved (in particular,
    // super and sub should be sized by the measurements of our inline parent
    // not us)
    private static void alignInlineContent(final LayoutContext c, final Box box,
                                           final float ascent, final float descent, final VerticalAlignContext vaContext) {
        final InlineBoxMeasurements measurements = vaContext.getParentMeasurements();

        final CalculatedStyle style = box.getStyle();

        if (style.isLength(CSSName.VERTICAL_ALIGN)) {
            box.setY((int) (measurements.getBaseline() - ascent -
                    style.getFloatPropertyProportionalTo(CSSName.VERTICAL_ALIGN, style.getLineHeight(c), c)));
        } else {
            final IdentValue vAlign = style.getIdent(CSSName.VERTICAL_ALIGN);

            if (vAlign == IdentValue.BASELINE) {
                box.setY(Math.round(measurements.getBaseline() - ascent));
            } else if (vAlign == IdentValue.TEXT_TOP) {
                box.setY(measurements.getTextTop());
            } else if (vAlign == IdentValue.TEXT_BOTTOM) {
                box.setY(Math.round(measurements.getTextBottom() - descent - ascent));
            } else if (vAlign == IdentValue.MIDDLE) {
                // FIXME: findbugs, loss of precision, try / (float)2
                box.setY(Math.round((measurements.getBaseline() - measurements.getTextTop()) / 2
                        - (ascent + descent) / 2));
            } else if (vAlign == IdentValue.SUPER) {
                box.setY(Math.round(measurements.getBaseline() - (3*ascent/2)));
            } else if (vAlign == IdentValue.SUB) {
                box.setY(Math.round(measurements.getBaseline() - ascent / 2));
            } else {
                box.setY(Math.round(measurements.getBaseline() - ascent));
            }
        }
    }

    private static InlineBoxMeasurements getInitialMeasurements(
            final LayoutContext c, final Box container, final FSFontMetrics strutM) {
        final float lineHeight = container.getStyle().getLineHeight(c);

        int halfLeading = Math.round((lineHeight -
                container.getStyle().getFont(c).size) / 2);
        if (halfLeading > 0) {
            halfLeading = Math.round((lineHeight -
                    (strutM.getDescent() + strutM.getAscent())) / 2);
        }

        final InlineBoxMeasurements measurements = new InlineBoxMeasurements();
        measurements.setBaseline((int) (halfLeading + strutM.getAscent()));
        measurements.setTextTop(halfLeading);
        measurements.setTextBottom((int) (measurements.getBaseline() + strutM.getDescent()));
        measurements.setInlineTop(halfLeading);
        measurements.setInlineBottom((int) (halfLeading + lineHeight));

        return measurements;
    }

    private static void positionInlineChildrenVertically(final LayoutContext c, final InlineLayoutBox current,
                                               final VerticalAlignContext vaContext) {
        for (int i = 0; i < current.getInlineChildCount(); i++) {
            final Object child = current.getInlineChild(i);
            if (child instanceof Box) {
                positionInlineContentVertically(c, vaContext, (Box)child);
            }
        }
    }

    private static void positionInlineContentVertically(final LayoutContext c,
            final VerticalAlignContext vaContext, final Box child) {
        VerticalAlignContext vaTarget = vaContext;
        if (! child.getStyle().isLength(CSSName.VERTICAL_ALIGN)) {
            final IdentValue vAlign = child.getStyle().getIdent(
                    CSSName.VERTICAL_ALIGN);
            if (vAlign == IdentValue.TOP || vAlign == IdentValue.BOTTOM) {
                vaTarget = vaContext.createChild(child);
            }
        }
        if (child instanceof InlineLayoutBox) {
            final InlineLayoutBox iB = (InlineLayoutBox) child;
            positionInlineVertically(c, vaTarget, iB);
        } else { // any other Box class
            positionInlineBlockVertically(c, vaTarget, (BlockBox)child);
        }
    }

    private static void saveLine(final LineBox current, final LayoutContext c,
                                 final BlockBox block, final int minHeight,
                                 final int maxAvailableWidth, final List<FloatLayoutResult> pendingFloats,
                                 final boolean hasFirstLinePCs, final List<Layer> pendingInlineLayers,
                                 final MarkerData markerData, final int contentStart, final boolean alwaysBreak) {
        current.setContentStart(contentStart);
        current.prunePendingInlineBoxes();

        final int totalLineWidth = positionHorizontally(c, current, 0);
        current.setContentWidth(totalLineWidth);

        positionVertically(c, block, current, markerData);

        // XXX Revisit this.  Do we need this when dealing with unbreakable
        // text?  Is a line required to always have a minimum height?
        if (current.getHeight() != 0 &&
                current.getHeight() < minHeight &&
                ! current.isContainsOnlyBlockLevelContent()) {
            current.setHeight(minHeight);
        }

        if (c.isPrint()) {
            current.checkPagePosition(c, alwaysBreak);
        }

        alignLine(c, current, maxAvailableWidth);

        current.calcChildLocations();

        block.addChildForLayout(c, current);

        if (pendingInlineLayers.size() > 0) {
            finishPendingInlineLayers(c, pendingInlineLayers);
            pendingInlineLayers.clear();
        }

        if (hasFirstLinePCs && current.isFirstLine()) {
            c.getFirstLinesTracker().clearStyles();
            block.styleText(c);
        }

        if (pendingFloats.size() > 0) {
            for (final FloatLayoutResult layoutResult : pendingFloats) {
                LayoutUtil.layoutFloated(c, current, layoutResult.getBlock(), maxAvailableWidth, null);
                current.addNonFlowContent(layoutResult.getBlock());
            }
            pendingFloats.clear();
        }
    }

    private static void alignLine(final LayoutContext c, final LineBox current, final int maxAvailableWidth) {
        if (! current.isContainsDynamicFunction() && ! current.getParent().getStyle().isTextJustify()) {
            current.setFloatDistances(new FloatDistances() {
                public int getLeftFloatDistance() {
                    return c.getBlockFormattingContext().getLeftFloatDistance(c, current, maxAvailableWidth);
                }

                public int getRightFloatDistance() {
                    return c.getBlockFormattingContext().getRightFloatDistance(c, current, maxAvailableWidth);
                }
            });
        } else {
            final FloatDistances distances = new FloatDistances();
            distances.setLeftFloatDistance(
                    c.getBlockFormattingContext().getLeftFloatDistance(
                            c, current, maxAvailableWidth));
            distances.setRightFloatDistance(
                    c.getBlockFormattingContext().getRightFloatDistance(
                            c, current, maxAvailableWidth));
            current.setFloatDistances(distances);
        }
        current.align(false);
        if (! current.isContainsDynamicFunction() && ! current.getParent().getStyle().isTextJustify()) {
            current.setFloatDistances(null);
        }
    }

    private static void finishPendingInlineLayers(final LayoutContext c, final List<Layer> layers) {
        for (int i = 0; i < layers.size(); i++) {
            final Layer l = layers.get(i);
            l.positionChildren(c);
        }
    }

    private static InlineText layoutText(final LayoutContext c, final CalculatedStyle style, final int remainingWidth,
                                         final LineBreakContext lbContext, final boolean needFirstLetter) {
        final InlineText result = new InlineText();
        String masterText = lbContext.getMaster();
        if (needFirstLetter) {
            masterText = TextUtil.transformFirstLetterText(masterText, style);
            lbContext.setMaster(masterText);
            Breaker.breakFirstLetter(c, lbContext, remainingWidth, style);
        } else {
            Breaker.breakText(c, lbContext, remainingWidth, style);
        }

        result.setMasterText(masterText);
        result.setTextNode(lbContext.getTextNode());
        result.setSubstring(lbContext.getStart(), lbContext.getEnd());
        result.setWidth(lbContext.getWidth());

        return result;
    }

    private static int processOutOfFlowContent(
            final LayoutContext c, final LineBox current, final BlockBox block,
            final int available, final List<FloatLayoutResult> pendingFloats) {
        int result = 0;
        final CalculatedStyle style = block.getStyle();
        if (style.isAbsolute() || style.isFixed()) {
            LayoutUtil.layoutAbsolute(c, current, block);
            current.addNonFlowContent(block);
        } else if (style.isFloated()) {
            final FloatLayoutResult layoutResult = LayoutUtil.layoutFloated(
                    c, current, block, available, pendingFloats);
            if (layoutResult.isPending()) {
                pendingFloats.add(layoutResult);
            } else {
                result = layoutResult.getBlock().getWidth();
                current.addNonFlowContent(layoutResult.getBlock());
            }
        } else if (style.isRunning()) {
            block.setStaticEquivalent(current);
            c.getRootLayer().addRunningBlock(block);
        }

        return result;
    }

    private static boolean hasTrimmableLeadingSpace(
            final LineBox line, final CalculatedStyle style, final LineBreakContext lbContext,
            final boolean zeroWidthInlineBlock) {
        if ((! line.isContainsContent() || zeroWidthInlineBlock) &&
                lbContext.getStartSubstring().startsWith(WhitespaceStripper.SPACE)) {
            final IdentValue whitespace = style.getWhitespace();
            if ( (whitespace == IdentValue.NORMAL || whitespace == IdentValue.NOWRAP
                        || whitespace == IdentValue.PRE_LINE) ||
                    ( whitespace == IdentValue.PRE_WRAP && ! lbContext.isEndsOnNL())) {
                return true;
            }
        }
        return false;
    }

    private static void trimLeadingSpace(final LineBreakContext lbContext) {
        final String s = lbContext.getStartSubstring();
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        lbContext.setStart(lbContext.getStart() + i);
    }

    private static LineBox newLine(final LayoutContext c, final LineBox previousLine, final Box box) {
        int y = 0;

        if (previousLine != null) {
            y = previousLine.getY() + previousLine.getHeight();
        }

        return newLine(c, y, box);
    }

    private static LineBox newLine(final LayoutContext c, final int y, final Box box) {
        final LineBox result = new LineBox();
        result.setStyle(box.getStyle().createAnonymousStyle(IdentValue.BLOCK));
        result.setParent(box);
        result.initContainingLayer(c);

        result.setY(y);

        result.calcCanvasLocation();

        return result;
    }

    private static InlineLayoutBox addOpenInlineBoxes(
            final LayoutContext c, final LineBox line, final List<InlineBox> openParents, final int cbWidth, final Map<InlineBox, InlineLayoutBox> iBMap) {
        final ArrayList<InlineBox> result = new ArrayList<InlineBox>();

        InlineLayoutBox currentIB = null;
        InlineLayoutBox previousIB = null;

        boolean first = true;
        for (final InlineBox iB : openParents) {
            currentIB = new InlineLayoutBox(c, iB.getElement(), iB.getStyle(), cbWidth);

            final InlineLayoutBox prev = iBMap.get(iB);
            if (prev != null) {
                currentIB.setPending(prev.isPending());
            }

            iBMap.put(iB, currentIB);

            result.add(iB);

            if (first) {
                line.addChildForLayout(c, currentIB);
                first = false;
            } else {
                previousIB.addInlineChild(c, currentIB, false);
            }
            previousIB = currentIB;
        }

        return currentIB;
    }
}


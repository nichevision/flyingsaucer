/*
 * {{{ header & license
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.render;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Iterator;
import java.util.List;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.CSSPrimitiveUnit;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSColor;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.style.BackgroundPosition;
import org.xhtmlrenderer.css.style.BackgroundSize;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.css.style.derived.FSLinearGradient;
import org.xhtmlrenderer.css.style.derived.LengthValue;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.util.Configuration;
import org.xhtmlrenderer.util.Uu;

/**
 * An abstract implementation of an {@link OutputDevice}.  It provides complete
 * implementations for many <code>OutputDevice</code> methods.
 */
public abstract class AbstractOutputDevice implements OutputDevice {

    private FontSpecification _fontSpec;

    protected abstract void drawLine(int x1, int y1, int x2, int y2);
    
    public void drawText(final RenderingContext c, final InlineText inlineText) {
        final InlineLayoutBox iB = inlineText.getParent();
        final String text = inlineText.getSubstring();

        if (text != null && text.length() > 0) {
            setColor(iB.getStyle().getColor());
            
            setFont(iB.getStyle().getFSFont(c));
            setFontSpecification(iB.getStyle().getFontSpecification());
            if (inlineText.getParent().getStyle().isTextJustify()) {
                final JustificationInfo info = inlineText.getParent().getLineBox().getJustificationInfo();
                if (info != null) {
                    c.getTextRenderer().drawString(
                            c.getOutputDevice(),
                            text,
                            iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline(),
                            info);
                } else {
                    c.getTextRenderer().drawString(
                            c.getOutputDevice(),
                            text,
                            iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline());
                }
            } else {
                c.getTextRenderer().drawString(
                        c.getOutputDevice(),
                        text,
                        iB.getAbsX() + inlineText.getX(), iB.getAbsY() + iB.getBaseline());
            }
        }

        if (c.debugDrawFontMetrics()) {
            drawFontMetrics(c, inlineText);
        }
    }

    private void drawFontMetrics(final RenderingContext c, final InlineText inlineText) {
        final InlineLayoutBox iB = inlineText.getParent();
        final String text = inlineText.getSubstring();

        setColor(new FSRGBColor(0xFF, 0x33, 0xFF));

        final FSFontMetrics fm = iB.getStyle().getFSFontMetrics(null);
        final int width = c.getTextRenderer().getWidth(
                c.getFontContext(),
                iB.getStyle().getFSFont(c), text);
        final int x = iB.getAbsX() + inlineText.getX();
        int y = iB.getAbsY() + iB.getBaseline();

        drawLine(x, y, x + width, y);

        y += (int) Math.ceil(fm.getDescent());
        drawLine(x, y, x + width, y);

        y -= (int) Math.ceil(fm.getDescent());
        y -= (int) Math.ceil(fm.getAscent());
        drawLine(x, y, x + width, y);
    }

    public void drawTextDecoration(
            final RenderingContext c, final InlineLayoutBox iB, final TextDecoration decoration) 
    {
        setColor(iB.getStyle().getColor());
        
        final Rectangle edge = iB.getContentAreaEdge(iB.getAbsX(), iB.getAbsY(), c);

        fillRect(edge.x, iB.getAbsY() + decoration.getOffset(),
                    edge.width, decoration.getThickness());
    }

    public void drawTextDecoration(final RenderingContext c, final LineBox lineBox) 
    {
        setColor(lineBox.getStyle().getColor());

        final Box parent = lineBox.getParent();
        final List<TextDecoration> decorations = lineBox.getTextDecorations();
        for (final Iterator<TextDecoration> i = decorations.iterator(); i.hasNext(); ) {
            final TextDecoration textDecoration = (TextDecoration)i.next();
            if (parent.getStyle().isIdent(
                    CSSName.FS_TEXT_DECORATION_EXTENT, IdentValue.BLOCK)) {
                fillRect(
                    lineBox.getAbsX(),
                    lineBox.getAbsY() + textDecoration.getOffset(),
                    parent.getAbsX() + parent.getTx() + parent.getContentWidth() - lineBox.getAbsX(),
                    textDecoration.getThickness());
            } else {
                fillRect(
                    lineBox.getAbsX(), lineBox.getAbsY() + textDecoration.getOffset(),
                    lineBox.getContentWidth(),
                    textDecoration.getThickness());
            }
        }
    }

    public void drawDebugOutline(final RenderingContext c, final Box box, final FSColor color) {
        setColor(color);
        final Rectangle rect = box.getMarginEdge(box.getAbsX(), box.getAbsY(), c, 0, 0);
        rect.height -= 1;
        rect.width -= 1;
        drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    public void paintCollapsedBorder(
            final RenderingContext c, final BorderPropertySet border, final Rectangle bounds, final int side) {
        BorderPainter.paint(bounds, side, border, c, 0, false);
    }

    public void paintBorder(final RenderingContext c, final Box box) {
        if (! box.getStyle().isVisible()) {
            return;
        }

        final Rectangle borderBounds = box.getPaintingBorderEdge(c);

        BorderPainter.paint(borderBounds, box.getBorderSides(), box.getBorder(c), c, 0, true);
    }

    public void paintBorder(final RenderingContext c, final CalculatedStyle style, final Rectangle edge, final int sides) {
        BorderPainter.paint(edge, sides, style.getBorder(c), c, 0, true);
    }

    private FSImage getBackgroundImage(final RenderingContext c, final CalculatedStyle style) {
    	if (! style.isIdent(CSSName.BACKGROUND_IMAGE, IdentValue.NONE)) {
            final String uri = style.getStringProperty(CSSName.BACKGROUND_IMAGE);
            try {
                return c.getUac().getImageResource(uri).getImage();
            } catch (final Exception ex) {
                ex.printStackTrace();
                Uu.p(ex);
            }
        }
        return null;
    }

    public void paintBackground(
            final RenderingContext c, final CalculatedStyle style,
            final Rectangle bounds, final Rectangle bgImageContainer, final BorderPropertySet border) {
        paintBackground0(c, style, bounds, bgImageContainer, border);
    }

    public void paintBackground(final RenderingContext c, final Box box) {
        if (! box.getStyle().isVisible()) {
            return;
        }

        final Rectangle backgroundBounds = box.getPaintingBorderEdge(c);
        final BorderPropertySet border = box.getStyle().getBorder(c);
        paintBackground0(c, box.getStyle(), backgroundBounds, backgroundBounds, border);
    }

    private void paintBackground0(
            final RenderingContext c, final CalculatedStyle style,
            final Rectangle backgroundBounds, final Rectangle bgImageContainer,
            final BorderPropertySet border) {
        if (!Configuration.isTrue("xr.renderer.draw.backgrounds", true)) {
            return;
        }

        final FSColor backgroundColor = style.getBackgroundColor();

        FSLinearGradient backgroundLinearGradient = null;
        FSImage backgroundImage = null;
        
        if (style.isLinearGradient())
        {
        	// TODO: Is this the correct width to use?
        	backgroundLinearGradient = style.getLinearGradient(c, bgImageContainer.width, bgImageContainer.height);
        }
        else
        {
        	backgroundImage = getBackgroundImage(c, style);
        }

        // If the image width or height is zero, then there's nothing to draw.
        // Also prevents infinte loop when trying to tile an image with zero size.
        if (backgroundImage == null || backgroundImage.getHeight() == 0 || backgroundImage.getWidth() == 0) {
            backgroundImage = null;
        }

        if ( (backgroundColor == null || backgroundColor == FSRGBColor.TRANSPARENT) &&
                backgroundImage == null && backgroundLinearGradient == null) {
            return;
        }

        final Shape borderBounds = border == null ? backgroundBounds : BorderPainter.generateBorderBounds(backgroundBounds, border, false);
        
        if (backgroundColor != null && backgroundColor != FSRGBColor.TRANSPARENT) {
            setColor(backgroundColor);
            fill(borderBounds);
        }

        if (backgroundImage != null || backgroundLinearGradient != null) {
            Rectangle localBGImageContainer = bgImageContainer;
            if (style.isFixedBackground()) {
                localBGImageContainer = c.getViewportRectangle();
            }

            int xoff = localBGImageContainer.x;
            int yoff = localBGImageContainer.y;

            if (border != null) {
                xoff += (int)border.left();
                yoff += (int)border.top();
            }

            final Shape oldclip = getClip();

            clip(borderBounds);
            
        	if (backgroundLinearGradient != null)
        	{
        		drawLinearGradient(backgroundLinearGradient,
        		backgroundBounds.x, backgroundBounds.y, backgroundBounds.width, backgroundBounds.height);
        		setClip(oldclip);
        		return;
        	}

            if (backgroundImage != null)
            {
            	scaleBackgroundImage(c, style, localBGImageContainer, backgroundImage);
            }
            
            final float imageWidth = backgroundImage.getWidth();
            final float imageHeight = backgroundImage.getHeight();

            final BackgroundPosition position = style.getBackgroundPosition();
            xoff += calcOffset(
                    c, style, position.getHorizontal(), localBGImageContainer.width, imageWidth);
            yoff += calcOffset(
                    c, style, position.getVertical(), localBGImageContainer.height, imageHeight);

            final boolean hrepeat = style.isHorizontalBackgroundRepeat();
            final boolean vrepeat = style.isVerticalBackgroundRepeat();

            if (! hrepeat && ! vrepeat) {
                final Rectangle imageBounds = new Rectangle(xoff, yoff, (int)imageWidth, (int)imageHeight);
                if (imageBounds.intersects(backgroundBounds)) 
                {
               		drawImage(backgroundImage, xoff, yoff);
                }
            } else if (hrepeat && vrepeat) {
                paintTiles(
                        backgroundImage,
                        adjustTo(backgroundBounds.x, xoff, (int)imageWidth),
                        adjustTo(backgroundBounds.y, yoff, (int)imageHeight),
                        backgroundBounds.x + backgroundBounds.width,
                        backgroundBounds.y + backgroundBounds.height);
            } else if (hrepeat) {
                xoff = adjustTo(backgroundBounds.x, xoff, (int)imageWidth);
                final Rectangle imageBounds = new Rectangle(xoff, yoff, (int)imageWidth, (int)imageHeight);
                if (imageBounds.intersects(backgroundBounds)) {
                    paintHorizontalBand(
                            backgroundImage,
                            xoff,
                            yoff,
                            backgroundBounds.x + backgroundBounds.width);
                }
            } else if (vrepeat) {
                yoff = adjustTo(backgroundBounds.y, yoff, (int)imageHeight);
                final Rectangle imageBounds = new Rectangle(xoff, yoff, (int)imageWidth, (int)imageHeight);
                if (imageBounds.intersects(backgroundBounds)) {
                    paintVerticalBand(
                            backgroundImage,
                            xoff,
                            yoff,
                            backgroundBounds.y + backgroundBounds.height);
                }
            }

            setClip(oldclip);
        }
    }

    private int adjustTo(final int target, final int current, final int imageDim) {
        int result = current;
        if (result > target) {
            while (result > target) {
                result -= imageDim;
            }
        } else if (result < target) {
            while (result < target) {
                result += imageDim;
            }
            if (result != target) {
                result -= imageDim;
            }
        }
        return result;
    }

    private void paintTiles(final FSImage image, final int left, final int top, final int right, final int bottom) {
        final int width = image.getWidth();
        final int height = image.getHeight();

        for (int x = left; x < right; x+= width) {
            for (int y = top; y < bottom; y+= height) {
                drawImage(image, x, y);
            }
        }
    }

    private void paintVerticalBand(final FSImage image, final int left, final int top, final int bottom) {
        final int height = image.getHeight();

        for (int y = top; y < bottom; y+= height) {
            drawImage(image, left, y);
        }
    }

    private void paintHorizontalBand(final FSImage image, final int left, final int top, final int right) {
        final int width = image.getWidth();

        for (int x = left; x < right; x+= width) {
            drawImage(image, x, top);
        }
    }

    private int calcOffset(final CssContext c, final CalculatedStyle style, final PropertyValue value, final float boundsDim, final float imageDim) {
        if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_PERCENTAGE) {
            final float percent = value.getFloatValue() / 100.0f;
            return Math.round(boundsDim*percent - imageDim*percent);
        } else { /* it's a <length> */
            return (int)LengthValue.calcFloatProportionalValue(
                    style,
                    CSSName.BACKGROUND_POSITION,
                    value.getCssText(),
                    value.getFloatValue(),
                    value.getPrimitiveTypeN(),
                    0,
                    c);
        }
    }

    private void scaleBackgroundImage(final CssContext c, final CalculatedStyle style, final Rectangle backgroundContainer, final FSImage image) {
        final BackgroundSize backgroundSize = style.getBackgroundSize();

        if (! backgroundSize.isBothAuto()) {
            if (backgroundSize.isCover() || backgroundSize.isContain()) {
                final int testHeight = (int)((double)image.getHeight() * backgroundContainer.width / image.getWidth());
                if (backgroundSize.isContain()) {
                    if (testHeight > backgroundContainer.height) {
                        image.scale(-1, backgroundContainer.height);
                    } else {
                        image.scale(backgroundContainer.width, -1);
                    }
                } else if (backgroundSize.isCover()) {
                    if (testHeight > backgroundContainer.height) {
                        image.scale(backgroundContainer.width, -1);
                    } else {
                        image.scale(-1, backgroundContainer.height);
                    }
                }
            } else {
                final int scaledWidth = calcBackgroundSizeLength(c, style, backgroundSize.getWidth(), backgroundContainer.width);
                final int scaledHeight = calcBackgroundSizeLength(c, style, backgroundSize.getHeight(), backgroundContainer.height);

                image.scale(scaledWidth, scaledHeight);
            }
        }
    }

    private int calcBackgroundSizeLength(final CssContext c, final CalculatedStyle style, final PropertyValue value, final float boundsDim) {
        if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_IDENT) { // 'auto'
            return -1;
        } else if (value.getPrimitiveTypeN() == CSSPrimitiveUnit.CSS_PERCENTAGE) {
            final float percent = value.getFloatValue() / 100.0f;
            return Math.round(boundsDim*percent);
        } else {
            return (int)LengthValue.calcFloatProportionalValue(
                    style,
                    CSSName.BACKGROUND_SIZE,
                    value.getCssText(),
                    value.getFloatValue(),
                    value.getPrimitiveTypeN(),
                    0,
                    c);
        }
    }

    /**
     * Gets the FontSpecification for this AbstractOutputDevice.
     *
     * @return current FontSpecification.
     */
    public FontSpecification getFontSpecification() {
	return _fontSpec;
    }

    /**
     * Sets the FontSpecification for this AbstractOutputDevice.
     *
     * @param fs current FontSpecification.
     */
    public void setFontSpecification(final FontSpecification fs) {
	_fontSpec = fs;
    }
}

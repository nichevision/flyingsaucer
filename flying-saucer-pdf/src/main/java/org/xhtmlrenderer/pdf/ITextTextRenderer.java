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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.pdf;

import java.awt.Rectangle;

import org.xhtmlrenderer.extend.FSGlyphVector;
import org.xhtmlrenderer.extend.FontContext;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.pdf.ITextFontResolver.FontDescription;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.JustificationInfo;

import com.lowagie.text.pdf.BaseFont;

public class ITextTextRenderer implements TextRenderer {
    private static float TEXT_MEASURING_DELTA = 0.01f;
    
    public void setup(final FontContext context) {
    }

    public void drawString(final OutputDevice outputDevice, final String string, final float x, final float y) {
        ((ITextOutputDevice)outputDevice).drawString(string, x, y, null);
    }
    
    public void drawString(
            final OutputDevice outputDevice, final String string, final float x, final float y, final JustificationInfo info) {
        ((ITextOutputDevice)outputDevice).drawString(string, x, y, info);
    }

    public FSFontMetrics getFSFontMetrics(final FontContext context, final FSFont font, final String string) {
        final FontDescription descr = ((ITextFSFont)font).getFontDescription();
        final BaseFont bf = descr.getFont();
        final float size = font.getSize2D();
        final ITextFSFontMetrics result = new ITextFSFontMetrics();
        result.setAscent(bf.getFontDescriptor(BaseFont.BBOXURY, size));
        result.setDescent(-bf.getFontDescriptor(BaseFont.BBOXLLY, size));
        
        result.setStrikethroughOffset(-descr.getYStrikeoutPosition() / 1000f * size);
        if (descr.getYStrikeoutSize() != 0) {
            result.setStrikethroughThickness(descr.getYStrikeoutSize() / 1000f * size);
        } else {
            result.setStrikethroughThickness(size / 12.0f);
        }
        
        result.setUnderlineOffset(-descr.getUnderlinePosition() / 1000f * size);
        result.setUnderlineThickness(descr.getUnderlineThickness() / 1000f * size);
        
        return result;
    }

    public int getWidth(final FontContext context, final FSFont font, final String string) {
        final BaseFont bf = ((ITextFSFont)font).getFontDescription().getFont();
        final float result = bf.getWidthPoint(string, font.getSize2D());
        if (result - Math.floor(result) < TEXT_MEASURING_DELTA) {
            return (int)result;
        } else {
            return (int)Math.ceil(result); 
        }
    }

    public void setFontScale(final float scale) {
    }

    public float getFontScale() {
        return 1.0f;
    }

    public void setSmoothingThreshold(final float fontsize) {
    }

    public int getSmoothingLevel() {
        return 0;
    }

    public void setSmoothingLevel(final int level) {
    }

    public Rectangle getGlyphBounds(final OutputDevice outputDevice, final FSFont font, final FSGlyphVector fsGlyphVector, final int index, final float x, final float y) {
        throw new UnsupportedOperationException();
    }

    public float[] getGlyphPositions(final OutputDevice outputDevice, final FSFont font, final FSGlyphVector fsGlyphVector) {
        throw new UnsupportedOperationException();
    }

    public FSGlyphVector getGlyphVector(final OutputDevice outputDevice, final FSFont font, final String string) {
        throw new UnsupportedOperationException();
    }

    public void drawGlyphVector(final OutputDevice outputDevice, final FSGlyphVector vector, final float x, final float y) {
        throw new UnsupportedOperationException();
    }
}

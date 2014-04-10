/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci, Torbjoern Gannholm
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
package org.xhtmlrenderer.swing;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.util.Map;

import org.xhtmlrenderer.extend.FSGlyphVector;
import org.xhtmlrenderer.extend.FontContext;
import org.xhtmlrenderer.extend.OutputDevice;
import org.xhtmlrenderer.extend.TextRenderer;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.FSFontMetrics;
import org.xhtmlrenderer.render.JustificationInfo;
import org.xhtmlrenderer.render.LineMetricsAdapter;
import org.xhtmlrenderer.util.Configuration;


/**
 * Renders to a Graphics2D instance.
 *
 * @author   Joshua Marinacci
 * @author   Torbjoern Gannholm
 */
public class Java2DTextRenderer implements TextRenderer {
    protected float scale;
    protected float threshold;
    protected Object antiAliasRenderingHint;
    protected Object fractionalFontMetricsHint;

    public Java2DTextRenderer() {
        scale = Configuration.valueAsFloat("xr.text.scale", 1.0f);
        threshold = Configuration.valueAsFloat("xr.text.aa-fontsize-threshhold", 25);

        final Object dummy = new Object();

        final Object aaHint = Configuration.valueFromClassConstant("xr.text.aa-rendering-hint", dummy);        
        if (aaHint == dummy) {
            try {
                Map map;
                // we should be able to look up the "recommended" AA settings (that correspond to the user's
                // desktop preferences and machine capabilities
                // see: http://java.sun.com/javase/6/docs/api/java/awt/doc-files/DesktopProperties.html
                final Toolkit tk = Toolkit.getDefaultToolkit();
                map = (Map) (tk.getDesktopProperty("awt.font.desktophints"));
                antiAliasRenderingHint = map.get(RenderingHints.KEY_TEXT_ANTIALIASING);
            } catch (final Exception e) {
                // conceivably could get an exception in a webstart environment? not sure
                antiAliasRenderingHint = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
            }
        } else {
            antiAliasRenderingHint = aaHint;
        }
        if("true".equals(Configuration.valueFor("xr.text.fractional-font-metrics", "false"))) {
            fractionalFontMetricsHint = RenderingHints.VALUE_FRACTIONALMETRICS_ON;
        } else {
            fractionalFontMetricsHint = RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
        }
    }

    /** {@inheritDoc} */
    public void drawString(final OutputDevice outputDevice, final String string, final float x, final float y ) {
        Object aaHint = null;
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        if ( graphics.getFont().getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        graphics.drawString( string, (int)x, (int)y );
        if ( graphics.getFont().getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
    }
    
    public void drawString(
            final OutputDevice outputDevice, final String string, final float x, final float y, final JustificationInfo info) {
        Object aaHint = null;
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        if ( graphics.getFont().getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        final GlyphVector vector = graphics.getFont().createGlyphVector(
                graphics.getFontRenderContext(), string);
        
        adjustGlyphPositions(string, info, vector);
        
        graphics.drawGlyphVector(vector, x, y);
        
        if ( graphics.getFont().getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
    }

    private void adjustGlyphPositions(
            final String string, final JustificationInfo info, final GlyphVector vector) {
        float adjust = 0.0f;
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (i != 0) {
                final Point2D point = vector.getGlyphPosition(i);
                vector.setGlyphPosition(
                        i, new Point2D.Double(point.getX() + adjust, point.getY()));
            }
            if (c == ' ' || c == '\u00a0' || c == '\u3000') {
                adjust += info.getSpaceAdjust();
            } else {
                adjust += info.getNonSpaceAdjust();
            }
        }
    }
    
    public void drawGlyphVector(final OutputDevice outputDevice, final FSGlyphVector fsGlyphVector, final float x, final float y ) {
        Object aaHint = null;
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        
        if ( graphics.getFont().getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);

        final GlyphVector vector = ((AWTFSGlyphVector)fsGlyphVector).getGlyphVector();
        graphics.drawGlyphVector(vector, (int)x, (int)y );
        if ( graphics.getFont().getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
    }

    /** {@inheritDoc} */
    public void setup(final FontContext fontContext) {
        //Uu.p("setup graphics called");
//        ((Java2DFontContext)fontContext).getGraphics().setRenderingHint( 
//                RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF );
    }

    public void setFontScale( final float scale ) {
        this.scale = scale;
    }

    public void setSmoothingThreshold( final float fontsize ) {
        threshold = fontsize;
    }

    public void setSmoothingLevel( final int level ) { /* no-op */ }

    public FSFontMetrics getFSFontMetrics(final FontContext fc, final FSFont font, final String string ) {
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DFontContext)fc).getGraphics();
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        final LineMetricsAdapter adapter = new LineMetricsAdapter(
                ((AWTFSFont)font).getAWTFont().getLineMetrics(
                        string, graphics.getFontRenderContext()));
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        return adapter;
    }
    
    public int getWidth(final FontContext fc, final FSFont font, final String string) {
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DFontContext)fc).getGraphics();
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        final Font awtFont = ((AWTFSFont)font).getAWTFont();
        int width = 0;
        if(fractionalFontMetricsHint == RenderingHints.VALUE_FRACTIONALMETRICS_ON) {
            width = (int)Math.round(
                    graphics.getFontMetrics(awtFont).getStringBounds(string, graphics).getWidth());            
        } else {
            width = (int)Math.ceil(
                    graphics.getFontMetrics(awtFont).getStringBounds(string, graphics).getWidth());
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        return width;
    }

    public float getFontScale() {
        return this.scale;
    }

    public int getSmoothingLevel() {
        return 0;
    }

    /**
     * If anti-alias text is enabled, the value from RenderingHints to use for AA smoothing in Java2D. Defaults to
     * {@link java.awt.RenderingHints#VALUE_TEXT_ANTIALIAS_ON}.
     *
     * @return Current AA rendering hint
     */
    public Object getRenderingHints() {
        return antiAliasRenderingHint;
    }

    /**
     * If anti-alias text is enabled, the value from RenderingHints to use for AA smoothing in Java2D. Defaults to
     * {@link java.awt.RenderingHints#VALUE_TEXT_ANTIALIAS_ON}.
     *
     * @param renderingHints  rendering hint for AA smoothing in Java2D
     */
    public void setRenderingHints(final Object renderingHints) {
        this.antiAliasRenderingHint = renderingHints;
    }

    public float[] getGlyphPositions(final OutputDevice outputDevice, final FSFont font, final String text) {
        Object aaHint = null;
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        final Font awtFont = ((AWTFSFont)font).getAWTFont();
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);

        
        final GlyphVector vector = awtFont.createGlyphVector(
                graphics.getFontRenderContext(),
                text);
        final float[] result = vector.getGlyphPositions(0, text.length() + 1, null);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return result;
    }

    public Rectangle getGlyphBounds(final OutputDevice outputDevice, final FSFont font, final FSGlyphVector fsGlyphVector, final int index, final float x, final float y) {
        Object aaHint = null;
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        final Font awtFont = ((AWTFSFont)font).getAWTFont();
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        final GlyphVector vector = ((AWTFSGlyphVector)fsGlyphVector).getGlyphVector();
        
        final Rectangle result = vector.getGlyphPixelBounds(index, graphics.getFontRenderContext(), x, y);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return result;
    }

    public float[] getGlyphPositions(final OutputDevice outputDevice, final FSFont font, final FSGlyphVector fsGlyphVector) {
        Object aaHint = null;
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        final Font awtFont = ((AWTFSFont)font).getAWTFont();
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        final GlyphVector vector = ((AWTFSGlyphVector)fsGlyphVector).getGlyphVector();
        
        final float[] result = vector.getGlyphPositions(0, vector.getNumGlyphs() + 1, null);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return result;
    }

    public FSGlyphVector getGlyphVector(final OutputDevice outputDevice, final FSFont font, final String text) {
        Object aaHint = null;
        Object fracHint = null;
        final Graphics2D graphics = ((Java2DOutputDevice)outputDevice).getGraphics();
        final Font awtFont = ((AWTFSFont)font).getAWTFont();
        
        if (awtFont.getSize() > threshold ) {
            aaHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, antiAliasRenderingHint );
        }
        fracHint = graphics.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalFontMetricsHint);
        
        final GlyphVector vector = awtFont.createGlyphVector(
                graphics.getFontRenderContext(),
                text);
        
        if (awtFont.getSize() > threshold ) {
            graphics.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, aaHint );
        }
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracHint);
        
        return new AWTFSGlyphVector(vector);
    }
}


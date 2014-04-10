/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package org.xhtmlrenderer.render;

import java.awt.RenderingHints;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.extend.FSImage;

/**
 * A utility class to paint list markers (all types).
 * @see MarkerData 
 */
public class ListItemPainter {
    public static void paint(final RenderingContext c, final BlockBox box) {
        if (box.getMarkerData() == null) {
            return;
        }
        
        final MarkerData markerData = box.getMarkerData();
        
        if (markerData.getImageMarker() != null) {
            drawImage(c, box, markerData);
        } else {
            final CalculatedStyle style = box.getStyle();
            final IdentValue listStyle = style.getIdent(CSSName.LIST_STYLE_TYPE);
            
            c.getOutputDevice().setColor(style.getColor());
    
            if (markerData.getGlyphMarker() != null) {
                drawGlyph(c, box, style, listStyle);
            } else if (markerData.getTextMarker() != null){
                drawText(c, box, listStyle);
            }
        }
    }

    private static void drawImage(final RenderingContext c, final BlockBox box, final MarkerData markerData) {
        FSImage img = null;
        final MarkerData.ImageMarker marker = markerData.getImageMarker();
        img = marker.getImage();
        if (img != null) {
            final StrutMetrics strutMetrics = box.getMarkerData().getStructMetrics();
            int x = getReferenceX(c, box);
            // FIXME: findbugs possible loss of precision, cf. int / (float)2
            x += -marker.getLayoutWidth() +
                    (marker.getLayoutWidth() / 2 - img.getWidth() / 2);
            c.getOutputDevice().drawImage(img, 
                    x,
                    (int)(getReferenceBaseline(c, box)
                        - strutMetrics.getAscent() / 2 - img.getHeight() / 2));
        }
    }
    
    private static int getReferenceX(final RenderingContext c, final BlockBox box) {
        final MarkerData markerData = box.getMarkerData();
        
        if (markerData.getReferenceLine() != null) {
            return markerData.getReferenceLine().getAbsX();
        } else {
            return box.getAbsX() + (int)box.getMargin(c).left();
        }
    }
    
    private static int getReferenceBaseline(final RenderingContext c, final BlockBox box) {
        final MarkerData markerData = box.getMarkerData();
        final StrutMetrics strutMetrics = box.getMarkerData().getStructMetrics();
        
        if (markerData.getReferenceLine() != null) {
            return markerData.getReferenceLine().getAbsY() + strutMetrics.getBaseline();
        } else {
            return box.getAbsY() + box.getTy() + strutMetrics.getBaseline();
        }
    }

    private static void drawGlyph(final RenderingContext c, final BlockBox box, 
            final CalculatedStyle style, final IdentValue listStyle) {
        // save the old AntiAliasing setting, then force it on
        final Object aa_key = c.getOutputDevice().getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        c.getOutputDevice().setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // calculations for bullets
        final StrutMetrics strutMetrics = box.getMarkerData().getStructMetrics();
        final MarkerData.GlyphMarker marker = box.getMarkerData().getGlyphMarker();
        int x = getReferenceX(c, box);
        x += -marker.getLayoutWidth();
        final int y = getReferenceBaseline(c, box) 
            - (int)strutMetrics.getAscent() / 2 - marker.getDiameter() / 2;
        if (listStyle == IdentValue.DISC) {
            c.getOutputDevice().fillOval(x, y, marker.getDiameter(), marker.getDiameter());
        } else if (listStyle == IdentValue.SQUARE) {
            c.getOutputDevice().fillRect(x, y, marker.getDiameter(), marker.getDiameter());
        } else if (listStyle == IdentValue.CIRCLE) {
            c.getOutputDevice().drawOval(x, y, marker.getDiameter(), marker.getDiameter());
        }

        // restore the old AntiAliasing setting
        c.getOutputDevice().setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                aa_key == null ? RenderingHints.VALUE_ANTIALIAS_DEFAULT : aa_key);
    }

    private static void drawText(final RenderingContext c, final BlockBox box, final IdentValue listStyle) {
        final MarkerData.TextMarker text = box.getMarkerData().getTextMarker();
        
        int x = getReferenceX(c, box);
        x += -text.getLayoutWidth();
        final int y = getReferenceBaseline(c, box);
        
        c.getOutputDevice().setColor(box.getStyle().getColor());
        c.getOutputDevice().setFont(box.getStyle().getFSFont(c));
        c.getTextRenderer().drawString(
                c.getOutputDevice(), text.getText(), x, y);
    }
}

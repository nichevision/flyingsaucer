/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
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
package org.xhtmlrenderer.render;

import java.awt.BasicStroke;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.extend.OutputDevice;

public class BorderPainter {
    public static final int TOP = 1;
    public static final int LEFT = 2;
    public static final int BOTTOM = 4;
    public static final int RIGHT = 8;
    public static final int ALL = TOP + LEFT + BOTTOM + RIGHT;
    public static final double ARC_TO_BEZIER = 0.55228475;
    
    private static class Position
    {
    	float x;
    	float y;
    }
    
    private static void relLineTo(final Path2D p, final float x, final float y, final Position pos)
    {
    	p.lineTo(x + pos.x, y + pos.y);
    	pos.x = x + pos.x;
    	pos.y = y + pos.y;
    }
    
    private static void relCurveTo(final Path2D p, final float p1, final float p2, final float p3, final float p4, final float p5, final float p6, final Position pos)
    {
    	p.curveTo(p1 + pos.x, p2 + pos.y, p3 + pos.x, p4 + pos.y, p5 + pos.x, p6 + pos.y);
    	pos.x = p5 + pos.x;
    	pos.y = p6 + pos.y;
    }
    
    /**
     * Simple rounded rect.
     * From: http://cairographics.org/cookbook/roundedrectangles/
     */
    private static void roundedRect(final Path2D cr, final float x, final float y, final float w, final float h, float radiusX, float radiusY)
    {
        if (radiusX > w - radiusX)
            radiusX = w / 2;
        if (radiusY > h - radiusY)
            radiusY = h / 2;

        // approximate (quite close) the arc using a bezier curve
        final float c1 = (float) (ARC_TO_BEZIER * radiusX);
        final float c2 = (float) (ARC_TO_BEZIER * radiusY);

        final Position pos = new Position();
        
        cr.moveTo(x + radiusX, y);

        pos.x = x + radiusX;
        pos.y = y;
        
        // Top
        relLineTo (cr, w - 2 * radiusX, 0.0f, pos);
        relCurveTo (cr, c1, 0.0f, radiusX, c2, radiusX, radiusY, pos);
        // Right
        relLineTo ( cr, 0, h - 2 * radiusY, pos);
        relCurveTo (cr, 0.0f, c2, c1 - radiusX, radiusY, -radiusX, radiusY, pos);
        // Bottom
        relLineTo ( cr, -w + 2 * radiusX, 0, pos);
        relCurveTo (cr, -c1, 0f, -radiusX, -c2, -radiusX, -radiusY, pos);
        // Left
        relLineTo (cr, 0, -h + 2 * radiusY, pos);
        relCurveTo (cr, 0.0f, -c2, radiusX - c1, -radiusY, radiusX, -radiusY, pos);
        cr.closePath();
    }
    
	/**
     * @param xOffset for determining starting point for patterns
     */
    public static void paint(
            final Rectangle bounds, int sides, final BorderPropertySet border, 
            final RenderingContext ctx, final int xOffset, final boolean bevel) 
    {
    	if ((sides & BorderPainter.TOP) == BorderPainter.TOP && border.noTop()) {
            sides -= BorderPainter.TOP;
        }
        if ((sides & BorderPainter.LEFT) == BorderPainter.LEFT && border.noLeft()) {
            sides -= BorderPainter.LEFT;
        }
        if ((sides & BorderPainter.BOTTOM) == BorderPainter.BOTTOM && border.noBottom()) {
            sides -= BorderPainter.BOTTOM;
        }
        if ((sides & BorderPainter.RIGHT) == BorderPainter.RIGHT && border.noRight()) {
            sides -= BorderPainter.RIGHT;
        }
        
        final OutputDevice dev = ctx.getOutputDevice();
        
    	if (border.isSquareRectStandard() &&
    		sides == BorderPainter.ALL &&
    		border.leftStyle() == IdentValue.SOLID &&
    		border.left() == 1)
    	{
        		// Deal with the easiest, fastest & most common case first.
    			// Example: border: 1px solid black;
        		dev.setStroke(new BasicStroke(1.0f));
        		dev.setColor(border.leftColor());
        		dev.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        		dev.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        		dev.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	}
    	else if (!border.hasBorderRadius())
    	{
			if ((sides & BorderPainter.TOP) == BorderPainter.TOP
					&& border.topColor() != FSRGBColor.TRANSPARENT) {
				paintBorderSide(ctx.getOutputDevice(), border, bounds, sides,
						BorderPainter.TOP, border.topStyle(), xOffset, bevel);
			}
			if ((sides & BorderPainter.LEFT) == BorderPainter.LEFT
					&& border.leftColor() != FSRGBColor.TRANSPARENT) {
				paintBorderSide(ctx.getOutputDevice(), border, bounds, sides,
						BorderPainter.LEFT, border.leftStyle(), xOffset, bevel);
			}
			if ((sides & BorderPainter.BOTTOM) == BorderPainter.BOTTOM
					&& border.bottomColor() != FSRGBColor.TRANSPARENT) {
				paintBorderSide(ctx.getOutputDevice(), border, bounds, sides,
						BorderPainter.BOTTOM, border.bottomStyle(), xOffset,
						bevel);
			}
			if ((sides & BorderPainter.RIGHT) == BorderPainter.RIGHT
					&& border.rightColor() != FSRGBColor.TRANSPARENT) {
				paintBorderSide(ctx.getOutputDevice(), border, bounds, sides,
						BorderPainter.RIGHT, border.rightStyle(), xOffset,
						bevel);
			}
		}
    	else if (border.isRoundedRectStandard() &&
        		sides == BorderPainter.ALL &&
        		border.leftStyle() == IdentValue.SOLID &&
        		border.left() == 1)
    	{
    		final Path2D path = new Path2D.Float();
    		roundedRect(path, bounds.x, bounds.y, bounds.width, bounds.height, border.radiusTopLeftOne(), border.radiusTopLeftTwo());
    		dev.setStroke(new BasicStroke(1.0f));
    		dev.setColor(border.leftColor());
    		dev.draw(path);
    	}
    	else
    	{
    		// We have a non-standard rect with radius!
    		RoundedBorderPainter.paint(bounds, sides, border, ctx, xOffset, bevel);
    	}
    }

    private static Rectangle shrinkRect(final Rectangle rect, final BorderPropertySet border, final int sides) {
        final Rectangle r2 = new Rectangle();
        r2.x = rect.x + ((sides & BorderPainter.LEFT) == 0 ? 0 : (int) border.left());
        r2.width = rect.width - ((sides & BorderPainter.LEFT) == 0 ? 0 : (int) border.left()) - ((sides & BorderPainter.RIGHT) == 0 ? 0 : (int) border.right());
        r2.y = rect.y + ((sides & BorderPainter.TOP) == 0 ? 0 : (int) border.top());
        r2.height = rect.height - ((sides & BorderPainter.TOP) == 0 ? 0 : (int) border.top()) - ((sides & BorderPainter.BOTTOM) == 0 ? 0 : (int) border.bottom());
        return r2;
    }

    private static void paintBorderSide(final OutputDevice outputDevice, 
            final BorderPropertySet border, final Rectangle bounds, final int sides, 
            final int currentSide, final IdentValue borderSideStyle, final int xOffset, final boolean bevel) {
        if (borderSideStyle == IdentValue.RIDGE || borderSideStyle == IdentValue.GROOVE) {
            final BorderPropertySet bd2 = new BorderPropertySet(border, (border.top() / 2),
                    (border.right() / 2),
                    (border.bottom() / 2),
                    (border.left() / 2));
            if (borderSideStyle == IdentValue.RIDGE) {
                paintBorderSidePolygon(
                        outputDevice, bounds, border, border.darken(borderSideStyle), 
                        border.lighten(borderSideStyle), sides, currentSide, bevel);
                paintBorderSidePolygon(
                        outputDevice, bounds, bd2, border.lighten(borderSideStyle), 
                        border.darken(borderSideStyle), sides, currentSide, bevel);
            } else {
                paintBorderSidePolygon(
                        outputDevice, bounds, border, border.lighten(borderSideStyle),
                        border.darken(borderSideStyle), sides, currentSide, bevel);
                paintBorderSidePolygon(
                        outputDevice, bounds, bd2, border.darken(borderSideStyle),
                        border.lighten(borderSideStyle), sides, currentSide, bevel);
            }
        } else if (borderSideStyle == IdentValue.OUTSET) {
            paintBorderSidePolygon(outputDevice, bounds, border,
                    border.lighten(borderSideStyle),
                    border.darken(borderSideStyle), sides, currentSide, bevel);
        } else if (borderSideStyle == IdentValue.INSET) {
            paintBorderSidePolygon(outputDevice, bounds, border,
                    border.darken(borderSideStyle),
                    border.lighten(borderSideStyle), sides, currentSide, bevel);
        } else if (borderSideStyle == IdentValue.SOLID) {
            paintSolid(outputDevice, bounds, border, border, sides, currentSide, bevel);
        } else if (borderSideStyle == IdentValue.DOUBLE) {
            paintDoubleBorder(outputDevice, border, bounds, sides, currentSide, bevel);
        } else {
            int thickness = 0;
            if (currentSide == BorderPainter.TOP) thickness = (int) border.top();
            if (currentSide == BorderPainter.BOTTOM) thickness = (int) border.bottom();
            if (currentSide == BorderPainter.RIGHT) thickness = (int) border.right();
            if (currentSide == BorderPainter.LEFT) thickness = (int) border.left();
            if (borderSideStyle == IdentValue.DASHED) {
                outputDevice.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                paintPatternedRect(outputDevice, bounds, border, border, new float[]{8.0f + thickness * 2, 4.0f + thickness}, sides, currentSide, xOffset);
                outputDevice.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            if (borderSideStyle == IdentValue.DOTTED) {
                // turn off anti-aliasing or the dots will be all blurry
                outputDevice.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                paintPatternedRect(outputDevice, bounds, border, border, new float[]{thickness, thickness}, sides, currentSide, xOffset);
                outputDevice.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
        }
    }
    
    private static DoubleBorderInfo calcDoubleBorderInfo(final int width) {
        final DoubleBorderInfo result = new DoubleBorderInfo();
        if (width == 1) {
            result.setOuter(1);
        } else if (width == 2) {
            result.setOuter(1);
            result.setInner(1);
        } else {
            final int extra = width % 3;
            switch (extra) {
                case 0:
                    result.setOuter(width / 3);
                    result.setCenter(width / 3);
                    result.setInner(width / 3);
                    break;
                case 1:
                    result.setOuter((width + 2) / 3 - 1);
                    result.setCenter((width + 2) / 3);
                    result.setInner((width + 2) / 3 - 1);
                    break;                    
                case 2:
                    result.setOuter((width + 1) / 3);
                    result.setCenter((width + 1) / 3 - 1);
                    result.setInner((width + 1) / 3);
                    break;
            }
        }
        return result;
    }

    private static void paintDoubleBorder(
            final OutputDevice outputDevice, final BorderPropertySet border, 
            final Rectangle bounds, final int sides, final int currentSide, final boolean bevel) {
        final DoubleBorderInfo topBorderInfo = calcDoubleBorderInfo((int)border.top());
        final DoubleBorderInfo rightBorderInfo = calcDoubleBorderInfo((int)border.right());
        final DoubleBorderInfo bottomBorderInfo = calcDoubleBorderInfo((int)border.bottom());
        final DoubleBorderInfo leftBorderInfo = calcDoubleBorderInfo((int)border.left());
        
        final BorderPropertySet outer = new BorderPropertySet(border,
                topBorderInfo.getOuter(), rightBorderInfo.getOuter(), 
                bottomBorderInfo.getOuter(), leftBorderInfo.getOuter());
        
        final BorderPropertySet center = new BorderPropertySet(border,
                topBorderInfo.getCenter(), rightBorderInfo.getCenter(), 
                bottomBorderInfo.getCenter(), leftBorderInfo.getCenter());
        
        final BorderPropertySet inner = new BorderPropertySet(border,
                topBorderInfo.getInner(), rightBorderInfo.getInner(), 
                bottomBorderInfo.getInner(), leftBorderInfo.getInner());

        Rectangle b2 = shrinkRect(bounds, outer, bevel ? sides : currentSide);
        b2 = shrinkRect(b2, center, bevel ? sides : currentSide);
        // draw outer border
        paintSolid(outputDevice, bounds, outer, border, sides, currentSide, bevel);
        // draw inner border
        paintSolid(outputDevice, b2, inner, border, sides, currentSide, bevel);
    }

    /**
     * Gets the polygon to be filled for the border
     */
    private static Polygon getBorderSidePolygon(
            final Rectangle bounds, final BorderPropertySet border, final int sides, 
            final int currentSide, final boolean bevel) {
        int rightCorner = 0;
        int leftCorner = 0;
        int topCorner = 0;
        int bottomCorner = 0;
        if (bevel) {
            rightCorner = (((sides & BorderPainter.RIGHT) == BorderPainter.RIGHT) ? (int) border.right() : 0);
            leftCorner = (((sides & BorderPainter.LEFT) == BorderPainter.LEFT) ? (int) border.left() : 0);
            topCorner = (((sides & BorderPainter.TOP) == BorderPainter.TOP) ? (int) border.top() : 0);
            bottomCorner = (((sides & BorderPainter.BOTTOM) == BorderPainter.BOTTOM) ? (int) border.bottom() : 0);
        }
        Polygon poly = null;
        if (currentSide == BorderPainter.TOP) {
            if ((int) border.top() != 1) {
                // use polygons for borders over 1px wide
                poly = new Polygon();
                poly.addPoint(bounds.x, bounds.y);
                poly.addPoint(bounds.x + bounds.width, bounds.y);
                poly.addPoint(bounds.x + bounds.width - rightCorner, bounds.y + (int) border.top() - 0);
                poly.addPoint(bounds.x + leftCorner, bounds.y + (int) border.top() - 0);
            }
        } else if (currentSide == BorderPainter.BOTTOM) {
            if ((int) border.bottom() != 1) {
                poly = new Polygon();
                // upper right
                poly.addPoint(bounds.x + bounds.width - rightCorner, bounds.y + bounds.height - (int) border.bottom());
                // upper left
                poly.addPoint(bounds.x + leftCorner, bounds.y + bounds.height - (int) border.bottom());
                // lower left
                poly.addPoint(bounds.x, bounds.y + bounds.height);
                // lower right
                poly.addPoint(bounds.x + bounds.width, bounds.y + bounds.height - 0);
            }
        } else if (currentSide == BorderPainter.RIGHT) {
            if ((int) border.right() != 1) {
                poly = new Polygon();
                poly.addPoint(bounds.x + bounds.width, bounds.y);
                poly.addPoint(bounds.x + bounds.width - (int) border.right(), bounds.y + topCorner);
                poly.addPoint(bounds.x + bounds.width - (int) border.right(), bounds.y + bounds.height - bottomCorner);
                poly.addPoint(bounds.x + bounds.width, bounds.y + bounds.height);
            }
        } else if (currentSide == BorderPainter.LEFT) {
            if ((int) border.left() != 1) {
                poly = new Polygon();
                poly.addPoint(bounds.x, bounds.y);
                poly.addPoint(bounds.x + (int) border.left(), bounds.y + topCorner);
                poly.addPoint(bounds.x + (int) border.left(), bounds.y + bounds.height - bottomCorner);
                poly.addPoint(bounds.x, bounds.y + bounds.height);
            }
        }
        return poly;
    }

    /**
     * @param xOffset     for inline borders, to determine dash_phase of top and bottom
     */
    private static void paintPatternedRect(final OutputDevice outputDevice, 
            final Rectangle bounds, final BorderPropertySet border, 
            final BorderPropertySet color, final float[] pattern, 
            final int sides, final int currentSide, final int xOffset) {
        final Stroke old_stroke = outputDevice.getStroke();

        if (currentSide == BorderPainter.TOP) {
            outputDevice.setColor(color.topColor());
            outputDevice.setStroke(new BasicStroke((int) border.top(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, xOffset));
            outputDevice.drawBorderLine(
                    bounds, BorderPainter.TOP, (int)border.top(), false);
        } else if (currentSide == BorderPainter.LEFT) {
            outputDevice.setColor(color.leftColor());
            outputDevice.setStroke(new BasicStroke((int) border.left(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, 0));
            outputDevice.drawBorderLine(
                    bounds, BorderPainter.LEFT, (int)border.left(), false);
        } else if (currentSide == BorderPainter.RIGHT) {
            outputDevice.setColor(color.rightColor());
            outputDevice.setStroke(new BasicStroke((int) border.right(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, 0));
            outputDevice.drawBorderLine(
                    bounds, BorderPainter.RIGHT, (int)border.right(), false);
        } else if (currentSide == BorderPainter.BOTTOM) {
            outputDevice.setColor(color.bottomColor());
            outputDevice.setStroke(new BasicStroke((int) border.bottom(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, xOffset));
            outputDevice.drawBorderLine(
                    bounds, BorderPainter.BOTTOM, (int)border.bottom(), false);
        }

        outputDevice.setStroke(old_stroke);
    }

    private static void paintBorderSidePolygon(final OutputDevice outputDevice, 
            final Rectangle bounds, final BorderPropertySet border, 
            final BorderPropertySet high, final BorderPropertySet low, 
            final int sides, final int currentSide, final boolean bevel) {
        if (currentSide == BorderPainter.TOP) {
            paintSolid(outputDevice, bounds, border, high, sides, currentSide, bevel);
        } else if (currentSide == BorderPainter.BOTTOM) {
            paintSolid(outputDevice, bounds, border, low, sides, currentSide, bevel);
        } else if (currentSide == BorderPainter.RIGHT) {
            paintSolid(outputDevice, bounds, border, low, sides, currentSide, bevel);
        } else if (currentSide == BorderPainter.LEFT) {
            paintSolid(outputDevice, bounds, border, high, sides, currentSide, bevel);
        }
    }

    private static void paintSolid(final OutputDevice outputDevice, 
            final Rectangle bounds, final BorderPropertySet border, 
            final BorderPropertySet bcolor, final int sides, final int currentSide,
            final boolean bevel) {
        final Polygon poly = getBorderSidePolygon(bounds, border, sides, currentSide, bevel);

        if (currentSide == BorderPainter.TOP) {
            outputDevice.setColor(bcolor.topColor());

            // draw a 1px border with a line instead of a polygon
            if ((int) border.top() == 1) {
                outputDevice.drawBorderLine(bounds, BorderPainter.TOP, 
                        (int)border.top(), true);
            } else {
                // use polygons for borders over 1px wide
                outputDevice.fill(poly);
            }
        } else if (currentSide == BorderPainter.BOTTOM) {
            outputDevice.setColor(bcolor.bottomColor());
            if ((int) border.bottom() == 1) {
                outputDevice.drawBorderLine(bounds, BorderPainter.BOTTOM, 
                        (int)border.bottom(), true);
            } else {
                outputDevice.fill(poly);
            }
        } else if (currentSide == BorderPainter.RIGHT) {
            outputDevice.setColor(bcolor.rightColor());
            if ((int) border.right() == 1) {
                outputDevice.drawBorderLine(bounds, BorderPainter.RIGHT, 
                        (int)border.right(), true);
            } else {
                outputDevice.fill(poly);
            }
        } else if (currentSide == BorderPainter.LEFT) {
            outputDevice.setColor(bcolor.leftColor());
            if ((int) border.left() == 1) {
                outputDevice.drawBorderLine(bounds, BorderPainter.LEFT, 
                        (int)border.left(), true);
            } else {
                outputDevice.fill(poly);
            }
        }
    }

    private static class DoubleBorderInfo {
        private int _outer;
        private int _center;
        private int _inner;
        
        public int getCenter() {
            return _center;
        }
        
        public void setCenter(final int center) {
            _center = center;
        }
        
        public int getInner() {
            return _inner;
        }
        
        public void setInner(final int inner) {
            _inner = inner;
        }
        
        public int getOuter() {
            return _outer;
        }
        
        public void setOuter(final int outer) {
            _outer = outer;
        }
    }

	public static Shape generateBorderBounds(final Rectangle bounds,
			final BorderPropertySet border, final boolean inside) {

		if (!border.hasBorderRadius())
		{
			return bounds;
		}
		else if (border.isRoundedRectStandard() &&
	    		border.leftStyle() == IdentValue.SOLID &&
	    		border.left() == 1)
		{
			final Path2D path = new Path2D.Float();
			roundedRect(path, bounds.x , bounds.y, bounds.width, bounds.height, border.radiusTopLeftOne(), border.radiusTopLeftTwo());
			return path;
		}
		else
		{
			return RoundedBorderPainter.generateBorderBounds(bounds, border, inside);
		}
	}
}

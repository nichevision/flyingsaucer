package org.xhtmlrenderer.render;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.parser.FSRGBColor;
import org.xhtmlrenderer.css.style.derived.BorderPropertySet;
import org.xhtmlrenderer.extend.OutputDevice;

public class RoundedBorderPainter {
    public static final int TOP = 1;
    public static final int LEFT = 2;
    public static final int BOTTOM = 4;
    public static final int RIGHT = 8;
    public static final int ALL = TOP + LEFT + BOTTOM + RIGHT;
    
    /**
     * Generates a full round rectangle that is made of bounds and border
     * @param bounds Dimmensions of the rect
     * @param border The border specs
     * @param Set true if you want the inner bounds of borders
     * @return A Path that is all sides of the round rectangle
     */
    public static Path2D generateBorderBounds(final Rectangle bounds, final BorderPropertySet border, final boolean inside) {
    	final Path2D path = generateBorderShape(bounds, TOP, border, false, inside ? 1 : 0, 1);
    	path.append(generateBorderShape(bounds, RIGHT, border, false, inside ? 1 : 0, 1), true);
    	path.append(generateBorderShape(bounds, BOTTOM, border, false, inside ? 1 : 0, 1), true);
    	path.append(generateBorderShape(bounds, LEFT, border, false, inside ? 1 : 0, 1), true);
    	return path;
    }
	
    // helper function for bezier curves
    private static Point2D subT(final double t, final Point2D a, final Point2D b) {
    	return new Point2D.Double(a.getX() + t*(b.getX()-a.getX()),
    			a.getY() + t*(b.getY()-a.getY()));
    }
    
    /**
     * Cubic bezier curve function, takes in points and spits out the location of b(t) and 2 new bezier curves that both start and end at b(t)
     * @param t as defined for bezier curves
     * @param P0 start point
     * @param P1 ctrl pt 1
     * @param P2 ctrl pt 2
     * @param P3 end point
     * @return [[curve 1 starting at P0 and ending at B(t)], [curve 2 starting at P(3) and ending at B(t)]]
     */
    private static Point2D[][] getSubCurve(final double t, final Point2D P0, final Point2D P1, final Point2D P2, final Point2D P3) {
    	final Point2D P4 = subT(t, P0, P1);
    	final Point2D P5 = subT(t, P1, P2);
    	final Point2D P6 = subT(t, P2, P3);
    	final Point2D P7 = subT(t, P4, P5);
    	final Point2D P8 = subT(t, P5, P6);
    	final Point2D P9 = subT(t, P7, P8);
    	return new Point2D [][] {
    			new Point2D[]{P0, P4, P7, P9},
    			new Point2D[]{P3, P6, P8, P9}};
    }
    

    // 2 helper functions to reduce the number of params you have to see as the last 2 are very option and rarely used
    public static Path2D generateBorderShape(final Rectangle bounds, final int side, final BorderPropertySet border, final boolean drawInterior) {
    	return generateBorderShape(bounds, side, border, drawInterior, 0, 1);
    }
    public static Path2D generateBorderShape(final Rectangle bounds, final int side, final BorderPropertySet border, final boolean drawInterior, final float scaledOffset) {
    	return generateBorderShape(bounds, side, border, drawInterior, scaledOffset, 1);
    }
    /**
     * Generates one side of a border
     * @param bounds bounds of the container
     * @param side what side you want
     * @param border border props
     * @param drawInterior if you want it to be 2d or not, if false it will be just a line
     * @param scaledOffset insets the border by multipling border widths by this variable, best use would be 1 or .5, cant see it for much other than that
     * @param widthScale scales the border widths by this factor, useful for drawing half borders for border types like groove or double
     * @return a path for the side chosen!
     */
    public static Path2D generateBorderShape(final Rectangle bounds, final int side, final BorderPropertySet border, final boolean drawInterior, final float scaledOffset, final float widthScale) {
    	
    	float sideWidth = -1, topWidth = widthScale, leftWidth = widthScale, rightWidth = widthScale;
    	double rotation = 0;
    	float interiorWidth = 0, interiorHeight = 0,
    			exteriorWidth = 0, exteriorHeight = 0;
    	BorderRadiusPair leftRadius = null, rightRadius = null;
    	int xOffset = 0, yOffset = 0;
    	
    	if ((side & BorderPainter.TOP) == BorderPainter.TOP) {
    		sideWidth = bounds.width;
    		
    		topWidth = widthScale*border.top();
    		leftWidth = widthScale*border.left();
    		rightWidth = widthScale*border.right();
    		
    		leftRadius = new BorderRadiusPair(border.radiusTopLeftOne(), border.radiusTopLeftTwo());
    		rightRadius = new BorderRadiusPair(border.radiusTopRightOne(), border.radiusTopRightTwo());

    		interiorWidth = bounds.width - (1+scaledOffset)*widthScale*border.left() - (1+scaledOffset)*widthScale*border.right();
    		interiorHeight = bounds.height - (1+scaledOffset)*widthScale*border.top() - (1+scaledOffset)*widthScale*border.bottom();
    		exteriorWidth = bounds.width - scaledOffset*widthScale*border.left() - scaledOffset*widthScale*border.right();
    		exteriorHeight = bounds.height - scaledOffset*widthScale*border.top() - scaledOffset*widthScale*border.bottom();
    		
    		rotation = 0;
    	} else if ((side & BorderPainter.RIGHT) == BorderPainter.RIGHT) {
    		sideWidth = bounds.height;
    		
    		topWidth = widthScale*border.right();
    		leftWidth = widthScale*border.top();
    		rightWidth = widthScale*border.bottom();
    		
    		leftRadius = new BorderRadiusPair(border.radiusTopRightOne(), border.radiusTopRightTwo());
    		rightRadius = new BorderRadiusPair(border.radiusBottomRightOne(), border.radiusBottomRightTwo());
    		
    		interiorHeight = bounds.width - (1+scaledOffset)*widthScale*border.left() - (1+scaledOffset)*widthScale*border.right();
    		interiorWidth = bounds.height - (1+scaledOffset)*widthScale*border.top() - (1+scaledOffset)*widthScale*border.bottom();
    		exteriorHeight = bounds.width - scaledOffset*widthScale*border.left() - scaledOffset*widthScale*border.right();
     		exteriorWidth = bounds.height - scaledOffset*widthScale*border.top() - scaledOffset*widthScale*border.bottom();

    		xOffset = bounds.width;
    		yOffset = 0;
    		rotation = Math.PI / 2;
    	} else if ((side & BorderPainter.BOTTOM) == BorderPainter.BOTTOM) {
    		sideWidth = bounds.width;
    		
    		topWidth = widthScale*border.bottom();
    		leftWidth = widthScale*border.right();
    		rightWidth = widthScale*border.left();
    		
    		leftRadius = new BorderRadiusPair(border.radiusBottomRightOne(), border.radiusBottomRightTwo());
    		rightRadius = new BorderRadiusPair(border.radiusBottomLeftOne(), border.radiusBottomLeftTwo());

    		interiorWidth = bounds.width - (1+scaledOffset)*widthScale*border.left() - (1+scaledOffset)*widthScale*border.right();
    		interiorHeight = bounds.height - (1+scaledOffset)*widthScale*border.top() - (1+scaledOffset)*widthScale*border.bottom();
    		exteriorWidth = bounds.width - scaledOffset*widthScale*border.left() - scaledOffset*widthScale*border.right();
    		exteriorHeight = bounds.height - scaledOffset*widthScale*border.top() - scaledOffset*widthScale*border.bottom();

    		xOffset = bounds.width;
    		yOffset = bounds.height;
    		rotation = Math.PI;
    	} else if ((side & BorderPainter.LEFT) == BorderPainter.LEFT) {
    		sideWidth = bounds.height;
    		
    		topWidth = widthScale*border.left();
    		leftWidth = widthScale*border.bottom();
    		rightWidth = widthScale*border.top();
    		
    		leftRadius = new BorderRadiusPair(border.radiusBottomLeftOne(), border.radiusBottomLeftTwo());
    		rightRadius = new BorderRadiusPair(border.radiusTopLeftOne(), border.radiusTopLeftTwo());
    		
    		interiorHeight = bounds.width - (1+scaledOffset)*widthScale*border.left() - (1+scaledOffset)*widthScale*border.right();
    		interiorWidth = bounds.height - (1+scaledOffset)*widthScale*border.top() - (1+scaledOffset)*widthScale*border.bottom();
    		exteriorHeight = (bounds.width - scaledOffset*widthScale*border.left() - scaledOffset*widthScale*border.right());
     		exteriorWidth = bounds.height - scaledOffset*widthScale*border.top() - scaledOffset*widthScale*border.bottom();
   		 
    		xOffset = 0;
    		yOffset = bounds.height;
    		rotation = 3 * Math.PI / 2;
    	}
    	
    	float tco = scaledOffset*topWidth;
    	float lco = scaledOffset*leftWidth;
    	float rco = scaledOffset*rightWidth;
    	
    	final float curveConstant = .45f;

    	// top left corner % of side space
		float lp = 1;
		if(leftWidth != 0)
			lp = leftWidth / (topWidth + leftWidth);
		else
			lp = 0;

		// top right corner % of side space
		float rp = 1;
		if(rightWidth != 0)
			rp = rightWidth / (topWidth + rightWidth);
		else
			rp = 0;

		
		
		
		final Path2D path = new Path2D.Float();
		
		if(leftRadius.getMaxRight(exteriorWidth) > 0) {
			
	    	final Point2D [][] leftCurvePoints = getSubCurve(1-lp, 
				new Point2D.Double(	leftRadius.getMaxRight(exteriorWidth) + lco, 					tco), 
				new Point2D.Double(	curveConstant*(leftRadius.getMaxRight(exteriorWidth)) + lco, 	tco), 
				new Point2D.Double(	lco, 															tco+curveConstant*(leftRadius.getMaxLeft(exteriorHeight))),
				new Point2D.Double(	lco, 															tco+leftRadius.getMaxLeft(exteriorHeight)));
			
			path.moveTo(	leftCurvePoints[0][3].getX(), 		leftCurvePoints[0][3].getY());
			path.curveTo(	leftCurvePoints[0][2].getX(), 		leftCurvePoints[0][2].getY(), 
							leftCurvePoints[0][1].getX(),		leftCurvePoints[0][1].getY(), 
							leftCurvePoints[0][0].getX(),		leftCurvePoints[0][0].getY());
		} else {
			path.moveTo(	lco, 				tco);
		}
		
		
		if(rightRadius.getMaxLeft(exteriorWidth) > 0) {
			
			final Point2D [][] rightCurvePoints = getSubCurve(1-rp, 
    				new Point2D.Double(	sideWidth - rightRadius.getMaxLeft(exteriorWidth) - rco, 						tco), 
    				new Point2D.Double(	sideWidth - curveConstant*(rightRadius.getMaxLeft(exteriorWidth)) - rco, 		tco), 
    				new Point2D.Double(	sideWidth - rco, 														   		tco + curveConstant*(rightRadius.getMaxRight(exteriorHeight))),
    				new Point2D.Double(	sideWidth - rco, 																tco + rightRadius.getMaxRight(exteriorHeight)));
			
			path.lineTo( 	rightCurvePoints[0][0].getX(), rightCurvePoints[0][0].getY());
			path.curveTo(	rightCurvePoints[0][1].getX(), rightCurvePoints[0][1].getY(), 
							rightCurvePoints[0][2].getX(), rightCurvePoints[0][2].getY(), 
							rightCurvePoints[0][3].getX(), rightCurvePoints[0][3].getY());
		} else {
			path.lineTo(sideWidth - rightRadius.getMaxLeft(exteriorWidth/2) - rco, 		tco);
		}

		
		if(drawInterior) {
	    	// start drawing interior
	    	tco = (1+scaledOffset)*topWidth;
	    	lco = (1+scaledOffset)*leftWidth;
	    	rco = (1+scaledOffset)*rightWidth;

	    	if(rightRadius.getMaxLeft(interiorWidth) > 0) {
	    		
				final Point2D [][] rightCurvePoints = getSubCurve(1-rp, 
	    				new Point2D.Double(	sideWidth - rightRadius.getMaxLeft(interiorWidth) - rco, 							tco), 
	    				new Point2D.Double(	sideWidth - curveConstant*(rightRadius.getMaxLeft(interiorWidth)) - rco, 			tco), 
	    				new Point2D.Double(	sideWidth - rco, 														   			tco + curveConstant*(rightRadius.getMaxRight(interiorHeight))),
	    				new Point2D.Double(	sideWidth - rco, 																	tco + rightRadius.getMaxRight(interiorHeight)));
		    	
				path.lineTo(rightCurvePoints[0][3].getX(), rightCurvePoints[0][3].getY());
				path.curveTo(	rightCurvePoints[0][2].getX(), rightCurvePoints[0][2].getY(), 
								rightCurvePoints[0][1].getX(), rightCurvePoints[0][1].getY(), 
								rightCurvePoints[0][0].getX(), rightCurvePoints[0][0].getY());
			} else {
				path.lineTo(sideWidth - rco, 				tco);
			}
			
			if(leftRadius.getMaxRight(interiorWidth) > 0) {
				
		    	final Point2D [][] leftCurvePoints = getSubCurve(1-lp, 
					new Point2D.Double(	leftRadius.getMaxRight(interiorWidth) + lco, 						tco), 
					new Point2D.Double(	curveConstant*(leftRadius.getMaxRight(interiorWidth)) + lco, 		tco), 
					new Point2D.Double(	lco, 																tco + curveConstant*(leftRadius.getMaxLeft(interiorHeight))),
					new Point2D.Double(	lco, 																tco + leftRadius.getMaxLeft(interiorHeight)));
		    	
		    	path.lineTo(leftCurvePoints[0][0].getX(), leftCurvePoints[0][0].getY());
				path.curveTo(	leftCurvePoints[0][1].getX(), leftCurvePoints[0][1].getY(), 
						leftCurvePoints[0][2].getX(), leftCurvePoints[0][2].getY(), 
						leftCurvePoints[0][3].getX(), leftCurvePoints[0][3].getY());
			} else {
		    	path.lineTo(leftRadius.getMaxRight(interiorHeight) +  lco, 				tco);
			}
			
			path.closePath();
		}
    	
		
		path.transform(AffineTransform.getRotateInstance(rotation, 0, 0));
		path.transform(AffineTransform.getTranslateInstance(bounds.x + xOffset, bounds.y + yOffset));
    	
    	return path;
    }
    
    /**
     * @param xOffset for determining starting point for patterns
     */
    public static void paint(
            final Rectangle bounds, int sides, final BorderPropertySet border, 
            final RenderingContext ctx, final int xOffset, final boolean bevel) {
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

        //Now paint!
        if ((sides & BorderPainter.TOP) == BorderPainter.TOP && border.topColor() != FSRGBColor.TRANSPARENT) {
            paintBorderSide(ctx.getOutputDevice(), 
                    border, bounds, sides, BorderPainter.TOP, border.topStyle(), xOffset, bevel);
        }
        if ((sides & BorderPainter.BOTTOM) == BorderPainter.BOTTOM && border.bottomColor() != FSRGBColor.TRANSPARENT) {
            paintBorderSide(ctx.getOutputDevice(), 
                    border, bounds, sides, BorderPainter.BOTTOM, border.bottomStyle(), xOffset, bevel);
        }
        if ((sides & BorderPainter.LEFT) == BorderPainter.LEFT && border.leftColor() != FSRGBColor.TRANSPARENT) {
            paintBorderSide(ctx.getOutputDevice(), 
                    border, bounds, sides, BorderPainter.LEFT, border.leftStyle(), xOffset, bevel);
        }
        if ((sides & BorderPainter.RIGHT) == BorderPainter.RIGHT && border.rightColor() != FSRGBColor.TRANSPARENT) {
            paintBorderSide(ctx.getOutputDevice(), 
                    border, bounds, sides, BorderPainter.RIGHT, border.rightStyle(), xOffset, bevel);
        }
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
        	   paintBorderSideShape(
                       outputDevice, bounds, bd2, border.lighten(borderSideStyle), 
                       border.darken(borderSideStyle),
                       0, 1, sides, currentSide, bevel);
        	   paintBorderSideShape(
                        outputDevice, bounds, border, border.darken(borderSideStyle), 
                        border.lighten(borderSideStyle),
                        1, .5f, sides, currentSide, bevel);
            } else {
            	paintBorderSideShape(
                        outputDevice, bounds, bd2, border.darken(borderSideStyle),
                        border.lighten(borderSideStyle),
                        0, 1, sides, currentSide, bevel);
            	paintBorderSideShape(
                        outputDevice, bounds, border, border.lighten(borderSideStyle),
                        border.darken(borderSideStyle),
                        1, .5f, sides, currentSide, bevel);
            }
        } else if (borderSideStyle == IdentValue.OUTSET) {
            paintBorderSideShape(outputDevice, bounds, border,
                    border.lighten(borderSideStyle),
                    border.darken(borderSideStyle), 
                    0, 1, sides, currentSide, bevel);
        } else if (borderSideStyle == IdentValue.INSET) {
        	paintBorderSideShape(outputDevice, bounds, border,
                    border.darken(borderSideStyle),
                    border.lighten(borderSideStyle),
                    0, 1, sides, currentSide, bevel);
        } else if (borderSideStyle == IdentValue.SOLID) {
        	outputDevice.setStroke(new BasicStroke(1f));
        	if(currentSide == TOP) {
            	outputDevice.setColor(border.topColor());
            	outputDevice.fill(generateBorderShape(bounds, TOP, border, true, 0, 1));
        	}
        	if(currentSide == RIGHT) {
            	outputDevice.setColor(border.rightColor());
            	outputDevice.fill(generateBorderShape(bounds, RIGHT, border, true, 0, 1));
        	}
        	if(currentSide == BOTTOM) {
            	outputDevice.setColor(border.bottomColor());
            	outputDevice.fill(generateBorderShape(bounds, BOTTOM, border, true, 0, 1));
        	}
        	if(currentSide == LEFT) {
            	outputDevice.setColor(border.leftColor());
            	outputDevice.fill(generateBorderShape(bounds, LEFT, border, true, 0, 1));
        	}
        	
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

    private static void paintDoubleBorder(
            final OutputDevice outputDevice, final BorderPropertySet border, 
            final Rectangle bounds, final int sides, final int currentSide, final boolean bevel) {
        // draw outer border
        paintSolid(outputDevice, bounds, border, 0, .5f, sides, currentSide, bevel);
        // draw inner border
        paintSolid(outputDevice, bounds, border, 2, .5f, sides, currentSide, bevel);
    }

    /**
     * @param xOffset     for inline borders, to determine dash_phase of top and bottom
     */
    private static void paintPatternedRect(final OutputDevice outputDevice, 
            final Rectangle bounds, final BorderPropertySet border, 
            final BorderPropertySet color, final float[] pattern, 
            final int sides, final int currentSide, final int xOffset) {
        final Stroke old_stroke = outputDevice.getStroke();

        final Path2D path = generateBorderShape(bounds, currentSide, border, false, .5f, 1);
        final Path2D clip = generateBorderShape(bounds, currentSide, border, true, 0, 1);
        
        final Shape old_clip = outputDevice.getClip();
        outputDevice.setClip(clip);
        		
        if (currentSide == BorderPainter.TOP) {
            outputDevice.setColor(color.topColor());
            outputDevice.setStroke(new BasicStroke((int) border.top(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, xOffset));
            outputDevice.drawBorderLine(
            		path, BorderPainter.TOP, (int)border.top(), false);
        } else if (currentSide == BorderPainter.LEFT) {
            outputDevice.setColor(color.leftColor());
            outputDevice.setStroke(new BasicStroke((int) border.left(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, 0));
            outputDevice.drawBorderLine(
            		path, BorderPainter.LEFT, (int)border.left(), false);
        } else if (currentSide == BorderPainter.RIGHT) {
            outputDevice.setColor(color.rightColor());
            outputDevice.setStroke(new BasicStroke((int) border.right(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, 0));
            outputDevice.drawBorderLine(
            		path, BorderPainter.RIGHT, (int)border.right(), false);
        } else if (currentSide == BorderPainter.BOTTOM) {
            outputDevice.setColor(color.bottomColor());
            outputDevice.setStroke(new BasicStroke((int) border.bottom(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, pattern, xOffset));
            outputDevice.drawBorderLine(
            		path, BorderPainter.BOTTOM, (int)border.bottom(), false);
        }

        outputDevice.setClip(old_clip);
        outputDevice.setStroke(old_stroke);
    }

    private static void paintBorderSideShape(final OutputDevice outputDevice, 
            final Rectangle bounds, final BorderPropertySet border, 
            final BorderPropertySet high, final BorderPropertySet low, 
            final float offset, final float scale,
            final int sides, final int currentSide, final boolean bevel) {
        if (currentSide == BorderPainter.TOP) {
            paintSolid(outputDevice, bounds, high, offset, scale, sides, currentSide, bevel);
        } else if (currentSide == BorderPainter.BOTTOM) {
            paintSolid(outputDevice, bounds, low, offset, scale, sides, currentSide, bevel);
        } else if (currentSide == BorderPainter.RIGHT) {
            paintSolid(outputDevice, bounds, low, offset, scale, sides, currentSide, bevel);
        } else if (currentSide == BorderPainter.LEFT) {
            paintSolid(outputDevice, bounds, high, offset, scale, sides, currentSide, bevel);
        }
    }

    private static void paintSolid(final OutputDevice outputDevice, 
            final Rectangle bounds, final BorderPropertySet border, 
            final float offset, final float scale, final int sides, final int currentSide,
            final boolean bevel) {
        
        if (currentSide == BorderPainter.TOP) {
            outputDevice.setColor(border.topColor());
            // draw a 1px border with a line instead of a polygon
            if ((int) border.top() == 1) {
            	final Shape line = generateBorderShape(bounds, currentSide, border, false, offset, scale);
            	outputDevice.draw(line);
            } else {
            	final Shape line = generateBorderShape(bounds, currentSide, border, true, offset, scale);
                // use polygons for borders over 1px wide
                outputDevice.fill(line);
            }
        } else if (currentSide == BorderPainter.BOTTOM) {
            outputDevice.setColor(border.bottomColor());
            if ((int) border.bottom() == 1) {
            	final Shape line = generateBorderShape(bounds, currentSide, border, false, offset, scale);
            	outputDevice.draw(line);
            } else {
            	final Shape line = generateBorderShape(bounds, currentSide, border, true, offset, scale);
                // use polygons for borders over 1px wide
                outputDevice.fill(line);
            }
        } else if (currentSide == BorderPainter.RIGHT) {
            outputDevice.setColor(border.rightColor());
            if ((int) border.right() == 1) {
            	final Shape line = generateBorderShape(bounds, currentSide, border, false, offset, scale);
            	outputDevice.draw(line);
            } else {
            	final Shape line = generateBorderShape(bounds, currentSide, border, true, offset, scale);
                // use polygons for borders over 1px wide
                outputDevice.fill(line);
            }
        } else if (currentSide == BorderPainter.LEFT) {
            outputDevice.setColor(border.leftColor());
            if ((int) border.left() == 1) {
            	final Shape line = generateBorderShape(bounds, currentSide, border, false, offset, scale);
            	outputDevice.draw(line);
            } else {
            	final Shape line = generateBorderShape(bounds, currentSide, border, true, offset, scale);
                // use polygons for borders over 1px wide
                outputDevice.fill(line);
            }
        }
    }
}

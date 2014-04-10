package org.xhtmlrenderer.css.style;

import org.xhtmlrenderer.css.parser.PropertyValue;

public class BorderRadiusCorner 
{
	private final PropertyValue one;
	private final PropertyValue two;
	
	public BorderRadiusCorner(final PropertyValue p1, final PropertyValue p2) 
	{
		one = p1;
		two = p2;
	}

	public static final BorderRadiusCorner ZERO = new BorderRadiusCorner(null, null);

	public PropertyValue getRadiusOne()
	{
		return one;
	}

	public PropertyValue getRadiusTwo()
	{
		return two;
	}
}

/*
 * Breaker.java
 * Copyright (c) 2004, 2005 Torbjoern Gannholm,
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
 *
 */
package org.xhtmlrenderer.layout;

import java.text.BreakIterator;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.render.FSFont;

/**
 * A utility class that scans the text of a single inline box, looking for the
 * next break point.
 * @author Torbjoern Gannholm
 */
public class Breaker {

    public static void breakFirstLetter(final LayoutContext c, final LineBreakContext context,
            final int avail, final CalculatedStyle style) {
        final FSFont font = style.getFSFont(c);
        context.setEnd(getFirstLetterEnd(context.getMaster(), context.getStart()));
        context.setWidth(c.getTextRenderer().getWidth(
                c.getFontContext(), font, context.getCalculatedSubstring()));

        if (context.getWidth() > avail) {
            context.setNeedsNewLine(true);
            context.setUnbreakable(true);
        }
    }

    private static int getFirstLetterEnd(final String text, final int start) {
        boolean letterFound = false;
        final int end = text.length();
        char currentChar;
        for ( int i = start; i < end; i++ ) {
            currentChar = text.charAt(i);
            if (!TextUtil.isFirstLetterSeparatorChar(currentChar)) {
                if (letterFound) {
                    return i;
                } else {
                    letterFound = true;
                }
            }
        }
        return end;
    }

    public static void breakText(final LayoutContext c,
            final LineBreakContext context, final int avail, final CalculatedStyle style) {
        final FSFont font = style.getFSFont(c);
        final IdentValue whitespace = style.getWhitespace();

        // ====== handle nowrap
        if (whitespace == IdentValue.NOWRAP) {
        	context.setEnd(context.getLast());
        	context.setWidth(c.getTextRenderer().getWidth(
                    c.getFontContext(), font, context.getCalculatedSubstring()));
            return;
        }

        //check if we should break on the next newline
        if (whitespace == IdentValue.PRE ||
                whitespace == IdentValue.PRE_WRAP ||
                whitespace == IdentValue.PRE_LINE) {
            final int n = context.getStartSubstring().indexOf(WhitespaceStripper.EOL);

            if (n > -1) {
                context.setEnd(context.getStart() + n + 1);
                context.setWidth(c.getTextRenderer().getWidth(
                        c.getFontContext(), font, context.getCalculatedSubstring()));
                context.setNeedsNewLine(true);
                context.setEndsOnNL(true);
            } else if (whitespace == IdentValue.PRE) {
            	context.setEnd(context.getLast());
                context.setWidth(c.getTextRenderer().getWidth(
                        c.getFontContext(), font, context.getCalculatedSubstring()));
            }
        }

        //check if we may wrap
        if (whitespace == IdentValue.PRE ||
                (context.isNeedsNewLine() && context.getWidth() <= avail)) {
            return;
        }

        context.setEndsOnNL(false);
        doBreakText(c, context, avail, style, false);
    }

    private static void doBreakText(final LayoutContext c,
            final LineBreakContext context, final int avail, final CalculatedStyle style,
            final boolean tryToBreakAnywhere)
    {
        final String currentString = context.getStartSubstring();

        final BreakIterator iter = c.getTextBreaker();
        iter.setText(currentString);

        final FSFont font = style.getFSFont(c);
        int width = 0;
        int next = 0;
        int last = 0;
        
        if (currentString.length() >= 5)
        {
        	// First we get the width of the first five characters.
        	// This should give us a crude idea of the average width of a char.
        	final float widthChar5 = c.getTextRenderer().getWidth(
        			c.getFontContext(), font, currentString.substring(0, 4));
        	
        	final float sampledCharLength = widthChar5 / 5; 

        	int estimate = 0;
        	        	
        	if (sampledCharLength != 0)
        		estimate = (int) (avail / sampledCharLength); 

            // Now iterate the possible line breaks until we reach the estimate.
            do
            {
            	next = iter.next();
            	if (next == BreakIterator.DONE)
            		break;
            	last = next;
            }
            while (next < estimate);

            // Next, measure our text at the break point.
            final String broken = currentString.substring(0, last);
            width = c.getTextRenderer().getWidth(c.getFontContext(), font, broken);
        }

        // If we still have room go to one break past.
        while (width < avail)
        {
        	next = iter.next();
        	if (next == BreakIterator.DONE)
        		break;
        	last = next;
        	final String broken = currentString.substring(0, next);
        	width = c.getTextRenderer().getWidth(c.getFontContext(), font, broken);
        }
        
        if (width >= avail)
        	context.setNeedsNewLine(true);
        
        while (width >= avail)
        {
        	next = iter.previous();
        	if (next == 0 || next == BreakIterator.DONE)
        		break;
        	last = next;
        	final String broken = currentString.substring(0, next);
        	width = c.getTextRenderer().getWidth(c.getFontContext(), font, broken);        	
        }
        
        if (width >= avail && !tryToBreakAnywhere)
        	context.setUnbreakable(true);
        else if (width >= avail)
        {
            while (width >= avail && last > 0)
            {
            	final String broken = currentString.substring(0, last);
            	width = c.getTextRenderer().getWidth(c.getFontContext(), font, broken);
            	last--;
            }
        }
        
        context.setWidth(width);
        context.setEnd(context.getStart() + last);
    }

}


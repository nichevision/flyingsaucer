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
package org.xhtmlrenderer.layout;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.util.Uu;


/**
 * Description of the Class
 *
 * @author   empty
 */
public class TextUtil {

    /**
     * Description of the Method
     *
     * @param text   PARAM
     * @param style
     * @return       Returns
     */
    public static String transformText( String text, CalculatedStyle style ) {
        IdentValue transform = style.getIdent( CSSName.TEXT_TRANSFORM );
        if ( transform == IdentValue.LOWERCASE ) {
            text = text.toLowerCase();
        }
        if ( transform == IdentValue.UPPERCASE ) {
            text = text.toUpperCase();
        }
        if ( transform == IdentValue.CAPITALIZE ) {
            text = capitalizeWords( text );
        }
        IdentValue fontVariant = style.getIdent( CSSName.FONT_VARIANT );
        if ( fontVariant == IdentValue.SMALL_CAPS ) {
            text = text.toUpperCase();
        }
        return text;
    }

    /**
     * Description of the Method
     *
     * @param text   PARAM
     * @param style
     * @return       Returns
     */
    public static String transformFirstLetterText( String text, CalculatedStyle style ) {
        if (text.length() > 0) {
            IdentValue transform = style.getIdent( CSSName.TEXT_TRANSFORM );
            IdentValue fontVariant = style.getIdent( CSSName.FONT_VARIANT );
            char currentChar;
            for ( int i = 0, end = text.length(); i < end; i++ ) {
                currentChar = text.charAt(i);
                if ( !isFirstLetterSeparatorChar( currentChar ) ) {
                    if ( transform == IdentValue.LOWERCASE ) {
                        currentChar = Character.toLowerCase( currentChar );
                        text = replaceChar( text, currentChar, i );
                    } else if ( transform == IdentValue.UPPERCASE || transform == IdentValue.CAPITALIZE || fontVariant == IdentValue.SMALL_CAPS ) {
                        currentChar = Character.toUpperCase( currentChar );
                        text = replaceChar( text, currentChar, i );
                    }
                    break;
                }
            }
        }
        return text;
    }

    /**
     * Replace character at the specified index by another.
     *
     * @param text    Source text
     * @param newChar Replacement character
     * @return        Returns the new text
     */
    public static String replaceChar( String text, char newChar, int index ) {
        int textLength = text.length();
        StringBuilder b = new StringBuilder(textLength);
        for (int i = 0; i < textLength; i++) {
            if (i == index) {
                b.append(newChar);
            } else {
                b.append(text.charAt(i));
            }
        }
        return b.toString();
    }

    /**
     * Description of the Method
     *
     * @param c     PARAM
     * @return      Returns
     */
    public static boolean isFirstLetterSeparatorChar( char c ) {
        switch (Character.getType(c)) {
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
            case Character.SPACE_SEPARATOR:
                return true;
            default:
                return false;
        }
    }


    /**
     * Description of the Method
     *
     * @param text  PARAM
     * @return      Returns
     */
    private static String capitalizeWords( String text ) {
        //Uu.p("start = -"+text+"-");
        if ( text.length() == 0 ) {
            return text;
        }

        StringBuffer sb = new StringBuffer();
        //Uu.p("text = -" + text + "-");

        // do first letter
        //Uu.p("first = " + text.substring(0,1));
        boolean cap = true;
        for ( int i = 0; i < text.length(); i++ ) {
            String ch = text.substring( i, i + 1 );
            //Uu.p("ch = " + ch + " cap = " + cap);


            if ( cap ) {
                sb.append( ch.toUpperCase() );
            } else {
                sb.append( ch );
            }
            cap = false;
            if ( ch.equals( " " ) ) {
                cap = true;
            }
        }

        //Uu.p("final = -"+sb.toString()+"-");
        if ( sb.toString().length() != text.length() ) {
            Uu.p( "error! to strings arent the same length = -" + sb.toString() + "-" + text + "-" );
        }
        return sb.toString();
    }

}

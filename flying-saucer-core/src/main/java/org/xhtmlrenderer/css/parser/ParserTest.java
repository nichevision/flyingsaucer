/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
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
package org.xhtmlrenderer.css.parser;

import java.io.StringReader;

import org.xhtmlrenderer.css.sheet.StylesheetInfo.CSSOrigin;

public class ParserTest {
    public static void main(final String[] args) throws Exception {
        final String test = "div { background-image: url('something') }\n";
        final StringBuffer longTest = new StringBuffer();
        for (int i = 0 ; i < 10000; i++) {
            longTest.append(test);
        }
        
        final CSSErrorHandler errorHandler = new CSSErrorHandler() {
            public void error(final String uri, final String message) {
                System.out.println(message);
            }
        };
        
        long total = 0;
        for (int i = 0; i < 40; i++) {
            final long start = System.currentTimeMillis();
            final CSSParser p = new CSSParser(errorHandler);
            p.parseStylesheet(null, CSSOrigin.AUTHOR, new StringReader(longTest.toString()));
            final long end = System.currentTimeMillis();
            // System.out.println("Took " + (end-start) + " ms");
            total += (end-start);
        }
        System.out.println("Average " + (total/10) + " ms");
        
        total = 0;
        for (int i = 0; i < 10; i++) {
            final long start = System.currentTimeMillis();
            final CSSParser p = new CSSParser(errorHandler);
            p.parseStylesheet(null, CSSOrigin.AUTHOR, new StringReader(longTest.toString()));
            final long end = System.currentTimeMillis();
            // System.out.println("Took " + (end-start) + " ms");
            total += (end-start);
        }
        System.out.println("Average " + (total/10) + " ms");
        
        final CSSParser p = new CSSParser(errorHandler);
        
        total = 0;
        for (int i = 0; i < 10; i++) {
            final long start = System.currentTimeMillis();
            for (int j = 0; j < 10000; j++) {
                p.parseStylesheet(null, CSSOrigin.AUTHOR, new StringReader(test));
            }
            final long end = System.currentTimeMillis();
            // System.out.println("Took " + (end-start) + " ms");
            total += (end-start);
        }
        System.out.println("Average " + (total/10) + " ms");
    }
}

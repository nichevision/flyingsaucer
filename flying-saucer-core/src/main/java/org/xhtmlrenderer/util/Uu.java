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
package org.xhtmlrenderer.util;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Description of the Class
 *
 * @author empty
 */
public class Uu extends Util {
    /**
     * Description of the Field
     */
    private static Util util;
    /**
     * Description of the Field
     */
    private static Util utilAsString;

    /**
     * Constructor for the Uu object
     */
    private Uu() {
        super(System.out);
    }

    /**
     * Description of the Method
     */
    public static void on() {
        init();
        util.setOn(true);
    }

    /**
     * Description of the Method
     */
    public static void off() {
        init();
        util.setOn(false);
    }

    /**
     * Description of the Method
     *
     * @param object PARAM
     */
    public static void p(Object object) {
        init();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        utilAsString.setPrintWriter(pw);
        utilAsString.print(object);// our log adds a newline
        pw.flush();
        if (XRLog.isLoggingEnabled()) {
            XRLog.general(sw.getBuffer().toString());
        }
    }

    /**
     * Description of the Method
     *
     * @param object PARAM
     */
    public static void pr(Object object) {
        init();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        utilAsString.setPrintWriter(pw);
        utilAsString.print(object);// our log adds a newline
        pw.flush();
        if (XRLog.isLoggingEnabled()) {
            XRLog.general(sw.getBuffer().toString());
        }
        //util.print( object );
    }

    /**
     * Description of the Method
     *
     * @param msec PARAM
     */
    public static void sleep(int msec) throws InterruptedException {
        Thread.sleep(msec);
    }

    /**
     * Description of the Method
     */
    public static void dump_stack() {
        p(stack_to_string(new Exception()));
    }

    /**
     * Description of the Method
     *
     * @param args PARAM
     */
    public static void main(String args[]) {
        try {
            Uu.p(new Object());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Description of the Method
     */
    private static void init() {
        if (util == null) {
            util = new Util(System.out);
        }
        if (utilAsString == null) {
            utilAsString = new Util(System.out);
        }
    }// end main()
}

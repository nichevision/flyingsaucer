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

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.*;
import java.text.DateFormat;
import java.util.*;


/**
 * Description of the Class
 *
 * @author empty
 */
public class Util {
    /**
     * Description of the Field
     */
    private PrintWriter pw = null;
    /**
     * Description of the Field
     */
    private boolean on = true;

    /**
     * Constructor for the Util object
     *
     * @param writer PARAM
     */
    public Util(final PrintWriter writer) {
        this.pw = writer;
    }

    /**
     * Constructor for the Util object
     *
     * @param out PARAM
     */
    public Util(final OutputStream out) {
        this.pw = new PrintWriter(out);
    }

    /*
     * ------------ static stuff -------------
     */
    /*
     * ---- general print functions -----
     */
    /**
     * Description of the Method
     *
     * @param o PARAM
     */
    public void print(final Object o) {
        println(o, false);
    }

    /**
     * Description of the Method
     *
     * @param o PARAM
     */
    public void println(final Object o) {
        println(o, true);
    }

    /**
     * Description of the Method
     *
     * @param o    PARAM
     * @param line PARAM
     */
    public void println(final Object o, final boolean line) {
        if (o == null) {
            ps("null");
            return;
        }
        //ps("in p: " + o.getClass());
        if (o instanceof Object[]) {
            print_array((Object[]) o);
            return;
        }
        if (o instanceof int[]) {
            print_array((int[]) o);
        }
        if (o instanceof String) {
            ps((String) o, line);
            return;
        }
        if (o instanceof Exception) {
            ps(stack_to_string((Exception) o));
            return;
        }
        if (o instanceof Vector) {
            print_vector((Vector) o);
            return;
        }
        if (o instanceof Hashtable) {
            print_hashtable((Hashtable) o);
            return;
        }
        if (o instanceof Date) {
            print_date((Date) o);
            return;
        }
        if (o instanceof Calendar) {
            print_calendar((Calendar) o);
            return;
        }

        ps(o.toString(), line);
    }


    /*
     * --- data type specific print functions ----
     */
    /**
     * Description of the Method
     *
     * @param v PARAM
     */
    public void print_vector(final Vector v) {
        ps("vector: size=" + v.size());
        for (int i = 0; i < v.size(); i++) {
            ps(v.elementAt(i).toString());
        }
    }

    /**
     * Description of the Method
     *
     * @param array PARAM
     */
    public void print_array(final int[][] array) {
        print("array: size=" + array.length + " by " + array[0].length);
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                //pr("i = " + i + " j = " + j);
                ps(array[i][j] + " ", false);
            }
            print("");
        }
    }

    /**
     * Description of the Method
     *
     * @param array PARAM
     */
    public void print_array(final Object[] array) {
        print("array: size=" + array.length);
        for (int i = 0; i < array.length; i++) {
            ps(" " + array[i].toString(), false);
        }
    }

    /**
     * Description of the Method
     *
     * @param array PARAM
     */
    public void print_array(final int[] array) {
        print("array: size=" + array.length);
        for (int i = 0; i < array.length; i++) {
            ps(" " + array[i], false);
        }
    }

    /**
     * Description of the Method
     *
     * @param h PARAM
     */
    public void print_hashtable(final Hashtable h) {
        print("hashtable size=" + h.size());
        final Enumeration keys = h.keys();
        while (keys.hasMoreElements()) {
            final String key = (String) keys.nextElement();
            print(key + " = ");
            print(h.get(key).toString());
        }
    }

    /**
     * Description of the Method
     *
     * @param array PARAM
     */
    public void print_array(final byte[] array) {
        print("byte array: size = " + array.length);
        for (int i = 0; i < array.length; i++) {
            print("" + array[i]);
        }
    }

    /**
     * Description of the Method
     *
     * @param date PARAM
     */
    public void print_date(final Date date) {
        final DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        print(date_format.format(date));
    }

    /**
     * Description of the Method
     *
     * @param cal PARAM
     */
    public void print_calendar(final Calendar cal) {
        print(cal.getTime());
    }

    /**
     * Description of the Method
     *
     * @param sec PARAM
     */
    public void printUnixtime(final long sec) {
        print(new Date(sec * 1000));
    }

    /**
     * Sets the on attribute of the Util object
     *
     * @param on The new on value
     */
    public void setOn(final boolean on) {
        this.on = on;
    }


    /**
     * Sets the printWriter attribute of the Util object
     *
     * @param writer The new printWriter value
     */
    public void setPrintWriter(final PrintWriter writer) {
        this.pw = writer;
    }

    /**
     * Description of the Method
     *
     * @param s PARAM
     */
    private void ps(final String s) {
        ps(s, true);
    }

    /**
     * Description of the Method
     *
     * @param s    PARAM
     * @param line PARAM
     */
    private void ps(final String s, final boolean line) {
        if (!on) {
            return;
        }
        if (line) {
            if (pw == null) {
                System.out.println(s);
            } else {
                //System.out.println(s);
                pw.println(s);
                //pw.println("<br>");
            }
        } else {
            if (pw == null) {
                System.out.print(s);
            } else {
                //System.out.print(s);
                pw.print(s);
                //pw.print("<br>");
            }
        }
    }


    /*
     * ----- other stuff ----
     */
    /**
     * Description of the Method
     *
     * @param filename PARAM
     * @return Returns
     * @throws FileNotFoundException Throws
     * @throws IOException           Throws
     */
    public static String file_to_string(final String filename)
            throws FileNotFoundException, IOException {
        final File file = new File(filename);
        return file_to_string(file);
    }

    /**
     * Description of the Method
     *
     * @param text PARAM
     * @param file PARAM
     * @throws IOException Throws
     */
    public static void string_to_file(final String text, final File file)
            throws IOException {
        FileWriter writer = null;
        writer = new FileWriter(file);
        try {
            final StringReader reader = new StringReader(text);
            final char[] buf = new char[1000];
            while (true) {
                final int n = reader.read(buf, 0, 1000);
                if (n == -1) {
                    break;
                }
                writer.write(buf, 0, n);
            }
            writer.flush();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Description of the Method
     *
     * @param str PARAM
     * @return Returns
     */
    public static int string_to_int(final String str) {
        return Integer.parseInt(str);
    }

    /**
     * Description of the Method
     *
     * @param e PARAM
     * @return Returns
     */
    public static String stack_to_string(final Exception e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * Description of the Method
     *
     * @param e PARAM
     * @return Returns
     */
    public static String stack_to_string(final Throwable e) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * Description of the Method
     *
     * @param in PARAM
     * @return Returns
     * @throws IOException Throws
     */
    public static String inputstream_to_string(final InputStream in)
            throws IOException {
        final Reader reader = new InputStreamReader(in);
        final StringWriter writer = new StringWriter();
        final char[] buf = new char[1000];
        while (true) {
            final int n = reader.read(buf, 0, 1000);
            if (n == -1) {
                break;
            }
            writer.write(buf, 0, n);
        }
        return writer.toString();
    }

    /**
     * Description of the Method
     *
     * @param file PARAM
     * @return Returns
     * @throws FileNotFoundException Throws
     * @throws IOException           Throws
     */
    public static String file_to_string(final File file)
            throws IOException {
        FileReader reader = null;
        StringWriter writer = null;
        String str;
        try {
            reader = new FileReader(file);
            writer = new StringWriter();
            final char[] buf = new char[1000];
            while (true) {
                final int n = reader.read(buf, 0, 1000);
                if (n == -1) {
                    break;
                }
                writer.write(buf, 0, n);
            }
            str = writer.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
        return str;
    }

    /**
     * Description of the Method
     *
     * @param source      PARAM
     * @param target      PARAM
     * @param replacement PARAM
     * @return Returns
     */
    public static String replace(final String source, final String target, final String replacement) {
        final StringBuffer output = new StringBuffer();
        int n = 0;
        while (true) {
            //print("n = " + n);
            final int off = source.indexOf(target, n);
            if (off == -1) {
                output.append(source.substring(n));
                break;
            }
            output.append(source.substring(n, off));
            output.append(replacement);
            n = off + target.length();
        }
//        output.append(source.substring(off+target.length()));
        return output.toString();
    }

    /**
     * Description of the Method
     *
     * @param v PARAM
     * @return Returns
     */
    public static String[] vector_to_strings(final Vector v) {
        final int len = v.size();
        final String[] ret = new String[len];
        for (int i = 0; i < len; i++) {
            ret[i] = v.elementAt(i).toString();
        }
        return ret;
    }

    /**
     * Description of the Method
     *
     * @param l PARAM
     * @return Returns
     */
    public static String[] list_to_strings(final List l) {
        final int len = l.size();
        final String[] ret = new String[len];
        for (int i = 0; i < len; i++) {
            ret[i] = l.get(i).toString();
        }
        return ret;
    }

    /**
     * Description of the Method
     *
     * @param array PARAM
     * @return Returns
     */
    public static List<Object> toList(final Object[] array) {
        return to_list(array);
    }

    /**
     * Description of the Method
     *
     * @param array PARAM
     * @return Returns
     */
    public static List<Object> to_list(final Object[] array) {
        final List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }

    /*
     * public void pr(Date date) {
     * DateFormat date_format = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
     * pr(date_format.format(date));
     * }
     */
    /*
     * public void pr(Calendar cal) {
     * pr(cal.getTime());
     * }
     */

    /**
     * Description of the Method
     *
     * @param msec PARAM
     */
    public static void sleep(final long msec) {
        try {
            Thread.sleep(msec);
        } catch (final InterruptedException ex) {
            org.xhtmlrenderer.util.Uu.p(stack_to_string(ex));
        }
    }

    /**
     * Description of the Method
     *
     * @param frame PARAM
     */
    public static void center(final JFrame frame) {
        //p("centering");
        final Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int) ((screen_size.getWidth() - frame.getWidth()) / 2),
                (int) ((screen_size.getHeight() - frame.getHeight()) / 2));
    }

    /**
     * Description of the Method
     *
     * @param frame PARAM
     */
    public static void center(final JDialog frame) {
        //p("centering");
        final Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int) ((screen_size.getWidth() - frame.getWidth()) / 2),
                (int) ((screen_size.getHeight() - frame.getHeight()) / 2));
    }


    /**
     * Gets the number attribute of the Util class
     *
     * @param str PARAM
     * @return The number value
     */
    public static boolean isNumber(final String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNullOrEmpty(final String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNullOrEmpty(final String str, final boolean trim) {
        return str == null || str.length() == 0 || (trim && str.trim().length() == 0);
    }

    public static boolean isEqual(final String str1, final String str2) {
        return str1 == str2 || (str1 != null && str1.equals(str2));
    }

    public static boolean isEqual(final String str1, final String str2, final boolean ignoreCase) {
        return str1 == str2 || (str1 != null && (ignoreCase ? str1.equalsIgnoreCase(str2) : str1.equals(str2)));
    }
}

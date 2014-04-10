/*
 * {{{ header & license
 * Copyright (c) 2004, 2005, 2008 Joshua Marinacci, Patrick Wright
 * Copyright (c) 2008 Patrick Wright
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


/**
 * Utility class for using the java.util.logging package. Relies on the standard
 * configuration for logging, but gives easier access to the various logs
 * (plumbing.load, .init, .render)
 *
 * @author empty
 */
public class XRLog {
    private static final List<String> LOGGER_NAMES = new ArrayList<String>(20);
    public final static String CONFIG = registerLoggerByName("org.xhtmlrenderer.config");
    public final static String EXCEPTION = registerLoggerByName("org.xhtmlrenderer.exception");
    public final static String GENERAL = registerLoggerByName("org.xhtmlrenderer.general");
    public final static String INIT = registerLoggerByName("org.xhtmlrenderer.init");
    public final static String JUNIT = registerLoggerByName("org.xhtmlrenderer.junit");
    public final static String LOAD = registerLoggerByName("org.xhtmlrenderer.load");
    public final static String MATCH = registerLoggerByName("org.xhtmlrenderer.match");
    public final static String CASCADE = registerLoggerByName("org.xhtmlrenderer.cascade");
    public final static String XML_ENTITIES = registerLoggerByName("org.xhtmlrenderer.load.xml-entities");
    public final static String CSS_PARSE = registerLoggerByName("org.xhtmlrenderer.css-parse");
    public final static String LAYOUT = registerLoggerByName("org.xhtmlrenderer.layout");
    public final static String RENDER = registerLoggerByName("org.xhtmlrenderer.render");

    private static String registerLoggerByName(final String loggerName) {
        LOGGER_NAMES.add(loggerName);
        return loggerName;
    }

    private static boolean initPending = true;
    private static XRLogger loggerImpl;

    private static boolean loggingEnabled = true;

    /**
     * Returns a list of all loggers that will be accessed by XRLog. Each entry is a String with a logger
     * name, which can be used to retrieve the logger using the corresponding Logging API; example name might be
     * "org.xhtmlrenderer.config"
     *
     * @return List of loggers, never null.
     */
    public static List<String> listRegisteredLoggers() {
        // defensive copy
        return new ArrayList<String>(LOGGER_NAMES);
    }


    public static void cssParse(final String msg) {
        cssParse(Level.INFO, msg);
    }

    public static void cssParse(final Level level, final String msg) {
        log(CSS_PARSE, level, msg);
    }

    public static void cssParse(final Level level, final String msg, final Throwable th) {
        log(CSS_PARSE, level, msg, th);
    }

    public static void xmlEntities(final String msg) {
        xmlEntities(Level.INFO, msg);
    }

    public static void xmlEntities(final Level level, final String msg) {
        log(XML_ENTITIES, level, msg);
    }

    public static void xmlEntities(final Level level, final String msg, final Throwable th) {
        log(XML_ENTITIES, level, msg, th);
    }

    public static void cascade(final String msg) {
        cascade(Level.INFO, msg);
    }

    public static void cascade(final Level level, final String msg) {
        log(CASCADE, level, msg);
    }

    public static void cascade(final Level level, final String msg, final Throwable th) {
        log(CASCADE, level, msg, th);
    }

    public static void exception(final String msg) {
        exception(msg, null);
    }

    public static void exception(final String msg, final Throwable th) {
        log(EXCEPTION, Level.WARNING, msg, th);
    }

    public static void general(final String msg) {
        general(Level.INFO, msg);
    }

    public static void general(final Level level, final String msg) {
        log(GENERAL, level, msg);
    }

    public static void general(final Level level, final String msg, final Throwable th) {
        log(GENERAL, level, msg, th);
    }

    public static void init(final String msg) {
        init(Level.INFO, msg);
    }

    public static void init(final Level level, final String msg) {
        log(INIT, level, msg);
    }

    public static void init(final Level level, final String msg, final Throwable th) {
        log(INIT, level, msg, th);
    }

    public static void junit(final String msg) {
        junit(Level.FINEST, msg);
    }

    public static void junit(final Level level, final String msg) {
        log(JUNIT, level, msg);
    }

    public static void junit(final Level level, final String msg, final Throwable th) {
        log(JUNIT, level, msg, th);
    }

    public static void load(final String msg) {
        load(Level.INFO, msg);
    }

    public static void load(final Level level, final String msg) {
        log(LOAD, level, msg);
    }

    public static void load(final Level level, final String msg, final Throwable th) {
        log(LOAD, level, msg, th);
    }

    public static void match(final String msg) {
        match(Level.INFO, msg);
    }

    public static void match(final Level level, final String msg) {
        log(MATCH, level, msg);
    }

    public static void match(final Level level, final String msg, final Throwable th) {
        log(MATCH, level, msg, th);
    }

    public static void layout(final String msg) {
        layout(Level.INFO, msg);
    }

    public static void layout(final Level level, final String msg) {
        log(LAYOUT, level, msg);
    }

    public static void layout(final Level level, final String msg, final Throwable th) {
        log(LAYOUT, level, msg, th);
    }

    public static void render(final String msg) {
        render(Level.INFO, msg);
    }

    public static void render(final Level level, final String msg) {
        log(RENDER, level, msg);
    }

    public static void render(final Level level, final String msg, final Throwable th) {
        log(RENDER, level, msg, th);
    }

    public static synchronized void log(final String where, final Level level, final String msg) {
        if (initPending) {
            init();
        }
        if (isLoggingEnabled()) {
            loggerImpl.log(where, level, msg);
        }
    }

    public static synchronized void log(final String where, final Level level, final String msg, final Throwable th) {
        if (initPending) {
            init();
        }
        if (isLoggingEnabled()) {
            loggerImpl.log(where, level, msg, th);
        }
    }

    public static void main(final String args[]) {
        try {
            XRLog.cascade("Cascade msg");
            XRLog.cascade(Level.WARNING, "Cascade msg");
            XRLog.exception("Exception msg");
            XRLog.exception("Exception msg", new Exception());
            XRLog.general("General msg");
            XRLog.general(Level.WARNING, "General msg");
            XRLog.init("Init msg");
            XRLog.init(Level.WARNING, "Init msg");
            XRLog.load("Load msg");
            XRLog.load(Level.WARNING, "Load msg");
            XRLog.match("Match msg");
            XRLog.match(Level.WARNING, "Match msg");
            XRLog.layout("Layout msg");
            XRLog.layout(Level.WARNING, "Layout msg");
            XRLog.render("Render msg");
            XRLog.render(Level.WARNING, "Render msg");
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void init() {
        synchronized (XRLog.class) {
            if (!initPending) {
                return;
            }

            XRLog.setLoggingEnabled(Configuration.isTrue("xr.util-logging.loggingEnabled", true));

            if (loggerImpl == null) {
                loggerImpl = new JDKXRLogger();
            }

            initPending = false;
        }
    }

    public static synchronized void setLevel(final String log, final Level level) {
        if (initPending) {
            init();
        }
        loggerImpl.setLevel(log, level);
    }

    /**
     * Whether logging is on or off.
     * @return Returns true if logging is enabled, false if not. Corresponds
     * to configuration file property xr.util-logging.loggingEnabled, or to
     * value passed to setLoggingEnabled(bool).
     */
    public static synchronized boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    /**
     * Turns logging on or off, without affecting logging configuration.
     *
     * @param loggingEnabled Flag whether logging is enabled or not;
     * if false, all logging calls fail silently. Corresponds
     * to configuration file property xr.util-logging.loggingEnabled
     */
    public static synchronized void setLoggingEnabled(final boolean loggingEnabled) {
        XRLog.loggingEnabled = loggingEnabled;
    }

    public static synchronized XRLogger getLoggerImpl() {
        return loggerImpl;
    }

    public static synchronized void setLoggerImpl(final XRLogger loggerImpl) {
        XRLog.loggerImpl = loggerImpl;
    }
}// end class

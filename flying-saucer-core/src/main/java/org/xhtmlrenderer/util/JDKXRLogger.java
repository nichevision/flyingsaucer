/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Joshua Marinacci
 * Copyright (c) 2007 Wisconsin Court System
 * Copyright (c) 2008 Patrick Wright
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
package org.xhtmlrenderer.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.Formatter;
import java.util.Properties;
import java.util.Iterator;
import java.util.List;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * An {@link XRLogger} interface that uses <code>java.util.logging</code>.
 */
public class JDKXRLogger implements XRLogger {
    private static boolean initPending = true;
    
    /** {@inheritdoc} */
    public void log(final String where, final Level level, final String msg) {
        if (initPending) {
            init();
        }

        getLogger(where).log(level, msg);
    }

    /** {@inheritdoc} */
    public void log(final String where, final Level level, final String msg, final Throwable th) {
        if (initPending) {
            init();
        }

        getLogger(where).log(level, msg, th);
    }

    /** {@inheritdoc} */
    public void setLevel(final String logger, final Level level) {
        getLogger(logger).setLevel(level);
    }

    /**
     * Same purpose as Logger.getLogger(), except that the static initialization
     * for XRLog will initialize the LogManager with logging levels and other
     * configuration. Use this instead of Logger.getLogger()
     *
     * @param log PARAM
     * @return The logger value
     */
    private static Logger getLogger(final String log) {
        return Logger.getLogger(log);
    }

    private static void init() {
        synchronized (JDKXRLogger.class) {
            if (!initPending) {
                return;
            }
            //now change this immediately, in case something fails
            initPending = false;
            try {
                final Properties props = retrieveLoggingProperties();

                if(!XRLog.isLoggingEnabled()) {
                    Configuration.setConfigLogger(Logger.getLogger(XRLog.CONFIG));
                    return;
                }
                initializeJDKLogManager(props);

                Configuration.setConfigLogger(Logger.getLogger(XRLog.CONFIG));
            } catch (final SecurityException e) {
                // may happen in a sandbox environment
            } catch (final FileNotFoundException e) {
                throw new XRRuntimeException("Could not initialize logs. " + e.getLocalizedMessage(), e);
            } catch (final IOException e) {
                throw new XRRuntimeException("Could not initialize logs. " + e.getLocalizedMessage(), e);
            }
        }
    }

    private static Properties retrieveLoggingProperties() {
        // pull logging properties from configuration
        // they are all prefixed as shown
        final String prefix = "xr.util-logging.";
        final Iterator<String> iter = Configuration.keysByPrefix(prefix);
        final Properties props = new Properties();
        while (iter.hasNext()) {
            final String fullkey = (String) iter.next();
            final String lmkey = fullkey.substring(prefix.length());
            final String value = Configuration.valueFor(fullkey);
            props.setProperty(lmkey, value);
        }
        return props;
    }

    private static void initializeJDKLogManager(final Properties fsLoggingProperties) throws IOException {
        final List<Logger> loggers = retrieveLoggers();

        configureLoggerHandlerForwarding(fsLoggingProperties, loggers);

        // load our properties into our log manager
        final Enumeration keys = fsLoggingProperties.keys();
        Map<String, Handler> handlers = new HashMap<String, Handler>();
        final Map<String, String> handlerFormatterMap = new HashMap<String, String>();
        while (keys.hasMoreElements()) {
            final String key = (String) keys.nextElement();
            final String prop = fsLoggingProperties.getProperty(key);
            if (key.endsWith("level")) {
                configureLogLevel(key.substring(0, key.lastIndexOf(".")), prop);
            } else if (key.endsWith("handlers")) {
                handlers = configureLogHandlers(loggers, prop);
            } else if (key.endsWith("formatter")) {
                final String k2 = key.substring(0, key.length() - ".formatter".length());
                handlerFormatterMap.put(k2, prop);
            }
        }

        // formatters apply to a specific handler we have initialized previously,
        // hence we need to wait until we've parsed the handler class
        for (final Iterator<String> it = handlerFormatterMap.keySet().iterator(); it.hasNext();) {
            final String handlerClassName = it.next();
            final String formatterClassName = handlerFormatterMap.get(handlerClassName);
            assignFormatter(handlers, handlerClassName, formatterClassName);
        }
    }

    private static void configureLoggerHandlerForwarding(final Properties fsLoggingProperties, final List<Logger> loggers) {
        final String val = fsLoggingProperties.getProperty("use-parent-handler");

        final boolean flag = val == null ? false : Boolean.valueOf(val).booleanValue();
        for (final Iterator<Logger> it = loggers.iterator(); it.hasNext();) {
            final Logger logger = it.next();
            logger.setUseParentHandlers(flag);
        }
    }

    private static void assignFormatter(final Map<String, Handler> handlers, final String handlerClassName, final String formatterClassName) {
        final Handler handler = handlers.get(handlerClassName);
        if (handler != null) {
            try {
                final Class<?> fclass = Class.forName(formatterClassName);
                final Formatter f = (Formatter) fclass.newInstance();
                handler.setFormatter(f);
            } catch (final ClassNotFoundException e) {
                throw new XRRuntimeException("Could not initialize logging properties; " +
                        "Formatter class not found: " + formatterClassName);
            } catch (final IllegalAccessException e) {
                throw new XRRuntimeException("Could not initialize logging properties; " +
                        "Can't instantiate Formatter class (IllegalAccessException): " + formatterClassName);
            } catch (final InstantiationException e) {
                throw new XRRuntimeException("Could not initialize logging properties; " +
                        "Can't instantiate Formatter class (InstantiationException): " + formatterClassName);
            }
        }
    }

    /* HACK: if loggers are not sequestered as strong references, they get garbage collected, losing their configuration */
    public static List<Logger> savedLoggers = null;

    /**
     * Returns a List of all Logger instances used by Flying Saucer from the JDK LogManager; these will
     * be automatically created if they aren't already available.
     */
    private static List<Logger> retrieveLoggers() {
        final List<String> loggerNames = XRLog.listRegisteredLoggers();
        final List<Logger> loggers = new ArrayList<Logger>(loggerNames.size());
        final Iterator<String> it = loggerNames.iterator();
        while (it.hasNext()) {
            final String ln = (String) it.next();
            loggers.add(Logger.getLogger(ln));
        }
        savedLoggers = loggers;
        return loggers;
    }

    /**
     * For each logger provided, assigns the logger an instance of the named log output handlers. Will attempt
     * to instantiate each handler; any which can't be instantiated will cause the method to throw a RuntimeException.
     *
     * @param loggers List of Logger instances.
     * @param handlerClassList A space-separated string (following the configuration convention for JDK logging
     * configuration files, for handlers) of FQN of log handlers.
     *
     * @return Map of handler class names to handler instances.
     */
    private static Map<String, Handler> configureLogHandlers(final List<Logger> loggers, final String handlerClassList) {
        final String[] names = handlerClassList.split(" ");
        final Map<String, Handler> handlers = new HashMap<String, Handler>(names.length);
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            try {
                final Class<?> handlerClass = Class.forName(name);
                final Handler handler = (Handler) handlerClass.newInstance();
                handlers.put(name, handler);
                final String hl = Configuration.valueFor("xr.util-logging." + name + ".level", "INFO");
                handler.setLevel(LoggerUtil.parseLogLevel(hl, Level.INFO));
            } catch (final ClassNotFoundException e) {
                throw new XRRuntimeException("Could not initialize logging properties; " +
                        "Handler class not found: " + name);
            } catch (final IllegalAccessException e) {
                throw new XRRuntimeException("Could not initialize logging properties; " +
                        "Can't instantiate Handler class (IllegalAccessException): " + name);
            } catch (final InstantiationException e) {
                throw new XRRuntimeException("Could not initialize logging properties; " +
                        "Can't instantiate Handler class (InstantiationException): " + name);
            }
        }

        // now assign each handler to each FS logger
        for (final Iterator<Logger> iterator = loggers.iterator(); iterator.hasNext();) {
            final Logger logger = iterator.next();
            for (final Iterator<Handler> ith = handlers.values().iterator(); ith.hasNext();) {
                final Handler handler = ith.next();
                logger.addHandler(handler);
            }
        }
        return handlers;
    }

    /**
     * Parses the levelValue into a Level instance and assigns to the Logger instance named by loggerName; if the
     * the levelValue is invalid (e.g. misspelled), assigns Level.OFF to the logger.
     */
    private static void configureLogLevel(final String loggerName, final String levelValue) {
        final Level level = LoggerUtil.parseLogLevel(levelValue, Level.OFF);
        final Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(level);
    }
}

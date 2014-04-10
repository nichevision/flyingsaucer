/*
 * {{{ header & license
 * Copyright (c) 2004 Joshua Marinacci
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
package org.xhtmlrenderer.demo.browser;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EtchedBorder;

import org.xhtmlrenderer.util.GeneralUtil;
import org.xhtmlrenderer.util.XRLog;

/**
 * Description of the Class
 *
 * @author empty
 */
public class BrowserStartup {
    /**
     * Description of the Field
     */
    public BrowserPanel panel;
    /**
     * Description of the Field
     */
    protected BrowserMenuBar menu;
    /**
     * Description of the Field
     */
    protected JFrame frame;
    /**
     * Description of the Field
     */
    protected JFrame validation_console = null;
    /**
     * Description of the Field
     */
    protected BrowserActions actions;
    /**
     * Page to view at startup
     */
    protected String startPage;

    /**
     * Description of the Field
     */
    protected ValidationHandler error_handler = new ValidationHandler();
    /**
     * Description of the Field
     */
    public static final Logger logger = Logger.getLogger("app.browser");

    /**
     * Constructor for the BrowserStartup object
     */
    public BrowserStartup() {
        this("demo:demos/splash/splash.html");
    }

    /**
     * Constructor for the BrowserStartup object
     */
    public BrowserStartup(final String startPage) {
        logger.info("starting up");
        this.startPage = startPage;
    }

    /**
     * Initializes all UI components but does not display frame and does not load any pages.
     */
    public void initUI() {
        if (GeneralUtil.isMacOSX()) {
            try {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "FS Browser");
            } catch (final Exception ex) {
                try {
                    logger.log(Level.SEVERE, "error initalizing the mac properties", ex);
                } catch (final Exception ex2) {
                    //System.out.println("error writing to the log file!" + ex2);
                    //ex2.printStackTrace();
                }
            }
        } else {
            setLookAndFeel();
        }

        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame = frame;
        logger.info("creating UI");
        actions = new BrowserActions(this);
        actions.init();

        panel = new BrowserPanel(this, new FrameBrowserPanelListener());
        panel.init();
        panel.createActions();

        menu = new BrowserMenuBar(this);
        menu.init();
        menu.createLayout();
        menu.createActions();

        frame.setJMenuBar(menu);

        frame.getContentPane().add(panel.toolbar, BorderLayout.PAGE_START);
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
        frame.getContentPane().add(panel.status, BorderLayout.PAGE_END);
        frame.pack();
        frame.setSize(1024, 768);
    }

    /**
     * The main program for the BrowserStartup class
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                final BrowserStartup bs = new BrowserStartup();
                bs.initUI();
                bs.launch();
            }
        });
    }

    /**
     * Loads the first page (specified in the constructor) and shows the frame.
     */
    public void launch() {
        try {
            panel.loadPage(startPage);

            frame.setVisible(true);
        } catch (final Exception ex) {
            XRLog.general(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static void setLookAndFeel() {
        boolean lnfSet = false;
        try {
            UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
            lnfSet = true;
        } catch (final Throwable th) {
        }
        if (!lnfSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                lnfSet = true;
            } catch (final Throwable th) {
                th.printStackTrace();
            }
        }
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    class FrameBrowserPanelListener implements BrowserPanelListener {
        /**
         * Description of the Method
         *
         * @param url   PARAM
         * @param title PARAM
         */
        public void pageLoadSuccess(final String url, final String title) {
            frame.setTitle(title + (title.length() > 0 ? " - " : "") + "Flying Saucer");
        }
    }

}

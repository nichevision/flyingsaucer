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
package org.xhtmlrenderer.demo.aboutbox;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

import org.xhtmlrenderer.util.Uu;

import com.github.danfickle.flyingsaucer.swing.XHTMLPanel;


/**
 * Description of the Class
 *
 * @author empty
 */
public class AboutBox extends JDialog implements Runnable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Description of the Field
     */
    JScrollPane scroll;
    /**
     * Description of the Field
     */
    JButton close_button;
    /**
     * Description of the Field
     */
    boolean go = false;

    /**
     * Description of the Field
     */
    Thread thread;

    /**
     * Constructor for the AboutBox object
     *
     * @param text PARAM
     * @param url  PARAM
     */
    public AboutBox(final String text, final String url) {
        super();
        Uu.p("starting the about box");
        setTitle(text);
        final XHTMLPanel panel = new XHTMLPanel(new DemoUserAgent());
        final int w = 400;
        final int h = 500;
        panel.setPreferredSize(new Dimension(w, h));

        scroll = new JScrollPane(panel);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(new Dimension(w, h));
        //panel.setViewportComponent(scroll);
        //panel.setJScrollPane(scroll);
        getContentPane().add(scroll, "Center");
        close_button = new JButton("Close");
        getContentPane().add(close_button, "South");
        close_button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                setVisible(false);
                go = false;
            }
        });

        try {
            loadPage(url, panel);
        } catch (final Exception ex) {
            Uu.p(ex);
        }
        pack();
        setSize(w, h);
        final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - w) / 2, (screen.height - h) / 2);
    }

    /**
     * Description of the Method
     *
     * @param url_text PARAM
     * @param panel    PARAM
     */
    public void loadPage(final String url_text, final XHTMLPanel panel) throws MalformedURLException {
URL ref = null;

if (url_text.startsWith("demo:")) {
    Uu.p("starts with demo");
    final DemoMarker marker = new DemoMarker();
    Uu.p("url text = " + url_text);
    String short_url = url_text.substring(5);
    if (!short_url.startsWith("/")) {
        short_url = "/" + short_url;
    }
    Uu.p("short url = " + short_url);
    ref = marker.getClass().getResource(short_url);
    Uu.p("ref = " + ref);
    panel.setDocument(ref.toExternalForm());
} else if (url_text.startsWith("http")) {
    panel.setDocument(url_text);
} else {
    ref = new File(url_text).toURL();
    panel.setDocument(ref.toExternalForm());
}
Uu.p("ref = " + ref);
Uu.p("url_text = " + url_text);
}

    /**
     * Description of the Method
     */
    public void startScrolling() {
        go = true;
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Main processing method for the AboutBox object
     */
    public void run() {
        while (go) {
            try {
                Thread.sleep(100);
            } catch (final Exception ex) {
                Uu.p(ex);
            }
            final JScrollBar sb = scroll.getVerticalScrollBar();
            sb.setValue(sb.getValue() + 1);
        }
    }

    /**
     * Sets the visible attribute of the AboutBox object
     *
     * @param vis The new visible value
     */
    public void setVisible(final boolean vis) {
        super.setVisible(vis);
        if (vis == true) {
            startScrolling();
        }
    }

    /**
     * The main program for the AboutBox class
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args) {
        final JFrame frame = new JFrame("About Box Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JButton launch = new JButton("Show About Box");
        frame.getContentPane().add(launch);
        frame.pack();
        frame.setVisible(true);

        launch.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent evt) {
                final AboutBox ab = new AboutBox("About Flying Saucer", "demo:demos/index.xhtml");
                ab.setVisible(true);
            }
        });
    }
}

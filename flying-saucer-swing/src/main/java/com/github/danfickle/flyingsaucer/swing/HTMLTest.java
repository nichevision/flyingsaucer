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
package com.github.danfickle.flyingsaucer.swing;

import org.xhtmlrenderer.swing.DOMInspector;
import org.xhtmlrenderer.util.Uu;
import org.xhtmlrenderer.util.XRLog;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;


/**
 * Description of the Class
 *
 * @author empty
 */
public class HTMLTest extends JFrame {
    private static final long serialVersionUID = 1L;

    /**
     * Description of the Field
     */
    private final XHTMLPanel panel;
    /** Description of the Field */
    //public final static int text_width = 600;
    /**
     * Description of the Field
     */
    private final static String BASE_TITLE = "Flying Saucer";

    /**
     * Constructor for the HTMLTest object
     *
     * @param args PARAM
     */
    public HTMLTest(final String[] args) {
        super(BASE_TITLE);
        panel = new XHTMLPanel();
        final int width = 360;
        final int height = 500;
        panel.setPreferredSize(new Dimension(width, height));
        final JScrollPane scroll = new JScrollPane(panel);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setPreferredSize(new Dimension(width, height));

        panel.addMouseTrackingListener(new LinkListener());

        if (args.length > 0) {
            loadDocument(args[0]);
        }

        getContentPane().add("Center", scroll);

        final JMenuBar mb = new JMenuBar();
        final JMenu file = new JMenu("File");
        mb.add(file);
        file.setMnemonic('F');
        file.add(new QuitAction());

        final JMenu view = new JMenu("View");
        mb.add(view);
        view.setMnemonic('V');
        view.add(new RefreshPageAction());
        view.add(new ReloadPageAction());


        final JMenu debug = new JMenu("Debug");
        mb.add(debug);
        debug.setMnemonic('D');

        final JMenu debugShow = new JMenu("Show");
        debug.add(debugShow);
        debugShow.setMnemonic('S');

        debugShow.add(new JCheckBoxMenuItem(new BoxOutlinesAction()));
        debugShow.add(new JCheckBoxMenuItem(new LineBoxOutlinesAction()));
        debugShow.add(new JCheckBoxMenuItem(new InlineBoxesAction()));
        debugShow.add(new JCheckBoxMenuItem(new FontMetricsAction()));


        final JMenu anti = new JMenu("Anti Aliasing");
        anti.add(new JCheckBoxMenuItem(new AntiAliasedAction("None", -1)));
        anti.add(new JCheckBoxMenuItem(new AntiAliasedAction("Low (Default)", 25)));
        anti.add(new JCheckBoxMenuItem(new AntiAliasedAction("Medium", 12)));
        anti.add(new JCheckBoxMenuItem(new AntiAliasedAction("Highest", 0)));
        debug.add(anti);

        debug.add(new ShowDOMInspectorAction());
/*
        debug.add(
                    new AbstractAction( "Print Box Tree" ) {
                        public void actionPerformed( ActionEvent evt ) {
                            panel.printTree();
                        }
                    } );
*/
        setJMenuBar(mb);
    }

    /**
     * Adds a feature to the FileLoadAction attribute of the HTMLTest object
     *
     * @param menu    The feature to be added to the FileLoadAction attribute
     * @param display The feature to be added to the FileLoadAction attribute
     * @param file    The feature to be added to the FileLoadAction attribute
     */
    public void addFileLoadAction(final JMenu menu, final String display, final String file) {
        menu.add(new AbstractAction(display) {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent evt) {
                loadDocument(file);
            }
        });
    }

    /**
     * Description of the Method
     *
     * @param uri taken to be a file, if not beginning with http://
     */
    private void loadDocument(final String uri) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    final long st = System.currentTimeMillis();

                    URL url = null;
                    if (uri.startsWith("http://"))
                        url = new URL(uri);
                    else
                        url = new File(uri).toURL();

                    System.err.println("loading " + url.toString() + "!");
                    panel.setDocument(url.toExternalForm());

                    final long el = System.currentTimeMillis() - st;
                    XRLog.general("loadDocument(" + url.toString() + ") in " + el + "ms, render may take longer");
                    HTMLTest.this.setTitle(BASE_TITLE + "-  " +
                            panel.getDocumentTitle() + "  " +
                            "(" + url.toString() + ")");
                } catch (final Exception ex) {
                    Uu.p(ex);
                }
                panel.repaint();
            }
        });
    }

    /**
     * The main program for the HTMLTest class
     *
     * @param args The command line arguments
     * @throws Exception Throws
     */
    public static void main(final String[] args)
            throws Exception {


        final JFrame frame = new HTMLTest(args);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        //frame.setSize( text_width, 300 );
        frame.setVisible(true);
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    static class QuitAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for the QuitAction object
         */
        QuitAction() {
            super("Quit");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_Q));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            System.exit(0);
        }
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    class BoxOutlinesAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for the BoxOutlinesAction object
         */
        BoxOutlinesAction() {
            super("Show Box Outlines");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_B));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            panel.getSharedContext().setDebug_draw_boxes(!panel.getSharedContext().debugDrawBoxes());
            panel.repaint();
        }
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    class LineBoxOutlinesAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for the LineBoxOutlinesAction object
         */
        LineBoxOutlinesAction() {
            super("Show Line Box Outlines");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            panel.getSharedContext().setDebug_draw_line_boxes(!panel.getSharedContext().debugDrawLineBoxes());
            panel.repaint();
        }
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    class InlineBoxesAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for the InlineBoxesAction object
         */
        InlineBoxesAction() {
            super("Show Inline Boxes");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            panel.getSharedContext().setDebug_draw_inline_boxes(!panel.getSharedContext().debugDrawInlineBoxes());
            panel.repaint();
        }
    }

    class FontMetricsAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for the InlineBoxesAction object
         */
        FontMetricsAction() {
            super("Show Font Metrics");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_F));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            panel.getSharedContext().setDebug_draw_font_metrics(!panel.getSharedContext().debugDrawFontMetrics());
            panel.repaint();
        }
    }

    class AntiAliasedAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        int fontSizeThreshold;

        AntiAliasedAction(final String text, final int fontSizeThreshold) {
            super(text);
            this.fontSizeThreshold = fontSizeThreshold;
        }

        public void actionPerformed(final ActionEvent evt) {
            panel.getSharedContext().getTextRenderer().setSmoothingThreshold(fontSizeThreshold);
            panel.repaint();
        }
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    class ShowDOMInspectorAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Description of the Field
         */
        private DOMInspector inspector;
        /**
         * Description of the Field
         */
        private JFrame inspectorFrame;

        /**
         * Constructor for the ShowDOMInspectorAction object
         */
        ShowDOMInspectorAction() {
            super("DOM Tree Inspector");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            if (inspectorFrame == null) {
                inspectorFrame = new JFrame("DOM Tree Inspector");
            }
            if (inspector == null) {
                // inspectorFrame = new JFrame("DOM Tree Inspector");

                inspector = new DOMInspector(panel.doc, panel.getSharedContext(), panel.getSharedContext().getCss());

                inspectorFrame.getContentPane().add(inspector);

                inspectorFrame.pack();
                inspectorFrame.setSize(400, 600);
                inspectorFrame.setVisible(true);
            } else {
                inspector.setForDocument(panel.doc, panel.getSharedContext(), panel.getSharedContext().getCss());
            }
            inspectorFrame.setVisible(true);
        }
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    static class RefreshPageAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for the RefreshPageAction object
         */
        RefreshPageAction() {
            super("Refresh Page");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke("F5"));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            // TODO
            System.out.println("Refresh Page triggered");
        }
    }

    /**
     * Description of the Class
     *
     * @author empty
     */
    static class ReloadPageAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for the ReloadPageAction object
         */
        ReloadPageAction() {
            super("Reload Page");
            putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
            putValue(ACCELERATOR_KEY,
                    KeyStroke.getKeyStroke(KeyEvent.VK_F5,
                            ActionEvent.CTRL_MASK));
        }

        /**
         * Description of the Method
         *
         * @param evt PARAM
         */
        public void actionPerformed(final ActionEvent evt) {
            // TODO
            System.out.println("Reload Page triggered");
        }
    }
}

/*
 * Copyright (c) 2008 Patrick Wright
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the
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
 */


import org.xhtmlrenderer.simple.FSScrollPane;

import com.github.danfickle.flyingsaucer.swing.XHTMLPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * This example shows the most basic use of Flying Saucer, to
 * display a single page, scrollable, within a JFrame. The input
 * is a file name.
 *
 *
 * @author Patrick Wright
 */
public class CenteredPreviewRender {
    private String fileName;

    public static void main(final String[] args) throws Exception {
        try {
            new CenteredPreviewRender().run(args);
        } catch (final IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void run(final String[] args) {
        loadAndCheckArgs(args);

        // Create a JPanel subclass to render the page
        final XHTMLPanel panel = new XHTMLPanel();
        panel.setInteractive(false);
        panel.getSharedContext().setPrint(true);
        panel.setBackground(Color.LIGHT_GRAY);
        panel.setCenteredPagedView(true);

        // Set the XHTML document to render. We use the simplest form
        // of the API call, which uses a File reference. There
        // are a variety of overloads for setDocument().
        try {
            panel.setDocument(new File(fileName));
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // Put our panel in a scrolling pane. You can use
        // a regular JScrollPane here, or our FSScrollPane.
        // FSScrollPane is already set up to move the correct
        // amount when scrolling 1 line or 1 page
        final FSScrollPane scroll = new FSScrollPane(panel);

        final JFrame frame = new JFrame("Flying Saucer: " + panel.getDocumentTitle());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.pack();
        frame.setSize(1024, 768);
        frame.setVisible(true);
    }

    private void loadAndCheckArgs(final String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Enter a file or URI.");
        }
        final String name = args[0];
        if (! new File(name).exists()) {
            throw new IllegalArgumentException("File " + name + " does not exist.");
        }
        this.fileName = name;
    }
}
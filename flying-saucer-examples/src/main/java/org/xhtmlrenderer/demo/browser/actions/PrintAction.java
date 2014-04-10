package org.xhtmlrenderer.demo.browser.actions;

import org.xhtmlrenderer.demo.browser.BrowserStartup;
import org.xhtmlrenderer.util.Uu;

import com.github.danfickle.flyingsaucer.swing.XHTMLPrintable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

public class PrintAction extends AbstractAction {
    protected BrowserStartup root;

    public PrintAction(final BrowserStartup root, final ImageIcon icon) {
        super("Print", icon);
        this.root = root;
    }

    public void actionPerformed(final ActionEvent evt) {
        Uu.p("printing");
        final PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(new XHTMLPrintable(root.panel.view));

        if (printJob.printDialog()) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Uu.p("starting printing");
                        printJob.print();
                        Uu.p("done printing");
                    } catch (final PrinterException ex) {
                        Uu.p(ex);
                    }
                }
            }).start();
        }

    }
}


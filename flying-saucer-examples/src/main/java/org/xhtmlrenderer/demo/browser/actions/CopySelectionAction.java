package org.xhtmlrenderer.demo.browser.actions;

import org.xhtmlrenderer.demo.browser.BrowserStartup;
import javax.swing.*;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

public class CopySelectionAction extends AbstractAction {

    protected BrowserStartup root;

    public CopySelectionAction(final BrowserStartup root) {
        super("Copy");
        this.root = root;
    }


    public void actionPerformed(final ActionEvent evt) {
        // ... collection seleciton here
        final Toolkit tk = Toolkit.getDefaultToolkit();
        final Clipboard clip = tk.getSystemClipboard();
        clip.setContents(new StringSelection("..."), null);
    }
}

